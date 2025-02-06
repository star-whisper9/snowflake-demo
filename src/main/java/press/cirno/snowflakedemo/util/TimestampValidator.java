package press.cirno.snowflakedemo.util;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class TimestampValidator implements ConstraintValidator<ValidTime, Long> {
    @Override
    public boolean isValid(Long value, ConstraintValidatorContext context) {
        return value > 0 && value <= System.currentTimeMillis();
    }
}
