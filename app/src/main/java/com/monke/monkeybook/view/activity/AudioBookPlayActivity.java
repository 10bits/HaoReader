package com.monke.monkeybook.view.activity;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.hwangjr.rxbus.RxBus;
import com.hwangjr.rxbus.annotation.Subscribe;
import com.hwangjr.rxbus.annotation.Tag;
import com.hwangjr.rxbus.thread.EventThread;
import com.monke.basemvplib.AppActivityManager;
import com.monke.basemvplib.impl.IPresenter;
import com.monke.monkeybook.R;
import com.monke.monkeybook.base.MBaseActivity;
import com.monke.monkeybook.bean.AudioPlayInfo;
import com.monke.monkeybook.bean.BookShelfBean;
import com.monke.monkeybook.bean.ChapterBean;
import com.monke.monkeybook.help.BitIntentDataManager;
import com.monke.monkeybook.help.BookshelfHelp;
import com.monke.monkeybook.help.RxBusTag;
import com.monke.monkeybook.service.AudioBookPlayService;
import com.monke.monkeybook.view.popupwindow.AudioChapterPop;
import com.monke.monkeybook.view.popupwindow.AudioTimerPop;
import com.monke.monkeybook.view.popupwindow.CheckAddShelfPop;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import jp.wasabeef.glide.transformations.BlurTransformation;

public class AudioBookPlayActivity extends MBaseActivity implements View.OnClickListener, AudioChapterPop.OnChapterSelectListener, AudioTimerPop.OnTimeSelectListener {

    @BindView(R.id.tv_title)
    TextView tvTitle;
    @BindView(R.id.iv_blur_cover)
    ImageView ivBlurCover;
    @BindView(R.id.iv_circle_cover)
    ImageView ivCover;
    @BindView(R.id.tv_progress)
    TextView tvProgress;
    @BindView(R.id.seekbar)
    SeekBar seekBar;
    @BindView(R.id.tv_duration)
    TextView tvDuration;
    @BindView(R.id.btn_timer)
    View btnTimer;
    @BindView(R.id.btn_previous)
    View btnPrevious;
    @BindView(R.id.btn_pause)
    ImageView btnPause;
    @BindView(R.id.btn_next)
    View btnNext;
    @BindView(R.id.btn_catalog)
    View btnCatalog;
    @BindView(R.id.loading_progress)
    ProgressBar progressBar;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("mm:ss", Locale.getDefault());

    private final Handler handler = new Handler(Looper.getMainLooper());

    private ValueAnimator animator;

    private AudioChapterPop audioChapterPop;
    private AudioTimerPop audioTimerPop;
    private CheckAddShelfPop checkAddShelfPop;

    private String noteUrl;

    public static void startThis(MBaseActivity activity, View transitionView, BookShelfBean bookShelf) {
        Intent intent = new Intent(activity, AudioBookPlayActivity.class);
        String key = String.valueOf(System.currentTimeMillis());
        intent.putExtra("data_key", key);
        BitIntentDataManager.getInstance().putData(key, bookShelf == null ? null : bookShelf.copy());
        if (transitionView != null) {
            activity.startActivityByAnim(intent, transitionView, transitionView.getTransitionName());
        } else {
            activity.startActivity(intent);
        }
    }

    @Override
    protected void initImmersionBar() {
        super.initImmersionBar();
        mImmersionBar.statusBarDarkFont(false);
        mImmersionBar.init();
    }

    @Override
    protected IPresenter initInjector() {
        return null;
    }

    @Override
    protected void onCreateActivity() {
        setContentView(R.layout.activity_audio_book_player);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        fromIntent(intent);
    }

    @Override
    protected void initData() {
    }

    @Override
    protected void bindView() {
        ButterKnife.bind(this);
        setCoverImage(null);
        setButtonEnabled(false);
        setMediaButtonEnabled(false);

        audioChapterPop = new AudioChapterPop(this, this);
        audioTimerPop = new AudioTimerPop(this, this);
    }

    @Override
    protected void bindEvent() {
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    AudioBookPlayService.seek(AudioBookPlayActivity.this, progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        btnTimer.setOnClickListener(this);
        btnPrevious.setOnClickListener(this);
        btnPause.setOnClickListener(this);
        btnNext.setOnClickListener(this);
        btnCatalog.setOnClickListener(this);
    }

    @Override
    protected void firstRequest() {
        RxBus.get().register(this);

        String key = getIntent().getStringExtra("data_key");
        BookShelfBean bookShelfBean = BitIntentDataManager.getInstance().getData(key, null);
        AudioBookPlayService.start(this, bookShelfBean);

        fromIntent(getIntent());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRotationAnim();
        RxBus.get().unregister(this);
        AudioBookPlayService.stopIfNotShelfBook(this);
    }

    @Override
    public void onBackPressed() {
        if (noteUrl != null) {
            if (!BookshelfHelp.isInBookShelf(noteUrl)) {
                showAddShelfPop(tvTitle.getText().toString());
                return;
            }
        }

        if (AppActivityManager.getInstance().isExist(AudioBookActivity.class)) {
            supportFinishAfterTransition();
        } else {
            finishByAnim(R.anim.anim_alpha_in, R.anim.anim_right_out);
        }
    }

    @Override
    public void onSelected(ChapterBean chapterBean) {
        AudioBookPlayService.play(this, chapterBean);
    }


    @Override
    public void onSelected(int timerMinute) {
        AudioBookPlayService.timer(this, timerMinute);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_pause:
                if (v.isSelected()) {
                    AudioBookPlayService.pause(this);
                } else {
                    AudioBookPlayService.resume(this);
                }
                break;
            case R.id.btn_previous:
                AudioBookPlayService.previous(this);
                break;
            case R.id.btn_next:
                AudioBookPlayService.next(this);
                break;

            case R.id.btn_catalog:
                audioChapterPop.showAtLocation(ivBlurCover, Gravity.BOTTOM, 0, 0);
                break;
            case R.id.btn_timer:
                audioTimerPop.showAtLocation(ivBlurCover, Gravity.BOTTOM, 0, 0);
                break;
        }
    }

    @Subscribe(thread = EventThread.MAIN_THREAD, tags = {@Tag(RxBusTag.AUDIO_PLAY)})
    public void onPlayEvent(AudioPlayInfo info) {
        String action = info.getAction();
        switch (action) {
            case AudioBookPlayService.ACTION_ATTACH:
                noteUrl = info.getNoteUrl();
                setTitle(info.getName());
                setCoverImage(info.getCover());
                break;
            case AudioBookPlayService.ACTION_PULL:
                setTitle(info.getName());
                setAlarmTimer(info.getTimerMinute());
                setCoverImage(info.getCover());
                setChapters(info.getChapterBeans(), info.getDurChapterIndex());
                setButtonEnabled(true);
                setMediaButtonEnabled(true);
                setProgress(info.getProgress(), info.getDuration());
                if (info.isPause()) {
                    setPause();
                } else {
                    setResume();
                }
                break;
            case AudioBookPlayService.ACTION_LOADING:
                showProgress(info.isLoading());
                break;
            case AudioBookPlayService.ACTION_START:
                setCoverImage(info.getCover());
                setChapters(info.getChapterBeans(), info.getDurChapterIndex());
                setButtonEnabled(true);
                setResume();
                break;
            case AudioBookPlayService.ACTION_PREPARE:
                updateIndex(info.getDurChapter().getDurChapterIndex());
                setTitle(info.getDurChapter().getDurChapterName());
                setMediaButtonEnabled(true);
                setProgress(0, 0);
                break;
            case AudioBookPlayService.ACTION_PAUSE:
                setPause();
                break;
            case AudioBookPlayService.ACTION_RESUME:
                setResume();
                break;
            case AudioBookPlayService.ACTION_PROGRESS:
                setProgress(info.getProgress(), info.getDuration());
                break;
            case AudioBookPlayService.ACTION_STOP:
                finish();
                break;
        }
    }

    private void setTitle(String title) {
        if (title != null) {
            tvTitle.setText(title);
        }
    }

    private void setProgress(int progress, int duration) {
        seekBar.setMax(duration);
        seekBar.setProgress(progress);
        tvProgress.setText(dateFormat.format(new Date(progress)));
        tvDuration.setText(dateFormat.format(new Date(duration)));
    }

    private void setChapters(List<ChapterBean> chapterBeans, int durChapter) {
        audioChapterPop.setDataSet(chapterBeans);
        audioChapterPop.upIndex(durChapter);
    }

    private void updateIndex(int durChapter) {
        audioChapterPop.upIndex(durChapter);
    }

    private void setAlarmTimer(int timer) {
        audioTimerPop.upIndexByValue(timer);
    }

    private void setPause() {
        btnPause.setSelected(false);
        btnPause.setImageResource(R.drawable.ic_play_circle_outline_black_24dp);
        stopRotationAnim();
    }

    private void setResume() {
        btnPause.setSelected(true);
        btnPause.setImageResource(R.drawable.ic_pause_circle_outline_black_24dp);
        startRotationAnim();
    }

    private void setButtonEnabled(boolean enabled) {
        btnTimer.setEnabled(enabled);
        btnCatalog.setEnabled(enabled);
    }

    private void setMediaButtonEnabled(boolean enabled) {
        btnPrevious.setEnabled(enabled);
        btnPause.setEnabled(enabled);
        btnNext.setEnabled(enabled);
        seekBar.setEnabled(enabled);
    }

    private void showProgress(boolean showProgress) {
        progressBar.setVisibility(showProgress ? View.VISIBLE : View.INVISIBLE);
    }

    private void startRotationAnim() {
        if (animator == null) {
            animator = ValueAnimator.ofFloat(0f, 360f);
            animator.addUpdateListener(animation -> ivCover.setRotation((Float) animation.getAnimatedValue()));
            animator.setDuration(30000);
            animator.setInterpolator(new LinearInterpolator());
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setStartDelay(1000);
            animator.start();
        } else {
            animator.resume();
        }
    }

    private void stopRotationAnim() {
        if (animator != null) {
            animator.pause();
        }
    }

    private void setCoverImage(String image) {
        Glide.with(AudioBookPlayActivity.this).load(image)
                .apply(new RequestOptions().dontAnimate().centerCrop()
                        .transforms(new CenterCrop(), new CircleCrop())
                        .error(R.drawable.img_cover_default)
                        .placeholder(R.drawable.img_cover_default)
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE))
                .into(ivCover);

        Glide.with(this).load(image)
                .apply(new RequestOptions()
                        .dontAnimate()
                        .centerCrop()
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                        .placeholder(R.drawable.img_cover_gs)
                        .error(R.drawable.img_cover_gs))
                .apply(RequestOptions.bitmapTransform(new BlurTransformation(25, 3)))
                .into(ivBlurCover);
    }

    private void showAddShelfPop(String bookName) {
        if (checkAddShelfPop == null) {
            checkAddShelfPop = new CheckAddShelfPop(this, bookName,
                    new CheckAddShelfPop.OnItemClickListener() {
                        @Override
                        public void clickExit() {
                            finish();
                        }

                        @Override
                        public void clickAddShelf() {
                            AudioBookPlayService.addShelf(AudioBookPlayActivity.this);
                            checkAddShelfPop.dismiss();
                        }
                    }, true);
        }
        if (!checkAddShelfPop.isShowing()) {
            checkAddShelfPop.showAtLocation(ivBlurCover, Gravity.CENTER, 0, 0);
        }
    }

    private void fromIntent(Intent intent) {
        boolean showTimer = intent.getBooleanExtra("showTimer", false);
        if (showTimer) {
            handler.postDelayed(() -> audioTimerPop.showAtLocation(ivBlurCover, Gravity.BOTTOM, 0, 0), 100L);
        }
    }
}
