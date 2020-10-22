package com.sepo.web.disk.common.models;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class User implements Sendable {

    private int id;
    private String email;
    private String password;

    public User(String email, String password) {
        this.email = email;
        this.password = passwordHash(password);
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    private String passwordHash(String pass) {
        String generatedPassword = null;
        try {
            var salt = new StringBuilder(email).reverse().toString();
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            md.update(salt.getBytes(StandardCharsets.UTF_8));
            byte[] bytes = md.digest(pass.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for(int i=0; i< bytes.length ;i++){
                sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
            }
            generatedPassword = sb.toString();
        }
        catch (NoSuchAlgorithmException e){
            e.printStackTrace();
        }
        return generatedPassword;
    }
}

