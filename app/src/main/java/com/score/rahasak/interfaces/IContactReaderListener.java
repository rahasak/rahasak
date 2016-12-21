package com.score.rahasak.interfaces;

import com.score.rahasak.pojo.Contact;

import java.util.ArrayList;

/**
 * Created by eranga on 12/10/16.
 */

public interface IContactReaderListener {
    void onPostRead(ArrayList<Contact> contactList);
}
