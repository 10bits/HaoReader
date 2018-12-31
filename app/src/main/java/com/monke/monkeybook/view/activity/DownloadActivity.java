package com.monke.monkeybook.view.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.appcompat.widget.Toolbar;

import android.view.Menu;
import android.view.MenuItem;

import com.monke.basemvplib.impl.IPresenter;
import com.monke.monkeybook.R;
import com.monke.monkeybook.base.MBaseActivity;
import com.monke.monkeybook.bean.DownloadBookBean;
import com.monke.monkeybook.service.DownloadService;
import com.monke.monkeybook.view.adapter.DownloadAdapter;
import com.monke.monkeybook.widget.refreshview.scroller.FastScrollRecyclerView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.monke.monkeybook.service.DownloadService.addDownloadAction;
import static com.monke.monkeybook.service.DownloadService.finishDownloadAction;
import static com.monke.monkeybook.service.DownloadService.obtainDownloadListAction;
import static com.monke.monkeybook.service.DownloadService.progressDownloadAction;
import static com.monke.monkeybook.service.DownloadService.removeDownloadAction;

public class DownloadActivity extends MBaseActivity {

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.recycler_view)
    FastScrollRecyclerView recyclerView;

    private DownloadAdapter adapter;

    private DownloadReceiver receiver;

    public static void startThis(Context context) {
        context.startActivity(new Intent(context, DownloadActivity.class));
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    protected void onDestroy() {
        if (receiver != null) {
            unregisterReceiver(receiver);
        }
        super.onDestroy();
    }

    /**
     * P层绑定   若无则返回null;
     */
    @Override
    protected IPresenter initInjector() {
        return null;
    }

    /**
     * 布局载入  setContentView()
     */
    @Override
    protected void onCreateActivity() {
        setContentView(R.layout.activity_recycler_vew);
        ButterKnife.bind(this);
        this.setSupportActionBar(toolbar);
        setupActionBar();

        receiver = new DownloadReceiver(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(addDownloadAction);
        filter.addAction(removeDownloadAction);
        filter.addAction(progressDownloadAction);
        filter.addAction(obtainDownloadListAction);
        filter.addAction(finishDownloadAction);
        registerReceiver(receiver, filter);
    }

    /**
     * 数据初始化
     */
    @Override
    protected void initData() {
        initRecyclerView();
    }

    private void initRecyclerView() {
        recyclerView.setFastScrollEnabled(false);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DownloadAdapter(this);
        recyclerView.setAdapter(adapter);
        recyclerView.setItemAnimator(null);

        DownloadService.obtainDownloadList(this);
    }

    //设置ToolBar
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.download_offline);
        }
    }

    // 添加菜单
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_book_download, menu);
        return super.onCreateOptionsMenu(menu);
    }

    //菜单
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_cancel:
                DownloadService.cancelDownload(this);
                break;
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private static class DownloadReceiver extends BroadcastReceiver {

        WeakReference<DownloadActivity> ref;

        public DownloadReceiver(DownloadActivity activity) {
            this.ref = new WeakReference<>(activity);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            DownloadAdapter adapter = ref.get().adapter;
            if (adapter == null || intent == null) {
                return;
            }
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case addDownloadAction:
                        DownloadBookBean downloadBook = intent.getParcelableExtra("downloadBook");
                        adapter.addData(downloadBook);
                        break;
                    case removeDownloadAction:
                        downloadBook = intent.getParcelableExtra("downloadBook");
                        adapter.removeData(downloadBook);
                        break;
                    case progressDownloadAction:
                        downloadBook = intent.getParcelableExtra("downloadBook");
                        adapter.upData(downloadBook);
                        break;
                    case finishDownloadAction:
                        adapter.upDataS(null);
                        break;
                    case obtainDownloadListAction:
                        ArrayList<DownloadBookBean> downloadBooks = intent.getParcelableArrayListExtra("downloadBooks");
                        adapter.upDataS(downloadBooks);
                        break;

                }
            }
        }
    }
}
