package com.monke.monkeybook.help;

import com.monke.monkeybook.model.annotation.BookType;
import com.monke.monkeybook.model.annotation.RuleType;
import com.monke.monkeybook.utils.FileUtil;

import java.io.File;

/**
 * Created by newbiechen on 17-4-16.
 */

public class Constant {

    //Book Date Convert Format
    public static final String FORMAT_BOOK_DATE = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String FORMAT_TIME = "HH:mm";
    public static final String FORMAT_FILE_DATE = "yyyy-MM-dd";

    //BookCachePath (因为getCachePath引用了Context，所以必须是静态变量，不能够是静态常量)
    public static String BOOK_CACHE_PATH = FileUtil.getSdCardPath() + File.separator
            + "YueDu" + File.separator + "chapters" + File.separator;
    public static String APP_CRASH_PATH = FileUtil.getSdCardPath() + File.separator
            + "YueDu" + File.separator + "crashes" + File.separator;


    public static final String[] BOOK_TYPES = {
            BookType.TEXT, BookType.AUDIO, BookType.DOWNLOAD
    };


    public static final String[] RULE_TYPES = {
            RuleType.DEFAULT, RuleType.XPATH, RuleType.JSON
    };

    public static final int GROUP_ZHUIGENG = 0;
    public static final int GROUP_YANGFEI = 1;
    public static final int GROUP_WANJIE = 2;
    public static final int GROUP_BENDI = 3;
    public static final int GROUP_AUDIO = 4;
}
