package com.appzonegroup.zoneapp;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.koushikdutta.ion.Ion;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

import dejavu.appzonegroup.com.dejavuandroid.DataBases.Contact;

/**
 * Created by emacodos on 2/26/2015.
 */

/**
 * @author Onyejekwe E. C emacodos
 * An {@link android.app.IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class ContactSyncService extends IntentService {

    public static final String TAG = ContactSyncService.class.getSimpleName();

    public static final String NAME_PHONE_NUMBERS = "PhoneNumbers";

    public static final String URL_CONTACT = "http://192.168.2.213:11984/api/Account/GetRegisteredContacts/";

    private Object mContactGroup = new Object();

    public ContactSyncService() {
        super("ContactSyncService");
    }

    public static void startContactSync(Context context) {
        Intent intent = new Intent(context, ContactSyncService.class);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        if (isNetworkAvailable()) {
            ArrayList<String> contactNames = new ArrayList<>();
            ArrayList<String> contactNumbers = new ArrayList<>();

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
            } while (people.moveToNext());

            JSONObject json = new JSONObject();
            JSONArray array = new JSONArray();
            for (int i = 0; i < contactNumbers.size(); i++) {
                array.put(contactNumbers.get(i));
            }
            try {
                json.put(NAME_PHONE_NUMBERS, array);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            String contactStr = getZoneContact(json.toString());

            if (contactStr != null) {
                try {
                    JSONArray jsonArray = new JSONArray(contactStr);

                    // Delete All Contacts in db
                    ArrayList<Contact> oldContact = Contact.getAllContact(this);
                    for (int i = 0; i < oldContact.size(); i++) {
                        oldContact.get(i).delete(this);
                    }

                    for (int i=0; i<jsonArray.length(); i++){
                        String name = null, number;
                        number = (String) jsonArray.get(i);
                        for (int j=0; j<contactNumbers.size(); j++){
                            if (contactNumbers.get(j).equals(number)){
                                name = contactNames.get(j);
                                break;
                            }
                        }
                        // Add new Contact
                        Contact contact = new Contact();
                        contact.setName(name);
                        contact.setNumber(number);
                        contact.save(this);
                    }
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String getZoneContact(String contact) {
        //This method connects to the internet to verify a contact
        String output = null;
        if (isNetworkAvailable()) {
            JsonObject json = new JsonParser().parse(contact).getAsJsonObject();
            try {
                output = Ion.with(this)
                        .load("GET", URL_CONTACT)
                        .setJsonObjectBody(json)
                        .group(mContactGroup)
                        .asString()
                        .get();
            }
            catch (Exception e) {
                e.printStackTrace();
                Log.e("err_contact_url", e.getMessage());
            }
            if (output != null)
                Log.e(TAG+"/Output", output);
            return output;
        }
        else {
            try {
                setMobileDataEnabled(this, true);
                if(isNetworkAvailable())
                    return getZoneContact(contact);
            }
            catch (Exception e) {
                e.printStackTrace();
                Log.e("err_enable_network", e.getMessage());
            }
        }
        return null;
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

    private void setMobileDataEnabled(Context context, boolean enabled) throws Exception {
        final ConnectivityManager conman = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final Class conmanClass = Class.forName(conman.getClass().getName());
        final Field iConnectivityManagerField = conmanClass.getDeclaredField("mService");
        iConnectivityManagerField.setAccessible(true);
        final Object iConnectivityManager = iConnectivityManagerField.get(conman);
        final Class iConnectivityManagerClass = Class.forName(iConnectivityManager.getClass().getName());
        final Method setMobileDataEnabledMethod = iConnectivityManagerClass.getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
        setMobileDataEnabledMethod.setAccessible(true);

        setMobileDataEnabledMethod.invoke(iConnectivityManager, enabled);
    }
}
