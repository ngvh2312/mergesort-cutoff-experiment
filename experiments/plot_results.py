"""
plot_results.py

Script trực quan hóa dữ liệu thực nghiệm Mergesort Cutoff theo chuẩn Sedgewick & Wayne 1.4:
- Đọc file CSV bằng thư viện chuẩn tích hợp sẵn của Python (KHÔNG phụ thuộc pandas / numpy).
- Tự động xuất:
  1. File báo cáo trực quan tương tác report/dashboard.html (mở xem trực tiếp trên mọi trình duyệt).
  2. Bốn biểu đồ PNG độ phân giải cao (300 DPI) vào report/figures/ nếu có thư viện matplotlib.

Cách chạy:
  python experiments/plot_results.py
"""

import csv
import json
import math
import os
import sys

def load_csv_data(csv_file):
    """Đọc dữ liệu CSV bằng thư viện chuẩn tích hợp sẵn của Python."""
    records = []
    with open(csv_file, 'r', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        for row in reader:
            records.append({
                'N': int(row['N']),
                'Distribution': row['Distribution'],
                'Cutoff': int(row['Cutoff']),
                'Mean_Time_ms': float(row['Mean_Time_ms']),
                'Min_Time_ms': float(row['Min_Time_ms']),
                'Max_Time_ms': float(row['Max_Time_ms']),
                'StdDev_Time_ms': float(row['StdDev_Time_ms']),
                'Key_Compares': int(row['Key_Compares']),
                'Array_Accesses': int(row['Array_Accesses']),
                'Speedup_Percent': float(row['Speedup_Percent'])
            })
    return records

def generate_html_dashboard(records, output_path):
    """Tạo trang báo cáo tương tác HTML/Chart.js chạy trực tiếp trên mọi máy không cần cài bất kỳ thư viện nào."""
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    
    # Lấy danh sách N và Distributions
    sizes = sorted(list(set(r['N'] for r in records)))
    dists = sorted(list(set(r['Distribution'] for r in records)))
    cutoffs = sorted(list(set(r['Cutoff'] for r in records)))
    
    records_json = json.dumps(records)

    html_content = f"""<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Báo cáo Thực nghiệm Mergesort Cutoff - Sedgewick & Wayne 1.4</title>
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <style>
        body {{ font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Arial, sans-serif; margin: 0; background: #f4f6f9; color: #333; }}
        header {{ background: linear-gradient(135deg, #1e3c72 0%, #2a5298 100%); color: white; padding: 25px 40px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); }}
        header h1 {{ margin: 0 0 8px 0; font-size: 24px; }}
        header p {{ margin: 0; opacity: 0.9; font-size: 14px; }}
        .container {{ max-width: 1200px; margin: 30px auto; padding: 0 20px; }}
        .card {{ background: white; border-radius: 10px; padding: 25px; margin-bottom: 30px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }}
        .card h2 {{ margin-top: 0; font-size: 18px; color: #1e3c72; border-bottom: 2px solid #eef2f6; padding-bottom: 10px; }}
        .grid {{ display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }}
        @media (max-width: 900px) {{ .grid {{ grid-template-columns: 1fr; }} }}
        .chart-box {{ position: relative; height: 350px; width: 100%; }}
        .tag {{ display: inline-block; padding: 3px 8px; border-radius: 4px; font-weight: bold; font-size: 12px; }}
        .tag-7 {{ background: #e8f5e9; color: #2e7d32; border: 1px solid #c8e6c9; }}
        .tag-10 {{ background: #e3f2fd; color: #1565c0; border: 1px solid #bbdefb; }}
        table {{ width: 100%; border-collapse: collapse; margin-top: 15px; font-size: 14px; }}
        th, td {{ padding: 10px 12px; text-align: left; border-bottom: 1px solid #e0e0e0; }}
        th {{ background: #f8fafc; color: #475569; font-weight: 600; }}
        tr:hover {{ background: #f1f5f9; }}
    </style>
</head>
<body>
    <header>
        <h1>Báo cáo Thực nghiệm: Tối ưu CUTOFF cho Mergesort</h1>
        <p>Phân tích thuật toán thực nghiệm bám sát giáo trình Robert Sedgewick & Kevin Wayne (Section 1.4 & 2.2)</p>
    </header>

    <div class="container">
        <div class="card">
            <h2>1. Tổng kết Trọng tâm: So sánh Cutoff 0 (Pure) vs Cutoff 7 (Sedgewick) vs Cutoff 10</h2>
            <p>Khảo sát trên kích thước mảng <b>N = {sizes[-1]:,}</b>. Điểm nhấn: <span class="tag tag-7">Cutoff = 7 (Khuyến nghị kinh điển)</span> và <span class="tag tag-10">Cutoff = 10 (Heuristic thực tế)</span></p>
            <div id="summaryTableContainer"></div>
        </div>

        <div class="grid">
            <div class="card">
                <h2>2. Thời gian thực thi theo Cutoff (5 dạng phân phối)</h2>
                <div class="chart-box"><canvas id="chartDistributions"></canvas></div>
            </div>
            <div class="card">
                <h2>3. Mô hình chi phí: Đánh đổi Array Accesses vs Compares</h2>
                <div class="chart-box"><canvas id="chartTradeoff"></canvas></div>
            </div>
        </div>

        <div class="card">
            <h2>4. So sánh tốc độ cải thiện (Speedup %) theo từng dạng phân phối</h2>
            <div class="chart-box" style="height: 380px;"><canvas id="chartBarComparison"></canvas></div>
        </div>
    </div>

    <script>
        const rawData = {records_json};
        const maxN = Math.max(...rawData.map(r => r.N));
        const filtered = rawData.filter(r => r.N === maxN);

        // Tạo bảng tóm tắt
        const dists = [...new Set(filtered.map(r => r.Distribution))];
        let tableHtml = '<table><thead><tr><th>Dạng dữ liệu</th><th>Pure Mergesort (C=0)</th><th>Cutoff = 7</th><th>Cutoff = 10</th><th>Tăng tốc (C=7)</th><th>Tăng tốc (C=10)</th></tr></thead><tbody>';
        
        dists.forEach(d => {{
            const r0 = filtered.find(r => r.Distribution === d && r.Cutoff === 0);
            const r7 = filtered.find(r => r.Distribution === d && r.Cutoff === 7);
            const r10 = filtered.find(r => r.Distribution === d && r.Cutoff === 10);
            if (r0 && r7 && r10) {{
                tableHtml += `<tr>
                    <td><b>${{d}}</b></td>
                    <td>${{r0.Mean_Time_ms.toFixed(3)}} ms</td>
                    <td><span class="tag tag-7">${{r7.Mean_Time_ms.toFixed(3)}} ms</span></td>
                    <td><span class="tag tag-10">${{r10.Mean_Time_ms.toFixed(3)}} ms</span></td>
                    <td style="color: #2e7d32; font-weight: bold;">+${{r7.Speedup_Percent.toFixed(1)}}%</td>
                    <td style="color: #1565c0; font-weight: bold;">+${{r10.Speedup_Percent.toFixed(1)}}%</td>
                </tr>`;
            }}
        }});
        tableHtml += '</tbody></table>';
        document.getElementById('summaryTableContainer').innerHTML = tableHtml;

        // Biểu đồ 1: Thời gian theo Cutoff
        const cutoffs = [...new Set(filtered.map(r => r.Cutoff))].sort((a,b)=>a-b);
        const palette = {{
            'RANDOM': '#1f77b4', 'NEARLY_SORTED': '#2ca02c', 'SORTED': '#ff7f0e',
            'REVERSED': '#d62728', 'DUPLICATES': '#9467bd'
        }};

        const datasetsDist = dists.map(d => ({{
            label: d,
            data: cutoffs.map(c => {{
                const found = filtered.find(r => r.Distribution === d && r.Cutoff === c);
                return found ? found.Mean_Time_ms : null;
            }}),
            borderColor: palette[d] || '#333',
            backgroundColor: palette[d] || '#333',
            tension: 0.2,
            borderWidth: 2,
            fill: false
        }}));

        new Chart(document.getElementById('chartDistributions'), {{
            type: 'line',
            data: {{ labels: cutoffs, datasets: datasetsDist }},
            options: {{
                responsive: true, maintainAspectRatio: false,
                plugins: {{ title: {{ display: true, text: 'Thời gian chạy (ms) theo ngưỡng Cutoff (N = ' + maxN.toLocaleString() + ')' }} }},
                scales: {{ x: {{ title: {{ display: true, text: 'Cutoff' }} }}, y: {{ title: {{ display: true, text: 'Thời gian (ms)' }} }} }}
            }}
        }});

        // Biểu đồ 2: Trade-off
        const randomData = filtered.filter(r => r.Distribution === 'RANDOM').sort((a,b)=>a.Cutoff-b.Cutoff);
        new Chart(document.getElementById('chartTradeoff'), {{
            type: 'line',
            data: {{
                labels: randomData.map(r => r.Cutoff),
                datasets: [
                    {{
                        label: 'Array Accesses (Đọc/Ghi mảng)',
                        data: randomData.map(r => r.Array_Accesses),
                        borderColor: '#1f77b4',
                        yAxisID: 'y1',
                        borderWidth: 2
                    }},
                    {{
                        label: 'Key Compares (Phép so sánh khóa)',
                        data: randomData.map(r => r.Key_Compares),
                        borderColor: '#d62728',
                        yAxisID: 'y2',
                        borderDash: [5, 5],
                        borderWidth: 2
                    }}
                ]
            }},
            options: {{
                responsive: true, maintainAspectRatio: false,
                scales: {{
                    y1: {{ type: 'linear', position: 'left', title: {{ display: true, text: 'Array Accesses' }} }},
                    y2: {{ type: 'linear', position: 'right', grid: {{ drawOnChartArea: false }}, title: {{ display: true, text: 'Key Compares' }} }}
                }}
            }}
        }});

        // Biểu đồ 3: Bar Comparison
        const c0Vals = [], c7Vals = [], c10Vals = [];
        dists.forEach(d => {{
            c0Vals.push(filtered.find(r => r.Distribution === d && r.Cutoff === 0)?.Mean_Time_ms || 0);
            c7Vals.push(filtered.find(r => r.Distribution === d && r.Cutoff === 7)?.Mean_Time_ms || 0);
            c10Vals.push(filtered.find(r => r.Distribution === d && r.Cutoff === 10)?.Mean_Time_ms || 0);
        }});

        new Chart(document.getElementById('chartBarComparison'), {{
            type: 'bar',
            data: {{
                labels: dists,
                datasets: [
                    {{ label: 'Pure Mergesort (Cutoff = 0)', data: c0Vals, backgroundColor: '#94a3b8' }},
                    {{ label: 'Cutoff = 7 (Sedgewick)', data: c7Vals, backgroundColor: '#22c55e' }},
                    {{ label: 'Cutoff = 10 (Heuristic)', data: c10Vals, backgroundColor: '#3b82f6' }}
                ]
            }},
            options: {{
                responsive: true, maintainAspectRatio: false,
                scales: {{ y: {{ title: {{ display: true, text: 'Thời gian trung bình (ms)' }} }} }}
            }}
        }});
    </script>
</body>
</html>
"""
    with open(output_path, 'w', encoding='utf-8') as f:
        f.write(html_content)
    print(f"  + [Báo cáo HTML Tương tác]: {os.path.abspath(output_path)}")

def generate_matplotlib_figures(records, output_dir):
    """Xuất biểu đồ PNG bằng matplotlib nếu máy có cài đặt."""
    try:
        import importlib
        matplotlib = importlib.import_module("matplotlib")
        matplotlib.use("Agg")
        plt = importlib.import_module("matplotlib.pyplot")
    except Exception as e:
        print(f"  * Ghi chú: Matplotlib chưa sẵn sàng ({e}). Đã tạo báo cáo HTML thay thế!")
        return

    os.makedirs(output_dir, exist_ok=True)
    max_n = max(r['N'] for r in records)
    filtered = [r for r in records if r['N'] == max_n]
    cutoffs = sorted(list(set(r['Cutoff'] for r in filtered)))
    dists = sorted(list(set(r['Distribution'] for r in filtered)))

    # 1. cutoff_vs_time_distributions.png
    fig, ax = plt.subplots(figsize=(10, 6), dpi=300)
    palette = {'RANDOM': '#1f77b4', 'NEARLY_SORTED': '#2ca02c', 'SORTED': '#ff7f0e', 'REVERSED': '#d62728', 'DUPLICATES': '#9467bd'}
    
    for d in dists:
        pts = sorted([r for r in filtered if r['Distribution'] == d], key=lambda x: x['Cutoff'])
        xs = [p['Cutoff'] for p in pts]
        ys = [p['Mean_Time_ms'] for p in pts]
        ax.plot(xs, ys, marker='o', label=d, color=palette.get(d, '#333'), linewidth=2)

    ax.axvline(x=7, color='#888888', linestyle='--', alpha=0.7, label='Cutoff = 7 (Sedgewick)')
    ax.axvline(x=10, color='#e377c2', linestyle=':', alpha=0.8, label='Cutoff = 10 (Heuristic)')
    ax.set_title(f'Thời gian chạy theo CUTOFF (N = {max_n:,})', fontsize=12, fontweight='bold')
    ax.set_xlabel('Ngưỡng CUTOFF', fontsize=10, fontweight='bold')
    ax.set_ylabel('Thời gian trung bình (ms)', fontsize=10, fontweight='bold')
    ax.set_xticks(cutoffs)
    ax.legend()
    ax.grid(True, linestyle='--', alpha=0.5)
    fig.tight_layout()
    fig.savefig(os.path.join(output_dir, "cutoff_vs_time_distributions.png"))
    plt.close(fig)
    print(f"  + [PNG Figure]: {os.path.join(output_dir, 'cutoff_vs_time_distributions.png')}")

    # 2. cost_model_tradeoff.png
    random_pts = sorted([r for r in filtered if r['Distribution'] == 'RANDOM'], key=lambda x: x['Cutoff'])
    fig, ax1 = plt.subplots(figsize=(10, 6), dpi=300)
    ax2 = ax1.twinx()
    
    xs = [p['Cutoff'] for p in random_pts]
    accesses = [p['Array_Accesses'] for p in random_pts]
    compares = [p['Key_Compares'] for p in random_pts]

    l1 = ax1.plot(xs, accesses, 's-', color='#1f77b4', linewidth=2.5, label='Array Accesses')
    l2 = ax2.plot(xs, compares, '^--', color='#d62728', linewidth=2.5, label='Key Compares')

    ax1.axvspan(5, 15, color='#2ca02c', alpha=0.15, label='Vùng tối ưu C* in [5..15]')
    ax1.set_xlabel('Ngưỡng CUTOFF', fontsize=10, fontweight='bold')
    ax1.set_ylabel('Array Accesses', color='#1f77b4', fontsize=10, fontweight='bold')
    ax2.set_ylabel('Key Compares', color='#d62728', fontsize=10, fontweight='bold')
    ax1.set_title(f'Đánh đổi giữa Array Accesses & Key Compares (N = {max_n:,})', fontsize=12, fontweight='bold')
    ax1.set_xticks(xs)
    ax1.grid(True, linestyle='--', alpha=0.5)

    lines = l1 + l2
    labels = [l.get_label() for l in lines] + ['Vùng tối ưu C* in [5..15]']
    ax1.legend(lines + [plt.Rectangle((0,0),1,1, color='#2ca02c', alpha=0.15)], labels, loc='center right')
    fig.tight_layout()
    fig.savefig(os.path.join(output_dir, "cost_model_tradeoff.png"))
    plt.close(fig)
    print(f"  + [PNG Figure]: {os.path.join(output_dir, 'cost_model_tradeoff.png')}")

    # 3. bar_comparison_0_7_10.png
    fig, ax = plt.subplots(figsize=(11, 6), dpi=300)
    width = 0.25
    x_indices = list(range(len(dists)))
    
    t0 = [next((r['Mean_Time_ms'] for r in filtered if r['Distribution'] == d and r['Cutoff'] == 0), 0) for d in dists]
    t7 = [next((r['Mean_Time_ms'] for r in filtered if r['Distribution'] == d and r['Cutoff'] == 7), 0) for d in dists]
    t10 = [next((r['Mean_Time_ms'] for r in filtered if r['Distribution'] == d and r['Cutoff'] == 10), 0) for d in dists]

    ax.bar([i - width for i in x_indices], t0, width, label='Pure Mergesort (Cutoff = 0)', color='#999999')
    ax.bar(x_indices, t7, width, label='Cutoff = 7 (Sedgewick)', color='#2ca02c')
    ax.bar([i + width for i in x_indices], t10, width, label='Cutoff = 10 (Heuristic)', color='#1f77b4')

    ax.set_title(f'So sánh Pure Mergesort vs Cutoff 7 vs Cutoff 10 (N = {max_n:,})', fontsize=12, fontweight='bold')
    ax.set_ylabel('Thời gian trung bình (ms)', fontsize=10, fontweight='bold')
    ax.set_xticks(x_indices)
    ax.set_xticklabels(dists, fontweight='bold')
    ax.legend()
    ax.grid(True, axis='y', linestyle='--', alpha=0.5)
    fig.tight_layout()
    fig.savefig(os.path.join(output_dir, "bar_comparison_0_7_10.png"))
    plt.close(fig)
    print(f"  + [PNG Figure]: {os.path.join(output_dir, 'bar_comparison_0_7_10.png')}")

def main():
    csv_file = sys.argv[1] if len(sys.argv) > 1 else "experiments/benchmark_results.csv"
    if not os.path.exists(csv_file):
        print(f"Lỗi: Không tìm thấy file '{csv_file}'. Vui lòng chạy BenchmarkExperiment trước!")
        sys.exit(1)

    print(f">>> Đang đọc dữ liệu từ: {csv_file}")
    records = load_csv_data(csv_file)
    print(f"    Đã nạp {len(records)} bản ghi thực nghiệm thành công!")

    # 1. Tạo Báo cáo HTML trực quan độc lập (chạy 100% không cần thư viện ngoài)
    dashboard_path = "report/dashboard.html"
    generate_html_dashboard(records, dashboard_path)

    # 2. Tạo hình ảnh PNG nếu có matplotlib
    figures_dir = "report/figures"
    generate_matplotlib_figures(records, figures_dir)

    print("\n>>> HOÀN TẤT TRỰC QUAN HÓA!")

if __name__ == "__main__":
    main()
