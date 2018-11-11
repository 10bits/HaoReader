package com.monke.monkeybook.widget.page;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.support.design.widget.Snackbar;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import com.monke.monkeybook.bean.BookShelfBean;
import com.monke.monkeybook.help.ReadBookControl;
import com.monke.monkeybook.view.activity.ReadBookActivity;
import com.monke.monkeybook.widget.animation.CoverPageAnim;
import com.monke.monkeybook.widget.animation.Direction;
import com.monke.monkeybook.widget.animation.HorizonPageAnim;
import com.monke.monkeybook.widget.animation.NonePageAnim;
import com.monke.monkeybook.widget.animation.PageAnimation;
import com.monke.monkeybook.widget.animation.SimulationPageAnim;
import com.monke.monkeybook.widget.animation.SlidePageAnim;

import java.lang.ref.WeakReference;


/**
 * 绘制页面显示内容的类
 */
public class PageView extends View {

    private WeakReference<ReadBookActivity> activity;

    private int mViewWidth = 0; // 当前View的宽
    private int mViewHeight = 0; // 当前View的高

    private int mStartX = 0;
    private int mStartY = 0;
    private boolean isMove = false;
    private int mPageIndex;
    private int mChapterIndex;
    // 是否允许点击
    private boolean canTouch = true;
    // 唤醒菜单的区域
    private RectF mCenterRect = null;
    private boolean isLayoutPrepared;
    // 动画类
    private PageAnimation mPageAnim;
    private boolean drawAfterComputeScroll = false;

    // 动画监听类
    private PageAnimation.OnPageChangeListener mPageAnimListener = new PageAnimation.OnPageChangeListener() {
        @Override
        public boolean hasPrev() {
            return PageView.this.hasPrevPage();
        }

        @Override
        public boolean hasNext() {
            return PageView.this.hasNextPage();
        }

        @Override
        public void pageCancel() {
            PageView.this.pageCancel();
        }
    };

    //点击监听
    private TouchListener mTouchListener;
    //内容加载器
    private PageLoader mPageLoader;

    private Snackbar mSnackbar;

    public PageView(Context context) {
        this(context, null);
    }

    public PageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mViewWidth = w;
        mViewHeight = h;

        isLayoutPrepared = true;

        if (mPageLoader != null) {
            mPageLoader.prepareDisplay(w, h);
        }
    }

    //设置翻页的模式
    void setPageMode(PageMode pageMode) {
        //视图未初始化的时候，禁止调用
        if (mViewWidth == 0 || mViewHeight == 0 || mPageLoader == null) return;
        switch (pageMode) {
            case SIMULATION:
                mPageAnim = new SimulationPageAnim(mViewWidth, mViewHeight, this, mPageAnimListener);
                break;
            case COVER:
                mPageAnim = new CoverPageAnim(mViewWidth, mViewHeight, this, mPageAnimListener);
                break;
            case SLIDE:
                mPageAnim = new SlidePageAnim(mViewWidth, mViewHeight, this, mPageAnimListener);
                break;
            case NONE:
                mPageAnim = new NonePageAnim(mViewWidth, mViewHeight, this, mPageAnimListener);
                break;
            default:
                mPageAnim = new SimulationPageAnim(mViewWidth, mViewHeight, this, mPageAnimListener);
        }
    }

    public ReadBookActivity getActivity() {
        return activity.get();
    }

    public Bitmap getContentBitmap() {
        if (mPageAnim == null) return null;
        return mPageAnim.getContentBitmap();
    }

    public Bitmap getBgBitmap() {
        if (mPageAnim == null) return null;
        return mPageAnim.getBgBitmap();
    }

    Direction getAnimDirection() {
        if (mPageAnim == null) return null;
        return mPageAnim.getDirection();
    }

    public boolean autoPrevPage() {
        startPageAnim(Direction.PREV);
        return true;
    }

    public boolean autoNextPage() {
        startPageAnim(Direction.NEXT);
        return true;
    }

    private void startPageAnim(Direction direction) {
        if (mTouchListener == null) return;
        //是否正在执行动画
        abortAnimation();
        if (direction == Direction.NEXT) {
            int x = mViewWidth;
            int y = mViewHeight;
            //初始化动画
            mPageAnim.setStartPoint(x, y);
            //设置点击点
            mPageAnim.setTouchPoint(x, y);
            //设置方向
            Boolean hasNext = hasNextPage();

            mPageAnim.setDirection(direction);
            if (!hasNext) {
                return;
            }
        } else {
            int x = 0;
            int y = mViewHeight;
            //初始化动画
            mPageAnim.setStartPoint(x, y);
            //设置点击点
            mPageAnim.setTouchPoint(x, y);
            mPageAnim.setDirection(direction);
            //设置方向方向
            Boolean hashPrev = hasPrevPage();
            if (!hashPrev) {
                return;
            }
        }
        mPageAnim.startAnim();
    }

    @Override
    protected void onDraw(Canvas canvas) {

        //绘制动画
        if (mPageAnim != null) {
            mPageAnim.draw(canvas);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);

        if (mPageAnim == null) {
            return true;
        }

        if (!canTouch && event.getAction() != MotionEvent.ACTION_DOWN) {
            return true;
        }

        int x = (int) event.getX();
        int y = (int) event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mStartX = x;
                mStartY = y;
                isMove = false;
                if (mTouchListener != null) {
                    canTouch = mTouchListener.onTouch();
                }
                if (!mPageLoader.isPageFrozen()) {
                    mPageAnim.onTouchEvent(event);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                // 判断是否大于最小滑动值。
                if (!isMove) {
                    isMove = Math.abs(mStartX - x) > 0 || Math.abs(mStartY - y) > 0;
                }

                if (!mPageLoader.isPageFrozen() && isMove) {
                    mPageAnim.onTouchEvent(event);
                }
                break;
            case MotionEvent.ACTION_UP:
                if (!isMove) {
                    //设置中间区域范围
                    if (mCenterRect == null) {
                        mCenterRect = new RectF(mViewWidth / 3, mViewHeight / 3,
                                mViewWidth * 2 / 3, mViewHeight * 2 / 3);
                    }

                    //是否点击了中间
                    if (mCenterRect.contains(x, y)) {
                        if (mTouchListener != null) {
                            mTouchListener.center();
                        }
                        return true;
                    }

                    if (!ReadBookControl.getInstance().getCanClickTurn()) {
                        return true;
                    }
                }
                if (!mPageLoader.isPageFrozen()) {
                    mPageAnim.onTouchEvent(event);
                }
                break;
        }
        return true;
    }

    /**
     * 判断是否存在上一页
     */
    private boolean hasPrevPage() {
        if (mPageLoader.prevPage()) {
            return true;
        } else {
            if (mSnackbar == null) {
                mSnackbar = Snackbar.make(this, "", Snackbar.LENGTH_SHORT);
            }

            if (!mSnackbar.isShown()) {
                mSnackbar.setText("没有上一页");
                mSnackbar.show();
            }
            return false;
        }
    }

    /**
     * 判断是否下一页存在
     */
    private boolean hasNextPage() {
        if (mPageLoader.nextPage()) {
            return true;
        } else {
            if (mSnackbar == null) {
                mSnackbar = Snackbar.make(this, "", Snackbar.LENGTH_SHORT);
            }

            if (!mSnackbar.isShown()) {
                mSnackbar.setText("没有下一页");
                mSnackbar.show();
            }
            return false;
        }
    }

    private void pageCancel() {
        mPageLoader.pageCancel();
    }

    @Override
    public void computeScroll() {
        //进行滑动
        if (mPageAnim != null) {
            mPageAnim.scrollAnim();
            if (mPageAnim.isStarted() && !mPageAnim.getScroller().computeScrollOffset()) {
                mPageAnim.resetAnim();
                if (drawAfterComputeScroll) {
                    drawCurrentPage();
                    drawAfterComputeScroll = false;
                }
                if (mPageLoader.getPagePosition() != mPageIndex | mPageLoader.getChapterPosition() != mChapterIndex) {
                    mPageLoader.dispatchPagingEndEvent();
                }
            }
        }

    }

    public void upPagePos(int chapterPos, int pagePos) {
        mChapterIndex = chapterPos;
        mPageIndex = pagePos;
    }

    //如果滑动状态没有停止就取消状态，重新设置Anim的触碰点
    public void abortAnimation() {
        mPageAnim.abortAnim();
    }

    public boolean isRunning() {
        return mPageAnim != null && mPageAnim.isRunning();
    }

    public boolean isStarted() {
        return mPageAnim != null && mPageAnim.isStarted();
    }

    public boolean isLayoutPrepared() {
        return isLayoutPrepared;
    }

    public void setTouchListener(TouchListener mTouchListener) {
        this.mTouchListener = mTouchListener;
    }

    /**
     * 绘制下一页
     */
    public void drawNextPage() {
        if (!isLayoutPrepared) return;

        if (mPageAnim instanceof HorizonPageAnim) {
            ((HorizonPageAnim) mPageAnim).changePage();
        }
        mPageLoader.drawPage(getContentBitmap());
    }

    /**
     * 绘制当前页。
     */
    public void drawCurrentPage() {
        drawCurrentPage(false);
    }

    public void postDrawCurrentPage() {
        drawAfterComputeScroll = true;
    }

    /**
     * 绘制当前页。
     */
    public void drawCurrentPage(boolean willNotDraw) {
        if (!isLayoutPrepared) return;
        if (mPageLoader != null) {
            mPageLoader.drawPage(getContentBitmap(), willNotDraw);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mPageAnim != null) {
            mPageAnim.abortAnim();
            mPageAnim.clear();
        }

        mPageLoader = null;
        mPageAnim = null;
    }

    /**
     * 获取 PageLoader
     */
    public PageLoader getPageLoader(ReadBookActivity activity, BookShelfBean collBook) {
        this.activity = new WeakReference<>(activity);

        if (TextUtils.equals(collBook.getTag(), BookShelfBean.LOCAL_TAG)) {
            mPageLoader = new LocalPageLoader(this, collBook);
        } else {
            mPageLoader = new NetPageLoader(this, collBook);
        }

        // 判断是否 PageView 已经初始化完成
        if (mViewWidth != 0 || mViewHeight != 0) {
            // 初始化 PageLoader 的屏幕大小
            mPageLoader.prepareDisplay(mViewWidth, mViewHeight);
        }

        return mPageLoader;
    }

    public interface TouchListener {
        boolean onTouch();

        void center();
    }

}