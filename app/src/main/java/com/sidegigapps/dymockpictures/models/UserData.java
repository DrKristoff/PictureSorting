package com.sidegigapps.dymockpictures.models;

import android.support.annotation.NonNull;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import java.util.HashMap;
import java.util.Map;

public class UserData {

    public String uuid;
    public String name;
    public String url;
    private long rotationsCount;
    private long viewsCount;
    private long sortedCount;
    private long transcribedCount;

    public UserData(){
        // Default constructor required for calls to DataSnapshot.getValue(UserData.class)
    }

    public UserData(HashMap<String, Object> map, String id) {

        HashMap<String,Object> results = (HashMap<String, Object>) map.get(id);

        this.uuid = (String) results.get("uuid");
        this.name = (String) results.get("name");
        this.url = (String) results.get("url");
    }

    public UserData(HashMap<String, Object> map) {

        this.uuid = (String) map.get("uuid");
        this.name = (String) map.get("name");
        this.url = (String) map.get("url");
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getRotationsCount() {
        return rotationsCount;
    }

    public void setRotationsCount(int rotationsCount) {
        this.rotationsCount = rotationsCount;
    }

    public long getViewsCount() {
        return viewsCount;
    }

    public void setViewsCount(int viewsCount) {
        this.viewsCount = viewsCount;
    }

    public long getSortedCount() {
        return sortedCount;
    }

    public void setSortedCount(int sortedCount) {
        this.sortedCount = sortedCount;
    }

    public long getTranscribedCount() {
        return transcribedCount;
    }

    public void setTranscribedCount(int transcribedCount) {
        this.transcribedCount = transcribedCount;
    }

    public UserData(String uuid, String name, String url){
        this.uuid = uuid;
        this.name = name;
        this.url = url;
    }

    public UserData(GoogleSignInAccount acct){
        this.uuid = acct.getId();
        this.name = acct.getDisplayName();
        this.url = acct.getPhotoUrl().toString();

    }

    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("uuid", uuid);
        result.put("name", name);
        result.put("url", uuid);
        //result.put("rotationsCount", rotationsCount);
        //result.put("viewsCount", viewsCount);
        //result.put("sortedCount", sortedCount);
        //result.put("transcribedCount", transcribedCount);
        return result;
    }

}
