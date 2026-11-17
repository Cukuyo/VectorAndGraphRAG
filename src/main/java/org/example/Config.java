package org.example;

public class Config {
    public static String API_KEY = System.getenv("API_KEY");
    public static String TEXT_MODEL = "qwen3.8-flash";
    public static String EMBEDDING_MODEL = "qwen3.7-text-embedding-flash";
    public static String RERANK_MODEL = "qwen3.7-text-rerank";

    public static String NEO4J_URL = System.getenv("NEO4J_URL");
    public static String NEO4J_USER = "neo4j";
    public static String NEO4J_PASSWD = System.getenv("NEO4J_PASSWD");
}
