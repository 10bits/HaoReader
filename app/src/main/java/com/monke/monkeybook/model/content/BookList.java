package com.monke.monkeybook.model.content;

import com.monke.monkeybook.bean.BookSourceBean;
import com.monke.monkeybook.bean.SearchBookBean;
import com.monke.monkeybook.model.analyzeRule.AnalyzeConfig;
import com.monke.monkeybook.model.analyzeRule.AnalyzerFactory;
import com.monke.monkeybook.model.analyzeRule.OutAnalyzer;

import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import retrofit2.Response;

final class BookList {
    private final OutAnalyzer<?, ?> analyzer;

    BookList(String tag, String name, BookSourceBean bookSourceBean) {
        this.analyzer = AnalyzerFactory.create(bookSourceBean.getBookSourceRuleType(), new AnalyzeConfig()
                .tag(tag).name(name).bookSource(bookSourceBean));
    }

    Observable<List<SearchBookBean>> analyzeSearchBook(final Response<String> response) {
        return Observable.create((ObservableOnSubscribe<List<SearchBookBean>>) e -> {
            String baseURL;
            okhttp3.Response networkResponse = response.raw().networkResponse();
            if (networkResponse != null) {
                baseURL = networkResponse.request().url().toString();
            } else {
                baseURL = response.raw().request().url().toString();
            }

            analyzer.apply(analyzer.newConfig().baseURL(baseURL));

            List<SearchBookBean> searchBookBeans = analyzer.getSearchBooks(response.body());
            e.onNext(searchBookBeans);
            e.onComplete();
        }).onErrorReturnItem(Collections.emptyList());
    }
}
