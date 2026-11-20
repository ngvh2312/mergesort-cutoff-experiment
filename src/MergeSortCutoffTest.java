import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * MergeSortCutoffTest.java
 *
 * Bộ kiểm thử toàn diện cho thuật toán MergeSortCutoff:
 * 1. Mảng rỗng (length = 0)
 * 2. Mảng 1 phần tử (length = 1)
 * 3. Mảng 2 phần tử (đã xếp, đảo ngược, bằng nhau)
 * 4. Mảng đã sắp xếp tăng dần
 * 5. Mảng sắp xếp giảm dần (ngược)
 * 6. Mảng nhiều phần tử trùng lặp (toàn bộ bằng nhau, hoặc tập giá trị nhỏ)
 * 7. Mảng ngẫu nhiên nhiều kích thước (chẵn, lẻ, lũy thừa của 2, kích thước lớn)
 * 8. Kiểm thử các giá trị cutoff biên: cutoff <= 0, cutoff = 1 (pure Mergesort),
 *    cutoff vừa (7, 8, 16), và cutoff >= N (chuyển hoàn toàn sang Insertion sort)
 * 9. Kiểm tra bảo toàn tập phần tử (không mất, không sinh thêm phần tử)
 * 10. Kiểm tra tính ổn định (stability) của thuật toán
 * 11. Kiểm tra mode đo lường SortMetrics (compares, array accesses, time)
 *
 * Có thể biên dịch và chạy trực tiếp bằng:
 *   javac MergeSortCutoffTest.java
 *   java MergeSortCutoffTest
 */
public class MergeSortCutoffTest {

    private static int totalTests = 0;
    private static int passedTests = 0;

    public static void main(String[] args) {
        System.out.println("======================================================");
        System.out.println("  BẮT ĐẦU UNIT TESTS CHO MERGESORT CUTOFF");
        System.out.println("======================================================");

        testEmptyArray();
        testSingleElement();
        testTwoElements();
        testAlreadySorted();
        testReverseSorted();
        testAllDuplicates();
        testManyDuplicates();
        testVariousCutoffValues();
        testRandomArraysMultipleSizes();
        testMultisetPreservation();
        testStability();
        testMetricsTracking();

        System.out.println("======================================================");
        System.out.printf("  KẾT QUẢ: %d / %d tests passed (%.1f%%)\n",
                passedTests, totalTests, (100.0 * passedTests / totalTests));
        System.out.println("======================================================");

        if (passedTests != totalTests) {
            System.err.println("CẢNH BÁO: Có test bị thất bại!");
            System.exit(1);
        } else {
            System.out.println("TẤT CẢ UNIT TESTS ĐỀU ĐẠT THÀNH CÔNG (PASSED)!");
        }
    }

    private static void assertTrue(String testName, boolean condition) {
        totalTests++;
        if (condition) {
            passedTests++;
            System.out.println(" [PASS] " + testName);
        } else {
            System.err.println(" [FAIL] " + testName);
        }
    }

    // 1. Mảng rỗng
    private static void testEmptyArray() {
        Integer[] a = new Integer[0];
        MergeSortCutoff.sort(a, 8);
        assertTrue("Mảng rỗng (cutoff=8)", a.length == 0 && MergeSortCutoff.isSorted(a));

        Integer[] b = new Integer[0];
        MergeSortCutoff.sort(b, 0);
        assertTrue("Mảng rỗng (cutoff=0 - pure mergesort)", b.length == 0 && MergeSortCutoff.isSorted(b));
    }

    // 2. Mảng 1 phần tử
    private static void testSingleElement() {
        Integer[] a = { 42 };
        MergeSortCutoff.sort(a, 8);
        assertTrue("Mảng 1 phần tử (cutoff=8)", a.length == 1 && a[0] == 42 && MergeSortCutoff.isSorted(a));

        Integer[] b = { -99 };
        MergeSortCutoff.sort(b, 1);
        assertTrue("Mảng 1 phần tử (cutoff=1)", b.length == 1 && b[0] == -99 && MergeSortCutoff.isSorted(b));
    }

    // 3. Mảng 2 phần tử
    private static void testTwoElements() {
        Integer[] a = { 2, 1 };
        MergeSortCutoff.sort(a, 8);
        assertTrue("Mảng 2 phần tử nghịch thế {2, 1} (cutoff=8)",
                MergeSortCutoff.isSorted(a) && a[0] == 1 && a[1] == 2);

        Integer[] b = { 1, 2 };
        MergeSortCutoff.sort(b, 0);
        assertTrue("Mảng 2 phần tử thuận thế {1, 2} (cutoff=0)",
                MergeSortCutoff.isSorted(b) && b[0] == 1 && b[1] == 2);

        Integer[] c = { 5, 5 };
        MergeSortCutoff.sort(c, 2);
        assertTrue("Mảng 2 phần tử trùng nhau {5, 5} (cutoff=2)",
                MergeSortCutoff.isSorted(c) && c[0] == 5 && c[1] == 5);
    }

    // 4. Mảng đã sắp xếp tăng dần
    private static void testAlreadySorted() {
        int n = 100;
        Integer[] a = new Integer[n];
        for (int i = 0; i < n; i++) {
            a[i] = i;
        }
        MergeSortCutoff.sort(a, 8);
        assertTrue("Mảng đã sắp xếp tăng dần 100 phần tử (cutoff=8)",
                MergeSortCutoff.isSorted(a) && a[0] == 0 && a[n - 1] == n - 1);
    }

    // 5. Mảng sắp xếp giảm dần (nghịch đảo hoàn toàn)
    private static void testReverseSorted() {
        int n = 100;
        Integer[] a = new Integer[n];
        for (int i = 0; i < n; i++) {
            a[i] = n - i;
        }
        MergeSortCutoff.sort(a, 8);
        assertTrue("Mảng giảm dần 100 phần tử (cutoff=8)",
                MergeSortCutoff.isSorted(a) && a[0] == 1 && a[n - 1] == n);

        Integer[] b = new Integer[n];
        for (int i = 0; i < n; i++) {
            b[i] = n - i;
        }
        MergeSortCutoff.sort(b, 0); // Pure mergesort
        assertTrue("Mảng giảm dần 100 phần tử (cutoff=0 - Pure mergesort)",
                MergeSortCutoff.isSorted(b) && b[0] == 1 && b[n - 1] == n);
    }

    // 6a. Mảng toàn bộ phần tử trùng lặp
    private static void testAllDuplicates() {
        int n = 64;
        Integer[] a = new Integer[n];
        Arrays.fill(a, 7);
        MergeSortCutoff.sort(a, 8);
        boolean allSeven = true;
        for (int v : a) {
            if (v != 7) { allSeven = false; break; }
        }
        assertTrue("Mảng 64 phần tử bằng nhau hoàn toàn {7, 7, ...}",
                MergeSortCutoff.isSorted(a) && allSeven);
    }

    // 6b. Mảng nhiều phần tử trùng lặp (giá trị trong tập nhỏ [0..4])
    private static void testManyDuplicates() {
        int n = 1000;
        Random rng = new Random(42);
        Integer[] a = new Integer[n];
        for (int i = 0; i < n; i++) {
            a[i] = rng.nextInt(5); // chỉ nhận 0, 1, 2, 3, 4
        }
        MergeSortCutoff.sort(a, 12);
        assertTrue("Mảng 1000 phần tử với tập giá trị hẹp [0..4] (cutoff=12)",
                MergeSortCutoff.isSorted(a));
    }

    // 7. Mảng ngẫu nhiên nhiều kích thước khác nhau
    private static void testRandomArraysMultipleSizes() {
        int[] sizes = { 3, 7, 15, 16, 17, 31, 32, 63, 64, 100, 256, 500, 1024 };
        Random rng = new Random(12345);

        boolean allPassed = true;
        for (int size : sizes) {
            Integer[] a = new Integer[size];
            for (int i = 0; i < size; i++) {
                a[i] = rng.nextInt(10000);
            }
            MergeSortCutoff.sort(a, 8);
            if (!MergeSortCutoff.isSorted(a)) {
                allPassed = false;
                break;
            }
        }
        assertTrue("Mảng ngẫu nhiên nhiều kích thước biên (lẻ/chẵn/lũy thừa 2)", allPassed);
    }

    // 8. Kiểm thử các giá trị cutoff khác nhau
    private static void testVariousCutoffValues() {
        int n = 200;
        Random rng = new Random(999);
        Integer[] template = new Integer[n];
        for (int i = 0; i < n; i++) {
            template[i] = rng.nextInt(5000);
        }

        int[] cutoffs = { -2, 0, 1, 2, 4, 7, 8, 15, 16, 31, 32, 64, 100, 200, 500 };
        boolean allPassed = true;

        for (int c : cutoffs) {
            Integer[] copy = Arrays.copyOf(template, template.length);
            MergeSortCutoff.sort(copy, c);
            if (!MergeSortCutoff.isSorted(copy)) {
                allPassed = false;
                System.err.println("Thất bại tại cutoff = " + c);
                break;
            }
        }
        assertTrue("Đúng đắn trên các giá trị cutoff đa dạng (-2, 0, 1, 7, 16, 64, >=N)", allPassed);
    }

    // 9. Kiểm tra bảo toàn tập phần tử (multiset preservation)
    private static void testMultisetPreservation() {
        int n = 500;
        Random rng = new Random(777);
        Integer[] a = new Integer[n];
        Map<Integer, Integer> freqBefore = new HashMap<>();

        for (int i = 0; i < n; i++) {
            a[i] = rng.nextInt(100);
            freqBefore.put(a[i], freqBefore.getOrDefault(a[i], 0) + 1);
        }

        MergeSortCutoff.sort(a, 10);

        Map<Integer, Integer> freqAfter = new HashMap<>();
        for (int v : a) {
            freqAfter.put(v, freqAfter.getOrDefault(v, 0) + 1);
        }

        boolean preserved = freqBefore.equals(freqAfter) && MergeSortCutoff.isSorted(a);
        assertTrue("Bảo toàn số lượng và tập giá trị phần tử (không mất/thừa)", preserved);
    }

    // 10. Kiểm tra tính ổn định (Stability)
    private static class Element implements Comparable<Element> {
        final int key;
        final int originalIndex;

        Element(int key, int originalIndex) {
            this.key = key;
            this.originalIndex = originalIndex;
        }

        @Override
        public int compareTo(Element o) {
            return Integer.compare(this.key, o.key);
        }
    }

    private static void testStability() {
        int n = 300;
        Random rng = new Random(2026);
        Element[] a = new Element[n];
        for (int i = 0; i < n; i++) {
            // Giá trị key nhỏ từ 0..4 để tạo rất nhiều phần tử trùng khóa
            a[i] = new Element(rng.nextInt(5), i);
        }

        MergeSortCutoff.sort(a, 8);

        boolean stable = true;
        for (int i = 1; i < n; i++) {
            if (a[i].key < a[i - 1].key) {
                stable = false;
                break;
            }
            if (a[i].key == a[i - 1].key && a[i].originalIndex < a[i - 1].originalIndex) {
                stable = false; // Bị đảo lộn thứ tự ban đầu của 2 phần tử cùng khóa
                break;
            }
        }
        assertTrue("Tính ổn định (Stability) được bảo toàn khi kết hợp Cutoff", stable);
    }

    // 11. Kiểm tra mode đo lường SortMetrics
    private static void testMetricsTracking() {
        Integer[] a = { 5, 2, 8, 1, 9, 3, 7, 4, 6 };
        SortMetrics metrics = MergeSortCutoff.sortTracked(a, 4);

        boolean valid = MergeSortCutoff.isSorted(a)
                && metrics.getCompares() > 0
                && metrics.getArrayAccesses() > 0
                && metrics.getElapsedTimeNs() >= 0;

        assertTrue("Đo lường SortMetrics (compares, arrayAccesses, time)", valid);
    }
}
