package org.example;

import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.rerank.TextReRank;
import com.alibaba.dashscope.rerank.TextReRankParam;
import com.alibaba.dashscope.rerank.TextReRankResult;

import java.util.List;

public class TextReRankUtil {
    private static final TextReRank TEXT_RERANK = new TextReRank();

    private static List<String> reRank(String query, List<String> documents, int topN, double threshold) throws NoApiKeyException, InputRequiredException {
        TextReRankParam param = TextReRankParam
                .builder()
                .apiKey(Config.apiKey)
                .model(Config.reRankModel)
                .query(query)
                .documents(documents)
                .topN(topN)
                .returnDocuments(true)
                .build();

        return TEXT_RERANK.call(param).getOutput().getResults().stream()
                .filter(cv -> cv.getRelevanceScore() >= threshold)
                .map(cv -> cv.getDocument().getText())
                .toList();
    }
}
