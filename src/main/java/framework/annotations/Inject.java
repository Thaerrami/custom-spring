package framework.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field as a dependency the container must inject after the bean is created.
 *
 * <p><b>Spring equivalent:</b> {@code @Autowired} or JSR-330 {@code javax/jakarta.inject.Inject}.
 *
 * <p><b>Learn:</b> Field injection is the simplest form of DI. Spring prefers constructor
 * injection in production code because it makes dependencies explicit and eases testing.
 * See {@code Container.injectDependencies()} for the reflection mechanics.
 *
 * <p><b>Try:</b> Remove this annotation from a field and rebuild — the field stays {@code null}
 * and you get a {@code NullPointerException} at runtime (no compile error!).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)             // legal only on instance fields
public @interface Inject {
}
