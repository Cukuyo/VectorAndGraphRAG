package org.example.use;

import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.rerank.TextReRank;
import com.alibaba.dashscope.rerank.TextReRankParam;
import org.example.Config;

import java.util.List;

public class TextReRankUtil {
    private static final TextReRank TEXT_RERANK = new TextReRank();

    private static List<String> reRank(String query, List<String> documents, int topN, double threshold) {
        TextReRankParam param = TextReRankParam
                .builder()
                .apiKey(Config.API_KEY)
                .model(Config.RERANK_MODEL)
                .query(query)
                .documents(documents)
                .topN(topN)
                .returnDocuments(true)
                .build();

        try {
            return TEXT_RERANK.call(param).getOutput().getResults().stream()
                    .filter(cv -> cv.getRelevanceScore() >= threshold)
                    .map(cv -> cv.getDocument().getText())
                    .toList();
        } catch (NoApiKeyException | InputRequiredException e) {
            throw new RuntimeException(e);
        }
    }
}
