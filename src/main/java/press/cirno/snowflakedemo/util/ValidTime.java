package press.cirno.snowflakedemo.util;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = TimestampValidator.class)
public @interface ValidTime {
    String message() default "时间戳不合法";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
