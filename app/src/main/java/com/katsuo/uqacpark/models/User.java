package com.katsuo.uqacpark.models;

import android.support.annotation.Nullable;

public class User {
    private String userId;
    private String username;
    private Boolean isAdmin;
    @Nullable
    private String urlPicture;

    public User() { }

    public User(String userId, String username, String urlPicture) {
        this.userId = userId;
        this.username = username;
        this.urlPicture = urlPicture;
        this.isAdmin = false;
    }


    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getUrlPicture() { return urlPicture; }
    public Boolean getIsAdmin() { return isAdmin; }


    public void setUsername(String username) { this.username = username; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setUrlPicture(String urlPicture) { this.urlPicture = urlPicture; }
    public void setIsAdmin(Boolean admin) { isAdmin = admin; }
}
