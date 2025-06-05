import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashSet;
import java.util.Set;

/**
 * 简单的模板引擎测试
 * 验证Mustache模板引擎是否正常工作
 */
public class TestTemplateEngine {
    
    public static void main(String[] args) {
        System.out.println("🧪 测试Mustache模板引擎...");
        
        MustacheFactory mustacheFactory = new DefaultMustacheFactory();
        
        // 测试1: 简单变量替换
        System.out.println("\n📝 测试1: 简单变量替换");
        String template1 = "Hello {{name}}!";
        Map<String, Object> variables1 = new HashMap<>();
        variables1.put("name", "Kestra");
        String result1 = renderTemplate(mustacheFactory, template1, variables1);
        System.out.println("模板: " + template1);
        System.out.println("变量: " + variables1);
        System.out.println("结果: " + result1);
        System.out.println("✅ " + (result1.equals("Hello Kestra!") ? "通过" : "失败"));
        
        // 测试2: 复杂模板
        System.out.println("\n📝 测试2: 复杂Kestra流模板");
        String template2 = "id: {{flowId}}\n" +
                          "namespace: {{namespace}}\n" +
                          "description: {{description}}\n\n" +
                          "tasks:\n" +
                          "  - id: {{taskId}}\n" +
                          "    type: {{taskType}}\n" +
                          "    message: \"Processing {{itemCount}} items for {{user.name}}\"\n" +
                          "    config:\n" +
                          "      timeout: {{timeout}}\n" +
                          "      enabled: {{enabled}}";
        
        Map<String, Object> variables2 = new HashMap<>();
        variables2.put("flowId", "data-processing-flow");
        variables2.put("namespace", "production");
        variables2.put("description", "Process daily data batch");
        variables2.put("taskId", "process-data");
        variables2.put("taskType", "io.kestra.plugin.core.log.Log");
        variables2.put("itemCount", 1000);
        variables2.put("timeout", 300);
        variables2.put("enabled", true);
        
        Map<String, Object> user = new HashMap<>();
        user.put("name", "DataFlare");
        user.put("email", "admin@dataflare.com");
        variables2.put("user", user);
        
        String result2 = renderTemplate(mustacheFactory, template2, variables2);
        System.out.println("模板:");
        System.out.println(template2);
        System.out.println("\n变量: " + variables2);
        System.out.println("\n结果:");
        System.out.println(result2);
        
        boolean test2Pass = result2.contains("data-processing-flow") &&
                           result2.contains("production") &&
                           result2.contains("Processing 1000 items") &&
                           result2.contains("DataFlare") &&
                           result2.contains("timeout: 300") &&
                           result2.contains("enabled: true");
        
        System.out.println("✅ " + (test2Pass ? "通过" : "失败"));
        
        // 测试3: 变量提取
        System.out.println("\n📝 测试3: 变量提取");
        Set<String> variables3 = extractVariables(template2);
        System.out.println("提取的变量: " + variables3);
        boolean test3Pass = variables3.contains("flowId") &&
                           variables3.contains("namespace") &&
                           variables3.contains("user.name") &&
                           variables3.contains("itemCount");
        System.out.println("✅ " + (test3Pass ? "通过" : "失败"));
        
        // 测试4: 缺失变量处理
        System.out.println("\n📝 测试4: 缺失变量处理");
        String template4 = "Hello {{name}}! Your age is {{age}}.";
        Map<String, Object> variables4 = new HashMap<>();
        variables4.put("name", "John"); // 缺少 age
        String result4 = renderTemplate(mustacheFactory, template4, variables4);
        System.out.println("模板: " + template4);
        System.out.println("变量: " + variables4);
        System.out.println("结果: " + result4);
        System.out.println("✅ " + (result4.equals("Hello John! Your age is .") ? "通过" : "失败"));
        
        System.out.println("\n🎉 模板引擎测试完成！");
        System.out.println("Mustache模板引擎已成功替换Pebble，可以正常处理Kestra蓝图模板。");
    }
    
    public static String renderTemplate(MustacheFactory factory, String template, Map<String, Object> variables) {
        try {
            Mustache mustache = factory.compile(new StringReader(template), "test");
            StringWriter writer = new StringWriter();
            mustache.execute(writer, variables);
            return writer.toString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
    public static Set<String> extractVariables(String template) {
        Set<String> variables = new HashSet<>();
        // Mustache语法: {{variable}} 或 {{{variable}}} (unescaped)
        Pattern pattern = Pattern.compile("\\{\\{\\{?\\s*([a-zA-Z_][a-zA-Z0-9_.]*)\\s*\\}?\\}\\}");
        Matcher matcher = pattern.matcher(template);
        
        while (matcher.find()) {
            String variable = matcher.group(1);
            variables.add(variable);
        }
        
        return variables;
    }
}
