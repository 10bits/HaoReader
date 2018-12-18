package com.monke.monkeybook.model.analyzeRule;

import android.text.TextUtils;

import com.monke.monkeybook.help.FormatWebText;
import com.monke.monkeybook.utils.NetworkUtil;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

import static android.text.TextUtils.isEmpty;
import static com.monke.monkeybook.model.analyzeRule.JsoupParser.getElements;
import static com.monke.monkeybook.model.analyzeRule.JsoupParser.getElementsSingle;

/**
 * Created by GKF on 2018/1/25.
 * 书源规则解析
 */

public class JsoupAnalyzer extends OutAnalyzer<Element, Element> {

    private XJsoupContentDelegate mDelegate;

    @Override
    public Element parseSource(String source) {
        return Jsoup.parse(source);
    }

    @Override
    public ContentDelegate getDelegate() {
        if (mDelegate == null) {
            mDelegate = new XJsoupContentDelegate(this);
        }
        return mDelegate;
    }

    /**
     * 合并内容列表,得到内容
     */
    @Override
    public String getResultContent(Element element, String ruleStr) {
        String result = "";
        if (element == null || isEmpty(ruleStr)) {
            return result;
        }
        //分离正则表达式
        RulePattern rulePattern = splitSourceRule(ruleStr.trim());
        if (isEmpty(rulePattern.elementsRule)) {
            result = element.data();
        } else {
            List<String> textS = getAllResults(element, rulePattern.elementsRule);
            if (textS.size() == 0) {
                return null;
            }
            StringBuilder content = new StringBuilder();
            for (String text : textS) {
                if (textS.size() > 1) {
                    if (text.length() > 0) {
                        if (content.length() > 0) {
                            content.append("\n");
                        }
                        content.append("\u3000\u3000").append(text);
                    }
                } else {
                    content.append(text);
                }
                result = content.toString();
            }
        }
        if (!isEmpty(rulePattern.replaceRegex)) {
            result = result.replaceAll(rulePattern.replaceRegex, rulePattern.replacement);
        }
        if (!isEmpty(rulePattern.javaScript)) {
            result = JSParser.evalJS(rulePattern.javaScript, result, getConfig().getBaseURL());
        }
        return result;
    }

    @Override
    public String getResultUrl(Element element, String ruleStr) {
        String result = "";
        if (element == null || isEmpty(ruleStr)) {
            return result;
        }
        RulePattern rulePattern = splitSourceRule(ruleStr.trim());
        List<String> urlList = getAllResults(element, rulePattern.elementsRule);
        if (urlList.size() > 0) {
            result = urlList.get(0);
        }
        if (!isEmpty(rulePattern.replaceRegex)) {
            result = result.replaceAll(rulePattern.replaceRegex, rulePattern.replacement);
        }
        if (!isEmpty(rulePattern.javaScript)) {
            result = JSParser.evalJS(rulePattern.javaScript, result, getConfig().getBaseURL());
        }
        return result;
    }

    @Override
    public List<Element> getRawList(String source, String rule) {
        if (source == null || isEmpty(rule)) {
            return new ArrayList<>();
        }
        return getElements(parseSource(source), rule);
    }

    @Override
    public List<Element> getRawList(Element source, String rule) {
        if (source == null || isEmpty(rule)) {
            return new ArrayList<>();
        }
        return getElements(source, rule);
    }

    /**
     * 获取所有内容列表
     */
    private List<String> getAllResults(Element element, String ruleStr) {
        List<String> textS = new ArrayList<>();
        if (element == null || isEmpty(ruleStr)) {
            return textS;
        }
        RulePattern rulePattern = splitSourceRule(ruleStr.trim());
        if (isEmpty(rulePattern.elementsRule)) {
            textS.add(element.data());
        } else {
            boolean isAnd;
            String ruleStrS[];
            if (rulePattern.elementsRule.contains("&")) {
                isAnd = true;
                ruleStrS = rulePattern.elementsRule.split("&+");
            } else {
                isAnd = false;
                ruleStrS = rulePattern.elementsRule.split("\\|+");
            }
            for (String ruleStrX : ruleStrS) {
                List<String> temp = getResults(element, ruleStrX);
                if (temp != null) {
                    textS.addAll(temp);
                }
                if (textS.size() > 0 && !isAnd) {
                    break;
                }
            }
        }
        if (!isEmpty(rulePattern.replaceRegex)) {
            ListIterator<String> it = textS.listIterator();
            while (it.hasNext()) {
                String text = it.next();
                it.set(text.replaceAll(rulePattern.replaceRegex, rulePattern.replacement));
            }
        }
        return textS;
    }

    /**
     * 获取内容列表
     */
    private List<String> getResults(Element element, String ruleStr) {
        if (element == null || isEmpty(ruleStr)) {
            return new ArrayList<>();
        }
        Elements elements = new Elements();
        elements.add(element);
        String[] rules = ruleStr.split("@");
        for (int i = 0; i < rules.length - 1; i++) {
            Elements es = new Elements();
            for (Element elt : elements) {
                es.addAll(getElementsSingle(elt, rules[i]));
            }
            elements.clear();
            elements = es;
        }
        if (elements.isEmpty()) {
            return new ArrayList<>();
        }
        return getLastResult(elements, rules[rules.length - 1]);
    }

    /**
     * 根据最后一个规则获取内容
     */
    private List<String> getLastResult(Elements elements, String lastRule) {
        List<String> textS = new ArrayList<>();
        try {
            switch (lastRule) {
                case "text":
                    for (Element element : elements) {
                        String text = element.text();
                        if (!isEmpty(text)) {
                            textS.add(text);
                        }
                    }
                    break;
                case "ownText":
                    List<String> keptTags = Arrays.asList("br", "b", "em", "strong");
                    for (Element element : elements) {
                        Element ele = element.clone();
                        for (Element child : ele.children()) {
                            if (!keptTags.contains(child.tagName())) {
                                child.remove();
                            }
                        }
                        String[] htmlS = ele.html().replaceAll("(?i)<br[\\s/]*>", "\n")
                                .replaceAll("<.*?>", "").split("\n");
                        for (String temp : htmlS) {
                            temp = FormatWebText.getContent(temp);
                            if (!isEmpty(temp)) {
                                textS.add(temp);
                            }
                        }
                    }
                    break;
                case "textNodes":
                    for (Element element : elements) {
                        List<TextNode> contentEs = element.textNodes();
                        for (int i = 0; i < contentEs.size(); i++) {
                            String temp = contentEs.get(i).text().trim();
                            temp = FormatWebText.getContent(temp);
                            if (!isEmpty(temp)) {
                                textS.add(temp);
                            }
                        }
                    }
                    break;
                case "html":
                    elements.select("script").remove();
                    String[] htmlS = elements.html().replaceAll("(?i)<(br[\\\\s/]*|p.*?|div.*?|/p|/div)>", "\n")
                            .replaceAll("<.*?>", "")
                            .split("\n");
                    for (String temp : htmlS) {
                        temp = FormatWebText.getContent(temp);
                        if (!isEmpty(temp)) {
                            textS.add(temp);
                        }
                    }
                    break;
                default:
                    for (Element element : elements) {
                        String url = NetworkUtil.getAbsoluteURL(getConfig().getBaseURL(), element.attr(lastRule));
                        if (!isEmpty(url) && !textS.contains(url)) {
                            textS.add(url);
                        }
                    }
            }
        } catch (Exception ignore) {
        }
        return textS;
    }

}

