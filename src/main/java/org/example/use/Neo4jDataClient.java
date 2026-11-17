package org.example.use;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;


public class Neo4jDataClient {
    private Driver driver;

    public static Neo4jDataClient build(String url, String user, String passwd) {
        Neo4jDataClient neo4JDataClient = new Neo4jDataClient();
        neo4JDataClient.driver = GraphDatabase.driver(url, AuthTokens.basic(user, passwd));
        neo4JDataClient.driver.verifyConnectivity();
        return neo4JDataClient;
    }

    public void close() {
        if (driver != null) {
            driver.close();
        }
    }
}
