package com.appzonegroup.zoneapp;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.content.LocalBroadcastManager;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

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

    public static final String TAG = ContactSyncService.class.getSimpleName();

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

        if (isNetworkAvailable()) {
            ArrayList<String> contactNames = new ArrayList<>();
            ArrayList<String> contactNumbers = new ArrayList<>();
            List<NameValuePair> nameValuePair = new ArrayList<NameValuePair>();

            Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
            String[] projection = new String[] {ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER};
            String selection       = ContactsContract.Contacts.HAS_PHONE_NUMBER + " = '1'";

            Cursor people = getContentResolver().query(uri, projection, selection, null, null);

            int indexName = people.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
            int indexNumber = people.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

            people.moveToFirst();
            do {
                String name   = people.getString(indexName);
                String number = people.getString(indexNumber);
                // Do work...
                contactNames.add(name);
                contactNumbers.add(number);
                nameValuePair.add(new BasicNameValuePair("name", name));
                nameValuePair.add(new BasicNameValuePair("phone", number));
            } while (people.moveToNext());

            ArrayList<String> contactNumberList = new ArrayList<String>();
            ArrayList<String> contactNameList = new ArrayList<String>();

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
                    contact.save(this, i);
                }
            }
        } else {
            //No Internet Available
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

    private void sendMessage(String message) {
        Intent intent = new Intent("my-event");
        // add data
        intent.putExtra("message", message);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
