package framework.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Stereotype for the data-access layer. Functionally identical to {@link Component}.
 *
 * <p><b>Spring equivalent:</b> {@code @Repository} — also enables persistence exception
 * translation (SQLException → DataAccessException) in full Spring.
 *
 * <p><b>Why a separate annotation?</b> Layered architecture: it documents intent.
 * A reader instantly knows this class talks to a database, not business logic.
 *
 * <p><b>Meta-annotation pattern:</b> {@code @Component} on this annotation means
 * {@code Container.isBeanCandidate()} treats {@code @Repository} classes as beans too.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component                              // meta-annotation: @Repository IS-A @Component
public @interface Repository {
    String value() default "";
}
