package com.appzonegroup.zoneapp;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.orm.androrm.Filter;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import database.ClientFlows;
import database.EntityFlows;
import database.Function;
import database.Link;

/**
 * Created by emacodos on 2/26/2015.
 */

/**
 * @author Onyejekwe E. C emacodos
 *
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 *
 *              // ACTION_SYNC //
 *
 * This Service Synchronise data on the local storage with that on the Server using
 * the following steps
 *
 * 1. download current server function list
 * 2. get the local function list
 * 3. Check the functions available in the local but not on the server and add to delete list
 *  a. delete the flows associated with the delete list from the db.
 *  b. delete the links   ""                               ""
 *  c. delete the functions on the delete list from the db
 * 4. Check for new function in the server list and not on the local list and add to new list
 *  a. get the link for the flows associated with the new list and save on the db
 *  b. download the flows and store in db
 *  c. mark flows as success
 *  d. mark function as success.
 * 5. Check for function on the server that has new version and add to update list.
 *  a. get the updated list of functions
 *  b. delete the old flows associated with it in the update list.
 *  c. download the latest flows
 *
 */
public class FlowSyncService extends IntentService {

    /**
     * Network connection timeout, in milliseconds.
     */
    private static final int NET_CONNECT_TIMEOUT_MILLIS = 15000;  // 15 seconds

    /**
     * Service class tableName.
     */
    private static final String TAG = FlowSyncService.class.getSimpleName();

    /**
     * Network read timeout, in milliseconds.
     */
    private static final int NET_READ_TIMEOUT_MILLIS = 10000;  // 10 seconds

    // Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    public static final String ACTION_SYNC = "com.appzonegroup.zoneapp.action.SYNC";
    public static final String ACTION_CLOUD_MESSAGE = "com.appzonegroup.zoneapp.action.CLOUD.MESSAGE";

    /**
     * Starts this service to perform action Download with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */

    private static final String PARAM_FUNCTION_ID = "com.appzonegroup.zoneapp.param.PARAM_FUNCTION_ID";

    // Customize helper method

    public static void startActionSync(Context context) {
        Intent intent = new Intent(context, FlowSyncService.class);
        intent.setAction(ACTION_SYNC);
        context.startService(intent);
    }

    public static void startActionCloudMessage(Context context, int function_Id) {
        Intent intent = new Intent(context, FlowSyncService.class);
        intent.setAction(ACTION_CLOUD_MESSAGE);
        intent.putExtra(PARAM_FUNCTION_ID, function_Id);
        context.startService(intent);
    }

    public FlowSyncService() {
        super("FlowSyncService");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "Service is starting",
                Toast.LENGTH_LONG).show();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_SYNC.equals(action)){
                handleActionSync();
            }
            else if (ACTION_CLOUD_MESSAGE.equals(action)){
                final int functionId = intent.getIntExtra(PARAM_FUNCTION_ID, 0);
                handleActionCloudMessage(functionId);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Toast.makeText(this, "Service is done updating",
                Toast.LENGTH_LONG).show();
    }

    /**
     * Handle action Sync in the provided background thread with the provided
     * parameters.
     */
    private void handleActionSync() {
        ArrayList<Function> serverList = (ArrayList<Function>) getFunctionListFromServer();
        if(serverList.size() > 0) {
            ArrayList<Function> localList = (ArrayList<Function>) Function.objects(this).all().toList();
            ArrayList<Function> newList = getNewList(serverList, localList),
                    deleteList = getDeleteList(serverList, localList),
                    updateList = getUpdateList(serverList, localList);

            //Delete the functions on deleteList and related components from the db.
            for(int i=0; i<deleteList.size(); i++){
                Function f = deleteList.get(i);
                deleteFlows(f);
                f.delete(this);

            }

            //Add new functions in the newList and its related components
            for(int i=0; i<newList.size(); i++){
                if(downloadFlows(newList.get(i))){
                    newList.get(i).setSuccess(true);
                    newList.get(i).save(this);
                }
            }

            //Update the function in updateList and its associated components
            ArrayList<Function> updatedList = getUpdatedList(serverList, localList);
            //Delete the flows associated with it
            for(int i=0; i<updateList.size(); i++){
                deleteFlows(updateList.get(i));
                updateList.get(i).delete(this);

            }
            //Add the Updated Flows
            for (int i=0; i<updatedList.size(); i++){
                if(downloadFlows(updatedList.get(i))) {
                    updatedList.get(i).setSuccess(true);
                    updatedList.get(i).save(this);
                }
            }
        }

        //Upload Data to server
    }

    /**
     * Handle action Download in the provided background thread with the provided
     * parameters.
     */
    private void handleActionCloudMessage(int functionId) {
        //handle what happens when message is gotten from the server
        Function function = Function.objects(this).get(functionId);
        //delete old flows
        deleteFlows(function);
        function.delete(this);

        //add new flows
        if(downloadFlows(function)) {
            function.setSuccess(true);
            function.save(this);
        }
    }

    private void deleteFlows(Function function) {
        //Delete the Links associated with the function
        List<Link> links = function.getLinks(this).all().toList();
        for (int j=0; j<links.size(); j++){
            Link l = links.get(j);
            l.delete(this);
        }
        // Delete the ClientFlows associated with the function
        List<ClientFlows> clientFlows = function.getClientFlows(this).all().toList();
        for(int j=0; j<clientFlows.size(); j++){
            ClientFlows c = clientFlows.get(j);
            c.delete(this);
        }
        // Delete the EntityFlows associated with the function
        List<EntityFlows> entityFlows = function.getEntityFlows(this).all().toList();
        for(int j=0; j<entityFlows.size(); j++){
            EntityFlows c = entityFlows.get(j);
            c.delete(this);
        }
    }

    private List<Link> getNewLinks(Function function) {
        //Save Function to db
        function.save(this);
        //check on links in db associated with the function not successful
        Filter filter = new Filter();
        filter.is("success",0);
        //save associated links
        Link link = new Link();
        link.setType("client");
        link.setUrl("http://blog.teamtreehouse.com/api/get_recent_summary/");
        link.setFunction(function);
        link.save(this);

        Link link1 = new Link();
        link1.setType("client");
        link1.setUrl("http://blog.teamtreehouse.com/api/get_recent_summary/");
        link1.setFunction(function);
        link1.save(this);

        Link link2 = new Link();
        link2.setType("entity");
        link2.setUrl("http://blog.teamtreehouse.com/api/get_recent_summary/");
        link2.setFunction(function);
        link2.save(this);

        return function.getLinks(this).all().toList();
    }

    private ArrayList<Function> getUpdatedList(List<Function> serverList, List<Function> localList) {
        ArrayList<Function> updatedList = new ArrayList<Function>();
        for (int i=0; i<localList.size(); i++){
            for (int j=0; j<serverList.size(); j++) {
                if (localList.get(i).getFunctionId().equalsIgnoreCase(serverList.get(j).getFunctionId())) {
                    if(localList.get(i).getVersion() < serverList.get(j).getVersion()){
                        updatedList.add(serverList.get(j));
                    }
                    break;
                }
            }
        }
        return updatedList;
    }

    private ArrayList<Function> getUpdateList(List<Function> serverList, List<Function> localList) {
        ArrayList<Function> updateList = new ArrayList<Function>();
        for (int i=0; i<localList.size(); i++){
            for (int j=0; j<serverList.size(); j++) {
                if (localList.get(i).getFunctionId().equalsIgnoreCase(serverList.get(j).getFunctionId())) {
                    if(localList.get(i).getVersion() < serverList.get(j).getVersion()){
                        updateList.add(localList.get(i));
                    }
                    break;
                }
            }
        }
        return updateList;
    }

    private ArrayList<Function> getNewList(ArrayList<Function> serverList, ArrayList<Function> localList) {
        ArrayList<Function> newList = new ArrayList<Function>();
        for (int i=0; i<serverList.size(); i++){
            if(localList.size() > 0) {
                for (int j = 0; j < localList.size(); j++) {
                    if (serverList.get(i).getFunctionId().equalsIgnoreCase(localList.get(j).getFunctionId())) {
                        break;
                    }
                    newList.add(serverList.get(i));
                }
            }
            else {
                newList = serverList;
            }
        }
        return newList;
    }

    private ArrayList<Function> getDeleteList(List<Function> serverList, List<Function> localList) {
        ArrayList<Function> deleteList = new ArrayList<Function>();
        for (int i=0; i<localList.size(); i++){
            for (int j=0; j<serverList.size(); j++) {
                if (localList.get(i).getFunctionId().equalsIgnoreCase(serverList.get(j).getFunctionId())) {
                    break;
                }
                else {
                    deleteList.add(localList.get(i));
                }
            }
        }
        return deleteList;
    }

    private List<Function> getFunctionListFromServer() {

        Function fxn1 = new Function();
        fxn1.setFunctionName("team");
        fxn1.setVersion(1);
        fxn1.setFunctionId("a1");

        Function fxn2 = new Function();
        fxn2.setFunctionName("team");
        fxn2.setVersion(1);
        fxn2.setFunctionId("a1");

        Function fxn3 = new Function();
        fxn3.setFunctionName("team");
        fxn3.setVersion(1);
        fxn3.setFunctionId("a1");

        ArrayList<Function> list = new ArrayList<Function>();
        list.add(fxn1);
        list.add(fxn2);
        list.add(fxn3);

        return list;
    }

    /*
    * Get flows based on functions
    */
    private boolean downloadFlows(Function function) {
        String flowsContent;
        boolean success = false;
        List<Link> urls = getNewLinks(function);
        for(int i=0; i<urls.size(); i++) {
            if (isNetworkAvailable()) {
                //Network Available
                try {
                    flowsContent = downloadFlowsAsString(urls.get(i).getUrl());
                    switch (urls.get(i).getType()) {
                        case "entity":
                            EntityFlows flow = new EntityFlows();
                            flow.setSync(true);
                            flow.setEntityFlow(flowsContent);
                            flow.setFunction(function);
                            flow.setLink(urls.get(i));
                            flow.save(this);
                            break;

                        case "client":
                            ClientFlows flows = new ClientFlows();
                            flows.setFunction(function);
                            flows.setClientFlow(flowsContent);
                            flows.setLink(urls.get(i));
                            flows.save(this);
                            break;

                        default:
                            //Unknown Flow
                    }
                    urls.get(i).setSuccess(true);
                    urls.get(i).save(this);

                    success = true;
                } catch (IOException e) {
                    //Interrupted Download
                    return false;
                }
            } else {
                // Network Unavailable
                return false;
            }
        }
        sendMessage(function.getId());
        return success;
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

    /*
    * Read data from the server as string
    */
    private String downloadFlowsAsString(String url) throws IOException{
        String contentAsString = null;
        InputStream inputStream = null;
        try{
            URL webUrl = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) webUrl.openConnection();
            conn.setReadTimeout(NET_READ_TIMEOUT_MILLIS /* milliseconds */);
            conn.setConnectTimeout(NET_CONNECT_TIMEOUT_MILLIS /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            // Starts the query
            conn.connect();
            int response = conn.getResponseCode();
            Log.d(TAG, "The response is: " + response);
            if(response == HttpURLConnection.HTTP_OK) {
                inputStream = conn.getInputStream();

                // Convert the InputStream into a string
                contentAsString = readIt(inputStream);
            }
            return contentAsString;

            // Makes sure that the InputStream is closed after the app is
            // finished using it.
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    /*
    * Reads an InputStream and converts it to a String.
    */
    public String readIt(InputStream stream) throws IOException {
        BufferedInputStream bis = new BufferedInputStream(stream);
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int result = bis.read();
        while(result != -1) {
            byte b = (byte)result;
            buf.write(b);
            result = bis.read();
        }
        return buf.toString();
    }

    private void sendMessage(int no) {
        Intent intent = new Intent("my-event");
        // add data
        intent.putExtra("message", no);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

}
