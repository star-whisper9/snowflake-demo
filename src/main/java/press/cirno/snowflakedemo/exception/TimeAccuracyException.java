package press.cirno.snowflakedemo.exception;

public class TimeAccuracyException extends RuntimeException {
    public TimeAccuracyException(String message) {
        super(message);
    }

    public TimeAccuracyException() {
        super();
    }
}
