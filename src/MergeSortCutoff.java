/**
 * MergeSortCutoff.java
 *
 * Thuật toán Mergesort kết hợp chuyển sang Insertion sort cho mảng nhỏ
 * dựa trên chuẩn triển khai của Robert Sedgewick & Kevin Wayne (Algorithms, 4th Edition, Section 2.2).
 *
 * Tính năng chính:
 * 1. Cutoff sang Insertion sort:
 *    - Khi kích thước đoạn con (hi - lo + 1) <= cutoff (với cutoff > 1), chuyển sang sắp xếp chèn (Insertion sort).
 *    - Khi cutoff <= 1 hoặc 0: chạy đệ quy Mergesort thuần túy tới kích thước 1.
 * 2. Cấp phát mảng phụ aux[] đúng 1 lần trước đệ quy (nguyên tắc của Sedgewick & Wayne).
 * 3. Hỗ trợ mô hình chi phí (Sedgewick & Wayne 1.4):
 *    - Đếm số phép so sánh khóa (compares).
 *    - Đếm số lần truy cập mảng (array accesses: read/write).
 *    - Đo thời gian thực thi độc lập không chịu overhead khi tắt mode đo.
 * 4. Các hàm kiểm tra tính đúng đắn (isSorted).
 */
public class MergeSortCutoff {

    // Giá trị CUTOFF mặc định thường dùng trong tài liệu Sedgewick & Wayne (~ 7 đến 15)
    public static final int DEFAULT_CUTOFF = 8;

    // Không cho phép khởi tạo đối tượng tiện ích
    private MergeSortCutoff() {}

    /**
     * Sắp xếp mảng sử dụng CUTOFF mặc định.
     */
    public static void sort(Comparable[] a) {
        sort(a, DEFAULT_CUTOFF);
    }

    /**
     * Sắp xếp mảng với giá trị CUTOFF tùy chỉnh (không đo lường chi phí).
     * @param a mảng cần sắp xếp
     * @param cutoff ngưỡng chuyển sang Insertion sort
     */
    public static void sort(Comparable[] a, int cutoff) {
        if (a == null || a.length <= 1) {
            return;
        }
        Comparable[] aux = new Comparable[a.length];
        sort(a, aux, 0, a.length - 1, cutoff, null);
    }

    /**
     * Sắp xếp mảng có kèm theo theo dõi các chỉ số chi phí (compares, array accesses, time).
     * @param a mảng cần sắp xếp
     * @param cutoff ngưỡng chuyển sang Insertion sort
     * @param metrics đối tượng SortMetrics để thu thập số liệu
     */
    public static void sort(Comparable[] a, int cutoff, SortMetrics metrics) {
        if (metrics != null) {
            metrics.startTimer();
        }

        if (a != null && a.length > 1) {
            Comparable[] aux = new Comparable[a.length];
            sort(a, aux, 0, a.length - 1, cutoff, metrics);
        }

        if (metrics != null) {
            metrics.stopTimer();
        }
    }

    /**
     * Tiện ích sắp xếp có bật đo lường và trả về đối tượng SortMetrics đã thu thập.
     */
    public static SortMetrics sortTracked(Comparable[] a, int cutoff) {
        SortMetrics metrics = new SortMetrics();
        sort(a, cutoff, metrics);
        return metrics;
    }

    /**
     * Hàm đệ quy Top-down Mergesort có ngưỡng cutoff.
     */
    private static void sort(Comparable[] a, Comparable[] aux, int lo, int hi, int cutoff, SortMetrics metrics) {
        int length = hi - lo + 1;

        // Nếu kích thước đoạn con <= cutoff (với cutoff > 1): dùng Insertion sort
        if (cutoff > 1 && length <= cutoff) {
            insertionSort(a, lo, hi, metrics);
            return;
        }

        // Điều kiện dừng cơ sở của Mergesort thuần túy (khi cutoff <= 1 hoặc mảng rỗng/1 phần tử)
        if (hi <= lo) {
            return;
        }

        int mid = lo + (hi - lo) / 2;
        sort(a, aux, lo, mid, cutoff, metrics);
        sort(a, aux, mid + 1, hi, cutoff, metrics);
        merge(a, aux, lo, mid, hi, metrics);
    }

    /**
     * Trộn 2 nửa đã được sắp xếp a[lo..mid] và a[mid+1..hi] sử dụng mảng phụ aux[].
     * Chuẩn Sedgewick & Wayne Section 2.2.
     */
    private static void merge(Comparable[] a, Comparable[] aux, int lo, int mid, int hi, SortMetrics metrics) {
        // 1. Sao chép sang mảng phụ aux[]
        for (int k = lo; k <= hi; k++) {
            aux[k] = a[k];
            if (metrics != null) {
                // 1 lần đọc từ a[k], 1 lần ghi vào aux[k]
                metrics.addArrayAccesses(2);
            }
        }

        // 2. Trộn ngược lại vào a[]
        int i = lo;
        int j = mid + 1;
        for (int k = lo; k <= hi; k++) {
            if (i > mid) {
                a[k] = aux[j++];
                if (metrics != null) metrics.addArrayAccesses(2); // 1 đọc aux, 1 ghi a
            } else if (j > hi) {
                a[k] = aux[i++];
                if (metrics != null) metrics.addArrayAccesses(2); // 1 đọc aux, 1 ghi a
            } else if (less(aux[j], aux[i], metrics)) {
                a[k] = aux[j++];
                if (metrics != null) metrics.addArrayAccesses(2); // 1 đọc aux, 1 ghi a
            } else {
                a[k] = aux[i++];
                if (metrics != null) metrics.addArrayAccesses(2); // 1 đọc aux, 1 ghi a
            }
        }
    }

    /**
     * Sắp xếp chèn (Insertion sort) trên đoạn con a[lo..hi].
     * Thuật toán ổn định (stable) và tối ưu trên mảng nhỏ.
     */
    private static void insertionSort(Comparable[] a, int lo, int hi, SortMetrics metrics) {
        for (int i = lo + 1; i <= hi; i++) {
            for (int j = i; j > lo; j--) {
                if (less(a[j], a[j - 1], metrics)) {
                    exch(a, j, j - 1, metrics);
                } else {
                    break;
                }
            }
        }
    }

    /**
     * So sánh 2 phần tử v và w (v < w).
     */
    @SuppressWarnings("unchecked")
    private static boolean less(Comparable v, Comparable w, SortMetrics metrics) {
        if (metrics != null) {
            metrics.incCompares();
            // Việc truyền v và w vào less tương ứng với 2 lần đọc phần tử từ mảng
            metrics.addArrayAccesses(2);
        }
        return v.compareTo(w) < 0;
    }

    /**
     * Đổi chỗ a[i] và a[j].
     */
    private static void exch(Comparable[] a, int i, int j, SortMetrics metrics) {
        Comparable temp = a[i];
        a[i] = a[j];
        a[j] = temp;
        if (metrics != null) {
            // 2 lần đọc (a[i], a[j]) và 2 lần ghi (a[i]=..., a[j]=...)
            metrics.addArrayAccesses(4);
        }
    }

    /**
     * Kiểm tra toàn bộ mảng đã được sắp xếp tăng dần hay chưa.
     */
    public static boolean isSorted(Comparable[] a) {
        if (a == null || a.length <= 1) {
            return true;
        }
        return isSorted(a, 0, a.length - 1);
    }

    /**
     * Kiểm tra đoạn con a[lo..hi] đã được sắp xếp tăng dần hay chưa.
     */
    @SuppressWarnings("unchecked")
    public static boolean isSorted(Comparable[] a, int lo, int hi) {
        if (a == null || hi <= lo) {
            return true;
        }
        for (int i = lo + 1; i <= hi; i++) {
            if (a[i].compareTo(a[i - 1]) < 0) {
                return false;
            }
        }
        return true;
    }
}
