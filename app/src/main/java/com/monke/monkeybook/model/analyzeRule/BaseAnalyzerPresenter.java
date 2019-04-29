package com.monke.monkeybook.model.analyzeRule;

import androidx.annotation.NonNull;

import com.monke.monkeybook.model.SimpleModel;
import com.monke.monkeybook.utils.NetworkUtil;
import com.monke.monkeybook.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import retrofit2.Response;

import static android.text.TextUtils.isEmpty;

abstract class BaseAnalyzerPresenter<S> implements IAnalyzerPresenter, JavaExecutor {

    private final OutAnalyzer<S> mAnalyzer;

    BaseAnalyzerPresenter(OutAnalyzer<S> analyzer) {
        this.mAnalyzer = analyzer;
    }

    final OutAnalyzer<S> getAnalyzer() {
        return mAnalyzer;
    }

    final SourceParser<S> getParser() {
        return mAnalyzer.getParser();
    }

    final AnalyzeConfig getConfig() {
        return mAnalyzer.getConfig();
    }

    RulePatterns fromRule(String rawRule, boolean withVariableStore) {
        if (withVariableStore) {
            return RulePatterns.fromRule(rawRule, getConfig().getVariableStore());
        } else {
            return RulePatterns.fromRule(rawRule);
        }
    }


    RulePattern fromSingleRule(String rawRule, boolean withVariableStore) {
        if (withVariableStore) {
            return RulePattern.fromRule(rawRule, getConfig().getVariableStore());
        } else {
            return RulePattern.fromRule(rawRule);
        }
    }

    String evalStringScript(@NonNull String string, @NonNull RulePattern rulePattern) {
        if (!rulePattern.javaScripts.isEmpty()) {
            for (String javaScript : rulePattern.javaScripts) {
                string = JSParser.evalStringScript(javaScript, this, string, getConfig().getBaseURL());
            }
        }
        return string;
    }

    List<String> evalArrayScript(@NonNull String string, @NonNull RulePattern rulePattern) {
        final List<String> list = new ArrayList<>();
        if (!rulePattern.javaScripts.isEmpty()) {
            for (String javaScript : rulePattern.javaScripts) {
                list.addAll(JSParser.evalArrayScript(javaScript, this, string, getConfig().getBaseURL()));
            }
        }
        return list;
    }

    void processResultContents(@NonNull List<String> result, @NonNull RulePattern rulePattern) {
        if (!rulePattern.javaScripts.isEmpty()) {
            ListIterator<String> iterator = result.listIterator();
            while (iterator.hasNext()) {
                iterator.set(evalStringScript(iterator.next(), rulePattern));
            }
        }

        if (!isEmpty(rulePattern.replaceRegex)) {
            ListIterator<String> iterator = result.listIterator();
            while (iterator.hasNext()) {
                String string = iterator.next();
                iterator.set(string.replaceAll(rulePattern.replaceRegex, rulePattern.replacement));
            }
        }
    }

    String processResultContent(@NonNull String result, @NonNull RulePattern rulePattern) {
        result = evalStringScript(result, rulePattern);

        if (!isEmpty(rulePattern.replaceRegex)) {
            result = result.replaceAll(rulePattern.replaceRegex, rulePattern.replacement);
        }
        return result;
    }


    String processResultUrl(@NonNull String result, @NonNull RulePattern rulePattern) {
        result = evalStringScript(result, rulePattern);

        if (!isEmpty(result)) {
            result = NetworkUtil.getAbsoluteURL(getConfig().getBaseURL(), result);
        }
        return result;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public final String ajax(String urlStr) {
        try {
            AnalyzeUrl analyzeUrl = new AnalyzeUrl(urlStr, getConfig().getBaseURL());
            Response<String> response = SimpleModel.getResponse(analyzeUrl)
                    .blockingFirst();
            return response.body();
        } catch (Exception e) {
            return e.getLocalizedMessage();
        }
    }

    @Override
    public final String base64Decode(String base64) {
        return StringUtils.base64Decode(base64);
    }


    @Override
    public String base64Encode(String string) {
        return StringUtils.base64Encode(string);
    }

    @Override
    public final String formatHtml(String string) {
        if (isEmpty(string)) {
            return "";
        }
        return string.replaceAll("(?i)<(br[\\s/]*|/*p.*?|/*div.*?)>", "\n")  // 替换特定标签为换行符
                .replaceAll("<[script>]*.*?>|&nbsp;", "")               // 删除script标签对和空格转义符
                .replaceAll("\\s*\\n+\\s*", "\n　　");                   // 移除空行,并增加段前缩进2个汉字
    }
}
