# run_experiments.ps1
# Script tự động hóa toàn bộ quy trình thực nghiệm Mergesort Cutoff (Sedgewick & Wayne 1.4)

Write-Host "==========================================================================" -ForegroundColor Cyan
Write-Host "  BẮT ĐẦU CHẠY THỰC NGHIỆM TỐI ƯU CUTOFF CHO MERGESORT" -ForegroundColor Cyan
Write-Host "==========================================================================" -ForegroundColor Cyan

# 1. Biên dịch mã nguồn Java
Write-Host "`n[1/4] Biên dịch mã nguồn Java trong src/ và experiments/..." -ForegroundColor Yellow
javac -cp src src/*.java experiments/BenchmarkExperiment.java
if ($LASTEXITCODE -ne 0) {
    Write-Host "Lỗi biên dịch Java!" -ForegroundColor Red
    exit 1
}
Write-Host "Biên dịch thành công!" -ForegroundColor Green

# 2. Sinh dữ liệu mẫu vào data/samples
Write-Host "`n[2/4] Sinh dữ liệu mẫu vào data/samples/..." -ForegroundColor Yellow
java -cp "src;experiments" DataGenerator data/samples

# 3. Chạy thí nghiệm thực tế (có thể thêm tham số --quick để chạy nhanh)
Write-Host "`n[3/4] Chạy chương trình thực nghiệm Java BenchmarkExperiment..." -ForegroundColor Yellow
java -cp "src;experiments" BenchmarkExperiment $args

# 4. Vẽ biểu đồ kết quả
Write-Host "`n[4/4] Đang vẽ biểu đồ phân tích thực nghiệm..." -ForegroundColor Yellow
python experiments/plot_results.py

Write-Host "`n==========================================================================" -ForegroundColor Green
Write-Host "  TẤT CẢ CÁC BƯỚC ĐÃ HOÀN TẤT!" -ForegroundColor Green
Write-Host "  - File CSV kết quả: experiments/benchmark_results.csv" -ForegroundColor White
Write-Host "  - Báo cáo tóm tắt: report/EXPERIMENT_SUMMARY.md" -ForegroundColor White
Write-Host "  - Hình vẽ biểu đồ: report/figures/" -ForegroundColor White
Write-Host "==========================================================================" -ForegroundColor Green
