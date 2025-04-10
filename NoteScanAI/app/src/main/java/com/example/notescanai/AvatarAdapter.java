package com.example.notescanai;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AvatarAdapter extends RecyclerView.Adapter<AvatarAdapter.AvatarViewHolder> {

    private final List<Integer> avatarList;
    private final OnAvatarSelectedListener listener;

    public interface OnAvatarSelectedListener {
        void onAvatarSelected(int avatarResourceId);
    }

    public AvatarAdapter(List<Integer> avatarList, OnAvatarSelectedListener listener) {
        this.avatarList = avatarList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AvatarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_avatar, parent, false);
        return new AvatarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AvatarViewHolder holder, int position) {
        int avatarResourceId = avatarList.get(position);
        holder.avatarImageView.setImageResource(avatarResourceId);
        holder.itemView.setOnClickListener(v -> listener.onAvatarSelected(avatarResourceId));
    }

    @Override
    public int getItemCount() {
        return avatarList.size();
    }

    static class AvatarViewHolder extends RecyclerView.ViewHolder {
        ImageView avatarImageView;

        public AvatarViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarImageView = itemView.findViewById(R.id.avatarImageView);
        }
    }
}