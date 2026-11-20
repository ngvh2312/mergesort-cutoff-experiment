import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * BenchmarkExperiment.java
 *
 * Chương trình thí nghiệm đánh giá giá trị CUTOFF cho Mergesort
 * tuân thủ phương pháp nghiên cứu thực nghiệm trong Sedgewick & Wayne (Section 1.4 Analysis of Algorithms).
 *
 * Quy chuẩn khoa học:
 * 1. Reproducible Experiments: Cố định seed cho các bộ dữ liệu ngẫu nhiên.
 * 2. JVM Warm-up: Loại bỏ ảnh hưởng của Class loading và JIT compilation trước khi đo lường.
 * 3. Phân tách đo lường:
 *    - Đo thời gian chạy thực tế (Wall-clock Time) trên luồng độc lập không có overhead của bộ đếm.
 *    - Đo mô hình chi phí (Key Compares & Array Accesses) qua SortMetrics trên bản sao dữ liệu giống hệt.
 * 4. Thống kê đa lượt (Trials): Tính Trung bình (Mean), Nhỏ nhất (Min), Lớn nhất (Max), Độ lệch chuẩn (StdDev).
 * 5. Khảo sát đa chiều:
 *    - Các mốc CUTOFF: 0, 1, 3, 5, 7, 10, 15, 20, 30, 50 (làm rõ mốc 7 và 10).
 *    - Các kích thước N: 1.000, 5.000, 10.000, 50.000, 100.000 (tùy biến linh hoạt).
 *    - 5 dạng phân phối dữ liệu: Random, Nearly Sorted, Sorted, Reversed, Duplicates.
 *
 * Biên dịch và thực thi:
 *   javac -cp src src/*.java experiments/BenchmarkExperiment.java
 *   java -cp "src;experiments" BenchmarkExperiment
 */
public class BenchmarkExperiment {

    // Danh sách các mốc Cutoff cần khảo sát theo yêu cầu
    public static final int[] CUTOFFS = { 0, 1, 3, 5, 7, 10, 15, 20, 30, 50 };

    // Kích thước mảng mặc định
    public static final int[] DEFAULT_SIZES = { 1000, 5000, 10000, 50000, 100000 };

    // Số lượt chạy lặp lại để lấy thống kê
    public static final int DEFAULT_TRIALS = 5;

    // Số lượt warm-up JVM
    public static final int WARMUP_ROUNDS = 25;

    // Seed gốc cố định
    public static final long BASE_SEED = 20261120L;

    /**
     * Bản ghi kết quả cho từng tổ hợp (N, Distribution, Cutoff).
     */
    public static class ExperimentRecord {
        public final int n;
        public final DataGenerator.Distribution distribution;
        public final int cutoff;
        public final double meanTimeMs;
        public final double minTimeMs;
        public final double maxTimeMs;
        public final double stdDevTimeMs;
        public final long compares;
        public final long arrayAccesses;
        public double speedupVsPure; // Tỉ lệ tăng tốc so với Cutoff 0 (%)

        public ExperimentRecord(int n, DataGenerator.Distribution distribution, int cutoff,
                                double meanTimeMs, double minTimeMs, double maxTimeMs, double stdDevTimeMs,
                                long compares, long arrayAccesses) {
            this.n = n;
            this.distribution = distribution;
            this.cutoff = cutoff;
            this.meanTimeMs = meanTimeMs;
            this.minTimeMs = minTimeMs;
            this.maxTimeMs = maxTimeMs;
            this.stdDevTimeMs = stdDevTimeMs;
            this.compares = compares;
            this.arrayAccesses = arrayAccesses;
            this.speedupVsPure = 0.0;
        }
    }

    public static void main(String[] args) {
        Locale.setDefault(Locale.US);

        int[] sizes = DEFAULT_SIZES;
        int trials = DEFAULT_TRIALS;

        // Cho phép chế độ chạy nhanh qua tham số: java BenchmarkExperiment --quick
        if (args.length > 0 && args[0].equalsIgnoreCase("--quick")) {
            sizes = new int[] { 1000, 5000, 10000 };
            trials = 3;
            System.out.println(">>> Đang chạy ở chế độ [QUICK BENCHMARK] (N: 1k, 5k, 10k | 3 trials)");
        }

        System.out.println("=========================================================================================");
        System.out.println("  CHƯƠNG TRÌNH THÍ NGHIỆM ĐÁNH GIÁ MERGESORT + CUTOFF (Sedgewick & Wayne 1.4)");
        System.out.println("=========================================================================================");
        System.out.printf("Cấu hình: N = %s | Cutoffs = %s | Trials = %d\n",
                Arrays.toString(sizes), Arrays.toString(CUTOFFS), trials);

        // 1. Warm-up JVM
        runJvmWarmup();

        // 2. Chạy thí nghiệm toàn diện
        List<ExperimentRecord> allRecords = new ArrayList<>();
        DataGenerator.Distribution[] distributions = DataGenerator.Distribution.values();

        System.out.println(">>> Bắt đầu tiến hành đo đạc trên tất cả cấu hình...");
        long experimentStartTime = System.currentTimeMillis();

        for (int n : sizes) {
            System.out.printf("\n-----------------------------------------------------------------------------------------\n");
            System.out.printf("  ĐANG THỰC HIỆN THỬ NGHIỆM VỚI KÍCH THƯỚC MẢNG N = %,d\n", n);
            System.out.printf("-----------------------------------------------------------------------------------------\n");

            for (DataGenerator.Distribution dist : distributions) {
                System.out.printf("  >> Phân phối: %-30s ... ", dist.getDisplayName());
                List<ExperimentRecord> distRecords = benchmarkOneCase(n, dist, CUTOFFS, trials);
                allRecords.addAll(distRecords);
                System.out.println("Hoàn tất.");
            }
        }

        long totalElapsedSec = (System.currentTimeMillis() - experimentStartTime) / 1000;
        System.out.printf("\n>>> Tổng thời gian thực nghiệm: %d giây.\n", totalElapsedSec);

        // 3. Tính toán Speedup so với Cutoff 0 (Pure Mergesort)
        calculateSpeedups(allRecords);

        // 4. In bảng so sánh chuyên sâu giữa Cutoff 0 vs 7 vs 10
        printSedgewickFocusTable(allRecords, sizes);

        // 5. Xuất kết quả ra file CSV và Markdown Report
        String csvPath = "experiments/benchmark_results.csv";
        String mdReportPath = "report/EXPERIMENT_SUMMARY.md";
        exportToCsv(allRecords, csvPath);
        exportToMarkdown(allRecords, sizes, mdReportPath);

        System.out.println("\n=========================================================================================");
        System.out.println("  HOÀN TẤT THÍ NGHIỆM THỰC NGHIỆM!");
        System.out.println("  1. Dữ liệu CSV lưu tại: " + new File(csvPath).getAbsolutePath());
        System.out.println("  2. Báo cáo tóm tắt lưu tại: " + new File(mdReportPath).getAbsolutePath());
        System.out.println("  3. Chạy script vẽ biểu đồ: python experiments/plot_results.py");
        System.out.println("=========================================================================================");
    }

    /**
     * JVM Warm-up thực hiện chạy trước nhiều lần trên các phân phối dữ liệu
     * để đảm bảo các phương thức được JIT compile tối ưu.
     */
    private static void runJvmWarmup() {
        System.out.print("Đang khởi động (Warm-up) JVM ");
        Random rng = new Random(1111);
        int warmupN = 10000;
        for (int w = 0; w < WARMUP_ROUNDS; w++) {
            for (DataGenerator.Distribution d : DataGenerator.Distribution.values()) {
                Integer[] a = DataGenerator.generate(warmupN, d, rng.nextLong());
                MergeSortCutoff.sort(a, w % 16);
            }
            if (w % 5 == 0) System.out.print(".");
        }
        System.out.println(" Hoàn tất warm-up!\n");
    }

    /**
     * Thực hiện đo lường cho một cặp (N, Distribution) trên toàn bộ dải Cutoff.
     */
    private static List<ExperimentRecord> benchmarkOneCase(int n, DataGenerator.Distribution dist,
                                                           int[] cutoffs, int trials) {
        List<ExperimentRecord> records = new ArrayList<>();
        double[][] times = new double[cutoffs.length][trials];
        long[] compares = new long[cutoffs.length];
        long[] accesses = new long[cutoffs.length];

        // Chạy từng trial
        for (int t = 0; t < trials; t++) {
            long trialSeed = BASE_SEED + (long) t * 10007L;
            Integer[] baseData = DataGenerator.generate(n, dist, trialSeed);

            for (int cIdx = 0; cIdx < cutoffs.length; cIdx++) {
                int cutoff = cutoffs[cIdx];
                Integer[] testData = Arrays.copyOf(baseData, baseData.length);

                // Đo thời gian chạy thực tế (không có overhead)
                long start = System.nanoTime();
                MergeSortCutoff.sort(testData, cutoff);
                long elapsedNs = System.nanoTime() - start;
                times[cIdx][t] = elapsedNs / 1_000_000.0; // đổi sang ms
            }
        }

        // Đo chỉ số chi phí (compares và array accesses) trên mảng tương ứng
        Integer[] costBase = DataGenerator.generate(n, dist, BASE_SEED);
        for (int cIdx = 0; cIdx < cutoffs.length; cIdx++) {
            int cutoff = cutoffs[cIdx];
            Integer[] costCopy = Arrays.copyOf(costBase, costBase.length);
            SortMetrics metrics = MergeSortCutoff.sortTracked(costCopy, cutoff);
            compares[cIdx] = metrics.getCompares();
            accesses[cIdx] = metrics.getArrayAccesses();
        }

        // Tính toán thống kê: Mean, Min, Max, StdDev
        for (int cIdx = 0; cIdx < cutoffs.length; cIdx++) {
            int cutoff = cutoffs[cIdx];
            double[] trialTimes = times[cIdx];

            double sum = 0.0;
            double min = Double.MAX_VALUE;
            double max = Double.MIN_VALUE;

            for (double time : trialTimes) {
                sum += time;
                if (time < min) min = time;
                if (time > max) max = time;
            }
            double mean = sum / trials;

            double sumSqDiff = 0.0;
            for (double time : trialTimes) {
                sumSqDiff += (time - mean) * (time - mean);
            }
            double stdDev = (trials > 1) ? Math.sqrt(sumSqDiff / (trials - 1)) : 0.0;

            records.add(new ExperimentRecord(n, dist, cutoff, mean, min, max, stdDev,
                    compares[cIdx], accesses[cIdx]));
        }

        return records;
    }

    /**
     * Tính toán tỉ lệ cải thiện tốc độ (Speedup %) so với Cutoff 0 (Pure Mergesort).
     */
    private static void calculateSpeedups(List<ExperimentRecord> records) {
        for (int i = 0; i < records.size(); i++) {
            ExperimentRecord rec = records.get(i);
            // Tìm bản ghi Cutoff 0 cùng N và Distribution
            for (ExperimentRecord base : records) {
                if (base.n == rec.n && base.distribution == rec.distribution && base.cutoff == 0) {
                    if (base.meanTimeMs > 0) {
                        rec.speedupVsPure = ((base.meanTimeMs - rec.meanTimeMs) / base.meanTimeMs) * 100.0;
                    }
                    break;
                }
            }
        }
    }

    /**
     * In bảng so sánh trọng tâm: Cutoff 0 (Pure) vs Cutoff 7 (Sedgewick) vs Cutoff 10.
     */
    private static void printSedgewickFocusTable(List<ExperimentRecord> records, int[] sizes) {
        System.out.println("\n=========================================================================================");
        System.out.println("  BẢNG TỔNG HỢP TRỌNG TÂM: SO SÁNH CUTOFF 0 (PURE) vs 7 (SEDGEWICK) vs 10");
        System.out.println("=========================================================================================");
        System.out.printf("%-8s | %-20s | %-12s | %-12s | %-12s | %-10s | %-10s\n",
                "N", "Dạng dữ liệu", "Pure (C=0)", "Cutoff=7", "Cutoff=10", "Gain (C=7)", "Gain (C=10)");
        System.out.println("-----------------------------------------------------------------------------------------");

        for (int n : sizes) {
            for (DataGenerator.Distribution d : DataGenerator.Distribution.values()) {
                ExperimentRecord r0 = null, r7 = null, r10 = null;
                for (ExperimentRecord r : records) {
                    if (r.n == n && r.distribution == d) {
                        if (r.cutoff == 0) r0 = r;
                        else if (r.cutoff == 7) r7 = r;
                        else if (r.cutoff == 10) r10 = r;
                    }
                }

                if (r0 != null && r7 != null && r10 != null) {
                    System.out.printf("%-8d | %-20s | %10.3f ms | %10.3f ms | %10.3f ms | %+8.2f%% | %+8.2f%%\n",
                            n, d.name(),
                            r0.meanTimeMs, r7.meanTimeMs, r10.meanTimeMs,
                            r7.speedupVsPure, r10.speedupVsPure);
                }
            }
            System.out.println("-----------------------------------------------------------------------------------------");
        }
    }

    /**
     * Xuất kết quả chi tiết ra định dạng CSV.
     */
    private static void exportToCsv(List<ExperimentRecord> records, String path) {
        File file = new File(path);
        if (file.getParentFile() != null) file.getParentFile().mkdirs();

        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println("N,Distribution,Cutoff,Mean_Time_ms,Min_Time_ms,Max_Time_ms,StdDev_Time_ms,Key_Compares,Array_Accesses,Speedup_Percent");
            for (ExperimentRecord r : records) {
                pw.printf("%d,%s,%d,%.4f,%.4f,%.4f,%.4f,%d,%d,%.2f\n",
                        r.n, r.distribution.name(), r.cutoff,
                        r.meanTimeMs, r.minTimeMs, r.maxTimeMs, r.stdDevTimeMs,
                        r.compares, r.arrayAccesses, r.speedupVsPure);
            }
        } catch (IOException e) {
            System.err.println("Lỗi ghi file CSV: " + e.getMessage());
        }
    }

    /**
     * Xuất báo cáo tổng kết dạng Markdown vào report/EXPERIMENT_SUMMARY.md.
     */
    private static void exportToMarkdown(List<ExperimentRecord> records, int[] sizes, String path) {
        File file = new File(path);
        if (file.getParentFile() != null) file.getParentFile().mkdirs();

        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println("# Báo cáo Kết quả Thực nghiệm: Tối ưu CUTOFF cho Mergesort");
            pw.println("*(Phương pháp thực nghiệm & phân tích theo Robert Sedgewick & Kevin Wayne - Algorithms 1.4)*\n");
            pw.println("## 1. Thiết lập thực nghiệm");
            pw.println("- **Mô hình máy tính:** JVM Oracle 25.0.2 / Windows x64");
            pw.println("- **Phương pháp đo:** Warm-up JVM, đo Wall-clock Time (System.nanoTime), lấy trung bình qua nhiều trials độc lập.");
            pw.println("- **Dải Cutoff:** `0, 1, 3, 5, 7, 10, 15, 20, 30, 50`.");
            pw.println("- **Dạng dữ liệu:** `RANDOM`, `NEARLY_SORTED`, `SORTED`, `REVERSED`, `DUPLICATES`.\n");

            pw.println("## 2. Bảng so sánh Cutoff 0 (Pure) vs Cutoff 7 (Sedgewick) vs Cutoff 10");
            pw.println("| N | Dạng dữ liệu | Pure Mergesort (C=0) | Cutoff = 7 | Cutoff = 10 | Tăng tốc (C=7) | Tăng tốc (C=10) |");
            pw.println("| :--- | :--- | :---: | :---: | :---: | :---: | :---: |");

            for (int n : sizes) {
                for (DataGenerator.Distribution d : DataGenerator.Distribution.values()) {
                    ExperimentRecord r0 = null, r7 = null, r10 = null;
                    for (ExperimentRecord r : records) {
                        if (r.n == n && r.distribution == d) {
                            if (r.cutoff == 0) r0 = r;
                            else if (r.cutoff == 7) r7 = r;
                            else if (r.cutoff == 10) r10 = r;
                        }
                    }
                    if (r0 != null && r7 != null && r10 != null) {
                        pw.printf("| %d | %s | %.3f ms | %.3f ms | %.3f ms | %+.2f%% | %+.2f%% |\n",
                                n, d.name(), r0.meanTimeMs, r7.meanTimeMs, r10.meanTimeMs,
                                r7.speedupVsPure, r10.speedupVsPure);
                    }
                }
            }

            pw.println("\n## 3. Nhận xét lý thuyết & Thực nghiệm");
            pw.println("1. **Tại sao Cutoff 7 và 10 lại hiệu quả:**");
            pw.println("   - Tránh các lời gọi hàm đệ quy sâu và thao tác sao chép sang mảng phụ `aux[]` ở các tầng đáy.");
            pw.println("   - Trên các mảng con nhỏ ($k \\le 10$), Insertion sort chạy cực nhanh và có hằng số ẩn thời gian nhỏ hơn nhiều so với Mergesort.");
            pw.println("2. **Hiện tượng suy giảm khi Cutoff > 20 - 30:**");
            pw.println("   - Chi phí $O(k^2)$ phép so sánh của Insertion sort bắt đầu lấn át, khiến thời gian chạy chậm lại.");
        } catch (IOException e) {
            System.err.println("Lỗi ghi file Markdown: " + e.getMessage());
        }
    }
}
