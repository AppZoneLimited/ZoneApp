package com.appzonegroup.zoneapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.orm.androrm.DatabaseAdapter;
import com.orm.androrm.Model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import database.ClientFlows;
import database.Contact;
import database.Entity;
import database.EntityFlows;
import database.Function;
import database.Link;
import json.Data;
import json.EntityData;
import json.Instruction;


public class MainActivity extends ActionBarActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private TextView mHello1, mHello2, mHello3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHello1 = (TextView) findViewById(R.id.textView1);
        mHello2 = (TextView) findViewById(R.id.textView2);
        mHello3 = (TextView) findViewById(R.id.textView3);

        syncDB();

//        startFlowDownloadService();
//        startAlarmService(this, 1000 * 60 * 60 * 24);

        copyDBToSDCard();

        Instruction in = new Instruction();
        in.setType(LocalEntityService.TYPE_LOCAL);
        in.setOperation(LocalEntityService.OPERATION_CREATE);
        in.setEntity("Customer");

        EntityData entityData = new EntityData();
        entityData.setName("firstname");
        entityData.setValue("emmanuel");
        EntityData entityData1 = new EntityData();
        entityData1.setName("address");
        entityData1.setValue("Lokoja");

        ArrayList<EntityData> datas = new ArrayList<EntityData>();
        datas.add(entityData);
        datas.add(entityData1);

        Data d = new Data();
        d.setData(datas);

        Gson json = new Gson();
        String ins = json.toJson(in), data = json.toJson(d);

        String name = LocalEntityService.localEntityService(this, ins, data);
        Log.e(TAG, name);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register mMessageReceiver to receive messages.
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("my-event"));
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister since the activity is not visible
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // handler for received Intents for the "my-event" event
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent
            int message = intent.getIntExtra("message", 0);
            if(message == 1)
            mHello1.setText(message+"");
            if(message == 2)
                mHello2.setText(message+"");
            if(message == 3)
                mHello3.setText(message+"");
            Log.d("receiver", "Got message: " + message+"");
        }
    };

    private void syncDB() {
        List<Class<? extends Model>> models = new ArrayList<Class<? extends Model>>();
        models.add(Function.class);
        models.add(ClientFlows.class);
        models.add(EntityFlows.class);
        models.add(Link.class);
        models.add(Entity.class);
        models.add(Contact.class);

        DatabaseAdapter.setDatabaseName("zone_db");
        DatabaseAdapter adapter = new DatabaseAdapter(getApplicationContext());
        adapter.setModels(models);
    }

    private void startFlowDownloadService() {
        // Handle action Download
        if(isNetworkAvailable()) {
            // Network is available
            FlowSyncService.startActionSync(this);
            ContactSyncService.startContactSync(this, getContact());
        }
        else {
            //No Internet Connection
            Log.i(TAG, "No Internet Access");
            Toast.makeText(this, "No Internet Access"
                    , Toast.LENGTH_LONG).show();
        }
    }

    public static Bundle getContact(){
        Bundle contact = new Bundle();

        ArrayList<String> names = new ArrayList<String>();
        names.add("Emma");
        names.add("Esther");
        ArrayList<String> numbers = new ArrayList<String>();
        numbers.add("07030324132");
        numbers.add("07003021346");
        contact.putStringArrayList(ContactSyncService.PARAM_CONTACT_NAMES, names);
        contact.putStringArrayList(ContactSyncService.PARAM_CONTACT_NUMBERS, numbers);

        return contact;
    }

    private void startAlarmService(Context context, int interval){
        AlarmManager service = (AlarmManager) context
                .getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, MyStartServiceReceiver.class);
        PendingIntent pending = PendingIntent.getBroadcast(context, 0, i,
                PendingIntent.FLAG_CANCEL_CURRENT);
        Calendar cal = Calendar.getInstance();
        // start 24 hrs after boot completed
        cal.add(Calendar.SECOND, 60 * 24);
        // fetch every 24 hours
        // InexactRepeating allows Android to optimize the energy consumption
//        service.setInexactRepeating(AlarmManager.RTC_WAKEUP,
//                cal.getTimeInMillis(), interval, pending);

         service.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(),
         interval, pending);
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

    public void copyDBToSDCard() {
        try {
            InputStream myInput = new FileInputStream("/data/data/com.appzonegroup.zoneapp/databases/zone_db");

            File file = new File(Environment.getExternalStorageDirectory().getPath()+"/zone_db");
            if (!file.exists()){
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    Log.e("FO","File creation failed for " + file);
                }
            }

            OutputStream myOutput = new FileOutputStream(Environment.getExternalStorageDirectory().getPath()+"/zone_db");

            byte[] buffer = new byte[1024];
            int length;
            while ((length = myInput.read(buffer))>0){
                myOutput.write(buffer, 0, length);
            }

            //Close the streams
            myOutput.flush();
            myOutput.close();
            myInput.close();
            Log.e("FO","copied");

        } catch (Exception e) {
            Log.e("FO","exception="+e);
        }
    }
}
