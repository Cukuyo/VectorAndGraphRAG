package org.example;

import com.alibaba.dashscope.embeddings.TextEmbedding;
import com.alibaba.dashscope.embeddings.TextEmbeddingParam;
import com.alibaba.dashscope.exception.NoApiKeyException;

import java.util.List;

public class TextEmbeddingUtil {
    private static final TextEmbedding TEXT_EMBEDDING = new TextEmbedding();

    private static List<Double> embedding(String text) throws NoApiKeyException {
        TextEmbeddingParam param = TextEmbeddingParam
                .builder()
                .apiKey(Config.apiKey)
                .model(Config.embeddingModel)
                .text(text)
                .build();
        return TEXT_EMBEDDING.call(param).getOutput().getEmbeddings().get(0).getEmbedding();
    }
}
