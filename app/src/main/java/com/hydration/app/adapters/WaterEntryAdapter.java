package com.hydration.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hydration.app.R;
import com.hydration.app.models.WaterEntry;

import java.util.List;

/**
 * WATER ENTRY ADAPTER
 *
 * RecyclerView.Adapter kết nối List<WaterEntry> với RecyclerView UI.
 *
 * Cách hoạt động của RecyclerView:
 *   - Chỉ tạo đủ số ViewHolder để fill màn hình (tiết kiệm memory)
 *   - Khi scroll, ViewHolder cũ được tái sử dụng (recycle) cho item mới
 *   - onCreateViewHolder: inflate layout (ít gọi)
 *   - onBindViewHolder: gán data vào view (gọi nhiều khi scroll)
 */
public class WaterEntryAdapter extends RecyclerView.Adapter<WaterEntryAdapter.ViewHolder> {

    // Interface callback để Activity xử lý event (không để logic trong Adapter)
    public interface OnDeleteListener {
        void onDelete(WaterEntry entry, int position);
    }

    private List<WaterEntry> entries;
    private OnDeleteListener deleteListener;

    public WaterEntryAdapter(List<WaterEntry> entries) {
        this.entries = entries;
    }

    public void setOnDeleteListener(OnDeleteListener listener) {
        this.deleteListener = listener;
    }

    /**
     * ViewHolder: giữ reference đến các View trong một item
     * Tránh gọi findViewById() mỗi lần scroll (tốn CPU)
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAmount;
        TextView tvTime;
        TextView tvDate;
        TextView tvNote;
        ImageButton btnDelete;
        View colorBar; // thanh màu bên trái

        public ViewHolder(View itemView) {
            super(itemView);
            tvAmount  = itemView.findViewById(R.id.tv_entry_amount);
            tvTime    = itemView.findViewById(R.id.tv_entry_time);
            tvDate    = itemView.findViewById(R.id.tv_entry_date);
            tvNote    = itemView.findViewById(R.id.tv_entry_note);
            btnDelete = itemView.findViewById(R.id.btn_delete_entry);
            colorBar  = itemView.findViewById(R.id.view_color_bar);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate layout item_water_entry.xml
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_water_entry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WaterEntry entry = entries.get(position);

        // Gán data vào các View
        holder.tvAmount.setText(entry.getAmount() + " ml");
        holder.tvTime.setText(entry.getFormattedTime());
        holder.tvDate.setText(entry.getFormattedDate());

        // Ẩn note nếu trống
        if (entry.getNote() != null && !entry.getNote().isEmpty()) {
            holder.tvNote.setVisibility(View.VISIBLE);
            holder.tvNote.setText(entry.getNote());
        } else {
            holder.tvNote.setVisibility(View.GONE);
        }

        // Màu thanh bên trái theo lượng nước
        int colorRes;
        if (entry.getAmount() >= 500)      colorRes = 0xFF1976D2; // blue dark
        else if (entry.getAmount() >= 300) colorRes = 0xFF42A5F5; // blue
        else                               colorRes = 0xFF90CAF9; // blue light
        holder.colorBar.setBackgroundColor(colorRes);

        // Nút xóa
        holder.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_ID) {
                    deleteListener.onDelete(entry, pos);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return entries == null ? 0 : entries.size();
    }

    // ===== PUBLIC METHODS cho Activity =====

    public void updateData(List<WaterEntry> newEntries) {
        this.entries = newEntries;
        notifyDataSetChanged();
    }

    public void removeItem(int position) {
        if (position >= 0 && position < entries.size()) {
            entries.remove(position);
            notifyItemRemoved(position);
        }
    }

    public void addItemAtTop(WaterEntry entry) {
        entries.add(0, entry);
        notifyItemInserted(0);
    }
}
