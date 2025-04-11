package com.example.myapplication;

import android.graphics.Color;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    public static final int VIEW_TYPE_EMPTY = 0;
    private static final int VIEW_TYPE_NORMAL = 1;
    private List<Song> songList;
    private OnSongDeleteListener deleteListener;
    private OnSongClickListener listener;
    private int playingPosition = -1; // 当前播放的歌曲索引


    public interface OnSongDeleteListener {
        void onSongDelete(int position);
    }

    public interface OnSongClickListener {
        void onSongClick(int position);
    }

    public SongAdapter(List<Song> songList, OnSongClickListener listener) {
        this.songList = songList == null ? new ArrayList<>() : songList;
        this.listener = listener;
    }

    public static class SongViewHolder extends RecyclerView.ViewHolder {
        TextView tvSongName;
        TextView tvSongDuration;

        public SongViewHolder(View itemView) {
            super(itemView);
            tvSongName = itemView.findViewById(R.id.tv_song_name);
            tvSongDuration = itemView.findViewById(R.id.tv_song_duration);
//            if (itemView.getTag() != null && itemView.getTag().equals("empty_item")) {
//                tvSongName.setTextSize(14f);
//                tvSongName.setTextColor(Color.GRAY);
//            }
        }
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_EMPTY) {
            // Inflate the empty layout
            View view = inflater.inflate(R.layout.item_empty, parent, false);
            // Set a tag or use the holder type to identify it later if needed
            return new SongViewHolder(view); // Use the same holder for simplicity if controls match
        } else {
            // Inflate the normal song item layout
            View view = inflater.inflate(R.layout.song_list, parent, false);
            return new SongViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        // --- Handle the empty view case ---
        if (getItemViewType(position) == VIEW_TYPE_EMPTY) {
            if (holder.tvSongName != null) { // Check if the view exists in the empty layout
                holder.tvSongName.setText("歌单为空，点击添加歌曲"); // Or your desired empty text
                // Set text color/size for empty view if needed
                holder.tvSongName.setTextColor(Color.GRAY);
            }
            if (holder.tvSongDuration != null) {
                holder.tvSongDuration.setText(""); // No duration for empty view
            }
            // Make the empty view clickable to add songs
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSongClick(-1); // Use -1 or a special constant to indicate "add" action
                }
            });
            // Reset background/highlighting for empty view
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);

        }
        // --- Handle the normal song item case ---
        else {
            Song song = songList.get(position);
            if (holder.tvSongName != null) {
                holder.tvSongName.setText(song.getName());
                // Reset text color for normal items before applying highlight
                holder.tvSongName.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.black));
            }
            if (holder.tvSongDuration != null) {
                holder.tvSongDuration.setText(formatDuration(song.getTimeDuration()));
            }

            // Highlight the currently playing song
            if (position == playingPosition) {
                if (holder.tvSongName != null) {
                    // Use a color resource for better practice
                    holder.tvSongName.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.red_700)); // Add this color to colors.xml
                }
                // Optional: Change background for playing item
                // holder.itemView.setBackgroundColor(Color.LTGRAY);
            } else {
                // Ensure non-playing items have default text color and background
                if (holder.tvSongName != null) {
                    holder.tvSongName.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.black));
                }
                // holder.itemView.setBackgroundColor(Color.TRANSPARENT); // Reset background
            }

            // Set click listener for playing the song
            holder.itemView.setOnClickListener(v -> {
                int adapterPosition = holder.getAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION && listener != null) {
                    // No need to set playing position here, MainActivity will do it when playSong starts
                    listener.onSongClick(adapterPosition);
                }
            });

            // Optional: Long click listener for deletion
            holder.itemView.setOnLongClickListener(v -> {
                int adapterPosition = holder.getAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    // You might want a specific delete listener interface or handle deletion in MainActivity via the click listener
                    // Example: Trigger delete action in MainActivity
                    // if (listener instanceof YourMainActivityListenerInterface) {
                    //    ((YourMainActivityListenerInterface)listener).onSongLongClickToDelete(adapterPosition);
                    // }
                    Toast.makeText(v.getContext(), "长按拖动: " + songList.get(adapterPosition).getName(), Toast.LENGTH_SHORT).show(); // Placeholder
                }
                return true; // Consume the long click
            });
        }
    }

    private String formatDuration(int milliseconds) {
        if (milliseconds <= 0) return "00:00";

        int totalSeconds = milliseconds / 1000; // 将毫秒转为秒
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }
    public int getItemViewType(int position) {
        return songList.isEmpty() ? VIEW_TYPE_EMPTY : VIEW_TYPE_NORMAL;
    }
    @Override
    public int getItemCount() {
        return songList.isEmpty() ? 1 : songList.size();
    }
    public void updateSongList(List<Song> newSongs) {
        // Clear the old list and add all from the new list
        this.songList.clear();
        if (newSongs != null) {
            this.songList.addAll(newSongs);
        }
        int oldPlayingPosition = this.playingPosition;
        this.playingPosition = -1; // Reset highlight when list changes
        notifyDataSetChanged(); // Notify adapter of the complete change
        Log.d("SongAdapter", "Updated song list. New size: " + this.songList.size());
    }
    //设定当前播放的歌曲索引
    public void setPlayingPosition(int position) {
        int previousPosition = playingPosition;
        playingPosition = position;
        // Notify changes for both the old and new playing items to update highlighting
        if (previousPosition >= 0 && previousPosition < getItemCount()) { // Check bounds for previous
            notifyItemChanged(previousPosition);
        }
        if (playingPosition >= 0 && playingPosition < getItemCount()) { // Check bounds for new
            notifyItemChanged(playingPosition);
        }
        Log.d("SongAdapter", "Playing position set to: " + position);
    }

    // 删除歌曲
//    public void removeSong(int position) {
//        if (position < 0 || position >= songList.size()) return;
//        songList.remove(position);
//        notifyItemRemoved(position);
//    }
    public void moveSong(int fromPosition, int toPosition) {
        if (songList == null || fromPosition < 0 || toPosition < 0 || fromPosition >= songList.size() || toPosition >= songList.size()) {
            return;
        }
        Collections.swap(songList, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);

        // Adjust playing position if it was involved in the swap
        // This logic might need refinement depending on exact behavior desired
        if (playingPosition == fromPosition) {
            playingPosition = toPosition;
        } else if (playingPosition > fromPosition && playingPosition <= toPosition) {
            // Item moved down, crossing playing position
            playingPosition--;
        } else if (playingPosition < fromPosition && playingPosition >= toPosition) {
            // Item moved up, crossing playing position
            playingPosition++;
        }
        // No need to call notifyItemChanged here for playingPosition, as move handles visual shift.
        // MainActivity saves the list after move.
        Log.d("SongAdapter", "Moved song from " + fromPosition + " to " + toPosition);
    }

//    private static class EmptyViewHolder extends RecyclerView.ViewHolder {
//        TextView tvEmpty;
//
//        public EmptyViewHolder(View itemView) {
//            super(itemView);
//            tvEmpty = itemView.findViewById(R.id.tv_empty); // 仅绑定空列表控件
//        }
//    }



}
