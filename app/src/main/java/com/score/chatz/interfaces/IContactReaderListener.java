package com.score.chatz.interfaces;

import com.score.chatz.pojo.Contact;

import java.util.ArrayList;

/**
 * Created by eranga on 12/10/16.
 */

public interface IContactReaderListener {
    void onPostRead(ArrayList<Contact> contactList);
}
