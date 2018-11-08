package com.monke.monkeybook.help;

import android.text.TextUtils;

import com.luhuiguo.chinese.ChineseUtils;
import com.monke.monkeybook.bean.BookShelfBean;
import com.monke.monkeybook.bean.ReplaceRuleBean;
import com.monke.monkeybook.model.ReplaceRuleManager;

import java.util.List;

public class ChapterContentHelp {

    /**
     * 转繁体
     */
    private static String toTraditional(int convert, String content) {
        switch (convert) {
            case 0:
                break;
            case 1:
                content = ChineseUtils.toSimplified(content);
                break;
            case 2:
                content = ChineseUtils.toTraditional(content);
                break;
        }
        return content;
    }

    /**
     * 替换净化
     */
    public static String replaceContent(BookShelfBean mBook, String content) {
        String allLine[] = content.split("\n\u3000\u3000");
        //替换
        List<ReplaceRuleBean> enabled = ReplaceRuleManager.getInstance().getEnabled();
        if (enabled != null && enabled.size() > 0) {
            StringBuilder contentBuilder = new StringBuilder();
            for (String line : allLine) {
                for (ReplaceRuleBean replaceRule : enabled) {
                    if (TextUtils.isEmpty(replaceRule.getUseTo()) || isUseTo(mBook, replaceRule.getUseTo())) {
                        try {
                            line = line.replaceAll(replaceRule.getRegex(), replaceRule.getReplacement());
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                    }
                }
                if (line.length() > 0) {
                    if (contentBuilder.length() == 0) {
                        contentBuilder.append(line);
                    } else {
                        contentBuilder.append("\n").append("\u3000\u3000").append(line);
                    }
                }
            }
            content = contentBuilder.toString();
            for (ReplaceRuleBean replaceRule : enabled) {
                if (TextUtils.isEmpty(replaceRule.getUseTo()) || isUseTo(mBook, replaceRule.getUseTo())) {
                    if (replaceRule.getRegex().contains("\\n")) {
                        try {
                            content = content.replaceAll(replaceRule.getRegex(), replaceRule.getReplacement());
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                    }
                }
            }
        }
        return toTraditional(ReadBookControl.getInstance().getTextConvert(), content);
    }

    private static boolean isUseTo(BookShelfBean mBook, String useTo) {
        return useTo.contains(mBook.getTag())
                || useTo.contains(mBook.getBookInfoBean().getName());
    }

}
