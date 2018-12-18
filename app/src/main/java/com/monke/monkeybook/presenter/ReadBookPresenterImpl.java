//Copyright (c) 2017. 章钦豪. All rights reserved.
package com.monke.monkeybook.presenter;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.hwangjr.rxbus.RxBus;
import com.hwangjr.rxbus.annotation.Subscribe;
import com.hwangjr.rxbus.annotation.Tag;
import com.hwangjr.rxbus.thread.EventThread;
import com.monke.basemvplib.BaseActivity;
import com.monke.basemvplib.BasePresenterImpl;
import com.monke.basemvplib.impl.IView;
import com.monke.monkeybook.BitIntentDataManager;
import com.monke.monkeybook.R;
import com.monke.monkeybook.base.observer.SimpleObserver;
import com.monke.monkeybook.bean.BookShelfBean;
import com.monke.monkeybook.bean.BookSourceBean;
import com.monke.monkeybook.bean.BookmarkBean;
import com.monke.monkeybook.bean.DownloadBookBean;
import com.monke.monkeybook.bean.SearchBookBean;
import com.monke.monkeybook.dao.BookSourceBeanDao;
import com.monke.monkeybook.dao.DbHelper;
import com.monke.monkeybook.help.BookShelfDataHolder;
import com.monke.monkeybook.help.BookshelfHelp;
import com.monke.monkeybook.help.ReadBookControl;
import com.monke.monkeybook.help.RxBusTag;
import com.monke.monkeybook.model.BookSourceManager;
import com.monke.monkeybook.model.WebBookModelImpl;
import com.monke.monkeybook.presenter.contract.ReadBookContract;
import com.monke.monkeybook.service.DownloadService;
import com.trello.rxlifecycle2.android.ActivityEvent;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class ReadBookPresenterImpl extends BasePresenterImpl<ReadBookContract.View> implements ReadBookContract.Presenter {

    private ReadBookControl readBookControl = ReadBookControl.getInstance();
    private boolean inBookShelf;
    private BookShelfBean bookShelf;

    private CompositeDisposable changeSourceDisp = new CompositeDisposable();

    public ReadBookPresenterImpl() {
    }

    @Override
    public void handleIntent(Intent intent) {
        boolean isRecreate = intent.getBooleanExtra("isRecreate", false);
        if (isRecreate && !intent.getBooleanExtra("fromUri", false)) {
            BookShelfDataHolder holder = BookShelfDataHolder.getInstance();
            bookShelf = holder.getBookShelf();
            if (bookShelf != null) {
                inBookShelf = holder.isInBookShelf();
                mView.prepareDisplay(false);
            } else {
                mView.finish();
            }
        } else {
            intent.putExtra("isRecreate", true).putExtra("fromUri", false);
            inBookShelf = intent.getBooleanExtra("inBookShelf", true);
            String key = intent.getStringExtra("data_key");
            bookShelf = (BookShelfBean) BitIntentDataManager.getInstance().getData(key);
            BitIntentDataManager.getInstance().cleanData(key);

            if (bookShelf == null) {
                mView.finish();
            } else {
                if (inBookShelf) {
                    readBookControl.setLastNoteUrl(bookShelf.getNoteUrl());
                }
                mView.prepareDisplay(true);
            }
        }
    }

    /**
     * 禁用当前书源
     */
    @Override
    public void disableDurBookSource() {
        try {
            switch (bookShelf.getTag()) {
                case BookShelfBean.LOCAL_TAG:
                    break;
                default:
                    BookSourceBean bookSource = DbHelper.getInstance().getmDaoSession().getBookSourceBeanDao().queryBuilder()
                            .where(BookSourceBeanDao.Properties.BookSourceUrl.eq(bookShelf.getTag())).unique();
                    bookSource.setEnable(false);
                    if (TextUtils.isEmpty(bookSource.getBookSourceGroup()))
                        bookSource.setBookSourceGroup("禁用");
                    mView.toast("已禁用" + bookSource.getBookSourceName());
                    BookSourceManager.getInstance().saveBookSource(bookSource);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void saveProgress() {
        if (bookShelf != null && inBookShelf) {
            Observable.create((ObservableOnSubscribe<BookShelfBean>) e -> {
                bookShelf.setFinalDate(System.currentTimeMillis());
                bookShelf.upDurChapterName();
                bookShelf.upLastChapterName();
                DbHelper.getInstance().getmDaoSession().getBookShelfBeanDao().insertOrReplace(bookShelf);
                e.onNext(bookShelf);
                e.onComplete();
            }).subscribeOn(Schedulers.io())
                    .subscribe(new SimpleObserver<BookShelfBean>() {
                        @Override
                        public void onNext(BookShelfBean value) {
                            RxBus.get().post(RxBusTag.UPDATE_BOOK_SHELF, bookShelf);
                        }

                        @Override
                        public void onError(Throwable e) {
                            e.printStackTrace();
                        }
                    });
        }
    }

    @Override
    public String getChapterTitle(int chapterIndex) {
        if (bookShelf.getChapterListSize() == 0) {
            return mView.getContext().getString(R.string.no_chapter);
        } else
            return bookShelf.getChapter(chapterIndex).getDurChapterName();
    }

    /**
     * 下载
     */
    @Override
    public void addDownload(int start, int end) {
        addToShelf(() -> {
            DownloadBookBean downloadBook = new DownloadBookBean();
            downloadBook.setTag(bookShelf.getTag());
            downloadBook.setName(bookShelf.getBookInfoBean().getName());
            downloadBook.setNoteUrl(bookShelf.getNoteUrl());
            downloadBook.setCoverUrl(bookShelf.getBookInfoBean().getCoverUrl());
            downloadBook.setStart(start);
            downloadBook.setEnd(end);
            downloadBook.setFinalDate(System.currentTimeMillis());
            DownloadService.addDownload(mView.getContext(), downloadBook);
        });
    }

    /**
     * 换源
     */
    @Override
    public void changeBookSource(SearchBookBean searchBook) {
        changeSourceDisp.clear();
        BookShelfBean bookShelfBean = BookshelfHelp.getBookFromSearchBook(searchBook);
        bookShelfBean.setSerialNumber(bookShelf.getSerialNumber());
        bookShelfBean.setLastChapterName(bookShelf.getLastChapterName());
        bookShelfBean.setDurChapterName(bookShelf.getDurChapterName());
        bookShelfBean.setDurChapter(bookShelf.getDurChapter());
        bookShelfBean.setDurChapterPage(bookShelf.getDurChapterPage());
        bookShelfBean.setGroup(bookShelf.getGroup());
        WebBookModelImpl.getInstance().getBookInfo(bookShelfBean)
                .flatMap(bookShelfBean1 -> WebBookModelImpl.getInstance().getChapterList(bookShelfBean1))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SimpleObserver<BookShelfBean>() {

                    @Override
                    public void onSubscribe(Disposable d) {
                        changeSourceDisp.add(d);
                    }

                    @Override
                    public void onNext(BookShelfBean bookShelfBean) {
                        bookShelfBean.setHasUpdate(false);

                        if (inBookShelf) {
                            saveChangedBook(bookShelfBean);
                        } else {
                            bookShelf = bookShelfBean;
                            mView.changeSourceFinish(true);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        mView.changeSourceFinish(false);
                    }
                });
    }

    @Override
    public void saveBookmark(BookmarkBean bookmarkBean) {
        Observable.create((ObservableOnSubscribe<BookmarkBean>) e -> {
            BookshelfHelp.saveBookmark(bookmarkBean);
            bookShelf.setBookmarkList(BookshelfHelp.queryBookmarkList(bookmarkBean.getBookName()));
            e.onNext(bookmarkBean);
            e.onComplete();
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();
    }

    @Override
    public void delBookmark(BookmarkBean bookmarkBean) {
        Observable.create((ObservableOnSubscribe<BookmarkBean>) e -> {
            BookshelfHelp.delBookmark(bookmarkBean);
            bookShelf.setBookmarkList(BookshelfHelp.queryBookmarkList(bookmarkBean.getBookName()));
            e.onNext(bookmarkBean);
            e.onComplete();
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();
    }

    /**
     * 保存换源后book
     */
    private void saveChangedBook(BookShelfBean bookShelfBean) {
        Observable.create((ObservableOnSubscribe<BookShelfBean>) e -> {
            BookshelfHelp.removeFromBookShelf(bookShelf);
            BookshelfHelp.saveBookToShelf(bookShelfBean);
            e.onNext(bookShelfBean);
            e.onComplete();
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SimpleObserver<BookShelfBean>() {

                    @Override
                    public void onSubscribe(Disposable d) {
                        changeSourceDisp.add(d);
                    }

                    @Override
                    public void onNext(BookShelfBean value) {
                        RxBus.get().post(RxBusTag.HAD_REMOVE_BOOK, bookShelf);
                        RxBus.get().post(RxBusTag.HAD_ADD_BOOK, value);
                        bookShelf = value;
                        mView.changeSourceFinish(true);
                    }

                    @Override
                    public void onError(Throwable e) {
                        mView.changeSourceFinish(false);
                    }
                });
    }

    @Override
    public boolean inBookShelf() {
        return inBookShelf;
    }

    @Override
    public BookShelfBean getBookShelf() {
        return bookShelf;
    }

    @Override
    public void checkBookInfo() {
        Observable.create((ObservableOnSubscribe<Boolean>) e -> {
            if (bookShelf.realChapterListEmpty()) {
                bookShelf.setChapterList(BookshelfHelp.queryChapterList(bookShelf.getNoteUrl()));
                if (!bookShelf.realChapterListEmpty()) {
                    bookShelf.upChapterListSize();
                }
            }
            if (bookShelf.realBookmarkListEmpty()) {
                bookShelf.setBookmarkList(BookshelfHelp.queryBookmarkList(bookShelf.getBookInfoBean().getName()));
            }
            bookShelf.setHasUpdate(false);
            e.onNext(true);
            e.onComplete();
        }).subscribeOn(Schedulers.single())
                .compose(((BaseActivity) mView.getContext()).bindUntilEvent(ActivityEvent.DESTROY))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SimpleObserver<Boolean>() {
                    @Override
                    public void onNext(Boolean value) {
                        mView.startLoadingBook();

                        if (value) {
                            saveProgress();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }
                });
    }

    @Override
    public void addToShelf(final OnAddListener addListener) {
        if (bookShelf != null) {
            Observable.create((ObservableOnSubscribe<Boolean>) e -> {
                BookshelfHelp.saveBookToShelf(bookShelf);
                e.onNext(true);
                e.onComplete();
            }).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new SimpleObserver<Boolean>() {
                        @Override
                        public void onNext(Boolean value) {
                            RxBus.get().post(RxBusTag.HAD_ADD_BOOK, bookShelf);
                            inBookShelf = true;
                            if (addListener != null)
                                addListener.addSuccess();
                        }

                        @Override
                        public void onError(Throwable e) {
                        }
                    });
        }
    }

    @Override
    public void removeFromShelf() {
        if (bookShelf != null) {
            Observable.create((ObservableOnSubscribe<Boolean>) e -> {
                BookshelfHelp.removeFromBookShelf(bookShelf);
                e.onNext(true);
                e.onComplete();
            }).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new SimpleObserver<Boolean>() {
                        @Override
                        public void onNext(Boolean aBoolean) {
                            RxBus.get().post(RxBusTag.HAD_REMOVE_BOOK, bookShelf);
                            inBookShelf = aBoolean;
                            mView.finish();
                        }

                        @Override
                        public void onError(Throwable e) {

                        }
                    });
        }
    }

    public interface OnAddListener {
        void addSuccess();
    }

    /////////////////////////////////////////////////

    @Override
    public void attachView(@NonNull IView iView) {
        super.attachView(iView);
        RxBus.get().register(this);
    }

    @Override
    public void detachView() {
        changeSourceDisp.dispose();
        RxBus.get().unregister(this);
    }

    /////////////////////RxBus////////////////////////

    @Subscribe(thread = EventThread.MAIN_THREAD, tags = {@Tag(RxBusTag.MEDIA_BUTTON)})
    public void onMediaButton(String command) {
        if (bookShelf != null) {
            mView.onMediaButton();
        }
    }

    @Subscribe(thread = EventThread.MAIN_THREAD, tags = {@Tag(RxBusTag.UPDATE_READ)})
    public void updateRead(Boolean recreate) {
        mView.refresh(recreate);
    }

    @Subscribe(thread = EventThread.MAIN_THREAD, tags = {@Tag(RxBusTag.ALOUD_STATE)})
    public void upAloudState(Integer state) {
        mView.upAloudState(state);
    }

    @Subscribe(thread = EventThread.MAIN_THREAD, tags = {@Tag(RxBusTag.ALOUD_MSG)})
    public void showMsg(String msg) {
        mView.toast(msg);
    }

    @Subscribe(thread = EventThread.MAIN_THREAD, tags = {@Tag(RxBusTag.ALOUD_INDEX)})
    public void speakIndex(Integer index) {
        mView.speakIndex(index);
    }

    @Subscribe(thread = EventThread.MAIN_THREAD, tags = {@Tag(RxBusTag.ALOUD_TIMER)})
    public void upAloudTimer(String timer) {
        mView.upAloudTimer(timer);
    }

    @Subscribe(thread = EventThread.MAIN_THREAD, tags = {@Tag(RxBusTag.UPDATE_BOOK_INFO)})
    public void updateTitle(BookShelfBean bookShelfBean) {
        mView.updateTitle(bookShelfBean.getBookInfoBean().getName());
    }
}
