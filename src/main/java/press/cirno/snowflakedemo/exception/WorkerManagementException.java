package press.cirno.snowflakedemo.exception;

public class WorkerManagementException extends RuntimeException {
    public WorkerManagementException(String string) {
        super(string);
    }

    public WorkerManagementException() {
        super();
    }
}
