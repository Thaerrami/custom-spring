package framework.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a managed bean (a "component" the container should create and wire).
 *
 * <p><b>Spring equivalent:</b> {@code @Component} — also the base meta-annotation for
 * {@code @Service}, {@code @Repository}, and {@code @Controller}.
 *
 * <p><b>Why RUNTIME retention?</b> The JVM must keep this annotation after compile so
 * {@code Container} can read it via reflection during {@code init()}. Annotations with
 * {@code SOURCE} retention (like {@code @Override}) disappear and cannot be scanned.
 *
 * <p><b>Learn:</b> Open {@code Container.isBeanCandidate()} to see how this annotation
 * triggers bean registration.
 */
@Target(ElementType.TYPE)               // legal only on classes, interfaces, enums
@Retention(RetentionPolicy.RUNTIME)    // survive compile → available to reflection
public @interface Component {
    /** Optional bean name — Spring uses this for {@code @Qualifier} lookups (not yet implemented here). */
    String value() default "";
}
