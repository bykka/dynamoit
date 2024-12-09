package ua.org.java.dynamoit;

import com.amazonaws.services.dynamodbv2.local.main.ServerRunner;
import com.amazonaws.services.dynamodbv2.local.server.DynamoDBProxyServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class DynamoDBTest {

    protected static DynamoDBProxyServer server;

    @BeforeAll
    public static void beforeAll() throws Exception {
        server = ServerRunner.createServerFromCommandLineArgs(new String[]{"-inMemory"});
    }

    @AfterAll
    public static void afterAll() throws Exception {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    public void test1() throws Exception {
        Thread.sleep(3000);
    }

}
