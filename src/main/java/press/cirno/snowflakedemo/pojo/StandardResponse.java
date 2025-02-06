package press.cirno.snowflakedemo.pojo;

import lombok.Data;

@Data
public class StandardResponse<T> {
    private int code;
    private String message;
    private T data;

    public StandardResponse() {
    }

    public StandardResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> StandardResponse<T> fail(String message) {
        return new StandardResponse<>(-100, message, null);
    }

    public static <T> StandardResponse<T> fail() {
        return new StandardResponse<>(-100, "请求失败", null);
    }

    public static <T> StandardResponse<T> fail(int code, String message) {
        return new StandardResponse<>(code, message, null);
    }

    public static <T> StandardResponse<T> success() {
        return new StandardResponse<>(0, "请求成功", null);
    }

    public static <T> StandardResponse<T> success(T data) {
        return new StandardResponse<>(0, "请求成功", data);
    }
}
