//Copyright (c) 2017. 章钦豪. All rights reserved.
package com.monke.monkeybook.view.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.navigation.NavigationView;
import com.monke.monkeybook.MApplication;
import com.monke.monkeybook.R;
import com.monke.monkeybook.base.MBaseActivity;
import com.monke.monkeybook.bean.BookShelfBean;
import com.monke.monkeybook.presenter.MainPresenterImpl;
import com.monke.monkeybook.presenter.contract.MainContract;
import com.monke.monkeybook.service.AudioBookPlayService;
import com.monke.monkeybook.utils.KeyboardUtil;
import com.monke.monkeybook.view.adapter.base.OnBookItemClickListenerTwo;
import com.monke.monkeybook.view.fragment.BookListFragment;
import com.monke.monkeybook.view.fragment.FileSelector;
import com.monke.monkeybook.widget.AppCompat;
import com.monke.monkeybook.widget.BookFloatingActionMenu;
import com.monke.monkeybook.widget.BookShelfSearchView;
import com.monke.monkeybook.widget.ScrimInsetsFrameLayout;
import com.monke.monkeybook.widget.modialog.MoDialogHUD;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends MBaseActivity<MainContract.Presenter> implements MainContract.View, BookListFragment.IConfigGetter {
    private static final int BACKUP_RESULT = 11;
    private static final int RESTORE_RESULT = 12;
    private static final int FILE_SELECT_RESULT = 13;

    private static final int[] BOOK_GROUPS = {R.string.item_group_zg, R.string.item_group_yf, R.string.item_group_wj, R.string.item_group_bd};

    @BindView(R.id.layout_container)
    ScrimInsetsFrameLayout container;
    @BindView(R.id.book_list_frame)
    FrameLayout frameContent;
    @BindView(R.id.drawer)
    DrawerLayout drawer;
    @BindView(R.id.navigation_view)
    NavigationView drawerLeft;
    @BindView(R.id.appBar)
    View appBar;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.bookshelf_search_view)
    BookShelfSearchView drawerRight;
    @BindView(R.id.book_shelf_menu)
    BookFloatingActionMenu bookShelfMenu;

    private Switch swNightTheme;
    private int group = -1;
    private boolean viewIsList;
    private MoDialogHUD moDialogHUD;
    private long exitTime = 0;
    private boolean isRecreate;

    private BookListFragment[] fragments = new BookListFragment[4];

    @Override
    protected MainContract.Presenter initInjector() {
        return new MainPresenterImpl();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            FragmentManager manager = getSupportFragmentManager();
            fragments[0] = (BookListFragment) manager.findFragmentByTag(getString(BOOK_GROUPS[0]));
            fragments[1] = (BookListFragment) manager.findFragmentByTag(getString(BOOK_GROUPS[1]));
            fragments[2] = (BookListFragment) manager.findFragmentByTag(getString(BOOK_GROUPS[2]));
            fragments[3] = (BookListFragment) manager.findFragmentByTag(getString(BOOK_GROUPS[3]));

            for (BookListFragment fragment : fragments) {
                if (fragment != null) {
                    fragment.setConfigGetter(this);
                    fragment.setItemClickListenerTwo(getAdapterListener());
                }
            }
        } else {
            showFragment(this.group);
        }
    }

    @Override
    protected void onCreateActivity() {
        setContentView(R.layout.activity_main);
    }

    /**
     * 沉浸状态栏
     */
    @Override
    public void initImmersionBar() {
        super.initImmersionBar();
    }

    @Override
    protected void initData() {
        viewIsList = getPreferences().getBoolean("bookshelfIsList", true);
        group = getPreferences().getInt("shelfGroup", 0);
        isRecreate = getIntent().getBooleanExtra("isRecreate", false);
        getIntent().putExtra("isRecreate", false);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (bookShelfMenu.isExpanded()) {
            Rect rect = new Rect();
            bookShelfMenu.getGlobalVisibleRect(rect);
            if (!rect.contains((int) ev.getX(), (int) ev.getY())) {
                bookShelfMenu.collapse();
                return true;
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void bindView() {
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);
        setupActionBar();
        initDrawer();
        upGroup(group);

        bookShelfMenu.setSelection(group);

        container.setOnInsetsCallback(insets -> {
            appBar.setPadding(0, insets.top, 0, 0);
            drawerLeft.getHeaderView(0).setPadding(0, insets.top, 0, 0);
            drawerRight.applyWindowInsets(insets);
        });

        moDialogHUD = new MoDialogHUD(this);
    }

    @Override
    protected void firstRequest() {
        if (!EasyPermissions.hasPermissions(this, MApplication.PerList)) {
            ActivityCompat.requestPermissions(this, MApplication.PerList, MApplication.RESULT_PERMS);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // 这个必须要，没有的话进去的默认是个箭头。。正常应该是三横杠的
        if (swNightTheme != null) {
            swNightTheme.setChecked(isNightTheme());
        }
    }

    @Override
    protected void bindEvent() {
        drawerRight.setOnItemClickListener(getAdapterListener());
        drawerRight.setIQuery(query -> mPresenter.queryBooks(query));

        bookShelfMenu.setOnActionMenuClickListener((index, menuView) -> {
            bookShelfMenu.postDelayed(() -> upGroup(index), 400L);
        });

        versionUpRun();
    }

    private OnBookItemClickListenerTwo getAdapterListener() {
        return new OnBookItemClickListenerTwo() {
            @Override
            public void onClick(View view, BookShelfBean bookShelf) {
                KeyboardUtil.hideKeyboard(drawerRight.getSearchAutoComplete(false));
                if (mPresenter.checkLocalBookNotExists(bookShelf)) {
                    moDialogHUD.showTwoButton(getString(R.string.delete_bookshelf_not_exist_s),
                            getString(R.string.ok),
                            v -> mPresenter.removeFromBookShelf(bookShelf),
                            getString(R.string.cancel), null);
                } else {
                    ReadBookActivity.startThis(MainActivity.this, bookShelf, true);
                }
            }

            @Override
            public void onLongClick(View view, BookShelfBean bookShelf) {
                KeyboardUtil.hideKeyboard(drawerRight.getSearchAutoComplete(false));
                if (mPresenter.checkLocalBookNotExists(bookShelf)) {
                    moDialogHUD.showTwoButton(getString(R.string.delete_bookshelf_not_exist_s),
                            getString(R.string.ok),
                            v -> mPresenter.removeFromBookShelf(bookShelf),
                            getString(R.string.cancel), null);
                } else {
                    BookDetailActivity.startThis(MainActivity.this, bookShelf);
                }
            }

        };
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem pauseMenu = menu.findItem(R.id.action_list_grid);
        if (viewIsList) {
            pauseMenu.setIcon(R.drawable.ic_view_grid_black_24dp);
            pauseMenu.setTitle(R.string.action_grid);
        } else {
            pauseMenu.setIcon(R.drawable.ic_view_list_black_24dp);
            pauseMenu.setTitle(R.string.action_list);
        }
        AppCompat.setTint(pauseMenu, getResources().getColor(R.color.menu_color_default));
        return super.onPrepareOptionsMenu(menu);
    }

    // 添加菜单
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * 菜单事件
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        SharedPreferences.Editor editor = getPreferences().edit();
        int id = item.getItemId();
        switch (id) {
            case R.id.action_search:
                //点击搜索
                startActivityByAnim(new Intent(this, SearchBookActivity.class), toolbar, "sharedView");
                break;
            case R.id.action_library:
                startActivity(new Intent(this, FindBookActivity.class));
                break;
            case R.id.action_audio:
                startActivity(new Intent(this, AudioBookActivity.class));
                break;
            case R.id.action_add_local:
                if (EasyPermissions.hasPermissions(this, MApplication.PerList)) {
                    fileSelectResult();
                } else {
                    EasyPermissions.requestPermissions(this, "添加本地书籍",
                            FILE_SELECT_RESULT, MApplication.PerList);
                }
                break;
            case R.id.action_add_url:
                moDialogHUD.showInputBox("添加书籍网址", null, null, inputText -> mPresenter.addBookUrl(inputText));
                break;
            case R.id.action_list_grid:
                viewIsList = !viewIsList;
                editor.putBoolean("bookshelfIsList", viewIsList);
                if (editor.commit()) {
                    for (BookListFragment fragment : fragments) {
                        if (fragment != null) {
                            fragment.updateLayoutType(viewIsList);
                        }
                    }
                }
                break;
            case R.id.action_clearCaches:
                moDialogHUD.showTwoButton(getString(R.string.clean_caches_s),
                        getString(R.string.ok),
                        v -> mPresenter.cleanCaches(),
                        getString(R.string.cancel), null);
                break;
            case R.id.action_clearBookshelf:
                moDialogHUD.showTwoButton(getString(R.string.clear_bookshelf_s),
                        getString(R.string.ok),
                        v -> mPresenter.clearBookshelf(),
                        getString(R.string.cancel), null);
                break;
            case R.id.action_refreshBookshelf:
                BookListFragment current = fragments[this.group];
                if (current != null) {
                    current.refreshBookShelf(true);
                }
                break;
            case android.R.id.home:
                if (drawer.isDrawerOpen(GravityCompat.START)) {
                    drawer.closeDrawers();
                } else {
                    drawer.openDrawer(GravityCompat.START);
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    //设置ToolBar
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationIcon(R.drawable.ic_menu_black_24dp);
        AppCompat.setToolbarNavIconTint(toolbar, getResources().getColor(R.color.menu_color_default));
    }

    //初始化侧边栏
    private void initDrawer() {
        drawer.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerClosed(View drawerView) {
                if (drawerView == drawerRight) {
                    return;
                }
                super.onDrawerClosed(drawerView);
            }

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                if (drawerView == drawerRight) {
                    if (slideOffset > 0) {
                        KeyboardUtil.hideKeyboard(getCurrentFocus());
                    }
                    return;
                }
                super.onDrawerSlide(drawerView, slideOffset);

            }

            @Override
            public void onDrawerOpened(View drawerView) {
                if (drawerView == drawerRight) {
                    KeyboardUtil.showKeyboard(drawerRight.getSearchAutoComplete(true));
                    return;
                }
                super.onDrawerOpened(drawerView);
            }
        });

        setUpNavigationView();
    }

    private void upGroup(int group) {
        if (this.group != group) {
            showFragment(group);

            getPreferences().edit().putInt("shelfGroup", group).apply();

            this.group = group;
        }
    }

    private void showFragment(int group) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        BookListFragment from = fragments[this.group];
        BookListFragment to = fragments[group];
        if (from != null) {
            transaction.hide(from);
        }

        if (to == null) {
            to = fragments[group] = BookListFragment.newInstance(group);
            to.setConfigGetter(this);
            to.setItemClickListenerTwo(getAdapterListener());
        }

        if (!to.isAdded()) {
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .add(R.id.book_list_frame, to, getString(BOOK_GROUPS[group]))
                    .show(to)
                    .commitAllowingStateLoss();
        } else if (to.isSupportHidden()) {
            transaction.setTransition(this.group > group ? FragmentTransaction.TRANSIT_FRAGMENT_OPEN : FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                    .show(to)
                    .commitAllowingStateLoss();
        }
    }

    //侧边栏按钮
    private void setUpNavigationView() {
        AppCompat.useCustomNavigationViewDivider(drawerLeft);
        Menu drawerMenu = drawerLeft.getMenu();
        swNightTheme = drawerMenu.findItem(R.id.action_night_theme).getActionView().findViewById(R.id.sw_night_theme);
        swNightTheme.setChecked(isNightTheme());
        swNightTheme.setOnCheckedChangeListener((compoundButton, b) -> {
            if (compoundButton.isPressed()) {
                setNightTheme(b);
            }
        });
        drawerLeft.setNavigationItemSelectedListener(menuItem -> {
            switch (menuItem.getItemId()) {
                case R.id.action_download:
                    new Handler().postDelayed(() -> DownloadActivity.startThis(this), 220L);
                    break;
                case R.id.action_book_source_manage:
                    new Handler().postDelayed(() -> BookSourceActivity.startThis(this), 220L);
                    break;
                case R.id.action_replace_rule:
                    new Handler().postDelayed(() -> ReplaceRuleActivity.startThis(this), 220L);
                    break;
                case R.id.action_setting:
                    new Handler().postDelayed(() -> SettingActivity.startThis(this), 220L);
                    break;
                case R.id.action_about:
                    new Handler().postDelayed(() -> AboutActivity.startThis(this), 220L);
                    break;
                case R.id.action_donate:
                    new Handler().postDelayed(() -> DonateActivity.startThis(this), 220L);
                    break;
                case R.id.action_backup:
                    backup();
                    break;
                case R.id.action_restore:
                    restore();
                    break;
                case R.id.action_night_theme:
                    getIntent().putExtra("isRecreate", true);
                    swNightTheme.setChecked(!isNightTheme());
                    setNightTheme(!isNightTheme());
                    break;
            }
            if (menuItem.getItemId() != R.id.action_night_theme) {
                drawer.closeDrawers();
            }
            return true;
        });
    }

    //备份
    private void backup() {
        if (EasyPermissions.hasPermissions(this, MApplication.PerList)) {
            moDialogHUD.showTwoButton(getString(R.string.backup_message),
                    getString(R.string.ok),
                    v -> mPresenter.backupData(),
                    getString(R.string.cancel), null);
        } else {
            EasyPermissions.requestPermissions(this, getString(R.string.backup_permission),
                    BACKUP_RESULT, MApplication.PerList);
        }
    }

    @AfterPermissionGranted(BACKUP_RESULT)
    private void backupResult() {
        backup();
    }

    //恢复
    private void restore() {
        if (EasyPermissions.hasPermissions(this, MApplication.PerList)) {
            moDialogHUD.showTwoButton(getString(R.string.restore_message),
                    getString(R.string.ok),
                    v -> mPresenter.restoreData(),
                    getString(R.string.cancel), null);
        } else {
            EasyPermissions.requestPermissions(this, getString(R.string.restore_permission),
                    RESTORE_RESULT, MApplication.PerList);
        }
    }

    @AfterPermissionGranted(RESTORE_RESULT)
    private void restoreResult() {
        restore();
    }

    @AfterPermissionGranted(FILE_SELECT_RESULT)
    private void fileSelectResult() {
        FileSelector.newInstance("选择文件", false, true, false, new String[]{"txt"}).show(this, new FileSelector.OnFileSelectedListener() {
            @Override
            public void onMultiplyChoice(List<String> paths) {
                mPresenter.importBooks(paths);
            }
        });
    }

    private void versionUpRun() {
        if (getPreferences().getInt("versionCode", 0) != MApplication.getVersionCode()) {
            //保存版本号
            SharedPreferences.Editor editor = getPreferences().edit();
            editor.putInt("versionCode", MApplication.getVersionCode());
            editor.apply();
        }
    }

    @Override
    public boolean isRecreate() {
        return isRecreate;
    }

    @Override
    public void clearBookshelf() {
        AudioBookPlayService.stop(this);
        drawerRight.clear();

        for (BookListFragment fragment : fragments) {
            if (fragment != null) {
                fragment.clearBookShelf();
            }
        }
    }

    @Override
    public void showQueryBooks(List<BookShelfBean> bookShelfBeans) {
        drawerRight.showQueryBooks(bookShelfBeans);
    }

    @Override
    public void updateBook(BookShelfBean bookShelfBean, boolean sort) {
        drawerRight.updateBookShelfIfNeed(bookShelfBean);
    }

    @Override
    public void addBookShelf(BookShelfBean bookShelfBean) {
        drawerRight.addBookShelfIfNeed(bookShelfBean);
    }

    @Override
    public void removeBookShelf(BookShelfBean bookShelfBean) {
        drawerRight.removeBookShelfIfNeed(bookShelfBean);
    }

    @Override
    public void dismissHUD() {
        moDialogHUD.dismiss();
    }

    @Override
    public void showLoading(String msg) {
        moDialogHUD.showLoading(msg);
    }

    @Override
    public void onRestore(String msg) {
        moDialogHUD.showLoading(msg);
    }

    @Override
    public void addSuccess(BookShelfBean bookShelfBean) {
        BookListFragment fragment = fragments[fragments.length - 1];
        if (fragment != null) {
            fragment.addBookShelf(bookShelfBean);
        }
        upGroup(bookShelfBean.getGroup());
        bookShelfMenu.setSelection(this.group);
    }

    @Override
    public void restoreSuccess() {
        for (BookListFragment fragment : fragments) {
            if (fragment != null) {
                fragment.refreshBookShelf(false);
            }
        }

        dismissHUD();
        initImmersionBar();
    }

    @Override
    protected View getSnackBarView() {
        return toolbar;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Boolean mo = moDialogHUD.onKeyDown(keyCode, event);
        if (mo) {
            return true;
        } else {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (bookShelfMenu.isExpanded()) {
                    bookShelfMenu.collapse();
                    return true;
                } else if (drawer.isDrawerOpen(GravityCompat.START)
                        || drawer.isDrawerOpen(GravityCompat.END)) {
                    drawer.closeDrawers();
                    return true;
                }
                exit();
                return true;
            }
            return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    public void recreate() {
        super.recreate();
    }

    public void exit() {
        if ((System.currentTimeMillis() - exitTime) > 2000) {
            showSnackBar("再按一次退出程序");
            exitTime = System.currentTimeMillis();
        } else {
            finish();
        }
    }

}