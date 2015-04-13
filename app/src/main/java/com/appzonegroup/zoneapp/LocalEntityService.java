package com.appzonegroup.zoneapp;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
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

import dejavu.appzonegroup.com.dejavuandroid.DataBases.Entity;


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
    public static final String NAME_EVENT_NAME = "EventName";
    public static final String NAME_EVENT_DATA = "EventData";
    public static final String NAME_REASON = "Reason";

    public static final String VALUE_CREATED = "Entity Created";
    public static final String VALUE_UPDATED = "Entity Updated";
    public static final String VALUE_RETRIEVED = "Entity Retrieved";
    public static final String VALUE_DELETED = "Entity Deleted";
    public static final String VALUE_UPLOADED = "Entity Uploaded";

    public static final String VALUE_ERROR = "Entity Operation Failed";

    public static final String TYPE_SERVER = "server";
    public static final String TYPE_LOCAL = "local";

    public static final String URL_ENTITY = "http://192.168.2.182:8030/api/entitydataservice/performcrud";

    public static final String INTENT_LOCAL = "local";
    public static final String EXTRA_MESSAGE = "message";

    private static Context mContext;
    public static Object mEntityGroup = new Object();

    public static void startLocalEntityService(Context context, String instruction, String data) {
        Intent intent = new Intent(context, LocalEntityService.class);
        intent.putExtra(PARAM_INSTRUCTION, instruction);
        intent.putExtra(PARAM_DATA, data);
        context.startService(intent);
    }

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

        Ion.getDefault(this).configure().setLogging("MyLogs", Log.ERROR);

        if(instruction == null || TextUtils.isEmpty(instruction)){
            return eventError("Null Instruction Value");
        }
        else {
            if (isNetworkAvailable()) {
                JsonObject dataObj;
                if(data == null || TextUtils.isEmpty(data.trim())) {
                    dataObj = null;
                }
                else {
                    dataObj = new JsonParser().parse(data).getAsJsonObject();
                }
                JsonObject insObj = new JsonParser().parse(instruction).getAsJsonObject();
                JsonObject json = new JsonObject();
                json.add(PARAM_INSTRUCTION, insObj);
                json.add(PARAM_DATA, dataObj);

                String mOutput;
                try {
                mOutput = Ion.with(mContext)
                        .load("POST", URL_ENTITY)
                        .setJsonObjectBody(json)
                        .group(mEntityGroup)
                        .asString()
                        .get();
            }
            catch (Exception e) {
                e.printStackTrace();
                mOutput = eventError("Connection Timed Out");
            }
                if (mOutput != null) {
                    Log.e("data", mOutput);
                    return mOutput;
                }
                else
                    return eventError("Cannot Connect to the Url");
            }
            else {
                try {
                    setMobileDataEnabled(mContext, true);
                    if(isNetworkAvailable())
                        return doServerOperation(instruction, data);
                    else
                        return eventError("Internet Unavailable");
                }
                catch (Exception e) {
                    e.printStackTrace();
                    return eventError("Internet Unavailable");
                }
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
        ArrayList<Entity> entities = Entity.getAllEntityByName(mContext, entity);
        if(data == null) {
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
            if (entities.size() > 0) {
                for (int i=0; i<entities.size(); i++) {
                    Entity table = entities.get(i);
                    JSONObject jsonObject = null;
                    try {
                        jsonObject = new JSONObject(table.getValue());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    String mId = null;
                    if (jsonObject != null) {
                        mId = jsonObject.optString(Entity.COLUMN_ID);
                    }
                    if (mId != null) {
                        if (mId.equals(id)) {
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
            ArrayList<Entity> entities = Entity.getAllEntityByName(mContext, entity);
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
        ArrayList<Entity> entities = Entity.getAllEntityByName(mContext, entity);

        if(data == null || TextUtils.isEmpty(data)){
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
            out.put(NAME_EVENT_NAME, VALUE_ERROR);
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
        Intent intent = new Intent(INTENT_LOCAL);
        // add data
        intent.putExtra(EXTRA_MESSAGE, message);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
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