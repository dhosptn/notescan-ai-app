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

    // Menyimpan catatan yang dipilih
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

        // Format dan set tanggal
        String formattedDate = dateFormat.format(new Date(note.getTimestamp()));
        holder.dateText.setText(formattedDate);

        // Atur warna dan ikon seleksi
        if (selectedItems.contains(position)) {
            holder.noteCard.setCardBackgroundColor(Color.parseColor("#2D1119"));
            holder.selectedIndicator.setImageResource(R.drawable.ic_check_circle);
        } else {
            holder.noteCard.setCardBackgroundColor(Color.parseColor("#5B2333"));
            holder.selectedIndicator.setImageResource(R.drawable.ic_circle_outline);
        }

        // Klik lama untuk masuk mode seleksi
        holder.noteCard.setOnLongClickListener(v -> {
            if (!isSelectionMode) {
                isSelectionMode = true;
            }

            if (!selectedItems.contains(position)) {
                selectedItems.add(position);
            }

            notifyItemChanged(position);
            selectionListener.onSelectionChanged(selectedItems.size());
            return true;
        });

        // Klik biasa untuk memilih/membatalkan pilihan
        holder.noteCard.setOnClickListener(v -> {
            if (isSelectionMode) {
                if (selectedItems.contains(position)) {
                    selectedItems.remove(position);
                    holder.noteCard.setCardBackgroundColor(Color.parseColor("#5B2333"));
                    holder.selectedIndicator.setImageResource(R.drawable.ic_circle_outline);
                } else {
                    selectedItems.add(position);
                    holder.noteCard.setCardBackgroundColor(Color.parseColor("#2D1119"));
                    holder.selectedIndicator.setImageResource(R.drawable.ic_check_circle);
                }

                if (selectedItems.isEmpty()) {
                    isSelectionMode = false;
                }

                notifyItemChanged(position);
                selectionListener.onSelectionChanged(getSelectedNotes().size());
            } else {
                // Jika tidak dalam mode seleksi, buka detail catatan
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

    // Fungsi untuk menghapus item yang dipilih
    public void deleteSelectedNotes() {
        List<Note> toRemove = new ArrayList<>();
        for (int pos : selectedItems) {
            toRemove.add(notesList.get(pos));
        }
        notesList.removeAll(toRemove);
        selectedItems.clear();
        isSelectionMode = false;
        notifyDataSetChanged();
        selectionListener.onSelectionChanged(0);
    }

    // Fungsi untuk mendapatkan daftar catatan yang dipilih
    public List<Note> getSelectedNotes() {
        List<Note> selectedNotes = new ArrayList<>();
        for (int pos : selectedItems) {
            selectedNotes.add(notesList.get(pos));
        }
        return selectedNotes;
    }

    public static class NoteViewHolder extends RecyclerView.ViewHolder {
        CardView noteCard;
        TextView textPreview;
        TextView dateText;
        ImageView selectedIndicator; // Tambahan indikator

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            noteCard = itemView.findViewById(R.id.noteCard);
            textPreview = itemView.findViewById(R.id.textPreview);
            dateText = itemView.findViewById(R.id.dateText);
            selectedIndicator = itemView.findViewById(R.id.selectedIndicator);
        }
    }

    // Interface untuk menangani perubahan jumlah item yang dipilih
    public interface OnSelectionChangedListener {
        void onSelectionChanged(int count);
    }
}
