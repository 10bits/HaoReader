//Copyright (c) 2017. 章钦豪. All rights reserved.
package com.monke.monkeybook.view.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.monke.monkeybook.R;
import com.monke.monkeybook.bean.BookShelfBean;
import com.monke.monkeybook.help.BookshelfHelp;
import com.monke.monkeybook.help.MyItemTouchHelpCallback;
import com.monke.monkeybook.view.activity.MainActivity;
import com.monke.monkeybook.view.adapter.base.BaseBookListAdapter;
import com.monke.monkeybook.view.adapter.base.OnBookItemClickListenerTwo;
import com.monke.mprogressbar.MHorProgressBar;
import com.victor.loading.rotate.RotateLoading;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import me.grantland.widget.AutofitTextView;

public class BookShelfGridAdapter extends BaseBookListAdapter<BookShelfGridAdapter.MyViewHolder> {

    public BookShelfGridAdapter(Context context, int group, String bookPx) {
        super(context, group, bookPx);
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bookshelf_grid, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, @SuppressLint("RecyclerView") int index) {
        final BookShelfBean item = getItem(holder.getLayoutPosition());
        assert item != null;
        holder.tvName.setText(item.getBookInfoBean().getName());
        if (TextUtils.isEmpty(item.getCustomCoverPath())) {
            Glide.with(getContext()).load(item.getBookInfoBean().getCoverUrl())
                    .apply(new RequestOptions().dontAnimate()
                            .centerCrop().placeholder(R.drawable.img_cover_default)
                            .error(R.drawable.img_cover_default))
                    .into(holder.ivCover);
        } else {
            Glide.with(getContext()).load(item.getCustomCoverPath())
                    .apply(new RequestOptions().dontAnimate()
                            .centerCrop().placeholder(R.drawable.img_cover_default)
                            .error(R.drawable.img_cover_default))
                    .into(holder.ivCover);
        }
        if (item.getHasUpdate()) {
            holder.ivHasNew.setVisibility(View.VISIBLE);
        } else {
            holder.ivHasNew.setVisibility(View.INVISIBLE);
        }

        holder.content.setOnClickListener(v -> onClick(v, item));

        if (Objects.equals(getBookshelfPx(), "2")) {
            holder.tvName.setClickable(true);
            holder.tvName.setOnClickListener(v -> onLongClick(v, item));
            holder.content.setOnLongClickListener(null);
        }else {
            holder.tvName.setClickable(false);
            holder.content.setOnLongClickListener(v -> {
                onLongClick(v, item);
                return true;
            });
        }

        //进度条
        holder.mpbDurProgress.setVisibility(View.VISIBLE);
        holder.mpbDurProgress.setMaxProgress(item.getChapterListSize());
        float speed = item.getChapterListSize() * 1.0f / 60;

        holder.mpbDurProgress.setSpeed(speed <= 0 ? 1 : speed);

        if (animationIndex < holder.getLayoutPosition()) {
            holder.mpbDurProgress.setDurProgressWithAnim(item.getDurChapter() + 1);
            animationIndex = holder.getLayoutPosition();
        } else {
            holder.mpbDurProgress.setDurProgress(item.getDurChapter() + 1);
        }

        if (item.isLoading()) {
            holder.ivHasNew.setVisibility(View.INVISIBLE);
            holder.rotateLoading.setVisibility(View.VISIBLE);
            holder.rotateLoading.start();
        } else {
            holder.rotateLoading.setVisibility(View.INVISIBLE);
            holder.rotateLoading.stop();
        }
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCover;
        ImageView ivHasNew;
        AutofitTextView tvName;
        MHorProgressBar mpbDurProgress;
        RotateLoading rotateLoading;
        View content;

        MyViewHolder(View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.iv_cover);
            ivHasNew = itemView.findViewById(R.id.iv_has_new);
            tvName = itemView.findViewById(R.id.tv_name);
            mpbDurProgress = itemView.findViewById(R.id.mpb_durProgress);
            rotateLoading = itemView.findViewById(R.id.rl_loading);
            content = itemView.findViewById(R.id.content_card);
        }
    }
}