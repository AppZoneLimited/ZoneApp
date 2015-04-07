package com.appzonegroup.zoneapp;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.koushikdutta.ion.Ion;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import database.ClientFlows;
import database.Entity;
import database.Function;
import database.FunctionCategory;

/**
 * Created by emacodos on 2/26/2015.
 */

/**
 * @author Onyejekwe E. C emacodos
 *
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 *
 */
public class FlowSyncService extends IntentService {

    private static final String TAG = FlowSyncService.class.getSimpleName();

    // Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    public static final String ACTION_NEW = "com.zoneapp.action.NEW";
    public static final String ACTION_SYNC = "com.zoneapp.action.SYNC";
    public static final String ACTION_CLOUD_MESSAGE = "com.zoneapp.action.CLOUD.MESSAGE";
    public static final String ACTION_DOWNLOAD_FUNCTION = "com.zoneapp.action.FUNCTION";
    public static final String ACTION_DOWNLOAD_FUNCTION_CATEGORY = "com.zoneapp.action.CATEGORY";
    public static final String ACTION_DOWNLOAD_FLOW = "com.zoneapp.action.FLOW";
    public static final String ACTION_DOWNLOAD_FLOW_ENTITY = "com.zoneapp.action.ENTITY";

    /**
     * Starts this service to perform action Download with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */

    private static final String PARAM_ID = "com.zoneapp.param.PARAM_ID";
    private static final String PARAM_DATA = "com.zoneapp.param.PARAM_DATA";
    private static final String PARAM_MESSAGE = "com.zoneapp.param.MESSAGE";

    public static final String URL_FUNCTION_CATEGORY = "http://192.168.2.213:11984/api/Function/GetAllFunctionCategories/";
    public static final String URL_FUNCTION = "http://192.168.2.213:11984/api/Function/GetAll/";
    public static final String URL_FUNCTION_CATEGORY_ID = "http://192.168.2.213:11984/api/FunctionCategory/Get/";
    public static final String URL_FUNCTION_ID = "http://192.168.2.213:11984/api/Function/Get/";
    public static final String URL_ENTITY = "http://192.168.2.213:11984/api/Flow/GetFlowEntities/";
    public static final String URL_FLOWS = "http://192.168.2.213:11984/api/Flow/Get/";
    public static final String URL_UPLOAD = "http://192.168.2.182:8030/api/entitydataservice/performupload";

    private static final int ENTITY_PER_BATCH = 2;

    private Object mFlowGroup = new Object();
    private String entityName;

    private ArrayList<Integer> mFunctions = new ArrayList<>();
    private ArrayList<String> mFlows = new ArrayList<>();
    private ArrayList<Integer> mCategory = new ArrayList<>();

    // Customize helper method

    public static void startActionNew(Context context) {
        Intent intent = new Intent(context, FlowSyncService.class);
        intent.setAction(ACTION_NEW);
        context.startService(intent);
    }

    public static void startActionSync(Context context) {
        Intent intent = new Intent(context, FlowSyncService.class);
        intent.setAction(ACTION_SYNC);
        context.startService(intent);
    }

    public static void startActionCloudMessage(Context context, String data) {
        Intent intent = new Intent(context, FlowSyncService.class);
        intent.setAction(ACTION_CLOUD_MESSAGE);
        intent.putExtra(PARAM_DATA, data);
        context.startService(intent);
    }

    public static void startActionDownloadFunctions(Context context) {
        Intent intent = new Intent(context, FlowSyncService.class);
        intent.setAction(ACTION_DOWNLOAD_FUNCTION);
        context.startService(intent);
    }

    public static void startActionDownloadFunctionCategories(Context context) {
        Intent intent = new Intent(context, FlowSyncService.class);
        intent.setAction(ACTION_DOWNLOAD_FUNCTION_CATEGORY);
        context.startService(intent);
    }

    public static void startActionDownloadFlows(Context context, int id) {
        Intent intent = new Intent(context, FlowSyncService.class);
        intent.setAction(ACTION_DOWNLOAD_FLOW);
        intent.putExtra(PARAM_ID, id);
        context.startService(intent);
    }

    public static void startActionDownloadEntities(Context context, String id) {
        Intent intent = new Intent(context, FlowSyncService.class);
        intent.setAction(ACTION_DOWNLOAD_FLOW_ENTITY);
        intent.putExtra(PARAM_ID, id);
        context.startService(intent);
    }

    public FlowSyncService() {
        super("FlowSyncService");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "Service is starting",
                Toast.LENGTH_LONG).show();
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(LocalEntityService.INTENT_LOCAL));

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            switch (action) {
                case ACTION_SYNC:
                    handleActionSync();
                    break;
                case ACTION_CLOUD_MESSAGE:
                    final String data = intent.getStringExtra(PARAM_DATA);
                    handleActionCloudMessage(data);
                    break;
                case ACTION_DOWNLOAD_FUNCTION_CATEGORY:
                    handleActionDownloadFunctionCategory();
                    break;
                case ACTION_DOWNLOAD_FUNCTION:
                    handleActionDownloadFunction();
                    break;
                case ACTION_DOWNLOAD_FLOW:
                    final int functionId = intent.getIntExtra(PARAM_ID, 0);
                    handleActionDownloadFlows(functionId);
                    break;
                case ACTION_DOWNLOAD_FLOW_ENTITY:
                    final String flowId = intent.getStringExtra(PARAM_ID);
                    handleActionDownloadEntity(flowId);
                    break;
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Toast.makeText(this, "Service is done updating",
                Toast.LENGTH_LONG).show();

        // Unregister since the activity is not visible
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    }

    /**
     * Handle action Sync: Synchronization of Flows and Entity
     */
    private void handleActionSync() {
        //Download Flows from Server
//        doDownload();

        //Upload Data to server
        doUpload();
    }

    /**
     * Handle action new: sync for the first time
     */
    private void handleActionNew() {
        //Download Flows from Server
        if (isNetworkAvailable()) {
            if (downloadFunctionCategories(URL_FUNCTION_CATEGORY)) {
                if (downloadFunction(URL_FUNCTION)) {
                    Log.e("fun", "here");
                    ArrayList<Function> functions = Function.getAllFunctions(this);
                    if (functions.size() > 0) {
                        for (int i = 0; i < functions.size(); i++) {
                            if (downloadFlows(functions.get(i))) {
                                ArrayList<ClientFlows> flows = ClientFlows.getAllFlows(this);
                                if (flows.size() > 0) {
                                    for (int j = 0; j < flows.size(); j++)
                                        downloadEntity(flows.get(i));
                                    Log.e("l", functions.get(i).getId()+"");
                                    sendMessage(ACTION_NEW, functions.get(i).getId());
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Handle action new: sync for the first time
     */
    private void handleActionDownloadFunction() {
        //Download Flows from Server
        if (downloadFunction(URL_FUNCTION)) {
            sendMessage(ACTION_DOWNLOAD_FUNCTION, 1);
        }
        else {
            sendMessage(ACTION_DOWNLOAD_FUNCTION, -1);
        }
    }

    /**
     * Handle action new: sync for the first time
     */
    private void handleActionDownloadFunctionCategory() {
        //Download Flows from Server
        if (downloadFunctionCategories(URL_FUNCTION_CATEGORY)) {
            sendMessage(ACTION_DOWNLOAD_FUNCTION_CATEGORY, 1);
        }
        else {
            sendMessage(ACTION_DOWNLOAD_FUNCTION_CATEGORY, -1);
        }
    }

    /**
     * Handle action new: sync for the first time
     */
    private void handleActionDownloadFlows(int id) {
        //Download Flows from Server
        Function function = Function.getFunctionById(this, id);
        if (function != null && downloadFlows(function)) {
            sendMessage(ACTION_DOWNLOAD_FLOW, function.getId());
        }
        else {
            sendMessage(ACTION_DOWNLOAD_FLOW, -1);
        }
    }

    /**
     * Handle action new: sync for the first time
     */
    private void handleActionDownloadEntity(String id) {
        //Download Flows from Server
        ClientFlows flows = ClientFlows.getFlowById(this, id);
        if (flows != null && downloadEntity(flows)) {
            sendMessage(ACTION_DOWNLOAD_FLOW_ENTITY, flows.getId());
        }
        else {
            sendMessage(ACTION_DOWNLOAD_FLOW_ENTITY, "");
        }
    }

    /**
     * Handle action cloud messages for update
     * {
     *     "type":"function, functionCategory, flow, entity"
     *     "id":"1"
     *     "operation":"create, update, delete"
     *     "name":"entity name"
     * }
     */
    private void handleActionCloudMessage(String data) {
        //handle what happens when message is gotten from the server
        JSONArray array = null;
        JSONObject object = null;
        String type = null, id = null, operation = null;
        try {
            array = new JSONArray(data);
            for (int i=0; i<array.length(); i++) {
                object = array.getJSONObject(i);
                type = object.getString("ObjectType");
                id = object.getString("Id");
                operation = object.getString("OperationType");
                entityName = object.optString("Name");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        switch (type){
            case "function":
                if (operation.equalsIgnoreCase("delete")){
                    Function function = Function.getFunctionById(this, Integer.parseInt(id));
                    function.delete(this);
                }
                else {
                    downloadFunction(URL_FUNCTION_ID + id);
                }
                break;

            case "functionCategory":
                if (operation.equalsIgnoreCase("delete")){
                    FunctionCategory category = FunctionCategory.getFunctionCategoryById(this, Integer.parseInt(id));
                    category.delete(this);
                }
                else {
                    downloadFunctionCategories(URL_FUNCTION_CATEGORY_ID + id);
                }
                break;

            case "flow":
                if (operation.equalsIgnoreCase("delete")){
                    ClientFlows flows = ClientFlows.getFlowById(this, id);
                    flows.delete(this);
                }
                else {
                    Function function = Function.getFunctionByFlowId(this, id);
                    downloadFlows(function);
                }
                break;

            case "entity":
                if (operation.equalsIgnoreCase("delete")){
                    Entity entity = Entity.getEntityById(this, Integer.parseInt(id));
                    entity.delete(this);
                }
                else {
                    JSONObject instruction = new JSONObject(), entityData = new JSONObject();
                    try {
                        instruction.put(LocalEntityService.NAME_TYPE, LocalEntityService.TYPE_SERVER);
                        instruction.put(LocalEntityService.NAME_OPERATION, LocalEntityService.OPERATION_RETRIEVE);
                        instruction.put(LocalEntityService.NAME_ENTITY, entityName);

                        entityData.put("ID", id);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    LocalEntityService.startLocalEntityService(this,
                            instruction.toString(),
                            entityData.toString());
                }
                break;
        }
    }

    private void doDownload() {
        // 1. Download Function Categories
        // 2. Download Functions
        // 3. Download Flows
        if (downloadFunctionCategories(URL_FUNCTION_CATEGORY)){
            ArrayList<Integer> ids = FunctionCategory.getAllCategoryIds(this);
            ArrayList<Integer> deleteList = getDeleteList(mCategory, ids);
            if(deleteList.size() > 0) {
                for (int i = 0; i < deleteList.size(); i++) {
                    FunctionCategory.objects(this).get(deleteList.get(i)).delete(this);
                }
            }
        }
        downloadFunction(URL_FUNCTION);
        ArrayList<Function> functions = Function.getAllFunctions(this);
        if (functions.size() > 0){
            for (int i=0; i<functions.size(); i++){
                downloadFlows(functions.get(i));
            }
        }
    }

//    private void deleteFlows(Function function) {
//        //Delete the Links associated with the function
//        List<Link> links = function.getLinks(this).all().toList();
//        for (int j=0; j<links.size(); j++){
//            Link l = links.get(j);
//            l.delete(this);
//        }
//        // Delete the ClientFlows associated with the function
//        List<ClientFlows> clientFlows = function.getClientFlows(this).all().toList();
//        for(int j=0; j<clientFlows.size(); j++){
//            ClientFlows c = clientFlows.get(j);
//            c.delete(this);
//        }
//        // Delete the EntityFlows associated with the function
//        List<EntityFlows> entityFlows = function.getEntityFlows(this).all().toList();
//        for(int j=0; j<entityFlows.size(); j++){
//            EntityFlows c = entityFlows.get(j);
//            c.delete(this);
//        }
//        //error message sent to the caller
//    }
//
//    private ArrayList<Function> getUpdatedList(List<Function> serverList, List<Function> localList) {
//        ArrayList<Function> updatedList = new ArrayList<Function>();
//        for (int i=0; i<localList.size(); i++){
//            for (int j=0; j<serverList.size(); j++) {
//                if (localList.get(i).getID().equalsIgnoreCase(serverList.get(j).getID())) {
//                    if(localList.get(i).getVersionNumber() < serverList.get(j).getVersionNumber()){
//                        updatedList.add(serverList.get(j));
//                    }
//                    break;
//                }
//            }
//        }
//        return updatedList;
//    }
//
//    private ArrayList<Function> getUpdateList(List<Function> serverList, List<Function> localList) {
//        ArrayList<Function> updateList = new ArrayList<Function>();
//        for (int i=0; i<localList.size(); i++){
//            for (int j=0; j<serverList.size(); j++) {
//                if (localList.get(i).getID().equalsIgnoreCase(serverList.get(j).getID())) {
//                    if(localList.get(i).getVersionNumber() < serverList.get(j).getVersionNumber()){
//                        updateList.add(localList.get(i));
//                    }
//                    break;
//                }
//            }
//        }
//        return updateList;
//    }
//
//    private ArrayList<Function> getNewList(ArrayList<Function> serverList, ArrayList<Function> localList) {
//        ArrayList<Function> newList = new ArrayList<Function>();
//        for (int i=0; i<serverList.size(); i++){
//            if(localList.size() > 0) {
//                for (int j = 0; j < localList.size(); j++) {
//                    if (serverList.get(i).getID().equalsIgnoreCase(localList.get(j).getID())) {
//                        break;
//                    }
//                    newList.add(serverList.get(i));
//                }
//            }
//            else {
//                newList = serverList;
//            }
//        }
//        return newList;
//    }

    private ArrayList<Integer> getDeleteList(ArrayList<Integer> serverList, ArrayList<Integer> localList) {
        ArrayList<Integer> deleteList = new ArrayList<>();
        for (int i=0; i<localList.size(); i++){
            for (int j=0; j<serverList.size(); j++) {
                if (localList.get(i).equals(serverList.get(j))) {
                    break;
                }
                else {
                    deleteList.add(localList.get(i));
                }
            }
        }
        return deleteList;
    }

    /*
    * download all function categories based on the user capability
    */
    private boolean downloadFunctionCategories(String url) {
        if (isNetworkAvailable()) {
            String mOutput = null;
            try {
                mOutput = Ion.with(this)
                        .load("GET", url)
                        .group(mFlowGroup)
                        .asString()
                        .get();
            }
            catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG+"/url/download/category", e.getMessage());
            }
            if (mOutput != null) {
                //Successful functions categories downloaded
                Log.e("categories", mOutput);
                JSONArray jsonArray = null;
                try {
                    JSONObject categories = new JSONObject(mOutput);
                    jsonArray = categories.getJSONArray("functionCategories");
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG+"/json/download_category", e.getMessage());
                }

                for (int i=0; i<jsonArray.length(); i++){
                    int id = 0;
                    JSONObject jsonObject = null;
                    try{
                    jsonObject = jsonArray.getJSONObject(i);

                    id = jsonObject.getInt(FunctionCategory.COLUMN_ID);

                    } catch (Exception e) {
                        e.printStackTrace();
                        //TODO: error message sent to the caller
                    }
                    String versionNumber = jsonObject.optString(FunctionCategory.COLUMN_VERSION_NUMBER);
                    String name = jsonObject.optString(FunctionCategory.COLUMN_NAME);
                    int parentId = jsonObject.optInt(FunctionCategory.COLUMN_PARENT_CATEGORY_ID);

                    FunctionCategory category = FunctionCategory.getFunctionCategoryById(this, id);
                    mCategory.add(id);

                    if (category == null){
                        FunctionCategory functionCategory = new FunctionCategory();
                        functionCategory.setName(name);
                        try {
                            functionCategory.setValue(jsonArray.get(i).toString());
                        }
                        catch (JSONException e) {
                            e.printStackTrace();
                        }
                        functionCategory.setVersionNumber(versionNumber);
                        functionCategory.setParentCategoryID(parentId);
                        functionCategory.save(this, id);
                    }
                    else {
                        category.setName(name);
                        try {
                            category.setValue(jsonArray.get(i).toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        category.setVersionNumber(versionNumber);
                        category.setParentCategoryID(parentId);
                        category.save(this);
                    }
                }
                return true;
            }
            else {
                //TODO: error message sent to the caller
            }
        }
        return false;
    }

    /*
    * download all function based on the user capability
    */
    private boolean downloadFunction(String url) {
        if (isNetworkAvailable()) {
            String output;
            try {
                output = Ion.with(this)
                        .load("GET", url)
                        .group(mFlowGroup)
                        .asString()
                        .get();
            }
            catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG+"/url/download_function", e.getMessage());
                return false;
            }
            if (output != null) {
                //Successful functions categories downloaded
                Log.e("function", output);
                JSONArray jsonArray = null;
                try {
                    JSONObject functions = new JSONObject(output);
                    jsonArray = functions.getJSONArray("functions");
                }
                catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
                for (int i=0; i<jsonArray.length(); i++){
                    JSONObject jsonObject = null;
                    int id = 0;
                    try {
                        jsonObject = jsonArray.getJSONObject(i);
                        id = jsonObject.getInt(Function.COLUMN_ID);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(TAG+"/json/function/id", e.getMessage());
                        return false;
                    }
                    String versionNumber = jsonObject.optString(Function.COLUMN_VERSION_NUMBER);
                    String name = jsonObject.optString(Function.COLUMN_NAME);
                    String description = jsonObject.optString(Function.COLUMN_DESCRIPTION);
                    String flowGuid = jsonObject.optString(Function.COLUMN_FLOWGUID);
                    int categoryId = jsonObject.optInt(Function.COLUMN_CATEGORY_ID);

                    Function function = Function.getFunctionById(this, id);
                    mFunctions.add(id);
                    if (function == null){
                        Function newFunction = new Function();
                        try {
                            newFunction.setValue(jsonArray.get(i).toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        newFunction.setVersionNumber(versionNumber);
                        newFunction.setName(name);
                        newFunction.setDescription(description);
                        newFunction.setFlowGuid(flowGuid);
                        newFunction.setCategoryID(categoryId);
                        newFunction.save(this, id);
                    }
                    else {
                        try {
                            function.setValue(jsonArray.get(i).toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        function.setVersionNumber(versionNumber);
                        function.setName(name);
                        function.setDescription(description);
                        function.setFlowGuid(flowGuid);
                        function.setCategoryID(categoryId);
                        function.save(this);
                    }
                }
                return true;
            }
            else {
                return false;
            }
        }
        return false;
    }

    /*
    * download flows based on functions
    */
    private boolean downloadFlows(Function function) {
        if (isNetworkAvailable()) {
            //Network Available
            try {
                String guid = function.getFlowGuid();
                String flowsContent;
                flowsContent = Ion.with(this)
                        .load("GET", URL_FLOWS + guid)
                        .group(mFlowGroup)
                        .asString()
                        .get();
                if (flowsContent != null) {
                    Log.e("flows", flowsContent);
                    String flowId = null;
                    JSONObject object = new JSONObject(flowsContent);
                    flowId = object.getString(ClientFlows.COLUMN_ID);

                    mFlows.add(flowId);

                    ClientFlows flows = ClientFlows.getFlowById(this, flowId);
                    if (flows == null) {
                        ClientFlows clientFlows = new ClientFlows();
                        clientFlows.setFlows(flowsContent);
                        clientFlows.setFlowsID(flowId);
                        clientFlows.save(this);
                    }
                    else {
                        flows.setFlows(flowsContent);
                        flows.setFlowsID(flowId);
                        flows.save(this);
                    }
                    return true;
                }
                else {
                    return false;
                }
            } catch (Exception e) {
                //Interrupted Download
                return false;
            }
        } else {
            // Network Unavailable
            return false;
        }
    }

    /*
    * download entities based on flows
    */
    private boolean downloadEntity(ClientFlows flows) {
        if (isNetworkAvailable()) {
            //Network Available
            try {
                String guid = flows.getFlowsID();
                String entityOutput;
                try {
                    entityOutput = Ion.with(this)
                            .load("GET", URL_ENTITY+guid)
                            .setStringBody(guid)
                            .group(mFlowGroup)
                            .asString()
                            .get();
                }
                catch (Exception e) {
                    e.printStackTrace();
                    return true;
                }
                if (entityOutput != null) {
                    Log.e("entities", entityOutput);
                    JSONArray jsonArray = new JSONArray(entityOutput);
                    for (int i=0; i<jsonArray.length(); i++){
                        JSONObject object = jsonArray.getJSONObject(i);
                        String entityName = object.getString("EntityType");
                        JSONObject entityJson = object.getJSONObject("Entity");

                        int id = entityJson.getInt(Entity.COLUMN_ID);

                        Entity entity = Entity.getEntityById(this, id);
                        if (entity == null) {
                            Entity newEntity = new Entity();
                            newEntity.setEntityName(entityName);
                            newEntity.setSync(true);
                            newEntity.setValue(entityOutput);
                            newEntity.save(this, id);
                        }
                        else {
                            entity.setEntityName(entityName);
                            entity.setSync(true);
                            entity.setValue(entityOutput);
                            entity.save(this);
                        }
                    }
                }
                else {
                    return false;
                }
            } catch (Exception e) {
                //Interrupted Download
                return false;
            }
        } else {
            // Network Unavailable
            return false;
        }
        return false;
    }

    /*
    * Perform Upload functions
    */
    private void doUpload(){
        ArrayList<Entity> allEntities = Entity.getUnSyncedEntity(this);

        if (allEntities.size() > 0) {
            //Create batches of entity to send to the server
            ArrayList<ArrayList<Entity>> splitted = new ArrayList<>();
            int arrayLength = allEntities.size();
            ArrayList<Entity> aBatch = new ArrayList<>(ENTITY_PER_BATCH);
            for (int i = 0; i < arrayLength; i++) {
                int arrayIndex = i % ENTITY_PER_BATCH;
                if (arrayIndex == 0) {
                    if (i != 0) {
                        splitted.add(aBatch);
                    }
                    aBatch = new ArrayList<>(ENTITY_PER_BATCH);
                }
                aBatch.add(arrayIndex, allEntities.get(i));
            }
            splitted.add(aBatch);

            for (int j = 0; j < splitted.size(); j++) {
                ArrayList<Entity> batchEntity = splitted.get(j);
                if (batchEntity != null) {
                    JSONArray batch = new JSONArray();
                    for (int i = 0; i < batchEntity.size(); i++) {
                        String input = batchEntity.get(i).getValue();
                        String entityName = batchEntity.get(i).getEntityName();
                        JSONObject object = new JSONObject();
                        try {
                            JSONObject data = new JSONObject(input);
                            object.put(LocalEntityService.NAME_ENTITY, entityName);
                            object.put(LocalEntityService.PARAM_DATA, data);
                            batch.put(object);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.e(TAG, input);
                        }
                    }
                    uploadEntities(batchEntity, batch);
                }
            }
        }
    }

    /*
    * Upload Entities to the server
    */
    private void uploadEntities(ArrayList<Entity> entities, JSONArray batch) {
        JsonArray jsonArray = new JsonParser().parse(batch.toString()).getAsJsonArray();
        String result;
        try {
            result = Ion.with(this)
                    .load("POST", URL_UPLOAD)
                    .setJsonArrayBody(jsonArray)
                    .group(LocalEntityService.mEntityGroup)
                    .asString()
                    .get();
            JSONObject output = new JSONObject(result);
            if (output.getString(LocalEntityService
                    .NAME_EVENT_NAME).equalsIgnoreCase(LocalEntityService.VALUE_UPLOADED)){
                for (int i=0; i<entities.size(); i++) {
                    Entity entity = entities.get(i);
                    entity.setSync(true);
                    entity.save(FlowSyncService.this);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.e("3", e.getMessage());
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

    /*
    * sends broadcast
    */
    private void sendMessage(String action, int id) {
        Intent intent = new Intent(action);
        // add data
        intent.putExtra(PARAM_MESSAGE, id);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    /*
    * sends broadcast
    */
    private void sendMessage(String action, String id) {
        Intent intent = new Intent(action);
        // add data
        intent.putExtra(PARAM_MESSAGE, id);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    // handler for received Intents for the "my-event" event
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent
            String message = intent.getStringExtra(LocalEntityService.EXTRA_MESSAGE);
            JSONObject eventData;
            try {
                JSONObject object = new JSONObject(message);
                JSONArray jsonArray = object.getJSONArray(LocalEntityService.NAME_EVENT_DATA);
                eventData = jsonArray.getJSONObject(0);
                int id = eventData.getInt(Entity.COLUMN_ID);
                Entity entity = Entity.getEntity(context, entityName, id);
                if (entity == null){
                    Entity newEntity = new Entity();
                    newEntity.setSync(true);
                    newEntity.setEntityName(entityName);
                    newEntity.setValue(eventData.toString());
                    newEntity.save(context);
                }
                else {
                    entity.setValue(eventData.toString());
                    entity.setSync(true);
                    entity.save(context);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
}
