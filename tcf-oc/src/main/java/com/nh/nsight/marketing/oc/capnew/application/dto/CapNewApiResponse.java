package com.nh.nsight.marketing.oc.capnew.application.dto;

public class CapNewApiResponse<T> {

    private boolean success;
    private String message;
    private T data;

    public static <T> CapNewApiResponse<T> ok(T data) {
        CapNewApiResponse<T> response = new CapNewApiResponse<>();
        response.success = true;
        response.data = data;
        return response;
    }

    public static <T> CapNewApiResponse<T> ok(T data, String message) {
        CapNewApiResponse<T> response = ok(data);
        response.message = message;
        return response;
    }

    public static <T> CapNewApiResponse<T> fail(String message) {
        CapNewApiResponse<T> response = new CapNewApiResponse<>();
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
