//Copyright (c) 2017. 章钦豪. All rights reserved.
package com.monke.monkeybook.model;

import com.monke.monkeybook.bean.BookContentBean;
import com.monke.monkeybook.bean.BookShelfBean;
import com.monke.monkeybook.bean.ChapterListBean;
import com.monke.monkeybook.bean.SearchBookBean;
import com.monke.monkeybook.help.BookshelfHelp;
import com.monke.monkeybook.model.content.DefaultModelImpl;
import com.monke.monkeybook.model.impl.IStationBookModel;
import com.monke.monkeybook.model.impl.IWebBookModel;
import com.monke.monkeybook.model.source.My716;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.reactivex.Observable;
import io.reactivex.Scheduler;

public class WebBookModelImpl implements IWebBookModel {

    private WebBookModelImpl() {

    }

    private static class Holder {
        private static final WebBookModelImpl SINGLETON = new WebBookModelImpl();
    }

    public static WebBookModelImpl getInstance() {
        return Holder.SINGLETON;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 网络请求并解析书籍信息
     * return BookShelfBean
     */
    @Override
    public Observable<BookShelfBean> getBookInfo(BookShelfBean bookShelfBean) {
        IStationBookModel bookModel = getBookSourceModel(bookShelfBean.getTag());
        if (bookModel != null) {
            return bookModel.getBookInfo(bookShelfBean);
        } else {
            return null;
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 网络解析图书目录
     * return BookShelfBean
     */
    @Override
    public Observable<BookShelfBean> getChapterList(final BookShelfBean bookShelfBean) {
        IStationBookModel bookModel = getBookSourceModel(bookShelfBean.getTag());
        if (bookModel != null) {
            return bookModel.getChapterList(bookShelfBean)
                    .flatMap((chapterList) -> getChapterList(bookShelfBean, chapterList));
        } else {
            return Observable.error(new Throwable(bookShelfBean.getBookInfoBean().getName() + "没有书源"));
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 章节缓存
     */
    @Override
    public Observable<BookContentBean> getBookContent(Scheduler scheduler, ChapterListBean chapter) {
        IStationBookModel bookModel = getBookSourceModel(chapter.getTag());
        if (bookModel != null) {
            return bookModel.getBookContent(scheduler, chapter.getDurChapterUrl(), chapter.getDurChapterIndex())
                    .flatMap(bookContentBean -> saveChapterInfo(chapter, bookContentBean));
        } else
            return Observable.create(e -> {
                e.onNext(new BookContentBean());
                e.onComplete();
            });
    }

    /**
     * 其他站点集合搜索
     */
    @Override
    public Observable<List<SearchBookBean>> searchOtherBook(String content, int page, String tag) {
        //获取所有书源类
        IStationBookModel bookModel = getBookSourceModel(tag);
        if (bookModel != null) {
            return bookModel.searchBook(content, page);
        } else {
            return Observable.create(e -> {
                e.onNext(new ArrayList<>());
                e.onComplete();
            });
        }
    }

    /**
     * 发现页
     */
    @Override
    public Observable<List<SearchBookBean>> findBook(String url, int page, String tag) {
        IStationBookModel bookModel = getBookSourceModel(tag);
        if (bookModel != null) {
            return bookModel.findBook(url, page);
        } else {
            return Observable.create(e -> {
                e.onNext(new ArrayList<>());
                e.onComplete();
            });
        }
    }

    //获取book source class
    private IStationBookModel getBookSourceModel(String tag) {
        switch (tag) {
            case BookShelfBean.LOCAL_TAG:
                return null;
            case My716.TAG:
                return My716.getInstance();
            default:
                return DefaultModelImpl.newInstance(tag);
        }
    }

    private Observable<BookShelfBean> getChapterList(BookShelfBean bookShelfBean, List<ChapterListBean> chapterList) {
        return Observable.create(e -> {
            boolean findDurChapter = false;
            for (int i = 0, size = chapterList.size(); i < size; i++) {
                ChapterListBean chapter = chapterList.get(i);
                chapter.setBookName(bookShelfBean.getBookInfoBean().getName());
                chapter.setDurChapterIndex(i);
                chapter.setTag(bookShelfBean.getTag());
                chapter.setNoteUrl(bookShelfBean.getNoteUrl());
                if (!findDurChapter && Objects.equals(chapter.getDurChapterName(), bookShelfBean.getDurChapterName())) {
                    bookShelfBean.setDurChapter(i);
                    findDurChapter = true;
                }
            }
            if (bookShelfBean.getChapterListSize() < chapterList.size()) {
                bookShelfBean.setHasUpdate(true);
                bookShelfBean.setNewChapters(chapterList.size() - bookShelfBean.getChapterListSize());
                bookShelfBean.setFinalRefreshData(System.currentTimeMillis());
                bookShelfBean.getBookInfoBean().setFinalRefreshData(System.currentTimeMillis());
            } else {
                bookShelfBean.setNewChapters(0);
            }
            bookShelfBean.setFinalRefreshData(System.currentTimeMillis());
            bookShelfBean.setChapterList(chapterList);
            bookShelfBean.upChapterListSize();
            bookShelfBean.setDurChapter(Math.min(bookShelfBean.getDurChapter(), bookShelfBean.getChapterListSize() - 1));
            bookShelfBean.upDurChapterName();
            bookShelfBean.upLastChapterName();
            e.onNext(bookShelfBean);
            e.onComplete();
        });
    }

    private Observable<BookContentBean> saveChapterInfo(ChapterListBean chapter, BookContentBean bookContentBean) {
        return Observable.create(e -> {
            if (bookContentBean.getRight()) {
                bookContentBean.setNoteUrl(chapter.getNoteUrl());
                if (BookshelfHelp.saveChapterInfo(BookshelfHelp.getCachePathName(chapter),
                        BookshelfHelp.getCacheFileName(chapter.getDurChapterIndex(), chapter.getDurChapterName()),
                        bookContentBean.getDurChapterContent())) {
                    e.onNext(bookContentBean);
                    e.onComplete();
                    return;
                }
            }
            e.onError(new Throwable("保存章节出错"));
            e.onComplete();
        });
    }
}
