package org.example.use;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.example.TextEmbeddingUtil;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.types.Relationship;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class Neo4jUseClient {
    private Driver driver;

    public static Neo4jUseClient build(String url, String user, String passwd) {
        Neo4jUseClient neo4JUseClient = new Neo4jUseClient();
        neo4JUseClient.driver = GraphDatabase.driver(url, AuthTokens.basic(user, passwd));
        neo4JUseClient.driver.verifyConnectivity();
        return neo4JUseClient;
    }

    @Tool(
            description = "向量检索。将待查询问题转换为向量后在向量节点检索信息，",
            readOnly = true
    )
    public String vector_search(
            @ToolParam(name = "query", description = "待查询问题") String query) {
        int topN = 10;
        int topK = 3;

        double[] embedding = TextEmbeddingUtil.embedding(query);
        List<String> searchResult = vector_search(embedding, topN);
        if (searchResult.isEmpty()) {
            return "未检索到相关信息，请修改问题后重试";
        }
        List<String> reRankList = TextReRankUtil.reRank(query, searchResult, topK, 0.5D);
        if (reRankList.isEmpty()) {
            return "未检索到相关信息，请修改问题后重试";
        }
        return String.join(System.lineSeparator(), reRankList);
    }

    @Tool(
            description = "最短路径查询。查找任意两个节点之间的最短关联路径，不支持TextChunk节点",
            readOnly = true
    )
    public String find_shortest_path(
            @ToolParam(name = "start_name", description = "起始节点名称") String start_name,
            @ToolParam(name = "start_type", required = false, description = "起始节点类型，为空代表不限制节点类型") String start_type,
            @ToolParam(name = "end_name", description = "目标节点名称") String end_name,
            @ToolParam(name = "end_type", required = false, description = "目标节点类型，为空代表不限制节点类型") String end_type) {
        start_type = start_type == null ? "" : ":" + start_type;
        end_type = end_type == null ? "" : ":" + end_type;
        String cypher = String.format("""
                MATCH p = shortestPath((a %s {name:$startName})-[*]-(b %s {name:$endName}))
                RETURN p
                """, start_type, end_type);

        try (Session session = driver.session()) {
            Result result = session.run(cypher, Map.of(
                    "startName", start_name,
                    "endName", end_name
            ));
            if (!result.hasNext()) {
                return "未找到二者在限制跳数内的关联路径";
            }

            return pathToStr(result.next().values().getFirst().asPath());
        }
    }

    @Tool(
            description = "节点信息查询。查找任意节点的属性信息，不支持TextChunk节点",
            readOnly = true
    )
    public String search_node(
            @ToolParam(name = "node_name", description = "节点名称") String node_name,
            @ToolParam(name = "node_type", required = false, description = "节点类型，为空代表不限制节点类型") String node_type) {
        node_type = node_type == null ? "" : ":" + node_type;
        String cypher = String.format("""
                MATCH (n %s {name:$nodeName})
                RETURN n
                """, node_type);

        try (Session session = driver.session()) {
            Result result = session.run(cypher, Map.of("nodeName", node_name));
            if (!result.hasNext()) {
                return "没有找到该节点";
            }
            Node node = result.next().values().getFirst().asNode();

            // 拼接节点全部属性
            return String.format("节点【%s】，标签：%s，属性：%s",
                    node.get("name").asString(), String.join(",", node.labels()), node.asMap());
        }
    }

    @Tool(
            description = "1:N跳邻居遍历。从起始节点向外多跳查找关联实体，返回关联关系、目标节点和跳数，不支持TextChunk节点",
            readOnly = true
    )
    public String search_neighbor(
            @ToolParam(name = "start_name", description = "起始节点名称") String start_name,
            @ToolParam(name = "start_type", required = false, description = "起始节点标签类型，为空不限制") String start_type,
            @ToolParam(name = "max_hop", required = false, description = "最大跳数，默认2，建议1~3，太大容易超时") Integer max_hop,
            @ToolParam(name = "limit", required = false, description = "返回结果上限，默认20") Integer limit) {
        // 默认值兜底
        start_type = start_type == null ? "" : ":" + start_type;
        int hop = max_hop == null ? 2 : Math.clamp(max_hop, 1, 4);
        int lim = limit == null ? 20 : Math.clamp(limit, 1, 50);

        // 重点改造：返回整条path，Java里面拆分每一段，不依赖 Cypher last()
        String cypher = String.format("""
                MATCH (start%s {name:$startName})
                MATCH path = (start)-[r*1..%d]-(connected)
                RETURN path
                ORDER BY length(path)
                LIMIT %d
                """, start_type, hop, lim);

        try (Session session = driver.session()) {
            Result result = session.run(cypher, Map.of("startName", start_name));
            if (!result.hasNext()) {
                return "该节点在指定跳数范围内没有找到邻居节点";
            }
            List<String> lines = new ArrayList<>();
            while (result.hasNext()) {
                lines.add(pathToStr(result.next().get("path").asPath()));
            }
            return String.join("\n", lines);
        }
    }

    private List<String> vector_search(double[] embedding, int topN) {
        List<String> searchResult = new ArrayList<>();
        try (Session session = driver.session()) {
            Result result = session.run("""
                    CALL db.index.vector.queryNodes('person_vector_index', $topN, $embedding)
                    YIELD node, score
                    RETURN node.content AS content, score, node.source_id AS sourceId
                    ORDER BY score DESC
                    """, Map.of("embedding", embedding, "topN", topN));
            while (result.hasNext()) {
                org.neo4j.driver.Record record = result.next();
                String content = record.get("content").asString();
                double score = record.get("score").asDouble();
                String sourceId = record.get("sourceId").isNull() ? "" : record.get("sourceId").asString();
                System.out.printf("[相似度:%.4f] 来源:%s \n", score, sourceId);
                searchResult.add(content);
            }
        }
        return searchResult;
    }

    private String pathToStr(Path path) {
        List<Node> nodes = new ArrayList<>();
        path.nodes().forEach(nodes::add);

        List<Relationship> rels = new ArrayList<>();
        path.relationships().forEach(rels::add);

        StringBuilder sb = new StringBuilder();
        sb.append(nodes.getFirst().get("name").asString());

        for (int i = 0; i < rels.size(); i++) {
            Node left = nodes.get(i);
            Node right = nodes.get(i + 1);
            Relationship rel = rels.get(i);
            String relType = rel.type();
            if (rel.startNodeElementId().equals(left.elementId()) && rel.endNodeElementId().equals(right.elementId())) {
                sb.append(" -[").append(relType).append("]->");
            } else {
                sb.append(" <-[").append(relType).append("]-");
            }
            sb.append(right.get("name").asString());
        }
        return sb.toString();
    }

    public void close() {
        if (driver != null) {
            driver.close();
        }
    }
}
