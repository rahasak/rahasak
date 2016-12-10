package com.score.chatz.pojo;

/**
 * Created by eranga on 12/10/16.
 */
public class Contact {
    private String id;
    private String name;
    private String phoneNo;

    public Contact(String id, String name, String phoneNo) {
        this.id = id;
        this.name = name;
        this.phoneNo = phoneNo;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNo() {
        return phoneNo;
    }

    public void setPhoneNo(String phoneNo) {
        this.phoneNo = phoneNo;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Contact)) {
            return false;
        }

        Contact that = (Contact) other;
        return this.name.equalsIgnoreCase(that.name) && this.phoneNo.equalsIgnoreCase(that.phoneNo);
    }
}
