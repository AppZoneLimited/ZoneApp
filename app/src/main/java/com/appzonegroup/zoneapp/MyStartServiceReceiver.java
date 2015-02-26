package com.appzonegroup.zoneapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MyStartServiceReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.

        //Start Flow Sync
        FlowSyncService.startActionSync(context);

        //Start Contact Sync
        ContactSyncService.startContactSync(context,MainActivity.getContact());
    }
}
