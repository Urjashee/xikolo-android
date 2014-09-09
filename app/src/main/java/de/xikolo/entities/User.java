package de.xikolo.entities;

import com.google.gson.annotations.SerializedName;

public class User {

    @SerializedName("id")
    public String id;

    @SerializedName("first_name")
    public String first_name;

    @SerializedName("last_name")
    public String last_name;

    @SerializedName("email")
    public String email;

}
