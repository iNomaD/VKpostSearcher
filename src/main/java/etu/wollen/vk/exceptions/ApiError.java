package etu.wollen.vk.exceptions;

public class ApiError extends Exception {

    public ApiError(String message){
        super(message);
    }

    public ApiError(String message, Throwable cause) {
        super(message, cause);
    }

    public ApiError(Throwable cause) {
        super(cause);
    }
}
