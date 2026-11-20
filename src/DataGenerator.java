import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * DataGenerator.java
 *
 * Tiện ích sinh dữ liệu thử nghiệm chuẩn khoa học (Sedgewick & Wayne 1.4 Analysis of Algorithms):
 * - Hỗ trợ cố định seed (Reproducible experiments).
 * - Cung cấp 5 dạng phân phối dữ liệu quan trọng trong đánh giá thuật toán:
 *   1. RANDOM: Ngẫu nhiên đều (Uniform random)
 *   2. NEARLY_SORTED: Mảng gần như đã sắp xếp (~3% vị trí bị xáo trộn)
 *   3. SORTED: Đã sắp xếp tăng dần hoàn toàn (Best case)
 *   4. REVERSED: Sắp xếp giảm dần hoàn toàn (Worst case)
 *   5. DUPLICATES: Rất nhiều phần tử trùng lặp (Tập giá trị hẹp)
 * - Khả năng sinh trực tiếp trong bộ nhớ hoặc lưu ra file CSV/TXT trong thư mục data/.
 */
public class DataGenerator {

    public static final long DEFAULT_SEED = 20261120L;

    public enum Distribution {
        RANDOM("Ngẫu nhiên (Random)"),
        NEARLY_SORTED("Gần như sắp xếp (Nearly Sorted)"),
        SORTED("Đã sắp xếp tăng dần (Sorted)"),
        REVERSED("Sắp xếp giảm dần (Reversed)"),
        DUPLICATES("Nhiều phần tử trùng lặp (Duplicates)");

        private final String displayName;

        Distribution(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private DataGenerator() {}

    /**
     * Sinh mảng với kích thước N, kiểu phân phối và seed chỉ định.
     */
    public static Integer[] generate(int n, Distribution dist, long seed) {
        if (n <= 0) return new Integer[0];

        Random rng = new Random(seed);
        Integer[] a = new Integer[n];

        switch (dist) {
            case RANDOM:
                for (int i = 0; i < n; i++) {
                    a[i] = rng.nextInt(n * 10);
                }
                break;

            case NEARLY_SORTED:
                for (int i = 0; i < n; i++) {
                    a[i] = i;
                }
                // Xáo trộn khoảng 3% cặp phần tử
                int swaps = Math.max(1, (int) (n * 0.03));
                for (int s = 0; s < swaps; s++) {
                    int i = rng.nextInt(n);
                    int j = rng.nextInt(n);
                    int temp = a[i];
                    a[i] = a[j];
                    a[j] = temp;
                }
                break;

            case SORTED:
                for (int i = 0; i < n; i++) {
                    a[i] = i;
                }
                break;

            case REVERSED:
                for (int i = 0; i < n; i++) {
                    a[i] = n - 1 - i;
                }
                break;

            case DUPLICATES:
                // Tập giá trị hẹp (tối đa 10 giá trị khác nhau)
                int distinctKeys = Math.min(10, Math.max(2, n / 100));
                for (int i = 0; i < n; i++) {
                    a[i] = rng.nextInt(distinctKeys);
                }
                break;
        }

        return a;
    }

    /**
     * Sinh mảng sử dụng seed mặc định.
     */
    public static Integer[] generate(int n, Distribution dist) {
        return generate(n, dist, DEFAULT_SEED);
    }

    /**
     * Lưu mảng số nguyên ra file CSV/TXT (mỗi số trên 1 dòng hoặc cách nhau bởi dấu phẩy).
     */
    public static void saveToFile(Integer[] a, String filePath) throws IOException {
        File file = new File(filePath);
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            for (int i = 0; i < a.length; i++) {
                pw.println(a[i]);
            }
        }
    }

    /**
     * Đọc mảng số nguyên từ file.
     */
    public static Integer[] loadFromFile(String filePath) throws IOException {
        List<Integer> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    list.add(Integer.parseInt(line));
                }
            }
        }
        return list.toArray(new Integer[0]);
    }

    /**
     * Xuất một số mẫu dữ liệu mẫu vào thư mục data/samples/ để kiểm tra trực quan.
     */
    public static void exportSampleDatasets(String baseDir) {
        int[] sampleSizes = { 1000, 5000 };
        Distribution[] dists = Distribution.values();

        System.out.println("Đang xuất các file dữ liệu mẫu vào: " + baseDir);
        for (int n : sampleSizes) {
            for (Distribution d : dists) {
                Integer[] data = generate(n, d, DEFAULT_SEED);
                String fileName = String.format("%s/sample_N%d_%s.txt",
                        baseDir, n, d.name().toLowerCase());
                try {
                    saveToFile(data, fileName);
                    System.out.printf("  + Đã tạo: %s (%d phần tử)\n", fileName, n);
                } catch (IOException e) {
                    System.err.println("Lỗi lưu file: " + fileName + " - " + e.getMessage());
                }
            }
        }
        System.out.println("Hoàn tất xuất dữ liệu mẫu.\n");
    }

    public static void main(String[] args) {
        String outputDir = "data/samples";
        if (args.length > 0) {
            outputDir = args[0];
        }
        exportSampleDatasets(outputDir);
    }
}
