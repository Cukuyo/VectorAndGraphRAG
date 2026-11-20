package org.example.data;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;


public class Neo4jDataClient {
    private Driver driver;

    public static Neo4jDataClient build(String url, String user, String passwd) {
        Neo4jDataClient neo4JDataClient = new Neo4jDataClient();
        neo4JDataClient.driver = GraphDatabase.driver(url, AuthTokens.basic(user, passwd));
        neo4JDataClient.driver.verifyConnectivity();
        return neo4JDataClient;
    }

    public void addPerson(PersonEntity person) {
        String cypher = """
                MERGE (p:Person{name:$name})
                SET p.alias=$alias,
                    p.nation=$nation,
                    p.ethnic=$ethnic,
                    p.birth_place=$birthPlace,
                    p.birth_date=$birthDate,
                    p.death_date=$deathDate,
                    p.job_title=$jobTitle
                """;
        try (Session session = driver.session()) {
            session.run(cypher, Values.parameters(
                    "name", person.get中文名(),
                    "alias", person.get附加名称(),
                    "nation", person.get国籍(),
                    "ethnic", person.get民族(),
                    "birthPlace", person.get出生地(),
                    "birthDate", person.get出生日期(),
                    "deathDate", person.get死亡日期(),
                    "jobTitle", person.get工作职责()
            ));
        }
    }

    public void addPersonToUniversity(String personName, String universityName) {
        String cypher = """
                MERGE (p:Person{name:$pName})
                MERGE (u:University{name:$uName})
                MERGE (p)-[:GRADUATED_FROM]->(u)
                """;
        try (Session session = driver.session()) {
            session.run(cypher, Values.parameters("pName", personName, "uName", universityName));
        }
    }

    public void addPersonToWork(String personName, String workTitle) {
        String cypher = """
                MERGE (p:Person{name:$pName})
                MERGE (w:Work{name:$wName})
                MERGE (p)-[:HAS_WORK]->(w)
                """;
        try (Session session = driver.session()) {
            session.run(cypher, Values.parameters("pName", personName, "wName", workTitle));
        }
    }

    public void addPersonToPerson(String fromName, String relType, String toName) {
        String cypher = String.format("""
                MERGE (a:Person{name:$aName})
                MERGE (b:Person{name:$bName})
                MERGE (a)%s(b)
                """, relType);
        try (Session session = driver.session()) {
            session.run(cypher, Values.parameters("aName", fromName, "bName", toName));
        }
    }

    public void addSourceTextChunk(String sourceId, String content, double[] embedding) {
        String cypher = """
                MERGE (tc:TextChunk{source_id:$sourceId})
                SET tc.content=$content, tc.embedding=$embedding
                """;
        try (Session session = driver.session()) {
            session.run(cypher, Values.parameters(
                    "sourceId", sourceId,
                    "content", content,
                    "embedding", embedding
            ));
        }
    }

    public void close() {
        if (driver != null) {
            driver.close();
        }
    }
}
