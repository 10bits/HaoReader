package com.monke.monkeybook.presenter.contract;

import android.graphics.Bitmap;

import com.monke.basemvplib.impl.IPresenter;
import com.monke.basemvplib.impl.IView;
import com.monke.monkeybook.bean.BookSourceBean;

public interface SourceEditContract {
    interface Presenter extends IPresenter {

        void saveSource(BookSourceBean bookSource, BookSourceBean bookSourceOld);

        void copySource(BookSourceBean bookSourceBean);

        void pasteSource();

        void setText(String bookSourceStr);

        Bitmap encodeAsBitmap(String str);

        void analyzeBitmap(String path);
    }

    interface View extends IView {

        void setText(BookSourceBean bookSourceBean);

        String getBookSourceStr();

        void saveSuccess();

        void showSnackBar(String msg);
    }
}
