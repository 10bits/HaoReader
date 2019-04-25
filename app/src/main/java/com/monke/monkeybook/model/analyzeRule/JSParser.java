package com.monke.monkeybook.model.analyzeRule;


import com.eclipsesource.v8.V8;
import com.monke.monkeybook.MApplication;
import com.monke.monkeybook.R;
import com.monke.monkeybook.help.AppConfigHelper;
import com.monke.monkeybook.help.JavaScriptEngine;
import com.monke.monkeybook.help.Logger;

import java.util.Collections;
import java.util.List;

import javax.script.SimpleBindings;

final class JSParser {

    JSParser() {

    }

    private int engineType;
    private V8 JSV8;

    void start() {
        engineType = getEngineType();
    }

    String evalStringScript(String jsStr, JavaExecutor java, String result, String baseUrl) {
        if (engineType == 0) {
            return evalStringScriptByV8(jsStr, java, result, baseUrl);
        } else {
            return evalStringScriptByRhino(jsStr, java, result, baseUrl);
        }
    }

    @SuppressWarnings("unchecked")
    List<Object> evalArrayScript(String jsStr, JavaExecutor java, String result, String baseUrl) {
        try {
            SimpleBindings bindings = new SimpleBindings();
            bindings.put("java", java);
            bindings.put("result", result);
            bindings.put("baseUrl", baseUrl);
            jsStr = jsStr.replace("parseResultContents", "java.parseResultContents")
                    .replace("parseList", "java.parseList");
            return (List<Object>) JavaScriptEngine.getEngine().eval(jsStr, bindings);
        } catch (Exception e) {
            Logger.e("Rhino", jsStr, e);
        }
        return Collections.emptyList();
    }

    private String evalStringScriptByRhino(String jsStr, JavaExecutor java, String result, String baseUrl) {
        try {
            SimpleBindings bindings = new SimpleBindings();
            bindings.put("java", java);
            bindings.put("result", result);
            bindings.put("baseUrl", baseUrl);
            jsStr = jsStr.replace("ajax", "java.ajax")
                    .replace("base64Decode", "java.base64Decode")
                    .replace("parseResultContent", "java.parseResultContent")
                    .replace("parseResultUrl", "java.parseResultUrl")
                    .replace("formatResultContent", "java.formatResultContent")
                    .replace("formatResultUrl", "java.formatResultUrl");
            return (String) JavaScriptEngine.getEngine().eval(jsStr, bindings);
        } catch (Exception e) {
            Logger.e("Rhino", jsStr, e);
        }
        return "";
    }

    private String evalStringScriptByV8(String jsStr, JavaExecutor java, String result, String baseUrl) {
        try {
            ensureV8Engine();
            JSV8.add("result", result);
            JSV8.add("baseUrl", baseUrl);
            if (java != null) {
                JSV8.registerJavaMethod(java, "ajax", "ajax", new Class[]{String.class});
                JSV8.registerJavaMethod(java, "base64Decode", "base64Decode", new Class[]{String.class});
                JSV8.registerJavaMethod(java, "parseResultContent", "parseResultContent", new Class[]{String.class, String.class});
                JSV8.registerJavaMethod(java, "parseResultUrl", "parseResultUrl", new Class[]{String.class, String.class});
                JSV8.registerJavaMethod(java, "formatResultContent", "formatResultContent", new Class[]{String.class});
                JSV8.registerJavaMethod(java, "formatResultUrl", "formatResultUrl", new Class[]{String.class});
            }
            return JSV8.executeStringScript(jsStr);
        } catch (Exception e) {
            Logger.e("V8", jsStr, e);
        }
        return "";
    }


    private int getEngineType() {
        String type = AppConfigHelper.get().getString(MApplication.getInstance().getString(R.string.pk_js_engine), "0");
        return Integer.parseInt(type);
    }

    private void ensureV8Engine() {
        if (JSV8 == null || JSV8.isReleased()) {
            JSV8 = V8.createV8Runtime();
        }
    }

    void stop() {
        if (JSV8 != null) {
            JSV8.release();
            JSV8 = null;
        }
    }

}
