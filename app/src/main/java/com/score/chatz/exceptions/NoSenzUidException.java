package com.score.chatz.exceptions;

/**
 * Exception what will be raised if senz packet cannot be identified.
 *
 * Created by Lakmal on 9/13/16.
 */
public class NoSenzUidException extends Exception{
    private static final long serialVersionUID = 1L;

    @Override
    public String toString() {
        return "No uid found in senz packet";
    }
}
