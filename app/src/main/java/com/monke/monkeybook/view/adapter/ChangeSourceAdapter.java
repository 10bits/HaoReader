package com.monke.monkeybook.view.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.monke.monkeybook.R;
import com.monke.monkeybook.bean.SearchBookBean;
import com.monke.monkeybook.widget.refreshview.RefreshRecyclerViewAdapter;

import java.util.ArrayList;
import java.util.List;

import static android.text.TextUtils.isEmpty;

/**
 * Created by GKF on 2017/12/22.
 * 书源Adapter
 */

public class ChangeSourceAdapter extends RefreshRecyclerViewAdapter {
    private List<SearchBookBean> searchBookBeans;
    private OnItemClickListener mOnItemClickListener;
    private Context mContext;

    public ChangeSourceAdapter(Context context, Boolean needLoadMore) {
        super(needLoadMore);
        mContext = context;
        searchBookBeans = new ArrayList<>();
    }

    public synchronized void addSourceAdapter(SearchBookBean value) {
        searchBookBeans.add(value);
        notifyDataSetChanged();
    }

    public synchronized void addAllSourceAdapter(List<SearchBookBean> value) {
        searchBookBeans.addAll(value);
        notifyDataSetChanged();
    }

    public synchronized void reSetSourceAdapter() {
        searchBookBeans.clear();
        notifyDataSetChanged();
    }

    public interface OnItemClickListener {
        void onItemClick(View view, int index);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mOnItemClickListener = listener;
    }

    public List<SearchBookBean> getSearchBookBeans() {
        return searchBookBeans;
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        TextView tvBookSource;
        TextView tvLastChapter;
        ImageView ivChecked;

        MyViewHolder(View itemView) {
            super(itemView);
            tvBookSource = itemView.findViewById(R.id.tv_source_name);
            tvLastChapter = itemView.findViewById(R.id.tv_lastChapter);
            ivChecked = itemView.findViewById(R.id.iv_checked);
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateIViewHolder(ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_change_source, parent, false));
    }

    @Override
    public void onBindIViewHolder(RecyclerView.ViewHolder holder, int position) {
        final int realPosition = holder.getLayoutPosition();
        final SearchBookBean item = searchBookBeans.get(realPosition);
        holder.itemView.setTag(realPosition);
        MyViewHolder myViewHolder = (MyViewHolder) holder;
        myViewHolder.tvBookSource.setText(item.getOrigin());
        if (isEmpty(item.getLastChapter())) {
            myViewHolder.tvLastChapter.setText(R.string.no_last_chapter);
        } else {
            myViewHolder.tvLastChapter.setText(item.getLastChapter());
        }
        if (item.getIsCurrentSource()) {
            myViewHolder.ivChecked.setVisibility(View.VISIBLE);
        } else {
            myViewHolder.ivChecked.setVisibility(View.INVISIBLE);
        }
        myViewHolder.itemView.setOnClickListener(view -> {
            if (mOnItemClickListener != null) {
                mOnItemClickListener.onItemClick(view, realPosition);
            }
        });
    }

    @Override
    public int getIViewType(int position) {
        return 0;
    }

    @Override
    public int getICount() {
        return searchBookBeans.size();
    }
}
