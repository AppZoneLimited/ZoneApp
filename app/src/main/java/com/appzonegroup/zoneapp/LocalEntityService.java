package com.appzonegroup.zoneapp;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.orm.androrm.Filter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import database.Entity;

/**
 * Created by emacodos on 3/7/2015.
 */

/*
* NOTES:
* For the Local event type
*
* 1. create: add new data to the data to the database ie the data passed on the create method
* 2. update: updates an existing record on the database. when data parameter on the update method
 *           is null, an error is returned. while otherwise the id from the data is used to identify
 *           the row to be updated.
 *3. retrieve: gets the records stored in the database, if data parameter is null, every row
 *             associated with the entity is retrieved. while otherwise the id from the data is used
 *             to identify the row to be retrieved.
 *4. delete: deletes the records stored in the database. if data parameter is null, every row
 *           associated with the entity is deleted. while otherwise the id from the data parameter
 *           is used to identify the row to be deleted.
*/

public class LocalEntityService extends IntentService{

    public static final String TAG = LocalEntityService.class.getSimpleName();

    public static final String PARAM_INSTRUCTION = "instruction";
    public static final String PARAM_DATA = "data";

    public static final String OPERATION_CREATE = "create";
    public static final String OPERATION_UPDATE = "update";
    public static final String OPERATION_RETRIEVE = "retrieve";
    public static final String OPERATION_DELETE = "delete";

    public static final String NAME_TYPE = "type";
    public static final String NAME_OPERATION = "operation";
    public static final String NAME_ENTITY = "entity";
    public static final String NAME_EVENT_NAME = "eventName";
    public static final String NAME_EVENT_DATA = "eventData";
    public static final String NAME_REASON = "reason";

    public static final String VALUE_CREATED = "Entity Created";
    public static final String VALUE_UPDATED = "Entity Updated";
    public static final String VALUE_RETRIEVED = "Entity Retrieved";
    public static final String VALUE_DELETED = "Entity Deleted";

    public static final String EVENT_NAME_ERROR = "Entity Operation Failed";

    public static final String TYPE_SERVER = "server";
    public static final String TYPE_LOCAL = "local";

    public static final String URL_ENTITY = "http://localhost/etrademanager";

    private static Context mContext;
    private Object mEntityGroup = new Object();
    private String mOutput;

    public LocalEntityService(){
        super("LocalEntityService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Ion.getDefault(this).configure().setLogging(TAG, Log.ERROR);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if(intent != null) {
            String inst = intent.getStringExtra(PARAM_INSTRUCTION);
            String dat = intent.getStringExtra(PARAM_DATA);
            String message = localEntityService(this, inst, dat);
            sendMessage(message);
        }
    }

    public static void startLocalEntityService(Context context, String instruction, String data) {
        Intent intent = new Intent(context, LocalEntityService.class);
        intent.putExtra(PARAM_INSTRUCTION, instruction);
        intent.putExtra(PARAM_DATA, data);
        context.startService(intent);
    }

    public String localEntityService(Context context, String instruction, String data){
        String type, operation, entity;

        mContext = context;

        if(instruction == null || TextUtils.isEmpty(instruction)){
            return eventError("Null instruction Value");
        }
        else {
            //Parse JSON instruction to Java Object
            try {
                JSONObject inst = new JSONObject(instruction);
                type = inst.getString(NAME_TYPE);
                operation = inst.getString(NAME_OPERATION);
                entity = inst.getString(NAME_ENTITY);
            }
            catch (Exception e){
                return eventError("Bad Instruction");
            }

            //Check for the method type
            switch (type) {
                case TYPE_SERVER:
                    //Do server work
                    return doServerOperation(instruction, data);

                case TYPE_LOCAL:
                    //Do local work
                    switch (operation) {
                        case OPERATION_CREATE:
                            //Do Create
                            return addNewRecord(entity, data);

                        case OPERATION_RETRIEVE:
                            //Do Retrieve
                            return queryDB(entity, data);

                        case OPERATION_UPDATE:
                            //Do Update
                            return updateRecord(entity, data);

                        case OPERATION_DELETE:
                            //Do Delete
                            return deleteRecord(entity, data);

                        default:
                            return eventError("Unknown Operation");
                    }
                default:
                    return eventError("Unknown Type");
            }
        }
    }


    public String doServerOperation(String instruction, String data) {
        if(instruction == null || TextUtils.isEmpty(instruction)){
            return eventError("Null Instruction Value");
        }
        else if(data == null || TextUtils.isEmpty(data)){
            return eventError("Null data Value");
        }
        else {
            if (isNetworkAvailable()) {
                Ion.with(mContext)
                        .load("POST", URL_ENTITY)
                        .setBodyParameter(PARAM_INSTRUCTION, instruction)
                        .setBodyParameter(PARAM_DATA, data)
                        .group(mEntityGroup)
                        .asString()
                        .setCallback(new FutureCallback<String>() {
                            @Override
                            public void onCompleted(Exception e, String result) {
                                if (e != null) {
                                    //success
                                    mOutput = result;
                                } else {
                                    //failure
                                    mOutput = eventError("Network Exception");
                                }
                            }
                        });
                if (mOutput != null)
                    return mOutput;
                else
                    return eventError("Cannot Connect to the Url");
            }
            else {
                return eventError("Internet Unavailable");
            }
        }
    }

    public static String addNewRecord(String entity, String data) {
        if(entity == null || TextUtils.isEmpty(entity)){
            return eventError("Null entity Value");
        }
        else if(data == null || TextUtils.isEmpty(data)){
            return eventError("Null data Value");
        }
        else {
            Entity table = new Entity();
            table.setEntityName(entity);
            table.setValue(data);
            if(table.save(mContext)){
                return eventSuccess(VALUE_CREATED, data);
            }
            else {
                return eventError("SQL Error");
            }
        }
    }

    public static String queryDB(String entity, String data) {
        if(data == null) {
            Filter filter = new Filter();
            filter.is(Entity.COLUMN_TABLE_NAME, entity);
            ArrayList<Entity> entities = (ArrayList<Entity>) Entity.objects(mContext)
                    .filter(filter).toList();

            if (entities.size() > 0) {
                JSONObject output = new JSONObject();
                JSONArray dataList = new JSONArray();
                for (int i = 0; i < entities.size(); i++) {
                    dataList.put(entities.get(i).getValue());
                }
                try {
                    output.put(NAME_EVENT_NAME, VALUE_RETRIEVED);
                    output.put(NAME_EVENT_DATA, dataList);
                    return output.toString();
                } catch (JSONException e) {
                    e.printStackTrace();
                    return eventError("JSON Error");
                }
            } else {
                return eventError("No such Entity Found");
            }
        }
        else {
            JSONObject object;
            try {
                object = new JSONObject(data);
            }
            catch (JSONException e) {
                e.printStackTrace();
                return eventError("Invalid Data Value");
            }
            String id;
            try {
                id= object.getString(Entity.COLUMN_ID);
            }
            catch (JSONException e) {
                e.printStackTrace();
                return eventError("Invalid ID");
            }
            Filter filter = new Filter();
            filter.is(Entity.COLUMN_TABLE_NAME, entity);
            ArrayList<Entity> entities = (ArrayList<Entity>) Entity.objects(mContext)
                    .filter(filter).toList();
            if (entities.size() > 0) {
                for (int i=0; i<entities.size(); i++) {
                    Entity table = entities.get(i);
                    JSONObject jsonObject = null;
                    try {
                        jsonObject = new JSONObject(table.getValue());
                    }
                    catch (JSONException e) {
                        e.printStackTrace();
                    }
                    String mId = null;
                    if (jsonObject != null) {
                        mId = jsonObject.optString(Entity.COLUMN_ID);
                    }
                    if (mId != null) {
                        if(mId.equals(id)){
                            JSONObject obj;
                            try {
                                obj = new JSONObject(table.getValue());
                            } catch (JSONException e) {
                                e.printStackTrace();
                                return eventError("JSON Error");
                            }
                            return eventSuccess(VALUE_RETRIEVED, obj.toString());
                        }
                    }
                }
            }
            else {
                return eventError("No such Entity Found");
            }
        }
        return eventError("No Match Found");
    }

    public static String updateRecord(String entity, String data) {
        if(data == null || TextUtils.isEmpty(data)){
            return eventError("Null data Value");
        }
        else {
            JSONObject object;
            try {
                 object = new JSONObject(data);
            }
            catch (JSONException e) {
                e.printStackTrace();
                return eventError("Invalid Data Value");
            }
            String id;
            try {
                id= object.getString(Entity.COLUMN_ID);
            }
            catch (JSONException e) {
                e.printStackTrace();
                return eventError("Invalid ID");
            }
            Filter filter = new Filter();
            filter.is(Entity.COLUMN_TABLE_NAME, entity);
            ArrayList<Entity> entities = (ArrayList<Entity>) Entity.objects(mContext)
                    .filter(filter).toList();
            if (entities.size() > 0) {
                for (int i=0; i<entities.size(); i++) {
                    Entity table = entities.get(i);
                    JSONObject jsonObject = null;
                    try {
                        jsonObject = new JSONObject(table.getValue());
                    }
                    catch (JSONException e) {
                        e.printStackTrace();
                    }
                    String mId = null;
                    if (jsonObject != null) {
                        mId = jsonObject.optString(Entity.COLUMN_ID);
                    }
                    if (mId != null) {
                        if (mId.equals(id)) {
                            table.delete(mContext);
                            table.setValue(data);
                            table.save(mContext);
                            return eventSuccess(VALUE_UPDATED, data);
                        }
                    }
                }
            }
            else {
                return eventError("No such Entity Found");
            }
        }
        return eventError("No Match Found");
    }

    public static String deleteRecord(String entity, String data) {
        if(data == null || TextUtils.isEmpty(data)){
            Filter filter = new Filter();
            filter.is(Entity.COLUMN_TABLE_NAME, entity);
            ArrayList<Entity> entities = (ArrayList<Entity>) Entity.objects(mContext)
                    .filter(filter).toList();

            if(entities.size() > 0) {
                JSONObject output = new JSONObject();
                JSONArray dataList = new JSONArray();
                for (int i = 0; i < entities.size(); i++) {
                    String dataStr = entities.get(i).getValue();
                    dataList.put(dataStr);
                    entities.get(i).delete(mContext);
                }
                try {
                    output.put(NAME_EVENT_NAME, VALUE_DELETED);
                    output.put(NAME_EVENT_DATA, dataList);
                    return output.toString();
                }
                catch (JSONException e) {
                    e.printStackTrace();
                    return eventError("JSON Error");
                }
            }
            else {
                return eventError("No Such Entity Found");
            }
        }
        else {
            JSONObject object;
            try {
                object = new JSONObject(data);
            }
            catch (JSONException e) {
                e.printStackTrace();
                return eventError("Invalid Data Value");
            }
            String id;
            try {
                id= object.getString(Entity.COLUMN_ID);
            }
            catch (JSONException e) {
                e.printStackTrace();
                return eventError("Invalid ID");
            }
            Filter filter = new Filter();
            filter.is(Entity.COLUMN_TABLE_NAME, entity);
            ArrayList<Entity> entities = (ArrayList<Entity>) Entity.objects(mContext)
                    .filter(filter).toList();
            if (entities.size() > 0) {
                for (int i=0; i<entities.size(); i++) {
                    Entity table = entities.get(i);
                    JSONObject jsonObject = null;
                    try {
                        jsonObject = new JSONObject(table.getValue());
                    }
                    catch (JSONException e) {
                        e.printStackTrace();
                    }
                    String mId = null;
                    if (jsonObject != null) {
                        mId = jsonObject.optString(Entity.COLUMN_ID);
                    }
                    if (mId != null) {
                        if(mId.equals(id)){
                            JSONObject obj;
                            try {
                                obj = new JSONObject(table.getValue());
                            } catch (JSONException e) {
                                e.printStackTrace();
                                return eventError("JSON Error");
                            }
                            table.delete(mContext);
                            return eventSuccess(VALUE_DELETED, obj.toString());
                        }
                    }
                }
            }
            else {
                return eventError("No such Entity Found");
            }
        }
        return eventError("No Match Found");
    }

    private static String eventError(String reason) {
        JSONObject out = new JSONObject();
        try {
            out.put(NAME_EVENT_NAME, EVENT_NAME_ERROR);
            out.put(NAME_REASON, reason);
            return out.toString();
        }
        catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String eventSuccess(String eventName, String data) {
        JSONObject output = new JSONObject();
        try {
            output.put(NAME_EVENT_NAME, eventName);
            output.put(NAME_EVENT_DATA, data);
            return output.toString();
        }
        catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
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
        Intent intent = new Intent("local");
        // add data
        intent.putExtra("message", message);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}