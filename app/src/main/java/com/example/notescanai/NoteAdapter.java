package com.example.notescanai;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    private Context context;
    private List<Note> notesList;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());

    private Set<Integer> selectedItems = new HashSet<>();
    private boolean isSelectionMode = false;
    private OnSelectionChangedListener selectionListener;

    public NoteAdapter(Context context, List<Note> notesList, OnSelectionChangedListener selectionListener) {
        this.context = context;
        this.notesList = notesList;
        this.selectionListener = selectionListener;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = notesList.get(position);

        holder.textPreview.setText(note.getPreview());

        // Format tanggal
        String formattedDate = dateFormat.format(new Date(note.getTimestamp()));
        holder.dateText.setText(formattedDate);

        // Tampilkan indikator hanya saat mode seleksi aktif
        if (isSelectionMode) {
            holder.selectedIndicator.setVisibility(View.VISIBLE);

            if (selectedItems.contains(position)) {
                holder.noteCard.setCardBackgroundColor(Color.parseColor("#2D1119"));
                holder.selectedIndicator.setImageResource(R.drawable.ic_check_circle);
            } else {
                holder.noteCard.setCardBackgroundColor(Color.parseColor("#5B2333"));
                holder.selectedIndicator.setImageResource(R.drawable.ic_circle_outline);
            }
        } else {
            holder.selectedIndicator.setVisibility(View.GONE);
            holder.noteCard.setCardBackgroundColor(Color.parseColor("#5B2333"));
        }

        // Klik lama untuk aktifkan mode seleksi
        holder.noteCard.setOnLongClickListener(v -> {
            if (!isSelectionMode) {
                isSelectionMode = true;
                notifyDataSetChanged(); // Refresh semua item agar indikator muncul
            }

            if (!selectedItems.contains(position)) {
                selectedItems.add(position);
            }

            notifyItemChanged(position);
            selectionListener.onSelectionChanged(selectedItems.size());
            return true;
        });

        // Klik biasa: toggle seleksi atau buka detail
        holder.noteCard.setOnClickListener(v -> {
            if (isSelectionMode) {
                if (selectedItems.contains(position)) {
                    selectedItems.remove(position);
                } else {
                    selectedItems.add(position);
                }

                // Jika sudah tidak ada yang dipilih, keluar dari mode seleksi
                if (selectedItems.isEmpty()) {
                    isSelectionMode = false;
                    notifyDataSetChanged(); // Refresh semua item untuk sembunyikan indikator
                } else {
                    notifyItemChanged(position);
                }

                selectionListener.onSelectionChanged(selectedItems.size());
            } else {
                // Buka detail note
                Intent intent = new Intent(context, NoteDetailActivity.class);
                intent.putExtra("NOTE_ID", note.getId());
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return notesList.size();
    }

    // Hapus catatan yang dipilih dan reset mode seleksi
    public void deleteSelectedNotes() {
        List<Note> toRemove = getSelectedNotes();

        // Hapus dari list utama
        notesList.removeAll(toRemove);

        // Reset seleksi dan mode
        selectedItems.clear();
        isSelectionMode = false;

        // Refresh UI
        notifyDataSetChanged();
        selectionListener.onSelectionChanged(0);
    }

    // Dapatkan catatan yang dipilih berdasarkan posisi
    public List<Note> getSelectedNotes() {
        List<Note> selectedNotes = new ArrayList<>();
        for (int pos : selectedItems) {
            if (pos >= 0 && pos < notesList.size()) {
                selectedNotes.add(notesList.get(pos));
            }
        }
        return selectedNotes;
    }

    // ViewHolder
    public static class NoteViewHolder extends RecyclerView.ViewHolder {
        CardView noteCard;
        TextView textPreview;
        TextView dateText;
        ImageView selectedIndicator;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            noteCard = itemView.findViewById(R.id.noteCard);
            textPreview = itemView.findViewById(R.id.textPreview);
            dateText = itemView.findViewById(R.id.dateText);
            selectedIndicator = itemView.findViewById(R.id.selectedIndicator);
        }
    }

    // Callback interface
    public interface OnSelectionChangedListener {
        void onSelectionChanged(int count);
    }
}
