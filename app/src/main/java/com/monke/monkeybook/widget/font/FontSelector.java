package com.monke.monkeybook.widget.font;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;

import com.monke.monkeybook.R;
import com.monke.monkeybook.help.FileHelp;
import com.monke.monkeybook.utils.FileUtil;

import java.io.File;

public class FontSelector {
    private AlertDialog.Builder builder;
    private FontAdapter adapter;
    private String fontPath;
    private OnThisListener thisListener;
    private AlertDialog alertDialog;

    public FontSelector(Context context, String selectPath) {
        builder = new AlertDialog.Builder(context);
        @SuppressLint("InflateParams") View view = LayoutInflater.from(context).inflate(R.layout.view_recycler_font, null);
        RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
        builder.setView(view);
        builder.setTitle(R.string.select_font);
        builder.setNegativeButton(R.string.cancel, null);
        fontPath = FileUtil.getSdCardPath() + "/YueDu/Fonts";
        adapter = new FontAdapter(context, selectPath,
                new OnThisListener() {
                    @Override
                    public void setDefault() {
                        if (thisListener != null) {
                            thisListener.setDefault();
                        }
                        alertDialog.dismiss();
                    }

                    @Override
                    public void setFontPath(String fontPath) {
                        if (thisListener != null) {
                            thisListener.setFontPath(fontPath);
                        }
                        alertDialog.dismiss();
                    }
                });
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
    }

    public FontSelector setListener(OnThisListener thisListener) {
        this.thisListener = thisListener;
        builder.setPositiveButton(R.string.default_font, ((dialogInterface, i) -> {
            thisListener.setDefault();
        }));
        return this;
    }

    public FontSelector setPath(String path) {
        fontPath = path;
        return this;
    }

    public FontSelector create() {
        adapter.upData(getFontFiles());
        builder.create();
        return this;
    }

    public void show() {
        alertDialog = builder.show();
    }

    private File[] getFontFiles() {
        try {
            File folder = FileHelp.getFolder(fontPath);
            return folder.listFiles(pathName -> pathName.getName().endsWith(".TTF") || pathName.getName().endsWith(".ttf"));
        } catch (Exception e) {
            return null;
        }
    }

    public interface OnThisListener {
        void setDefault();

        void setFontPath(String fontPath);
    }
}
