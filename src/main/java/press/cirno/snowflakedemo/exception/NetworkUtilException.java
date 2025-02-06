package press.cirno.snowflakedemo.exception;

public class NetworkUtilException extends RuntimeException {
    // 强制异常必须抛出异常提示
    public NetworkUtilException(String string) {
        super(string);
    }

    public NetworkUtilException(String string, Throwable throwable) {
        super(string, throwable);
    }
}
