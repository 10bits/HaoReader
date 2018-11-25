package com.monke.monkeybook.view.adapter;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.monke.monkeybook.R;
import com.monke.monkeybook.bean.RipeFile;
import com.monke.monkeybook.help.BookshelfHelp;
import com.monke.monkeybook.presenter.contract.FileSelectorContract;
import com.monke.monkeybook.utils.ScreenUtils;
import com.monke.monkeybook.widget.AppCompat;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class FileSelectorAdapter extends RecyclerView.Adapter<FileSelectorAdapter.FileViewHolder> {

    private final List<RipeFile> files;

    private WeakReference<Fragment> fragment;
    private LayoutInflater inflater;

    private boolean singleChoice;
    private boolean checkBookAdded;
    private boolean isImage;

    private int filesCount;

    private Drawable placeholder;

    private RipeFile lastSelectedFile = null;

    private OnItemClickListener itemClickListener;


    public void setOnItemClickListener(OnItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    public FileSelectorAdapter(Fragment context, boolean singleChoice, boolean checkBookAdded, boolean isImage) {
        this.fragment = new WeakReference<>(context);
        this.singleChoice = singleChoice;
        this.checkBookAdded = checkBookAdded;
        this.isImage = isImage;
        this.inflater = LayoutInflater.from(fragment.get().getContext());

        placeholder = context.getResources().getDrawable(R.drawable.ic_image_placeholder);
        AppCompat.setTint(placeholder, context.getResources().getColor(R.color.tv_text_summary));

        files = new ArrayList<>();
    }

    public void setItems(List<RipeFile> files) {
        this.files.clear();
        if (files != null && !files.isEmpty()) {
            this.files.addAll(files);
        }
        notifyDataSetChanged();
    }

    public void reset() {
        lastSelectedFile = null;
        for (RipeFile file : files) {
            file.setSelected(false);
        }
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new FileViewHolder(inflater.inflate(R.layout.item_file_selector, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder thisViewHolder, int i) {

    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (!payloads.isEmpty()) {
            holder.mChecker.setChecked(false);
        } else {
            final RipeFile item = files.get(holder.getLayoutPosition());
            holder.mTvName.setText(item.getName());
            if (item.isDirectory()) {
                holder.mIcon.setImageResource(R.drawable.ic_folder_black_24dp);
                AppCompat.setTint(holder.mIcon, holder.mChecker.getResources().getColor(R.color.colorAccent));
                holder.mIcon.setVisibility(View.VISIBLE);
                holder.mAdded.setVisibility(View.INVISIBLE);
                holder.mCover.setVisibility(View.INVISIBLE);
                holder.mTvTag.setVisibility(View.GONE);
                holder.mChecker.setVisibility(View.GONE);
                holder.mTvInfo.setText(String.format(Locale.getDefault(), "%d项 | %s", item.getChildCount(), item.getDate()));
            } else {
                holder.mTvTag.setVisibility(View.VISIBLE);
                holder.mChecker.setVisibility(View.VISIBLE);
                AppCompat.setTint(holder.mIcon, holder.mChecker.getResources().getColor(R.color.transparent));
                if (isImage) {
                    holder.mIcon.setVisibility(View.INVISIBLE);
                    holder.mAdded.setVisibility(View.INVISIBLE);
                    holder.mCover.setVisibility(View.VISIBLE);
                    Glide.with(fragment.get()).load(item.getPath())
                            .apply(new RequestOptions().placeholder(placeholder)
                                    .transforms(new CenterCrop(), new RoundedCorners(ScreenUtils.dpToPx(3))))
                            .transition(new DrawableTransitionOptions().crossFade())
                            .into(holder.mCover);
                } else if (checkBookAdded && isAdded(item.getFile())) {
                    holder.mIcon.setVisibility(View.INVISIBLE);
                    holder.mAdded.setVisibility(View.VISIBLE);
                    holder.mCover.setVisibility(View.INVISIBLE);
                    holder.mIcon.setImageResource(R.drawable.ic_book_added_black_24dp);
                    holder.mChecker.setVisibility(View.GONE);
                } else {
                    holder.mIcon.setVisibility(View.VISIBLE);
                    holder.mAdded.setVisibility(View.INVISIBLE);
                    holder.mCover.setVisibility(View.INVISIBLE);
                    holder.mIcon.setImageResource(R.drawable.ic_file_black_24dp);
                    AppCompat.setTint(holder.mIcon, holder.mChecker.getResources().getColor(R.color.light_red));
                }
                holder.mTvTag.setText(item.getSuffix());
                holder.mTvInfo.setText(String.format(Locale.getDefault(), "%s | %s", item.getSize(), item.getDate()));
            }

            holder.mChecker.setChecked(item.isSelected());

            holder.itemView.setOnClickListener(v -> {
                if (singleChoice) {
                    int index;
                    if (lastSelectedFile != null && (index = files.indexOf(lastSelectedFile)) >= 0) {
                        lastSelectedFile.setSelected(false);
                        notifyItemChanged(index, index);
                    }

                    lastSelectedFile = item;

                    if (!item.isSelected()) {
                        item.setSelected(true);
                    }
                    if (!item.isDirectory()) {
                        holder.mChecker.setChecked(true);
                    }
                } else {
                    if (!item.isDirectory()) {
                        holder.mChecker.setChecked(!item.isSelected());
                    }
                    item.setSelected(!item.isSelected());
                }

                if (itemClickListener != null) {
                    itemClickListener.onItemClick(v, item);
                }
            });

            if (isImage) {
                holder.mCover.setOnClickListener(v -> {
                    if (fragment.get() instanceof FileSelectorContract.View) {
                        ((FileSelectorContract.View) fragment.get()).showBigImage(holder.mCover, item.getPath());
                    }
                });
            }
        }
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    public String getSelectedFile() {
        return lastSelectedFile == null ? null : lastSelectedFile.getPath();
    }

    public void sort(Comparator<RipeFile> comparator) {
        Collections.sort(files, comparator);
        notifyDataSetChanged();
    }

    public List<String> getSelectedFiles() {
        filesCount = 0;
        List<String> list = new ArrayList<>();
        for (RipeFile file : files) {
            if (!file.isDirectory()) {
                filesCount++;
            }

            if (file.isSelected()) {
                list.add(file.getPath());
            }
        }
        return list;
    }

    public void selectAll() {
        int selectCount = getSelectedFiles().size();
        if (selectCount < filesCount) {
            for (RipeFile file : files) {
                file.setSelected(true);
            }
        } else {
            for (RipeFile file : files) {
                file.setSelected(false);
            }
        }
        notifyDataSetChanged();
    }

    private boolean isAdded(File file) {
        if (!checkBookAdded || file.isDirectory()) {
            return false;
        }

        return BookshelfHelp.isInBookShelf(file.getAbsolutePath());
    }

    static class FileViewHolder extends RecyclerView.ViewHolder {

        private ImageView mIcon, mAdded, mCover;
        private TextView mTvName;
        private TextView mTvTag;
        private TextView mTvInfo;
        private CheckBox mChecker;

        FileViewHolder(@NonNull View itemView) {
            super(itemView);

            mIcon = itemView.findViewById(R.id.file_iv_icon);
            mAdded = itemView.findViewById(R.id.file_iv_added);
            mCover = itemView.findViewById(R.id.file_iv_cover);
            mChecker = itemView.findViewById(R.id.file_rb_checker);
            mTvName = itemView.findViewById(R.id.file_tv_name);
            mTvTag = itemView.findViewById(R.id.file_tv_tag);
            mTvInfo = itemView.findViewById(R.id.file_tv_info);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(View view, RipeFile file);
    }
}
