/**
 * SortMetrics.java
 *
 * Thu thập và quản lý các chỉ số thực nghiệm của thuật toán sắp xếp
 * bám theo mô hình chi phí trong Sedgewick & Wayne (Section 1.4 Analysis of Algorithms):
 * - Số phép so sánh khóa (comparisons)
 * - Số lần truy cập mảng (array accesses: read/write)
 * - Thời gian thực thi (elapsed time: nanoseconds / milliseconds)
 */
public class SortMetrics {
    private long compares;
    private long arrayAccesses;
    private long startTimeNs;
    private long elapsedTimeNs;

    public SortMetrics() {
        reset();
    }

    /**
     * Đặt lại tất cả các bộ đếm về 0.
     */
    public void reset() {
        this.compares = 0;
        this.arrayAccesses = 0;
        this.startTimeNs = 0;
        this.elapsedTimeNs = 0;
    }

    /**
     * Bắt đầu bấm giờ đo thời gian.
     */
    public void startTimer() {
        this.startTimeNs = System.nanoTime();
    }

    /**
     * Dừng bấm giờ và lưu lại khoảng thời gian đã trôi qua.
     */
    public void stopTimer() {
        if (this.startTimeNs > 0) {
            this.elapsedTimeNs = System.nanoTime() - this.startTimeNs;
        }
    }

    public void incCompares() {
        this.compares++;
    }

    public void addCompares(long delta) {
        this.compares += delta;
    }

    public void incArrayAccesses() {
        this.arrayAccesses++;
    }

    public void addArrayAccesses(long delta) {
        this.arrayAccesses += delta;
    }

    public long getCompares() {
        return compares;
    }

    public long getArrayAccesses() {
        return arrayAccesses;
    }

    public long getElapsedTimeNs() {
        return elapsedTimeNs;
    }

    public double getElapsedMillis() {
        return elapsedTimeNs / 1_000_000.0;
    }

    @Override
    public String toString() {
        return String.format("Compares: %,d | Array Accesses: %,d | Time: %.3f ms",
                compares, arrayAccesses, getElapsedMillis());
    }
}
