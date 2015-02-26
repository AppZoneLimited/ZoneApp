package com.appzonegroup.zoneapp;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

import database.Contact;

/**
 * Created by emacodos on 2/26/2015.
 */

/**
 * @author Onyejekwe E. C emacodos
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class ContactSyncService extends IntentService {

    public static final String PARAM_CONTACT_NAMES = "comm.appzonegroup.zoneapp.CONTACTS_NAMES";
    public static final String PARAM_CONTACT_NUMBERS = "comm.appzonegroup.zoneapp.CONTACTS_NUMBERS";
    public static final String PARAM_CONTACTS = "comm.appzonegroup.zoneapp.CONTACTS";

    public ContactSyncService() {
        super("ContactSyncService");
    }

    public static void startContactSync(Context context, Bundle contacts) {
        Intent intent = new Intent(context, ContactSyncService.class);
        intent.putExtra(PARAM_CONTACTS, contacts);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        if(extras != null) {
            Bundle contacts = extras.getBundle(PARAM_CONTACTS);
            ArrayList<String> contactNumberList = new ArrayList<String>();
            ArrayList<String> contactNameList = new ArrayList<String>();
            if (isNetworkAvailable()) {
                ArrayList<String> contactNames = contacts.getStringArrayList(PARAM_CONTACT_NAMES);
                ArrayList<String> contactNumbers = contacts.getStringArrayList(PARAM_CONTACT_NUMBERS);

                for (int i = 0; i < contactNumbers.size(); i++) {
                    if (isZoneContact(contactNumbers.get(i))) {
                        contactNumberList.add(contactNumbers.get(i));
                        contactNameList.add(contactNames.get(i));
                    }
                }

                if (contactNumberList.size() > 0) {
                    // Delete All Contacts in db
                    List<Contact> oldContact = Contact.objects(this).all().toList();
                    for (int i = 0; i < oldContact.size(); i++) {
                        oldContact.get(i).delete(this);
                    }
                    // Add new Contact
                    for (int i = 0; i < contactNumberList.size(); i++) {
                        Contact contact = new Contact();
                        contact.setName(contactNameList.get(i));
                        contact.setNumber(contactNumberList.get(i));
                        contact.save(this);
                    }
                }
            } else {
                //No Internet Available
            }
        }
    }

    private boolean isZoneContact(String contact) {
        //This method connects to the internet to verify a contact
        return true;
    }

    /*
    * Check for network connection availability
    */
    private boolean isNetworkAvailable(){
        ConnectivityManager connectivityManager = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }
}
