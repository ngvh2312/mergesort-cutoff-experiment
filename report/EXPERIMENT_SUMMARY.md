# Báo cáo Kết quả Thực nghiệm: Tối ưu CUTOFF cho Mergesort
*(Phương pháp thực nghiệm & phân tích theo Robert Sedgewick & Kevin Wayne - Algorithms 1.4)*

## 1. Thiết lập thực nghiệm
- **Mô hình máy tính:** JVM Oracle 25.0.2 / Windows x64
- **Phương pháp đo:** Warm-up JVM, đo Wall-clock Time (System.nanoTime), lấy trung bình qua nhiều trials độc lập.
- **Dải Cutoff:** `0, 1, 3, 5, 7, 10, 15, 20, 30, 50`.
- **Dạng dữ liệu:** `RANDOM`, `NEARLY_SORTED`, `SORTED`, `REVERSED`, `DUPLICATES`.

## 2. Bảng so sánh Cutoff 0 (Pure) vs Cutoff 7 (Sedgewick) vs Cutoff 10
| N | Dạng dữ liệu | Pure Mergesort (C=0) | Cutoff = 7 | Cutoff = 10 | Tăng tốc (C=7) | Tăng tốc (C=10) |
| :--- | :--- | :---: | :---: | :---: | :---: | :---: |
| 1000 | RANDOM | 0.097 ms | 0.070 ms | 0.071 ms | +28.49% | +27.46% |
| 1000 | NEARLY_SORTED | 0.190 ms | 0.151 ms | 0.123 ms | +20.54% | +34.93% |
| 1000 | SORTED | 0.167 ms | 0.111 ms | 0.094 ms | +33.90% | +44.06% |
| 1000 | REVERSED | 0.188 ms | 0.124 ms | 0.109 ms | +33.97% | +41.84% |
| 1000 | DUPLICATES | 0.244 ms | 0.167 ms | 0.148 ms | +31.48% | +39.15% |
| 5000 | RANDOM | 1.016 ms | 0.666 ms | 0.592 ms | +34.41% | +41.70% |
| 5000 | NEARLY_SORTED | 0.352 ms | 0.277 ms | 0.271 ms | +21.18% | +23.01% |
| 5000 | SORTED | 0.295 ms | 0.226 ms | 0.206 ms | +23.62% | +30.37% |
| 5000 | REVERSED | 0.311 ms | 0.275 ms | 0.322 ms | +11.74% | -3.48% |
| 5000 | DUPLICATES | 0.464 ms | 0.385 ms | 0.451 ms | +17.01% | +2.85% |
| 10000 | RANDOM | 1.207 ms | 1.072 ms | 1.034 ms | +11.23% | +14.38% |
| 10000 | NEARLY_SORTED | 0.714 ms | 0.638 ms | 0.568 ms | +10.65% | +20.49% |
| 10000 | SORTED | 0.637 ms | 0.497 ms | 0.464 ms | +21.93% | +27.10% |
| 10000 | REVERSED | 0.660 ms | 0.583 ms | 0.595 ms | +11.57% | +9.79% |
| 10000 | DUPLICATES | 0.952 ms | 0.788 ms | 0.778 ms | +17.23% | +18.36% |

## 3. Nhận xét lý thuyết & Thực nghiệm
1. **Tại sao Cutoff 7 và 10 lại hiệu quả:**
   - Tránh các lời gọi hàm đệ quy sâu và thao tác sao chép sang mảng phụ `aux[]` ở các tầng đáy.
   - Trên các mảng con nhỏ ($k \le 10$), Insertion sort chạy cực nhanh và có hằng số ẩn thời gian nhỏ hơn nhiều so với Mergesort.
2. **Hiện tượng suy giảm khi Cutoff > 20 - 30:**
   - Chi phí $O(k^2)$ phép so sánh của Insertion sort bắt đầu lấn át, khiến thời gian chạy chậm lại.
