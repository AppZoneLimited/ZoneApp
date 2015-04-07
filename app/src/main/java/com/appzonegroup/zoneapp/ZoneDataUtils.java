package com.appzonegroup.zoneapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.orm.androrm.DatabaseAdapter;
import com.orm.androrm.Model;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import database.ClientFlows;
import database.Contact;
import database.Entity;
import database.Function;
import database.FunctionCategory;

/**
 * Created by emacodos on 4/2/2015.
 */
public class ZoneDataUtils {


    /*
    * Sync database tables
    */
    public static void syncDB(Context context) {
        List<Class<? extends Model>> models = new ArrayList<Class<? extends Model>>();
        models.add(Function.class);
        models.add(FunctionCategory.class);
        models.add(ClientFlows.class);
        models.add(Entity.class);
        models.add(Contact.class);

        DatabaseAdapter.setDatabaseName("zone_db");
        DatabaseAdapter adapter = new DatabaseAdapter(context);
        adapter.setModels(models);
    }

    /*
    * Set alarm service for daily data sync
    */
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
    private boolean isNetworkAvailable(Context context){
        ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }
}
