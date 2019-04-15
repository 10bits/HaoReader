package com.monke.monkeybook.model.analyzeRule;

import java.util.Iterator;
import java.util.List;

final class AnalyzeCollection {

    private OutAnalyzer mAnalyzer;
    private Iterator mIterator;

    AnalyzeCollection(OutAnalyzer analyzer, List rawList) {
        this.mAnalyzer = analyzer;
        this.mIterator = rawList.iterator();
    }

    @SuppressWarnings("unchecked")
    boolean hasNext() {
        if (mIterator.hasNext()) {
            mAnalyzer.setContent(mIterator.next());
            return true;
        }
        return false;
    }


    OutAnalyzer<?, ?> mutable() {
        return mAnalyzer;
    }
}
