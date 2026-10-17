package com.example.proyecto_grupo_3.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class AuthRequest implements Serializable {

    @SerializedName("email")
    private String email;

    @SerializedName("password")
    private String password;

    public AuthRequest() {
    }

    public AuthRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
