package com.example.proyecto_grupo_3.api;

import com.google.gson.annotations.SerializedName;

public class ApiResponse<T> {

    @SerializedName("code")
    private int code;

    @SerializedName("data")
    private T data;

    @SerializedName("msge")
    private String msge;

    public ApiResponse() {
    }

    public ApiResponse(int code, T data, String msge) {
        this.code = code;
        this.data = data;
        this.msge = msge;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getMsge() {
        return msge;
    }

    public void setMsge(String msge) {
        this.msge = msge;
    }
}
