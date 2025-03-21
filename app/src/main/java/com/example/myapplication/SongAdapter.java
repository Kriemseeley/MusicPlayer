//package com.example.myapplication;
//
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.TextView;
//import androidx.annotation.NonNull;
//import androidx.recyclerview.widget.RecyclerView;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {
//
//    private List<Song> songList;
//    private OnSongDeleteListener deleteListener;
//    private OnSongClickListener listener;
//    private int playingPosition = -1;//索引当前播放列表的歌曲
//
//    public interface OnSongDeleteListener {
//        void onSongDelete(int position);
//    }
//
//    public interface OnSongClickListener {
//        void onSongClick(int position);
//    }
//    public SongAdapter(List<Song> songList,OnSongClickListener listener) {
//        this.songList = songList == null ? new ArrayList<>() : songList;
//        this.listener = listener;
//
//    }
//    public static class SongViewHolder extends RecyclerView.ViewHolder {
//        TextView tvSongName;
//
//        public SongViewHolder(View itemView) {
//            super(itemView);
//            tvSongName = itemView.findViewById(R.id.tv_song_name);
//        }
//    }
//    @NonNull
//    @Override
//    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        View view = LayoutInflater.from(parent.getContext())
//                .inflate(R.layout.song_list, parent, false);
//        return new SongViewHolder(view);
//    }
//
//    @Override
//    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
//        Song song = songList.get(position);
//        holder.tvSongName.setText(song.getName());
//
//        if (position == playingPosition) {
//            holder.tvSongName.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.holo_red_light));
//        } else {
//            holder.tvSongName.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.black));
//        }
//
//        holder.itemView.setOnClickListener(v -> {
//            if (listener != null) {
//                listener.onSongClick(position);
//            }
//        });
//    }
//
//    @Override
//    public int getItemCount() {
//        return songList.size();
//    }
//
//    public void updateData(List<Song> newSongList) {
//        // 如果传入的 newSongList 为 null，则创建一个空的 ArrayList
//        this.songList = newSongList == null ? new ArrayList<>() : newSongList;
//        notifyDataSetChanged();
//    }
//}
package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

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

        public SongViewHolder(View itemView) {
            super(itemView);
            tvSongName = itemView.findViewById(R.id.tv_song_name);
        }
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.song_list, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Song song = songList.get(position);
        holder.tvSongName.setText(song.getName());

        // 高亮正在播放的歌曲
        if (position == playingPosition) {
            holder.tvSongName.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.holo_red_light));
        } else {
            holder.tvSongName.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.black));
        }

        // 监听点击播放
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                setPlayingPosition(position); // 更新播放状态
                listener.onSongClick(position);
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

    @Override
    public int getItemCount() {
        return songList.size();
    }

    // 更新歌曲列表（更高效的方式）
    public void updateData(List<Song> newSongList) {
        this.songList = newSongList == null ? new ArrayList<>() : newSongList;
        notifyDataSetChanged();
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

    // 设置删除监听器
    public void setOnSongDeleteListener(OnSongDeleteListener listener) {
        this.deleteListener = listener;
    }
}
