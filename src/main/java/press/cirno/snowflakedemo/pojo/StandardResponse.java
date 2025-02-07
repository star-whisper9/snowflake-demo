package press.cirno.snowflakedemo.pojo;

import lombok.Data;

/**
 * 标准响应<br >
 * 返回 code：0 成功，-100 默认失败。其他遵循 负数失败，正数成功/未知
 *
 * @param <T> 返回体类型
 */
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
