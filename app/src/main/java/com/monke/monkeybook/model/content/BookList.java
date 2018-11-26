package com.monke.monkeybook.model.content;

import com.monke.monkeybook.bean.BookSourceBean;
import com.monke.monkeybook.bean.SearchBookBean;
import com.monke.monkeybook.help.FormatWebText;
import com.monke.monkeybook.model.analyzeRule.AnalyzeElement;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import retrofit2.Response;

import static android.text.TextUtils.isEmpty;

public class BookList {
    private String tag;
    private String name;
    private BookSourceBean bookSourceBean;

    BookList(String tag, String name, BookSourceBean bookSourceBean) {
        this.tag = tag;
        this.name = name;
        this.bookSourceBean = bookSourceBean;
    }

    public Observable<List<SearchBookBean>> analyzeSearchBook(final Response<String> response) {
        return Observable.create(e -> {
            try {
                String baseURI;
                okhttp3.Response networkResponse = response.raw().networkResponse();
                if (networkResponse != null && networkResponse.request() != null) {
                    baseURI = networkResponse.request().url().toString();
                } else {
                    baseURI = response.raw().request().url().toString();
                }
                Document doc = null;
                if (response.body() != null) {
                    doc = Jsoup.parse(response.body());
                }
                Elements booksE = AnalyzeElement.getElements(doc, bookSourceBean.getRuleSearchList());
                if (null != booksE && booksE.size() > 0) {
                    List<SearchBookBean> books = new ArrayList<>();
                    for (int i = 0, size = booksE.size(); i < size; i++) {
                        SearchBookBean item = new SearchBookBean();
                        item.setTag(tag);
                        item.setOrigin(name);
                        AnalyzeElement analyzeElement = new AnalyzeElement(booksE.get(i), baseURI);
                        item.setAuthor(FormatWebText.getAuthor(analyzeElement.getResultContent(bookSourceBean.getRuleSearchAuthor())));
                        item.setKind(analyzeElement.getResultContent(bookSourceBean.getRuleSearchKind()));
                        item.setLastChapter(FormatWebText.trim(analyzeElement.getResultContent(bookSourceBean.getRuleSearchLastChapter())));
                        item.setName(FormatWebText.getBookName(analyzeElement.getResultContent(bookSourceBean.getRuleSearchName())));
                        item.setNoteUrl(analyzeElement.getResultUrl(bookSourceBean.getRuleSearchNoteUrl()));
                        if (isEmpty(item.getNoteUrl())) {
                            item.setNoteUrl(baseURI);
                        }
                        item.setCoverUrl(analyzeElement.getResultUrl(bookSourceBean.getRuleSearchCoverUrl()));
                        if (!isEmpty(item.getName())) {
                            books.add(item);
                        }
                    }
                    e.onNext(books);
                } else {
                    e.onNext(new ArrayList<>());
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                e.onNext(new ArrayList<>());
            }
            e.onComplete();
        });
    }
}
