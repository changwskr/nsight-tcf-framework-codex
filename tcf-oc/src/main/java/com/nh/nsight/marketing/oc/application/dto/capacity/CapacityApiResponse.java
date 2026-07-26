package com.nh.nsight.marketing.oc.application.dto.capacity;

public class CapacityApiResponse<T> {

    private boolean success;
    private String message;
    private T data;

    public static <T> CapacityApiResponse<T> ok(T data) {
        CapacityApiResponse<T> response = new CapacityApiResponse<>();
        response.success = true;
        response.data = data;
        return response;
    }

    public static <T> CapacityApiResponse<T> ok(T data, String message) {
        CapacityApiResponse<T> response = ok(data);
        response.message = message;
        return response;
    }

    public static <T> CapacityApiResponse<T> fail(String message) {
        CapacityApiResponse<T> response = new CapacityApiResponse<>();
        response.success = false;
        response.message = message;
        return response;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
