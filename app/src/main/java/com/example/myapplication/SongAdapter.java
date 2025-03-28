package com.example.myapplication;

import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    private static final int VIEW_TYPE_EMPTY = 0;
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
        if (viewType == VIEW_TYPE_EMPTY) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_empty, parent, false);
            return new SongViewHolder(view);
        }else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.song_list, parent, false);
            return new SongViewHolder(view);
        }

    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        if (songList.isEmpty()) {
            if (holder.tvSongName != null) {
                holder.tvSongName.setText("暂无歌曲，点击添加");
            }
            if (holder.tvSongDuration != null) {
                holder.tvSongDuration.setText("");
            }
            holder.itemView.setOnClickListener(v -> {
                // 触发添加歌曲操作
                if (listener != null) listener.onSongClick(-1);
            });
            return;
        }
        Song song = songList.get(position);
        if (holder.tvSongName != null) {
            holder.tvSongName.setText(song.getName());
        }
        if (holder.tvSongDuration != null) {
            holder.tvSongDuration.setText(formatDuration(song.getTimeDuration()));
        }

        // 高亮正在播放的歌曲
        if (position == playingPosition) {
            if (holder.tvSongName != null) {
                holder.tvSongName.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.holo_red_light));
            }
        } else {
            if (holder.tvSongName != null) {
                holder.tvSongName.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.black));
            }
        }

        // 监听点击播放
        holder.itemView.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                setPlayingPosition(adapterPosition);
                listener.onSongClick(adapterPosition);
            }
        });

        // 监听长按删除（可选）
        holder.itemView.setOnLongClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onSongDelete(position);
                removeSong(position);
            }
            return true;
        });
    }

    //    @Override
//    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
//        if (songList.isEmpty()) {
//            holder.tvSongName.setText("暂无歌曲，点击添加");
//            holder.tvSongDuration.setText("");
//            holder.itemView.setOnClickListener(v -> {
//                // 触发添加歌曲操作
//                if (listener != null) listener.onSongClick(-1);
//            });
//            return;
//        }
//        Song song = songList.get(position);
//        holder.tvSongName.setText(song.getName());
//        holder.tvSongDuration.setText(formatDuration(song.getTimeDuration()));
//
//        // 高亮正在播放的歌曲
//        if (position == playingPosition) {
//            holder.tvSongName.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.holo_red_light));
//        } else {
//            holder.tvSongName.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.black));
//        }
//
//        // 监听点击播放
//        holder.itemView.setOnClickListener(v -> {
//            int adapterPosition = holder.getAdapterPosition();
//            if (adapterPosition != RecyclerView.NO_POSITION) {
//                setPlayingPosition(adapterPosition);
//                listener.onSongClick(adapterPosition);
//            }
//        });
//
//        // 监听长按删除（可选）
//        holder.itemView.setOnLongClickListener(v -> {
//            if (deleteListener != null) {
//                deleteListener.onSongDelete(position);
//                removeSong(position);
//            }
//            return true;
//        });
//    }
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

    //设定当前播放的歌曲索引
    public void setPlayingPosition(int position) {
        int previousPosition = playingPosition;
        playingPosition = position;
        notifyItemChanged(previousPosition);
        notifyItemChanged(playingPosition);
    }

    // 删除歌曲
    public void removeSong(int position) {
        if (position < 0 || position >= songList.size()) return;
        songList.remove(position);
        notifyItemRemoved(position);
    }
    public void moveSong(int fromPosition, int toPosition) {
        // 确保位置有效
        if (fromPosition < 0 || toPosition < 0 || fromPosition >= songList.size() || toPosition >= songList.size()) {
            return;
        }

        // 交换歌曲位置
        Collections.swap(songList, fromPosition, toPosition);

        // 通知适配器更新
        notifyItemMoved(fromPosition, toPosition);

        // 更新正在播放的位置
        if (playingPosition == fromPosition) {
            playingPosition = toPosition;
        } else if (playingPosition == toPosition) {
            playingPosition = fromPosition;
        }
    }
    private static class EmptyViewHolder extends RecyclerView.ViewHolder {
        TextView tvEmpty;

        public EmptyViewHolder(View itemView) {
            super(itemView);
            tvEmpty = itemView.findViewById(R.id.tv_empty); // 仅绑定空列表控件
        }
    }



}
