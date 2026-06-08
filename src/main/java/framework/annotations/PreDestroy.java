package framework.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method to run when the container shuts down — release resources cleanly.
 *
 * <p><b>Spring equivalent:</b> {@code @PreDestroy} (JSR-250) or {@code DisposableBean.destroy()}.
 *
 * <p><b>Typical uses:</b> close database connections, flush buffers, deregister listeners.
 * Called from {@code Container.shutdown()} before the JVM exits.
 *
 * <p><b>Learn:</b> Without cleanup, in-memory H2 databases and connection pools can leak
 * resources in long-running apps. Always pair open with close.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PreDestroy {
}
