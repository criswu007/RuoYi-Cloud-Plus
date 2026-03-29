package org.dromara.address;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;

/**
 * 地址模块启动类。
 * 目的：作为标准地址服务的 Spring Boot 入口，负责装配业务模块并启动应用进程。
 * 入参/出参：`main(String[] args)` 接收标准启动参数，无直接返回值。
 * 关键约束：启动类本身只保留与所有运行模式通用的基础装配；带环境差异的中间件启用逻辑需下沉到 profile 配置类中。
 * 异常与副作用：应用启动过程中会初始化 Spring 上下文、数据源及 Web 容器，失败时进程直接退出。
 */
@SpringBootApplication
public class RuoYiAddressApplication {

    /**
     * 目的：启动地址模块应用。
     * 入参：`args` 为命令行启动参数，可覆盖 Spring Boot 默认配置。
     * 出参：无。
     * 关键约束：仅负责启动流程编排，不承载环境分支判断与业务初始化逻辑。
     * 异常与副作用：启动失败时会抛出异常并终止进程；启动成功后保持 JVM 常驻等待外部请求。
     */
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(RuoYiAddressApplication.class);
        application.setApplicationStartup(new BufferingApplicationStartup(2048));
        application.run(args);
        System.out.println("(♥◠‿◠)ﾉﾞ  地址模块启动成功   ლ(´ڡ`ლ)ﾞ  ");
    }
}
