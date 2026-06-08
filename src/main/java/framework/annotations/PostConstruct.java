package framework.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method to run <em>after</em> all {@link Inject} dependencies are wired.
 *
 * <p><b>Spring equivalent:</b> {@code @PostConstruct} (JSR-250) or {@code InitializingBean.afterPropertiesSet()}.
 *
 * <p><b>Typical uses:</b> open database connections, run schema migrations, warm caches.
 * In this project: {@code DataSourceManager.verifyConnection()} and {@code SchemaRunner.runSchema()}.
 *
 * <p><b>Order matters:</b> injection completes first, then ALL {@code @PostConstruct} methods run.
 * Spring runs them in dependency order; our container uses simpler iteration (see docs for details).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PostConstruct {
}
