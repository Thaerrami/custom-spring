package framework.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Stereotype for the business/application layer.
 *
 * <p><b>Spring equivalent:</b> {@code @Service} — holds business rules, orchestrates
 * repositories, should NOT contain SQL or HTTP code.
 *
 * <p><b>Layered rule of thumb:</b>
 * <pre>
 *   Controller  → HTTP in/out
 *   Service     → business logic     ← you are here
 *   Repository  → database access
 * </pre>
 *
 * @see Repository
 * @see Component
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface Service {
    String value() default "";
}
