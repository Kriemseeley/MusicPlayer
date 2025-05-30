package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import android.widget.TextView;
import android.widget.CheckBox;
public class OnlineSongAdapter extends RecyclerView.Adapter<OnlineSongAdapter.ViewHolder> {
    private List<Song> songList;
    private Set<Integer> selectedPositions = new HashSet<>();

    public OnlineSongAdapter(List<Song> songList) {
        this.songList = songList;
    }

    public List<Song> getSelectedSongs() {
        List<Song> selected = new ArrayList<>();
        for (Integer pos : selectedPositions) {
            selected.add(songList.get(pos));
        }
        return selected;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_online_song, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Song song = songList.get(position);
        holder.tvName.setText(song.getName());
//        holder.tvSinger.setText(song.gets()); // 你可以自定义显示内容
        holder.checkBox.setChecked(selectedPositions.contains(position));
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked)
                selectedPositions.add(position);
            else
                selectedPositions.remove(position);
        });
    }

    @Override
    public int getItemCount() {
        return songList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvSinger;
        CheckBox checkBox;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_song_name);
            tvSinger = itemView.findViewById(R.id.tv_song_singer);
            checkBox = itemView.findViewById(R.id.cb_select);
        }
    }
}