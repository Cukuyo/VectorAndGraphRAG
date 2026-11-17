package org.example;

import com.alibaba.dashscope.embeddings.TextEmbedding;
import com.alibaba.dashscope.embeddings.TextEmbeddingParam;
import com.alibaba.dashscope.exception.NoApiKeyException;

public class TextEmbeddingUtil {
    private static final TextEmbedding TEXT_EMBEDDING = new TextEmbedding();

    public static double[] embedding(String text) {
        TextEmbeddingParam param = TextEmbeddingParam
                .builder()
                .apiKey(Config.API_KEY)
                .model(Config.EMBEDDING_MODEL)
                .dimension(1024)
                .text(text)
                .build();

        try {
            return TEXT_EMBEDDING.call(param).getOutput().getEmbeddings().getFirst().getEmbedding()
                    .stream()
                    .mapToDouble(Double::doubleValue)
                    .toArray();
        } catch (NoApiKeyException e) {
            throw new RuntimeException(e);
        }
    }
}
