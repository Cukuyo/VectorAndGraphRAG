package org.example.use;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEventType;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.event.ToolCallStartEvent;
import io.agentscope.core.message.UserMessage;
import io.agentscope.extensions.model.dashscope.DashScopeChatModel;
import io.agentscope.extensions.model.dashscope.formatter.DashScopeChatFormatter;
import org.example.Config;

import java.util.Scanner;

public class Main {
    static DashScopeChatModel model = DashScopeChatModel.builder()
            .apiKey(Config.API_KEY)
            .modelName(Config.TEXT_MODEL)
            .enableThinking(true)
            .stream(true)
            .formatter(new DashScopeChatFormatter())
            .build();

    static ReActAgent agent = ReActAgent.builder()
            .name("近代人物关系知识助手")
            .sysPrompt("""
                    你是图书馆内近代人物关系知识讲解助手，回答风格幽默风趣，通过知识图谱检索信息和使用工具回答近代人物相关问题，当前知识图谱里包含3种节点类型：
                    Person-近代人物
                    University-毕业学校
                    Work-发表作品
                    TextChunk-向量节点
                    """)
            .model(model)
            .build();

    static RuntimeContext ctx = RuntimeContext.builder().userId("demo").sessionId("demo-session").build();

    static Neo4jUseClient neo4jUseClient = Neo4jUseClient.build(Config.NEO4J_URL, Config.NEO4J_USER, Config.NEO4J_PASSWD);

    static {
        agent.getToolkit().registerTool(neo4jUseClient);
    }

    static void main() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("###");

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.equals("q")) {
                break;
            }

            agent.streamEvents(new UserMessage(line), ctx)
                    .doOnNext(event -> {
                        if (event.getType() == AgentEventType.TEXT_BLOCK_DELTA) {
                            System.out.print(((TextBlockDeltaEvent) event).getDelta());
                        } else if (event.getType() == AgentEventType.TOOL_CALL_START) {
                            System.out.println("\n[tool] " + ((ToolCallStartEvent) event).getToolCallName());
                        }
                    })
                    .blockLast();
            System.out.print("\n###");
        }

        agent.close();
        neo4jUseClient.close();
    }
}
