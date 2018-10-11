//Copyright (c) 2017. 章钦豪. All rights reserved.
package com.monke.monkeybook.view.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.hwangjr.rxbus.RxBus;
import com.monke.basemvplib.AppActivityManager;
import com.monke.monkeybook.BitIntentDataManager;
import com.monke.monkeybook.R;
import com.monke.monkeybook.base.MBaseActivity;
import com.monke.monkeybook.help.RxBusTag;
import com.monke.monkeybook.presenter.BookDetailPresenterImpl;
import com.monke.monkeybook.presenter.ReadBookPresenterImpl;
import com.monke.monkeybook.presenter.contract.BookDetailContract;
import com.monke.monkeybook.widget.modialog.MoProgressHUD;
import com.victor.loading.rotate.RotateLoading;

import butterknife.BindView;
import butterknife.ButterKnife;
import jp.wasabeef.glide.transformations.BlurTransformation;

import static com.monke.monkeybook.presenter.BookDetailPresenterImpl.FROM_BOOKSHELF;
import static com.monke.monkeybook.utils.NetworkUtil.isNetworkAvailable;

public class BookDetailActivity extends MBaseActivity<BookDetailContract.Presenter> implements BookDetailContract.View {
    @BindView(R.id.ifl_content)
    FrameLayout iflContent;
    @BindView(R.id.iv_blur_cover)
    ImageView ivBlurCover;
    @BindView(R.id.iv_cover)
    ImageView ivCover;
    @BindView(R.id.tv_name)
    TextView tvName;
    @BindView(R.id.tv_author)
    TextView tvAuthor;
    @BindView(R.id.tv_origin)
    TextView tvOrigin;
    @BindView(R.id.tv_chapter)
    TextView tvChapter;
    @BindView(R.id.tv_intro)
    TextView tvIntro;
    @BindView(R.id.ll_shelf_zg)
    View llShelfZg;
    @BindView(R.id.tv_shelf_zg)
    TextView tvShelfZg;
    @BindView(R.id.ll_shelf_yf)
    View llShelfYf;
    @BindView(R.id.tv_shelf_yf)
    TextView tvShelfYf;
    @BindView(R.id.ll_read)
    View llRead;
    @BindView(R.id.ll_loading)
    View llLoading;
    @BindView(R.id.rl_loading)
    RotateLoading progressBar;
    @BindView(R.id.tv_loading_msg)
    TextView tvLoadingMsg;
    @BindView(R.id.iv_refresh)
    ImageView ivRefresh;
    @BindView(R.id.tv_change_origin)
    TextView tvChangeOrigin;

    private Animation animHideLoading;
    private Animation animShowInfo;
    private MoProgressHUD moProgressHUD;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected BookDetailContract.Presenter initInjector() {
        return new BookDetailPresenterImpl();
    }

    @Override
    protected void onCreateActivity() {
        setContentView(R.layout.activity_book_detail);
    }

    @Override
    protected void initData() {
        mPresenter.initData(getIntent());
        animShowInfo = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        animHideLoading = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
        animHideLoading.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                llLoading.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }


    @Override
    protected void bindView() {
        ButterKnife.bind(this);
        //弹窗
        moProgressHUD = new MoProgressHUD(this);

        tvIntro.setMovementMethod(ScrollingMovementMethod.getInstance());
        initView();
    }

    @Override
    public void updateView() {
        if (null != mPresenter.getBookShelf()) {
            tvName.setText(mPresenter.getBookShelf().getBookInfoBean().getName());
            tvAuthor.setText(mPresenter.getBookShelf().getBookInfoBean().getAuthor());
            int group = mPresenter.getBookShelf().getGroup();
            boolean inShelf = mPresenter.getInBookShelf();
            if (inShelf) {
                changeGroup(group);
                tvChapter.setText(getString(R.string.read_dur_progress, mPresenter.getBookShelf().getDurChapterName()));
            } else {
                changeGroup(-1);
                tvChapter.setText(getString(R.string.book_search_last, mPresenter.getBookShelf().getLastChapterName()));
            }
            if (mPresenter.getBookShelf().getBookInfoBean().getIntroduce() != null) {
                tvIntro.setText(mPresenter.getBookShelf().getBookInfoBean().getIntroduce());
            }
            if (tvIntro.getVisibility() != View.VISIBLE) {
                tvIntro.setVisibility(View.VISIBLE);
                tvIntro.startAnimation(animShowInfo);
            }
            if (mPresenter.getBookShelf().getBookInfoBean().getOrigin() != null && mPresenter.getBookShelf().getBookInfoBean().getOrigin().length() > 0) {
                tvOrigin.setVisibility(View.VISIBLE);
                tvOrigin.setText(getString(R.string.origin_format, mPresenter.getBookShelf().getBookInfoBean().getOrigin()));
            } else {
                tvOrigin.setVisibility(View.INVISIBLE);
            }
            if (!this.isFinishing()) {
                String coverImage;
                if (TextUtils.isEmpty(mPresenter.getBookShelf().getCustomCoverPath())) {
                    coverImage = mPresenter.getBookShelf().getBookInfoBean().getCoverUrl();
                } else {
                    coverImage = mPresenter.getBookShelf().getCustomCoverPath();
                }

                Glide.with(this).load(coverImage)
                        .apply(new RequestOptions().dontAnimate().diskCacheStrategy(DiskCacheStrategy.RESOURCE).centerCrop()
                                .placeholder(R.drawable.img_cover_default)).into(ivCover);

                Glide.with(this).load(coverImage)
                        .apply(new RequestOptions()
                                .dontAnimate()
                                .diskCacheStrategy(DiskCacheStrategy.RESOURCE).centerCrop()
                                .placeholder(R.drawable.img_cover_gs))
                        .apply(RequestOptions.bitmapTransform(new BlurTransformation(30, 5)))
                        .into(ivBlurCover);
            }
        }
        showLoading(false);
    }

    private void showLoading(boolean show) {
        if (show) {
            llLoading.setVisibility(View.VISIBLE);
            tvLoadingMsg.setText(R.string.data_loading);
            progressBar.setVisibility(View.VISIBLE);
            progressBar.start();
            llLoading.setOnClickListener(null);
        } else {
            if(llLoading.getVisibility() == View.GONE){
                return;
            }
            llLoading.startAnimation(animHideLoading);
            llLoading.setOnClickListener(null);
        }
    }

    private void changeGroup(int group) {
        if (group == 0) {
            tvShelfZg.setText(R.string.remove_from_bookshelf_zg);
            tvShelfYf.setText(R.string.add_from_bookshelf_yf);
        } else if (group == 1) {
            tvShelfZg.setText(R.string.add_from_bookshelf_zg);
            tvShelfYf.setText(R.string.remove_from_bookshelf_yf);
        } else {
            tvShelfZg.setText(R.string.add_from_bookshelf_zg);
            tvShelfYf.setText(R.string.add_from_bookshelf_yf);
        }
    }

    @Override
    public void getBookShelfError() {
        llLoading.setVisibility(View.VISIBLE);
        tvLoadingMsg.setText("加载失败,点击重试");
        progressBar.setVisibility(View.GONE);
        progressBar.stop();
        llLoading.setOnClickListener(v -> {
            showLoading(true);
            mPresenter.getBookShelfInfo();
        });
    }

    @Override
    protected void firstRequest() {
        super.firstRequest();
        if (mPresenter.getOpenFrom() == BookDetailPresenterImpl.FROM_SEARCH) {
            //网络请求
            mPresenter.getBookShelfInfo();
        }
    }

    private void initView() {
        String coverUrl;
        String customCoverPath = null;
        String name;
        String author;
        if (mPresenter.getOpenFrom() == FROM_BOOKSHELF) {
            if (mPresenter.getBookShelf() == null) return;
            coverUrl = mPresenter.getBookShelf().getBookInfoBean().getCoverUrl();
            customCoverPath = mPresenter.getBookShelf().getCustomCoverPath();
            name = mPresenter.getBookShelf().getBookInfoBean().getName();
            author = mPresenter.getBookShelf().getBookInfoBean().getAuthor();
            if (mPresenter.getBookShelf().getBookInfoBean().getOrigin() != null && mPresenter.getBookShelf().getBookInfoBean().getOrigin().length() > 0) {
                tvOrigin.setVisibility(View.VISIBLE);
                tvOrigin.setText(getString(R.string.origin_format, mPresenter.getBookShelf().getBookInfoBean().getOrigin()));
            } else {
                tvOrigin.setVisibility(View.INVISIBLE);
            }
            updateView();
        } else {
            if (mPresenter.getSearchBook() == null) return;
            coverUrl = mPresenter.getSearchBook().getCoverUrl();
            name = mPresenter.getSearchBook().getName();
            author = mPresenter.getSearchBook().getAuthor();
            if (mPresenter.getSearchBook().getOrigin() != null && mPresenter.getSearchBook().getOrigin().length() > 0) {
                tvOrigin.setVisibility(View.VISIBLE);
                tvOrigin.setText(getString(R.string.origin_format, mPresenter.getSearchBook().getOrigin()));
            } else {
                tvOrigin.setVisibility(View.INVISIBLE);
            }
            tvChapter.setText(getString(R.string.book_search_last, mPresenter.getSearchBook().getLastChapter()));
            tvIntro.setVisibility(View.INVISIBLE);
            showLoading(true);
        }
        if (!this.isFinishing()) {
            String coverImage;
            if (TextUtils.isEmpty(customCoverPath)) {
                coverImage = coverUrl;
            } else {
                coverImage = customCoverPath;
            }

            Glide.with(this).load(coverImage)
                    .apply(new RequestOptions().dontAnimate().diskCacheStrategy(DiskCacheStrategy.RESOURCE).centerCrop()
                            .placeholder(R.drawable.img_cover_default)).into(ivCover);

            Glide.with(this).load(coverImage)
                    .apply(new RequestOptions()
                            .dontAnimate()
                            .diskCacheStrategy(DiskCacheStrategy.RESOURCE).centerCrop()
                            .placeholder(R.drawable.img_cover_gs))
                    .apply(RequestOptions.bitmapTransform(new BlurTransformation(30, 5)))
                    .into(ivBlurCover);
        }
        tvName.setText(name);
        tvAuthor.setText(author);
    }

    @Override
    protected void bindEvent() {
        iflContent.setOnClickListener(v -> finish());

        llShelfZg.setOnClickListener(v -> {
            if(mPresenter.getInBookShelf() && mPresenter.getBookShelf().getGroup() == 0){
                mPresenter.removeFromBookShelf();
                changeGroup(-1);
            }else {
                mPresenter.getBookShelf().setGroup(0);
                mPresenter.addToBookShelf();
                changeGroup(0);
            }
        });

        llShelfYf.setOnClickListener(v -> {
            if(mPresenter.getInBookShelf() && mPresenter.getBookShelf().getGroup() == 1){
                mPresenter.removeFromBookShelf();
                changeGroup(-1);
            }else {
                mPresenter.getBookShelf().setGroup(1);
                mPresenter.addToBookShelf();
                changeGroup(1);
            }
        });

        tvChangeOrigin.setOnClickListener(view -> changeSource());

        llRead.setOnClickListener(v -> {
            //进入阅读
            Intent intent = new Intent(BookDetailActivity.this, ReadBookActivity.class);
            intent.putExtra("openFrom", ReadBookPresenterImpl.OPEN_FROM_APP);
            String key = String.valueOf(System.currentTimeMillis());
            intent.putExtra("data_key", key);

            try {
                BitIntentDataManager.getInstance().putData(key, mPresenter.getBookShelf().clone());
            } catch (CloneNotSupportedException e) {
                BitIntentDataManager.getInstance().putData(key, mPresenter.getBookShelf());
                e.printStackTrace();
            }
            startActivityByAnim(intent, android.R.anim.fade_in, android.R.anim.fade_out);

            finish();
        });

        ivRefresh.setOnClickListener(view -> {
            AnimationSet animationSet = new AnimationSet(true);
            RotateAnimation rotateAnimation = new RotateAnimation(0, 360,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f);
            rotateAnimation.setDuration(1000);
            animationSet.addAnimation(rotateAnimation);
            ivRefresh.startAnimation(animationSet);
            showLoading(true);
            mPresenter.getBookShelfInfo();
        });

        ivCover.setOnClickListener(view -> {
            if (mPresenter.getOpenFrom() == FROM_BOOKSHELF) {
                BookInfoActivity.startThis(this, mPresenter.getBookShelf().getNoteUrl());
            }
        });

        tvAuthor.setOnClickListener(view -> {
            if (!AppActivityManager.getInstance().isExist(SearchBookActivity.class)) {
                SearchBookActivity.startByKey(this, tvAuthor.getText().toString());
            } else {
                RxBus.get().post(RxBusTag.SEARCH_BOOK, tvAuthor.getText().toString());
            }
            finish();
        });

        tvName.setOnClickListener(view -> {
            if (!AppActivityManager.getInstance().isExist(SearchBookActivity.class)) {
                SearchBookActivity.startByKey(this, tvName.getText().toString());
            } else {
                RxBus.get().post(RxBusTag.SEARCH_BOOK, tvName.getText().toString());
            }
            finish();
        });
    }

    private void changeSource() {
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "网络不可用，无法换源!", Toast.LENGTH_SHORT).show();
            return;
        }
        moProgressHUD.showChangeSource(this, mPresenter.getBookShelf(),
                searchBookBean -> {
                    tvOrigin.setText(getString(R.string.origin_format, searchBookBean.getOrigin()));
                    showLoading(true);
                    if (mPresenter.getInBookShelf()) {
                        mPresenter.changeBookSource(searchBookBean);
                    } else {
                        mPresenter.initBookFormSearch(searchBookBean);
                        mPresenter.getBookShelfInfo();
                    }
                });
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, android.R.anim.fade_out);
    }
}