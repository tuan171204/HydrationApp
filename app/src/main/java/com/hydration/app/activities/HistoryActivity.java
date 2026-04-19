package com.hydration.app.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hydration.app.R;
import com.hydration.app.adapters.WaterEntryAdapter;
import com.hydration.app.database.DatabaseHelper;
import com.hydration.app.models.WaterEntry;

import java.util.List;

/**
 * HISTORY ACTIVITY
 *
 * Hiển thị lịch sử uống nước trong RecyclerView.
 * RecyclerView là component UI hiệu quả cho danh sách dài:
 *   - Chỉ tạo đủ View để fill màn hình
 *   - Tái sử dụng View khi scroll (thay vì tạo mới)
 *   - Cần: Adapter + LayoutManager
 */
public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private WaterEntryAdapter adapter;
    private TextView tvEmpty;
    private TextView tvTodayTotal;

    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // Hiện nút back trên ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Lịch sử uống nước");
        }

        db = DatabaseHelper.getInstance(this);
        initViews();
        loadData();
    }

    private void initViews() {
        recyclerView  = findViewById(R.id.recycler_history);
        tvEmpty       = findViewById(R.id.tv_empty_history);
        tvTodayTotal  = findViewById(R.id.tv_today_total);

        // Thiết lập RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true); // tối ưu performance
    }

    private void loadData() {
        // Query SQLite lấy tất cả entries
        List<WaterEntry> entries = db.getAllEntries();

        // Hiển thị tổng hôm nay
        int todayTotal = db.getTodayTotal();
        tvTodayTotal.setText("Hôm nay: " + todayTotal + " ml");

        if (entries.isEmpty()) {
            // Empty state
            recyclerView.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
            return;
        }

        recyclerView.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        // Tạo và set Adapter
        adapter = new WaterEntryAdapter(entries);
        recyclerView.setAdapter(adapter);

        // Xử lý xóa entry
        adapter.setOnDeleteListener((entry, position) -> {
            // Confirm dialog
            new AlertDialog.Builder(this)
                    .setTitle("Xóa bản ghi?")
                    .setMessage("Xóa " + entry.getAmount() + "ml lúc " + entry.getFormattedTime() + "?")
                    .setPositiveButton("Xóa", (dialog, which) -> {
                        db.deleteEntry(entry.getId());
                        adapter.removeItem(position);
                        Toast.makeText(this, "Đã xóa", Toast.LENGTH_SHORT).show();

                        // Cập nhật tổng hôm nay
                        int newTotal = db.getTodayTotal();
                        tvTodayTotal.setText("Hôm nay: " + newTotal + " ml");

                        // Kiểm tra empty state
                        if (adapter.getItemCount() == 0) {
                            recyclerView.setVisibility(View.GONE);
                            tvEmpty.setVisibility(View.VISIBLE);
                        }
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
