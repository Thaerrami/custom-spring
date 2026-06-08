package com.example.demo;

import com.example.demo.service.UserService;
import framework.core.Container;

/**
 * Application entry point — bootstraps the IoC container and runs the demo.
 *
 * <p><b>Spring equivalent:</b> {@code @SpringBootApplication} + {@code SpringApplication.run()}.
 *
 * <p><b>What happens when you run {@code ./scripts/run.sh}:</b>
 * <pre>
 *   1. JVM loads this class and calls main()
 *   2. Container scans "framework" + "com.example.demo" packages
 *   3. All @Component/@Service/@Repository beans are created and wired
 *   4. @PostConstruct runs (DB connect, schema.sql)
 *   5. UserService.runDemo() inserts and queries users
 *   6. container.shutdown() runs @PreDestroy (close DB)
 * </pre>
 *
 * <p><b>Try:</b> Add a new @Service class in this package, inject it into UserService,
 * rebuild, and watch the container log pick it up automatically.
 */
public class Application {

    public static void main(String[] args) throws Exception {
        Container container = new Container();

        // Scan both the framework internals AND your application code
        container.init("framework", "com.example.demo");

        // Retrieve the fully-wired service (all @Inject fields already populated)
        UserService userService = container.getBean(UserService.class);
        userService.runDemo();

        // Always shut down cleanly — releases DB connections
        container.shutdown();
    }
}
