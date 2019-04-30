package com.monke.monkeybook.model.analyzeRule;

import com.monke.monkeybook.help.FormatWebText;
import com.monke.monkeybook.help.Logger;
import com.monke.monkeybook.utils.ListUtils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static android.text.TextUtils.isEmpty;

final class JsoupParser extends SourceParser<Element> {

    private static final String TAG = "JSOUP";

    JsoupParser() {

    }

    @Override
    String sourceToString(Object source) {
        if (source instanceof String) {
            return (String) source;
        } else if (source instanceof Element) {
            return source.toString();
        }
        return "";
    }

    @Override
    Element fromSource(Object source) {
        Objects.requireNonNull(source);

        if (source instanceof String) {
            return Jsoup.parse((String) source);
        } else if (source instanceof Element) {
            return (Element) source;
        }
        throw new IllegalAccessError("JsoupParser can not support the source type");
    }

    @Override
    List<Object> getList(Rule rule) {
        String ruleStr = rule.getRule();
        if (isOuterBody(ruleStr)) {
            return ListUtils.mutableList(getSource());
        }
        return ListUtils.toObjectList(parseList(getSource(), ruleStr));
    }

    @Override
    List<Object> parseList(String source, Rule rule) {
        String ruleStr = rule.getRule();
        return ListUtils.toObjectList(parseList(fromSource(source), ruleStr));
    }

    @Override
    String getString(Rule rule) {
        String ruleStr = rule.getRule();
        if (isEmpty(ruleStr)) {
            return "";
        }

        if (isOuterBody(ruleStr)) {
            return getStringSource();
        }

        return parseString(getSource(), ruleStr);
    }

    @Override
    String parseString(String source, Rule rule) {
        String ruleStr = rule.getRule();
        if (isEmpty(ruleStr)) {
            return "";
        }
        return parseString(fromSource(source), ruleStr);
    }

    @Override
    String getStringFirst(Rule rule) {
        if (isOuterBody(rule.getRule())) {
            return getStringSource();
        }
        final List<String> result = getStringList(rule);
        if (!result.isEmpty()) {
            return result.get(0);
        }
        return "";
    }

    @Override
    String parseStringFirst(String source, Rule rule) {
        final List<String> result = parseStringList(source, rule);
        if (!result.isEmpty()) {
            return result.get(0);
        }
        return "";
    }

    private String parseString(Element source, String rule) {
        final List<String> textS = parseStringList(source, rule);
        final StringBuilder content = new StringBuilder();
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
        }
        return content.toString();
    }

    @Override
    List<String> getStringList(Rule rule) {
        String ruleStr = rule.getRule();
        if (isEmpty(ruleStr)) {
            return ListUtils.mutableList();
        }

        if (isOuterBody(ruleStr)) {
            return ListUtils.mutableList(getStringSource());
        }

        return parseStringList(getSource(), ruleStr);
    }


    @Override
    List<String> parseStringList(String source, Rule rule) {
        String ruleStr = rule.getRule();
        if (isEmpty(ruleStr)) {
            return ListUtils.mutableList();
        }

        return parseStringList(fromSource(source), ruleStr);
    }


    private List<String> parseStringList(Element element, String rule) {
        final List<String> textS = new ArrayList<>();
        Elements elements = new Elements();
        elements.add(element);
        String[] ruleS = rule.split("@");
        for (int i = 0, length = ruleS.length - 1; i < length; i++) {
            Elements es = new Elements();
            for (Element elt : elements) {
                es.addAll(parseList(elt, ruleS[i]));
            }
            elements.clear();
            elements.addAll(es);
        }
        if (!elements.isEmpty()) {
            return parseLastResult(elements, ruleS[ruleS.length - 1]);
        }
        return textS;
    }

    /**
     * 根据最后一个规则获取内容
     */
    private List<String> parseLastResult(Elements elements, String lastRule) {
        final List<String> textS = new ArrayList<>();
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
                        String attr = element.attr(lastRule);
                        if (!isEmpty(attr) && !textS.contains(attr)) {
                            textS.add(attr);
                        }
                    }
            }
        } catch (Exception e) {
            Logger.e(TAG, lastRule, e);
        }
        return textS;
    }

    /**
     * 获取Elements按照一个规则
     */
    private List<Element> parseList(Element temp, String rule) {
        final Elements elements = new Elements();
        try {
            String[] ruleS = rule.split("@");
            if (ruleS.length > 1) {
                elements.add(temp);
                for (String singleRule : ruleS) {
                    Elements es = new Elements();
                    for (Element et : elements) {
                        es.addAll(parseList(et, singleRule));
                    }
                    elements.clear();
                    elements.addAll(es);
                }
            } else {
                String[] rulePcx = rule.split("!");
                String[] rulePc = rulePcx[0].trim().split(">");
                String[] rules = rulePc[0].trim().split("\\.");
                String[] filterRules = ensureFilterRules(rulePc);
                switch (rules[0]) {
                    case "children":
                        Elements children = temp.children();
                        if (filterRules != null) {
                            children = filterElements(children, filterRules);
                        }
                        elements.addAll(children);
                        break;
                    case "class":
                        Elements elementsByClass = temp.getElementsByClass(rules[1]);
                        if (rules.length == 3) {
                            int index = Integer.parseInt(rules[2]);
                            if (index < 0) {
                                elements.add(elementsByClass.get(elementsByClass.size() + index));
                            } else {
                                elements.add(elementsByClass.get(index));
                            }
                        } else {
                            if (filterRules != null) {
                                elementsByClass = filterElements(elementsByClass, filterRules);
                            }
                            elements.addAll(elementsByClass);
                        }
                        break;
                    case "tag":
                        Elements elementsByTag = temp.getElementsByTag(rules[1]);
                        if (rules.length == 3) {
                            int index = Integer.parseInt(rules[2]);
                            if (index < 0) {
                                elements.add(elementsByTag.get(elementsByTag.size() + index));
                            } else {
                                elements.add(elementsByTag.get(index));
                            }
                        } else {
                            if (filterRules != null) {
                                elementsByTag = filterElements(elementsByTag, filterRules);
                            }
                            elements.addAll(elementsByTag);
                        }
                        break;
                    case "id":
                        elements.add(temp.getElementById(rules[1]));
                        break;
                    case "text":
                        Elements elementsByText = temp.getElementsContainingOwnText(rules[1]);
                        if (filterRules != null) {
                            elementsByText = filterElements(elementsByText, filterRules);
                        }
                        elements.addAll(elementsByText);
                        break;
                }
                if (rulePcx.length > 1) {
                    String[] rulePcs = rulePcx[1].split(":");
                    Elements removes = new Elements();
                    if (rulePcs.length < elements.size() - 1) {
                        for (String pc : rulePcs) {
                            int pcInt = Integer.parseInt(pc);
                            if (pcInt < 0 && elements.size() + pcInt >= 0) {
                                removes.add(elements.get(elements.size() + pcInt));
                            } else if (pcInt < elements.size()) {
                                removes.add(elements.get(pcInt));
                            }
                        }
                    }
                    elements.removeAll(removes);
                }
            }

        } catch (Exception e) {
            Logger.e(TAG, rule, e);
        }
        return elements;
    }

    private String[] ensureFilterRules(String[] rules) {
        final String[] filterRules;
        final boolean valid = rules.length > 1 && !isEmpty(rules[1]);
        if (valid) {
            filterRules = rules[1].split("\\.");
            List<String> validKeys = Arrays.asList("class", "id", "tag", "text");
            if (filterRules.length < 2 || !validKeys.contains(filterRules[0]) || isEmpty(filterRules[1])) {
                return null;
            }
        } else {
            filterRules = null;
        }
        return filterRules;
    }

    private Elements filterElements(Elements elements, String[] rules) {
        if (rules == null || rules.length < 2) return elements;
        final Elements selectedEls = new Elements();
        for (Element ele : elements) {
            boolean isOk = false;
            switch (rules[0]) {
                case "class":
                    isOk = ele.getElementsByClass(rules[1]).size() > 0;
                    break;
                case "id":
                    isOk = ele.getElementById(rules[1]) != null;
                    break;
                case "tag":
                    isOk = ele.getElementsByTag(rules[1]).size() > 0;
                    break;
                case "text":
                    isOk = ele.getElementsContainingOwnText(rules[1]).size() > 0;
                    break;
            }
            if (isOk) {
                selectedEls.add(ele);
            }
        }
        return selectedEls;
    }
}
