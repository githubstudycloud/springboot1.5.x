import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;

@Component
public class ConfigurableComponent {

    @Value("${your.config.key}")
    private String configValue;

    private static String staticConfigValue;

    @PostConstruct
    private void init() {
        staticConfigValue = configValue;
    }

    public static String getConfigValue() {
        return staticConfigValue;
    }

    // 你的静态方法
    public static void yourStaticMethod() {
        // 使用配置值
        String value = getConfigValue();
        // 使用value进行操作
        System.out.println("Config value in static method: " + value);
    }

    // 非静态方法示例
    public void nonStaticMethod() {
        System.out.println("Config value in non-static method: " + configValue);
    }
}
