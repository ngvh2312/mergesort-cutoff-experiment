import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Random;

/**
 * ExperimentRunner.java
 *
 * Thực nghiệm và phân tích giá trị CUTOFF tối ưu cho Mergesort + Insertion sort
 * bám sát phương pháp thực nghiệm trong Sedgewick & Wayne (Section 1.4 Analysis of Algorithms).
 *
 * Phương pháp:
 * 1. Warm-up JVM: Cho JIT Compiler biên dịch và tối ưu hóa bytecode trước khi đo lường chính thức.
 * 2. Độc lập & Khách quan: Mỗi giá trị Cutoff trong cùng một lượt chạy (trial)
 *    đều nhận một bản sao (clone) của mảng dữ liệu ngẫu nhiên giống hệt nhau.
 * 3. Thống kê nhiều lượt (Trials): Lấy trung bình cộng (Average) qua T lượt chạy
 *    để triệt tiêu nhiễu từ hệ điều hành / GC.
 * 4. Đo đạc đa chiều:
 *    - Thời gian thực thi (Wall-clock Time - ms)
 *    - Số phép so sánh khóa (Key Comparisons)
 *    - Số lần truy cập mảng (Array Accesses)
 * 5. Xuất kết quả: Bảng biểu trực quan trên Console và file CSV (phục vụ vẽ đồ thị/làm báo cáo).
 *
 * Cách chạy:
 *   javac *.java
 *   java ExperimentRunner
 * Hoặc tùy chỉnh tham số:
 *   java ExperimentRunner <N> <MaxCutoff> <Trials>
 */
public class ExperimentRunner {

    // Kích thước mảng thử nghiệm mặc định
    private static final int DEFAULT_N = 100_000;
    // Ngưỡng cutoff tối đa khảo sát
    private static final int DEFAULT_MAX_CUTOFF = 32;
    // Số lượt chạy thử nghiệm để lấy trung bình
    private static final int DEFAULT_TRIALS = 15;
    // Số lượt warm-up JVM
    private static final int WARMUP_RUNS = 20;

    public static void main(String[] args) {
        int n = DEFAULT_N;
        int maxCutoff = DEFAULT_MAX_CUTOFF;
        int trials = DEFAULT_TRIALS;

        if (args.length >= 1) n = Integer.parseInt(args[0]);
        if (args.length >= 2) maxCutoff = Integer.parseInt(args[1]);
        if (args.length >= 3) trials = Integer.parseInt(args[2]);

        System.out.println("================================================================================");
        System.out.println("  THỰC NGHIỆM TÌM CUTOFF TỐI ƯU CHO MERGESORT (Sedgewick & Wayne 1.4)");
        System.out.println("================================================================================");
        System.out.printf("Cấu hình: N = %,d phần tử | Cutoff khảo sát: 0 -> %d | Trials: %d lượt\n\n",
                n, maxCutoff, trials);

        // Bước 1: Khởi động JVM (Warm-up)
        warmUpJVM(n);

        // Bước 2: Chạy thực nghiệm đo thời gian (chế độ không overhead đo lường để lấy thời gian thực tế)
        System.out.println(">>> Đang tiến hành đo thời gian thực thi (Wall-clock Time)...");
        double[] avgTimes = measureWallClockTime(n, maxCutoff, trials);

        // Bước 3: Chạy thực nghiệm đo chỉ số chi phí (Compares & Array Accesses)
        System.out.println(">>> Đang tiến hành đo mô hình chi phí (Compares & Array Accesses)...");
        long[] compares = new long[maxCutoff + 1];
        long[] arrayAccesses = new long[maxCutoff + 1];
        measureCostModel(n, maxCutoff, compares, arrayAccesses);

        // Bước 4: In bảng kết quả tổng hợp
        printReport(maxCutoff, avgTimes, compares, arrayAccesses);

        // Bước 5: Xuất dữ liệu ra file CSV
        String csvPath = "../data/cutoff_benchmark.csv";
        exportToCsv(csvPath, n, maxCutoff, trials, avgTimes, compares, arrayAccesses);
    }

    /**
     * Khởi động JVM để JIT Compiler tối ưu hóa các hàm sort, merge, insertionSort.
     */
    private static void warmUpJVM(int n) {
        System.out.printf("Đang warm-up JVM (%d lượt, kích thước mảng nhỏ hơn để tăng tốc)...\n", WARMUP_RUNS);
        Random rng = new Random(12345);
        int warmupSize = Math.min(n, 20_000);
        for (int w = 0; w < WARMUP_RUNS; w++) {
            Integer[] a = generateRandomArray(warmupSize, rng);
            MergeSortCutoff.sort(a, w % 16);
        }
        System.out.println("Hoàn tất warm-up JVM.\n");
    }

    /**
     * Đo thời gian thực tế (Wall-clock time) bằng System.nanoTime(),
     * gọi sort thuần túy không kèm chi phí cập nhật bộ đếm metrics.
     */
    private static double[] measureWallClockTime(int n, int maxCutoff, int trials) {
        double[] avgTimes = new double[maxCutoff + 1];
        Random rng = new Random(42);

        for (int t = 1; t <= trials; t++) {
            // Tạo mảng gốc ngẫu nhiên cho trial này
            Integer[] baseArray = generateRandomArray(n, rng);

            for (int c = 0; c <= maxCutoff; c++) {
                Integer[] copy = Arrays.copyOf(baseArray, baseArray.length);

                long start = System.nanoTime();
                MergeSortCutoff.sort(copy, c);
                long elapsed = System.nanoTime() - start;

                avgTimes[c] += elapsed / 1_000_000.0; // đổi sang ms
            }
        }

        for (int c = 0; c <= maxCutoff; c++) {
            avgTimes[c] /= trials;
        }

        return avgTimes;
    }

    /**
     * Đo đếm số phép so sánh và truy cập mảng bám theo cost model Sedgewick 1.4.
     */
    private static void measureCostModel(int n, int maxCutoff, long[] outCompares, long[] outAccesses) {
        Random rng = new Random(2026);
        Integer[] baseArray = generateRandomArray(n, rng);

        for (int c = 0; c <= maxCutoff; c++) {
            Integer[] copy = Arrays.copyOf(baseArray, baseArray.length);
            SortMetrics metrics = new SortMetrics();
            MergeSortCutoff.sort(copy, c, metrics);

            outCompares[c] = metrics.getCompares();
            outAccesses[c] = metrics.getArrayAccesses();
        }
    }

    /**
     * In bảng thống kê kết quả và chỉ ra giá trị CUTOFF tối ưu.
     */
    private static void printReport(int maxCutoff, double[] times, long[] compares, long[] accesses) {
        System.out.println("\n================================================================================");
        System.out.printf("%-8s | %-16s | %-18s | %-20s | %-10s\n",
                "Cutoff", "Thời gian (ms)", "Key Compares", "Array Accesses", "Ghi chú");
        System.out.println("--------------------------------------------------------------------------------");

        int bestCutoffTime = 0;
        double minTime = Double.MAX_VALUE;

        for (int c = 0; c <= maxCutoff; c++) {
            if (times[c] < minTime) {
                minTime = times[c];
                bestCutoffTime = c;
            }
        }

        for (int c = 0; c <= maxCutoff; c++) {
            String note = "";
            if (c == 0 || c == 1) {
                note = "Pure Mergesort";
            }
            if (c == bestCutoffTime) {
                note = (note.isEmpty() ? "" : note + " | ") + "★ TỐI ƯU THỜI GIAN";
            }

            System.out.printf("%-8d | %16.3f | %18s | %20s | %s\n",
                    c, times[c],
                    String.format("%,d", compares[c]),
                    String.format("%,d", accesses[c]),
                    note);
        }
        System.out.println("================================================================================");
        System.out.printf(">>> KẾT LUẬN THỰC NGHIỆM: Cutoff tối ưu về thời gian là C* = %d (%.3f ms)\n",
                bestCutoffTime, minTime);

        double pureTime = (times[0] + times[1]) / 2.0;
        double speedup = ((pureTime - minTime) / pureTime) * 100.0;
        if (speedup > 0) {
            System.out.printf(">>> Tốc độ cải thiện so với Mergesort thuần túy: +%.2f%%\n", speedup);
        }
        System.out.println("================================================================================\n");
    }

    /**
     * Xuất kết quả sang CSV để phục vụ phân tích / vẽ biểu đồ trong báo cáo.
     */
    private static void exportToCsv(String path, int n, int maxCutoff, int trials,
                                    double[] times, long[] compares, long[] accesses) {
        try {
            // Đảm bảo thư mục cha tồn tại
            java.io.File file = new java.io.File(path);
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }

            try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
                pw.println("# Mergesort Cutoff Experiment (Sedgewick & Wayne 1.4 Analysis of Algorithms)");
                pw.printf("# N = %d, Trials = %d\n", n, trials);
                pw.println("Cutoff,Time_ms,Key_Compares,Array_Accesses");
                for (int c = 0; c <= maxCutoff; c++) {
                    pw.printf("%d,%.4f,%d,%d\n", c, times[c], compares[c], accesses[c]);
                }
            }
            System.out.println("Đã lưu dữ liệu thực nghiệm vào: " + file.getCanonicalPath());
        } catch (IOException e) {
            System.err.println("Không thể ghi file CSV: " + e.getMessage());
        }
    }

    /**
     * Sinh mảng Integer ngẫu nhiên.
     */
    private static Integer[] generateRandomArray(int n, Random rng) {
        Integer[] a = new Integer[n];
        for (int i = 0; i < n; i++) {
            a[i] = rng.nextInt();
        }
        return a;
    }
}
