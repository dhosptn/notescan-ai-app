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
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    private Context context;
    private List<Note> notesList;
    private List<Note> originalList;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
    private Set<String> selectedItems = new HashSet<>();
    private boolean isSelectionMode = false;
    private OnSelectionChangedListener selectionListener;
    private String deviceId;

    public NoteAdapter(Context context, List<Note> notesList, OnSelectionChangedListener selectionListener, String deviceId) {
        this.context = context;
        this.notesList = notesList;
        this.originalList = new ArrayList<>(notesList);
        this.selectionListener = selectionListener;
        this.deviceId = deviceId;

        sortNotes();
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
        String formattedDate = dateFormat.format(new Date(note.getTimestamp()));
        holder.dateText.setText(formattedDate);

        holder.pinIndicator.setVisibility(View.VISIBLE);
        holder.pinIndicator.setImageResource(note.isPinned() ? R.drawable.ic_pin : R.drawable.ic_pin_outline);

        if (isSelectionMode) {
            holder.selectedIndicator.setVisibility(View.VISIBLE);
            if (selectedItems.contains(note.getId())) {
                holder.noteCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.wine));
                holder.selectedIndicator.setImageResource(R.drawable.ic_check_circle);
            } else {
                holder.noteCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.wine));
                holder.selectedIndicator.setImageResource(R.drawable.ic_circle_outline);
            }
        } else {
            holder.selectedIndicator.setVisibility(View.GONE);
            holder.noteCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.wine));
        }

        holder.noteCard.setOnLongClickListener(v -> {
            if (!isSelectionMode) {
                isSelectionMode = true;
                notifyDataSetChanged();
            }
            selectedItems.add(note.getId());
            notifyItemChanged(position);
            selectionListener.onSelectionChanged(selectedItems.size());
            return true;
        });

        holder.pinIndicator.setOnClickListener(v -> togglePinStatus(position));

        holder.noteCard.setOnClickListener(v -> {
            if (isSelectionMode) {
                if (selectedItems.contains(note.getId())) {
                    selectedItems.remove(note.getId());
                } else {
                    selectedItems.add(note.getId());
                }

                if (selectedItems.isEmpty()) {
                    isSelectionMode = false;
                    notifyDataSetChanged();
                } else {
                    notifyItemChanged(position);
                }
                selectionListener.onSelectionChanged(selectedItems.size());
            } else {
                Intent intent = new Intent(context, NoteDetailActivity.class);
                intent.putExtra("NOTE_ID", note.getId());
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        });
    }

    private void togglePinStatus(int position) {
        Note note = notesList.get(position);
        boolean newPinStatus = !note.isPinned();
        note.setPinned(newPinStatus);

        DatabaseReference noteRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(deviceId)
                .child("notes")
                .child(note.getId());

        noteRef.child("pinned").setValue(newPinStatus);

        for (Note originalNote : originalList) {
            if (originalNote.getId().equals(note.getId())) {
                originalNote.setPinned(newPinStatus);
                break;
            }
        }

        sortNotes();
    }

    public void sortNotes() {
        Collections.sort(notesList, (n1, n2) -> {
            if (n1.isPinned() && !n2.isPinned()) {
                return -1;
            } else if (!n1.isPinned() && n2.isPinned()) {
                return 1;
            } else {
                return Long.compare(n2.getTimestamp(), n1.getTimestamp());
            }
        });
        notifyDataSetChanged();
    }

    public void addNote(Note note) {
        notesList.add(note);
        originalList.add(note);
        sortNotes();
    }

    public void updateNotes(List<Note> newNotes) {
        this.notesList.clear();
        this.notesList.addAll(newNotes);
        this.originalList.clear();
        this.originalList.addAll(newNotes);
        sortNotes();
    }

    public void resetOrder() {
        notesList.clear();
        notesList.addAll(originalList);
        sortNotes();
    }

    @Override
    public int getItemCount() {
        return notesList.size();
    }

    public void deleteSelectedNotes() {
        List<Note> toRemove = getSelectedNotes();

        notesList.removeAll(toRemove);
        originalList.removeAll(toRemove);

        clearSelection();
    }

    public List<Note> getSelectedNotes() {
        List<Note> selectedNotes = new ArrayList<>();
        for (Note note : notesList) {
            if (selectedItems.contains(note.getId())) {
                selectedNotes.add(note);
            }
        }
        return selectedNotes;
    }

    public void pinSelectedNotes(boolean pinned) {
        List<Note> selectedNotes = getSelectedNotes();

        for (Note note : selectedNotes) {
            note.setPinned(pinned);

            DatabaseReference noteRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(deviceId)
                    .child("notes")
                    .child(note.getId());

            noteRef.child("pinned").setValue(pinned);
        }

        for (Note originalNote : originalList) {
            for (Note selected : selectedNotes) {
                if (originalNote.getId().equals(selected.getId())) {
                    originalNote.setPinned(pinned);
                    break;
                }
            }
        }

        clearSelection();
        sortNotes();
    }

    public void clearSelection() {
        selectedItems.clear();
        isSelectionMode = false;
        notifyDataSetChanged();
        selectionListener.onSelectionChanged(0);
    }

    public static class NoteViewHolder extends RecyclerView.ViewHolder {
        CardView noteCard;
        TextView textPreview;
        TextView dateText;
        ImageView selectedIndicator, pinIndicator;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            noteCard = itemView.findViewById(R.id.noteCard);
            textPreview = itemView.findViewById(R.id.textPreview);
            dateText = itemView.findViewById(R.id.dateText);
            selectedIndicator = itemView.findViewById(R.id.selectedIndicator);
            pinIndicator = itemView.findViewById(R.id.pinIndicator);
        }
    }

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int count);
    }
}
