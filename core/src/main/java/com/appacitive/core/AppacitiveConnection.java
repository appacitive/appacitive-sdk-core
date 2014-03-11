package com.appacitive.core;

import com.appacitive.core.exceptions.AppacitiveException;
import com.appacitive.core.exceptions.ValidationException;
import com.appacitive.core.infra.*;
import com.appacitive.core.interfaces.AsyncHttp;
import com.appacitive.core.apjson.APJSONArray;
import com.appacitive.core.apjson.APJSONException;
import com.appacitive.core.apjson.APJSONObject;
import com.appacitive.core.model.AppacitiveEndpoint;
import com.appacitive.core.model.AppacitiveStatus;
import com.appacitive.core.model.Callback;
import com.appacitive.core.model.PagedList;
import com.appacitive.core.query.AppacitiveQuery;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by sathley.
 */
public class AppacitiveConnection extends AppacitiveEntity implements Serializable, APSerializable {

    public final static Logger LOGGER = Logger.getLogger(AppacitiveConnection.class.getName());

    public AppacitiveConnection(String relationType) {
        this.relationType = relationType;
    }

    public AppacitiveConnection(long relationId) {
        this.relationId = relationId;
    }

    public void setSelf(APJSONObject connection) {
        super.setSelf(connection);
        if (connection != null) {
            this.relationId = Long.parseLong(connection.optString(SystemDefinedProperties.relationId, "0"));
            this.relationType = (connection.optString(SystemDefinedProperties.relationType, null));

            APJSONObject object = connection.optJSONObject(SystemDefinedProperties.endpointA);
            this.endpointA.setSelf(object);

            object = connection.optJSONObject(SystemDefinedProperties.endpointB);
            this.endpointB.setSelf(object);
        }
    }

    @Override
    public APJSONObject getMap() throws APJSONException {
        APJSONObject nativeMap = super.getMap();
        nativeMap.put(SystemDefinedProperties.relationType, this.relationType);
        nativeMap.put(SystemDefinedProperties.relationId, String.valueOf(this.relationId));
        nativeMap.put(SystemDefinedProperties.endpointA, this.endpointA.getMap());
        nativeMap.put(SystemDefinedProperties.endpointB, this.endpointB.getMap());
        return nativeMap;
    }

    public void setRelationType(String relationType) {
        this.relationType = relationType;
    }

    public void setRelationId(long relationId) {
        this.relationId = relationId;
    }

    public String relationType = null;

    public long relationId = 0;

    public AppacitiveEndpoint endpointA = new AppacitiveEndpoint();

    public AppacitiveEndpoint endpointB = new AppacitiveEndpoint();

    public String getRelationType() {
        return relationType;
    }

    public long getRelationId() {
        return relationId;
    }

    public AppacitiveConnection fromNewObject(String label, AppacitiveObject object) {
        this.endpointA.label = label;
        this.endpointA.object = object;
        return this;
    }

    public AppacitiveConnection toNewObject(String label, AppacitiveObject object) {
        this.endpointB.label = label;
        this.endpointB.object = object;
        return this;
    }

    public AppacitiveConnection fromNewUser(String label, AppacitiveUser user) {
        this.endpointA.label = label;
        this.endpointA.object = user;
        return this;
    }

    public AppacitiveConnection toNewUser(String label, AppacitiveUser user) {
        this.endpointB.label = label;
        this.endpointB.object = user;
        return this;
    }

    public AppacitiveConnection fromNewDevice(String label, AppacitiveDevice device) {
        this.endpointA.label = label;
        this.endpointA.object = device;
        return this;
    }

    public AppacitiveConnection toNewDevice(String label, AppacitiveDevice device) {
        this.endpointB.label = label;
        this.endpointB.object = device;
        return this;
    }

    public AppacitiveConnection fromExistingObject(String label, long objectId) {
        this.endpointA.label = label;
        this.endpointA.objectId = objectId;
        return this;
    }

    public AppacitiveConnection toExistingObject(String label, long objectId) {
        this.endpointB.label = label;
        this.endpointB.objectId = objectId;
        return this;
    }

    public AppacitiveConnection fromExistingUser(String label, long userId) {
        return this.fromExistingObject(label, userId);
    }

    public AppacitiveConnection toExistingUser(String label, long userId) {
        return this.toExistingObject(label, userId);
    }

    public AppacitiveConnection fromExistingDevice(String label, long deviceId) {
        return this.fromExistingObject(label, deviceId);
    }

    public AppacitiveConnection toExistingDevice(String label, long deviceId) {
        return this.toExistingObject(label, deviceId);
    }

    public void createInBackground(final Callback<AppacitiveConnection> callback) throws ValidationException {
        if ((this.relationType == null || this.relationType.isEmpty()) && (this.relationId <= 0)) {
            throw new ValidationException("Relation Type and Relation Id both cannot be empty while creating an object.");
        }

        if (this.endpointA == null || this.endpointA.label == null || this.endpointA.label.isEmpty() || (this.endpointA.object == null && this.endpointA.objectId <= 0))
            throw new ValidationException("Endpoint A is not correctly initialized.");

        if (this.endpointB == null || this.endpointB.label == null || this.endpointB.label.isEmpty() || (this.endpointB.object == null && this.endpointB.objectId <= 0))
            throw new ValidationException("Endpoint B is not correctly initialized.");


        final String url = Urls.ForConnection.createConnectionUrl(this.relationType).toString();
        final Map<String, String> headers = Headers.assemble();
        final APJSONObject payload;
        try {
            payload = this.getMap();
        } catch (APJSONException e) {
            throw new RuntimeException(e);
        }
        final AppacitiveConnection connection = this;
        AsyncHttp asyncHttp = APContainer.build(AsyncHttp.class);
        asyncHttp.put(url, headers, payload.toString(), new APCallback() {
            @Override
            public void success(String result) {
                try {
                    APJSONObject jsonObject = new APJSONObject(result);
                    AppacitiveStatus status = new AppacitiveStatus(jsonObject.optJSONObject("status"));
                    if (status.isSuccessful()) {
                        connection.setSelf(jsonObject.optJSONObject("connection"));
                        if (callback != null) {
                            callback.success(connection);
                        }
                    } else {
                        if (callback != null)
                            callback.failure(null, new AppacitiveException(status));
                    }
                } catch (Exception e) {
                    if (callback != null)
                        callback.failure(null, e);
                }
            }

            @Override
            public void failure(Exception e) {
                if (callback != null)
                    callback.failure(null, e);
            }
        });
    }

    public static void getInBackground(String relationType, long id, List<String> fields, final Callback<AppacitiveConnection> callback) throws ValidationException {
        if (relationType == null || relationType.isEmpty())
            throw new ValidationException("RelationType cannot be null or empty.");
        if (id <= 0)
            throw new ValidationException("Connection id should be greater than equal to 0.");

        final String url = Urls.ForConnection.getConnectionUrl(relationType, id, fields).toString();
        final Map<String, String> headers = Headers.assemble();

        AsyncHttp asyncHttp = APContainer.build(AsyncHttp.class);
        asyncHttp.get(url, headers, new APCallback() {
            @Override
            public void success(String result) {
                try {
                    APJSONObject jsonObject = new APJSONObject(result);
                    AppacitiveStatus status = new AppacitiveStatus(jsonObject.optJSONObject("status"));
                    if (status.isSuccessful()) {
                        AppacitiveConnection connection = new AppacitiveConnection("");
                        connection.setSelf(jsonObject.optJSONObject("connection"));
                        if (callback != null)
                            callback.success(connection);
                    } else {
                        if (callback != null)
                            callback.failure(null, new AppacitiveException(status));
                    }
                } catch (Exception e) {
                    if (callback != null)
                        callback.failure(null, e);
                }
            }

            @Override
            public void failure(Exception e) {
                if (callback != null)
                    callback.failure(null, e);
            }
        });
    }

    public void deleteInBackground(final Callback<Void> callback) {
        final String url = Urls.ForConnection.deleteConnectionUrl(this.relationType, this.getId()).toString();
        final Map<String, String> headers = Headers.assemble();
        AsyncHttp asyncHttp = APContainer.build(AsyncHttp.class);
        asyncHttp.delete(url, headers, new APCallback() {
            @Override
            public void success(String result) {
                try {
                    APJSONObject jsonObject = new APJSONObject(result);
                    AppacitiveStatus status = new AppacitiveStatus(jsonObject.optJSONObject("status"));
                    if (status.isSuccessful()) {
                        if (callback != null)
                            callback.success(null);
                    } else {
                        if (callback != null)
                            callback.failure(null, new AppacitiveException(status));
                    }
                } catch (Exception e) {
                    if (callback != null)
                        callback.failure(null, e);
                }
            }

            @Override
            public void failure(Exception e) {
                if (callback != null)
                    callback.failure(null, e);
            }
        });
    }

    public static void bulkDeleteInBackground(String relationType, List<Long> connectionIds, final Callback<Void> callback) {
        final String url = Urls.ForConnection.bulkDeleteConnectionUrl(relationType).toString();
        final Map<String, String> headers = Headers.assemble();

        final List<String> strIds = new ArrayList<String>();
        for (long id : connectionIds) {
            strIds.add(String.valueOf(id));
        }
        final APJSONObject payload = new APJSONObject();
        try {
            payload.put("idlist", new APJSONArray(strIds));
        } catch (APJSONException e) {
            throw new RuntimeException(e);
        }

        // API should accept ids without quotes
        AsyncHttp asyncHttp = APContainer.build(AsyncHttp.class);
        asyncHttp.post(url, headers, payload.toString(), new APCallback() {
            @Override
            public void success(String result) {
                try {
                    APJSONObject jsonObject = new APJSONObject(result);
                    AppacitiveStatus status = new AppacitiveStatus(jsonObject.optJSONObject("status"));
                    if (status.isSuccessful()) {
                        if (callback != null)
                            callback.success(null);
                    } else {
                        if (callback != null)
                            callback.failure(null, new AppacitiveException(status));
                    }
                } catch (Exception e) {
                    if (callback != null)
                        callback.failure(null, e);
                }
            }

            @Override
            public void failure(Exception e) {
                if (callback != null)
                    callback.failure(null, e);
            }
        });
    }

    public void updateInBackground(boolean withRevision, final Callback<AppacitiveConnection> callback) {
        final String url = Urls.ForConnection.updateConnectionUrl(this.relationType, this.getId(), withRevision, this.getRevision()).toString();
        final Map<String, String> headers = Headers.assemble();
        final APJSONObject payload;
        try {
            payload = super.getUpdateCommand();
        } catch (APJSONException e) {
            throw new RuntimeException(e);
        }
        final AppacitiveConnection connection = this;
        AsyncHttp asyncHttp = APContainer.build(AsyncHttp.class);
        asyncHttp.post(url, headers, payload.toString(), new APCallback() {
            @Override
            public void success(String result) {
                try {
                    APJSONObject jsonObject = new APJSONObject(result);
                    AppacitiveStatus status = new AppacitiveStatus(jsonObject.optJSONObject("status"));
                    if (status.isSuccessful()) {
                        connection.setSelf(jsonObject.optJSONObject("connection"));
                        if (callback != null) {
                            callback.success(connection);
                        }
                    } else {
                        if (callback != null)
                            callback.failure(null, new AppacitiveException(status));
                    }
                } catch (Exception e) {
                    if (callback != null)
                        callback.failure(null, e);
                }
            }

            @Override
            public void failure(Exception e) {
                if (callback != null)
                    callback.failure(null, e);
            }
        });
    }

    public void fetchLatestInBackground(final Callback<Void> callback) {
        final String url = Urls.ForConnection.getConnectionUrl(relationType, this.getId(), null).toString();
        final Map<String, String> headers = Headers.assemble();

        final AppacitiveConnection connection = this;
        AsyncHttp asyncHttp = APContainer.build(AsyncHttp.class);
        asyncHttp.get(url, headers, new APCallback() {
            @Override
            public void success(String result) {
                try {
                    APJSONObject jsonObject = new APJSONObject(result);
                    AppacitiveStatus status = new AppacitiveStatus(jsonObject.optJSONObject("status"));
                    if (status.isSuccessful()) {
                        connection.setSelf(jsonObject.optJSONObject("connection"));
                        if (callback != null) {
                            callback.success(null);
                        }
                    } else {
                        if (callback != null)
                            callback.failure(null, new AppacitiveException(status));
                    }
                } catch (Exception e) {
                    if (callback != null)
                        callback.failure(null, e);
                }
            }

            @Override
            public void failure(Exception e) {
                if (callback != null)
                    callback.failure(null, e);
            }
        });
    }

    public static void multiGetInBackground(String relationType, List<Long> ids, List<String> fields, final Callback<List<AppacitiveConnection>> callback) throws ValidationException {
        if (relationType.isEmpty())
            throw new ValidationException("Relation Type cannot be empty.");
        final String url = Urls.ForConnection.multiGetConnectionUrl(relationType, ids, fields).toString();
        final Map<String, String> headers = Headers.assemble();

        final List<AppacitiveConnection> appacitiveConnections = new ArrayList<AppacitiveConnection>();
        AsyncHttp asyncHttp = APContainer.build(AsyncHttp.class);
        asyncHttp.get(url, headers, new APCallback() {
            @Override
            public void success(String result) {
                try {
                    APJSONObject jsonObject = new APJSONObject(result);
                    AppacitiveStatus status = new AppacitiveStatus(jsonObject.optJSONObject("status"));
                    if (status.isSuccessful()) {
                        APJSONArray connectionsArray = jsonObject.optJSONArray("connections");
                        for (int i = 0; i < connectionsArray.length(); i++) {
                            AppacitiveConnection connection = new AppacitiveConnection("");
                            connection.setSelf(connectionsArray.optJSONObject(i));
                            appacitiveConnections.add(connection);
                        }
                        if (callback != null)
                            callback.success(appacitiveConnections);
                    } else if (callback != null)
                        callback.failure(null, new AppacitiveException(status));
                } catch (Exception e) {
                    if (callback != null)
                        callback.failure(null, e);
                }
            }

            @Override
            public void failure(Exception e) {
                if (callback != null)
                    callback.failure(null, e);
            }
        });
    }

    public static void findInBackground(String relationType, AppacitiveQuery query, List<String> fields, final Callback<PagedList<AppacitiveConnection>> callback) {
        final String url = Urls.ForConnection.findConnectionsUrl(relationType, query, fields).toString();
        final Map<String, String> headers = Headers.assemble();

        final List<AppacitiveConnection> appacitiveConnections = new ArrayList<AppacitiveConnection>();
        final PagedList<AppacitiveConnection> pagedResult = new PagedList<AppacitiveConnection>();
        AsyncHttp asyncHttp = APContainer.build(AsyncHttp.class);
        asyncHttp.get(url, headers, new APCallback() {
            @Override
            public void success(String result) {
                try {
                    APJSONObject jsonObject = new APJSONObject(result);
                    AppacitiveStatus status = new AppacitiveStatus(jsonObject.optJSONObject("status"));
                    if (status.isSuccessful()) {
                        APJSONArray connectionsArray = jsonObject.optJSONArray("connections");
                        for (int i = 0; i < connectionsArray.length(); i++) {
                            AppacitiveConnection connection = new AppacitiveConnection("");
                            connection.setSelf(connectionsArray.optJSONObject(i));
                            appacitiveConnections.add(connection);
                        }
                        pagedResult.results = appacitiveConnections;
                        pagedResult.pagingInfo.setSelf(jsonObject.optJSONObject("paginginfo"));
                        if (callback != null)
                            callback.success(pagedResult);
                    } else if (callback != null)
                        callback.failure(null, new AppacitiveException(status));
                } catch (Exception e) {
                    if (callback != null)
                        callback.failure(null, e);
                }
            }

            @Override
            public void failure(Exception e) {
                if (callback != null)
                    callback.failure(null, e);
            }
        });
    }

    public static void findByObjectsInBackground(long objectId1, long objectId2, List<String> fields, final Callback<PagedList<AppacitiveConnection>> callback) {
        final String url = Urls.ForConnection.findForObjectsUrl(objectId1, objectId2, fields).toString();
        final Map<String, String> headers = Headers.assemble();

        final List<AppacitiveConnection> appacitiveConnections = new ArrayList<AppacitiveConnection>();
        final PagedList<AppacitiveConnection> pagedResult = new PagedList<AppacitiveConnection>();
        AsyncHttp asyncHttp = APContainer.build(AsyncHttp.class);
        asyncHttp.get(url, headers, new APCallback() {
            @Override
            public void success(String result) {
                try {
                    APJSONObject jsonObject = new APJSONObject(result);
                    AppacitiveStatus status = new AppacitiveStatus(jsonObject.optJSONObject("status"));
                    if (status.isSuccessful()) {
                        APJSONArray connectionsArray = jsonObject.optJSONArray("connections");
                        for (int i = 0; i < connectionsArray.length(); i++) {
                            AppacitiveConnection connection = new AppacitiveConnection("");
                            connection.setSelf(connectionsArray.optJSONObject(i));
                            appacitiveConnections.add(connection);
                        }
                        pagedResult.results = appacitiveConnections;
                        pagedResult.pagingInfo.setSelf(jsonObject.optJSONObject("paginginfo"));
                        if (callback != null)
                            callback.success(pagedResult);
                    } else if (callback != null)
                        callback.failure(null, new AppacitiveException(status));
                } catch (Exception e) {
                    if (callback != null)
                        callback.failure(null, e);
                }
            }

            @Override
            public void failure(Exception e) {
                if (callback != null)
                    callback.failure(null, e);
            }
        });
    }

    public static void findByObjectsAndRelationInBackground(String relationType, long objectId1, long objectId2, List<String> fields, final Callback<AppacitiveConnection> callback) throws ValidationException {
        final String url = Urls.ForConnection.findForObjectsAndRelationUrl(relationType, objectId1, objectId2, fields).toString();
        final Map<String, String> headers = Headers.assemble();

        AsyncHttp asyncHttp = APContainer.build(AsyncHttp.class);
        asyncHttp.get(url, headers, new APCallback() {
            @Override
            public void success(String result) {
                try {
                    APJSONObject jsonObject = new APJSONObject(result);
                    AppacitiveStatus status = new AppacitiveStatus(jsonObject.optJSONObject("status"));
                    if (status.isSuccessful()) {
                        AppacitiveConnection connection = new AppacitiveConnection("");
                        connection.setSelf(jsonObject.optJSONObject("connection"));
                        if (callback != null)
                            callback.success(connection);
                    } else {
                        if (callback != null)
                            callback.failure(null, new AppacitiveException(status));
                    }
                } catch (Exception e) {
                    if (callback != null)
                        callback.failure(null, e);
                }
            }

            @Override
            public void failure(Exception e) {
                if (callback != null)
                    callback.failure(null, e);
            }
        });
    }

    public static void findInterconnectsInBackground(Long object1Id, List<Long> object2Ids, List<String> fields, final Callback<PagedList<AppacitiveConnection>> callback) {
        final String url = Urls.ForConnection.findInterconnectsUrl(fields).toString();
        final Map<String, String> headers = Headers.assemble();
        final Map<String, Object> map = new HashMap<String, Object>();
        List<String> strIds = new ArrayList<String>();
        for (long id : object2Ids)
            strIds.add(String.valueOf(id));
        map.put("object1id", String.valueOf(object1Id));
        map.put("object2ids", strIds);
        APJSONObject payload = new APJSONObject(map);
        final List<AppacitiveConnection> appacitiveConnections = new ArrayList<AppacitiveConnection>();
        final PagedList<AppacitiveConnection> pagedResult = new PagedList<AppacitiveConnection>();
        AsyncHttp asyncHttp = APContainer.build(AsyncHttp.class);
        asyncHttp.post(url, headers, payload.toString(), new APCallback() {
            @Override
            public void success(String result) {
                try {
                    APJSONObject jsonObject = new APJSONObject(result);
                    AppacitiveStatus status = new AppacitiveStatus(jsonObject.optJSONObject("status"));
                    if (status.isSuccessful()) {
                        APJSONArray connectionsArray = jsonObject.optJSONArray("connections");
                        for (int i = 0; i < connectionsArray.length(); i++) {
                            AppacitiveConnection connection = new AppacitiveConnection("");
                            connection.setSelf(connectionsArray.optJSONObject(i));
                            appacitiveConnections.add(connection);
                        }
                        pagedResult.results = appacitiveConnections;
                        pagedResult.pagingInfo.setSelf(jsonObject.optJSONObject("paginginfo"));
                        if (callback != null)
                            callback.success(pagedResult);
                    } else if (callback != null)
                        callback.failure(null, new AppacitiveException(status));
                } catch (Exception e) {
                    if (callback != null)
                        callback.failure(null, e);
                }
            }

            @Override
            public void failure(Exception e) {
                if (callback != null)
                    callback.failure(null, e);
            }
        });
    }

    public static void findByObjectAndLabelInBackground(String relationType, long objectId, String label, List<String> fields, final Callback<PagedList<AppacitiveConnection>> callback) {
        final String url = Urls.ForConnection.findByObjectAndLabelUrl(relationType, objectId, label, fields).toString();
        final Map<String, String> headers = Headers.assemble();

        final List<AppacitiveConnection> appacitiveConnections = new ArrayList<AppacitiveConnection>();
        final PagedList<AppacitiveConnection> pagedResult = new PagedList<AppacitiveConnection>();
        AsyncHttp asyncHttp = APContainer.build(AsyncHttp.class);
        asyncHttp.get(url, headers, new APCallback() {
            @Override
            public void success(String result) {
                try {
                    APJSONObject jsonObject = new APJSONObject(result);
                    AppacitiveStatus status = new AppacitiveStatus(jsonObject.optJSONObject("status"));
                    if (status.isSuccessful()) {
                        APJSONArray connectionsArray = jsonObject.optJSONArray("connections");
                        for (int i = 0; i < connectionsArray.length(); i++) {
                            AppacitiveConnection connection = new AppacitiveConnection("");
                            connection.setSelf(connectionsArray.optJSONObject(i));
                            appacitiveConnections.add(connection);
                        }
                        pagedResult.results = appacitiveConnections;
                        pagedResult.pagingInfo.setSelf(jsonObject.optJSONObject("paginginfo"));
                        if (callback != null)
                            callback.success(pagedResult);
                    } else if (callback != null)
                        callback.failure(null, new AppacitiveException(status));
                } catch (Exception e) {
                    if (callback != null)
                        callback.failure(null, e);
                }
            }

            @Override
            public void failure(Exception e) {
                if (callback != null)
                    callback.failure(null, e);
            }
        });
    }
}