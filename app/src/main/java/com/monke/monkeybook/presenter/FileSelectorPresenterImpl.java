package com.monke.monkeybook.presenter;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;

import androidx.fragment.app.Fragment;

import com.monke.basemvplib.BasePresenterImpl;
import com.monke.monkeybook.bean.FileSnapshot;
import com.monke.monkeybook.bean.RipeFile;
import com.monke.monkeybook.help.ACache;
import com.monke.monkeybook.presenter.contract.FileSelectorContract;
import com.monke.monkeybook.utils.FileUtil;
import com.monke.monkeybook.utils.RxUtils;
import com.monke.monkeybook.utils.StringUtils;

import java.io.File;
import java.io.FileFilter;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;

import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.disposables.Disposable;
import okhttp3.internal.cache.DiskLruCache;

public class FileSelectorPresenterImpl extends BasePresenterImpl<FileSelectorContract.View> implements FileSelectorContract.Presenter, FileFilter {

    private int orderIndex = 0;
    private String[] suffixes;
    private boolean isSingleChoice;
    private boolean checkBookAdded;
    private boolean isImage;
    private String title;

    private String key;

    private boolean sortChanged;
    private FileSnapshot current;
    private FileSnapshot root;

    private final Stack<FileSnapshot> snapshots = new Stack<>();

    private final Collator collator = Collator.getInstance(java.util.Locale.CHINA);

    @Override
    public void init(Fragment fragment) {
        Bundle bundle = fragment.getArguments();
        assert bundle != null;
        title = bundle.getString("title");
        isSingleChoice = bundle.getBoolean("isSingleChoice");
        checkBookAdded = bundle.getBoolean("checkBookAdded");
        isImage = bundle.getBoolean("isImage");
        ArrayList<String> list = bundle.getStringArrayList("suffixes");
        assert list != null;
        suffixes = new String[list.size()];
        list.toArray(suffixes);

        key = StringUtils.join(",", list);
    }

    @Override
    public Comparator<RipeFile> sort(int orderIndex) {
        if (this.orderIndex != orderIndex) {
            this.orderIndex = orderIndex;
            sortChanged = true;
        }

        return new FileComparator(orderIndex);
    }

    @SuppressLint("SdCardPath")
    @Override
    public void startLoad() {
        mView.showLoading();

        Single.create((SingleOnSubscribe<FileSnapshot>) emitter -> {
            loadRoot();


            List<FileSnapshot> snapshotList = null;
            try {
                snapshotList = (List<FileSnapshot>) ACache.get(mView.getContext()).getAsObject(key);
            } catch (Exception ignore) {
            }

            if (snapshotList != null && !snapshotList.isEmpty()) {
                snapshots.addAll(snapshotList);
                FileSnapshot old = snapshots.pop();
                current = loadFolder(old.getParent());
                if(current != null) {
                    current.setScrollOffset(old.getScrollOffset());
                }else {
                    current = old;
                }
            } else if (root != null) {
                current = root;
            }

            if (current != null) {
                emitter.onSuccess(current);
            } else {
                emitter.onError(new Exception("file load failed!"));
            }
        }).compose(RxUtils::toSimpleSingle)
                .subscribe(new SingleObserver<FileSnapshot>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onSuccess(FileSnapshot snapshot) {
                        mView.onShow(snapshot, true);
                    }

                    @Override
                    public void onError(Throwable e) {
                        mView.hideLoading();
                    }
                });
    }

    @Override
    public void pop() {
        if (!snapshots.empty()) {
            current = snapshots.pop();
            if (current != null) {
                if (sortChanged) {
                    sortFiles(current.getFiles(), orderIndex);
                }
                mView.onShow(current, true);
            } else if (root != null) {
                mView.onShow(current = root, true);
            }
        }
    }

    @Override
    public void push(RipeFile folder, int offset) {
        final FileSnapshot next =  loadFolder(folder);
        if(next != null){
            current.setScrollOffset(offset);
            snapshots.push(current);
            current = next;
            mView.onShow(current, false);
        }
    }

    @Override
    public boolean canGoBack() {
        return !snapshots.empty();
    }

    @Override
    public boolean isSingleChoice() {
        return isSingleChoice;
    }

    @Override
    public boolean checkBookAdded() {
        return checkBookAdded;
    }

    @Override
    public boolean isImage() {
        return isImage;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public boolean accept(java.io.File pathname) {
        String fileName = pathname.getName();
        if (fileName.startsWith(".")) {
            return false;
        }
        //文件夹内部数量为0
        if (pathname.isDirectory() && pathname.list().length == 0) {
            return false;
        }

        if (pathname.isDirectory()) {
            return true;
        }

        for (String suffix : suffixes) {
            if (fileName.toUpperCase().endsWith("." + suffix.toUpperCase())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void detachView() {
        if (!snapshots.empty()) {
            if (current != null) {
                current.setScrollOffset(mView.getScrollOffset());
                snapshots.push(current);
            }
        } else if (root != null) {
            root.setScrollOffset(mView.getScrollOffset());
            snapshots.push(root);
        }
        ACache.get(mView.getContext()).put(key, snapshots);
    }

    private void loadRoot() {
        List<String> list = FileUtil.getStorageData(mView.getContext());
        if (list != null) {
            root = new FileSnapshot();
            RipeFile parent = new RipeFile();
            parent.setPath("/");
            root.setParent(parent);
            List<RipeFile> fileList = new ArrayList<>();
            for (String path : list) {
                RipeFile file = new RipeFile();
                file.setFile(new File(path));
                fileList.add(file);
            }
            root.setFiles(fileList);
        }
    }

    private FileSnapshot loadFolder(RipeFile folder) {
        if (folder.isDirectory()) {
            File[] files = folder.getFile().listFiles(FileSelectorPresenterImpl.this);
            if (files != null && files.length > 0) {
                FileSnapshot snapshot = new FileSnapshot();
                snapshot.setParent(folder);
                List<RipeFile> fileList = new ArrayList<>();
                RipeFile ripeFile;
                for (File file : files) {
                    ripeFile = new RipeFile();
                    ripeFile.setFile(file);
                    fileList.add(ripeFile);
                }
                sortFiles(fileList, orderIndex);
                snapshot.setFiles(fileList);
                return snapshot;
            }
        }
        return null;
    }

    private void sortFiles(List<RipeFile> files, int orderIndex) {
        if (files != null) {
            Collections.sort(files, new FileComparator(orderIndex));
            sortChanged = false;
        }
    }

    private class FileComparator implements Comparator<RipeFile> {

        int orderIndex;

        FileComparator(int orderIndex) {
            this.orderIndex = orderIndex;
        }

        @Override
        public int compare(RipeFile file1, RipeFile file2) {
            File o1 = file1.getFile();
            File o2 = file2.getFile();
            if (o1.isDirectory() && o2.isFile()) {
                return -1;
            }
            if (o2.isDirectory() && o1.isFile()) {
                return 1;
            }
            if (orderIndex == 0) {
                return collator.compare(o1.getName(), o2.getName());
            } else if (orderIndex == 1) {
                return collator.compare(o2.getName(), o1.getName());
            } else if (orderIndex == 2) {
                return Long.compare(o1.lastModified(), o2.lastModified());
            } else if (orderIndex == 3) {
                return Long.compare(o2.lastModified(), o1.lastModified());
            } else if (orderIndex == 4) {
                return Long.compare(o1.length(), o2.length());
            }
            return Long.compare(o2.length(), o1.length());
        }
    }
}
