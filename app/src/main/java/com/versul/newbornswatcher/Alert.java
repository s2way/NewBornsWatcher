package com.versul.newbornswatcher;

/**
 * Created by dev on 1/20/16.
 */
public class Alert {

    private String origin;
    private String message;

    public Alert(String origin, String message) {
        this.origin = origin;
        this.message = message;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
