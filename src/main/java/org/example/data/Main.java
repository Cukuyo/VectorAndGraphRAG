package org.example.data;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import org.example.Config;
import org.example.TextEmbeddingUtil;

public class Main {
    public static void main(String[] args) throws Exception {
        JSONArray jsonArray = JSONArray.parseArray(Files.readString(Paths.get("src\\main\\resources\\data-json.json")));
        List<PersonEntity> personEntityList = jsonArray.toList(PersonEntity.class);

        Neo4jDataClient neo4JDataClient = Neo4jDataClient.build(Config.NEO4J_URL, Config.NEO4J_USER, Config.NEO4J_PASSWD);

        for (PersonEntity personEntity : personEntityList) {
            neo4JDataClient.addPerson(personEntity);
            System.out.printf("add %s\n", personEntity.get中文名());
        }

        for (PersonEntity personEntity : personEntityList) {
            for (String university : personEntity.get毕业于()) {
                neo4JDataClient.addPersonToUniversity(personEntity.get中文名(), university);
                System.out.printf("add %s %s\n", personEntity.get中文名(), university);
            }
        }

        for (PersonEntity personEntity : personEntityList) {
            for (String workName : personEntity.get作品()) {
                neo4JDataClient.addPersonToWork(personEntity.get中文名(), workName);
                System.out.printf("add %s %s\n", personEntity.get中文名(), workName);
            }
        }

        for (PersonEntity personEntity : personEntityList) {
            for (Map.Entry<String, List<String>> entry : personEntity.get相关人物().entrySet()) {
                if (entry.getKey().equals("未知")) {
                    continue;
                }
                for (String toName : entry.getValue()) {
                    neo4JDataClient.addPersonToPerson(personEntity.get中文名(), entry.getKey(), toName);
                    System.out.printf("add %s %s %s\n", personEntity.get中文名(), entry.getKey(), toName);
                }
            }
        }

        // 用了大概159K tokens，免费额度999K，剩余84.16%
        for (PersonEntity personEntity : personEntityList) {
            String content = JSONObject.toJSONString(personEntity, JSONWriter.Feature.PrettyFormat);
            neo4JDataClient.addSourceTextChunk(personEntity.get中文名(), content, TextEmbeddingUtil.embedding(content));
        }

        neo4JDataClient.close();
    }
}
