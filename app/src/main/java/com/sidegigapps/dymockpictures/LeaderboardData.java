package com.sidegigapps.dymockpictures;


import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import java.util.HashMap;
import java.util.Map;

public class LeaderboardData {

    public String uuid;
    public String name;
    public String url;
    public long rotationsCount;
    public long viewsCount;
    public long sortedCount;
    public long transcribedCount;

    public LeaderboardData(HashMap<String, Object> map){
        // Default constructor required for calls to DataSnapshot.getValue(LeaderboardData.class)
    }

    public LeaderboardData(HashMap<String, Object> map, String id) {

        HashMap<String,Object> results = (HashMap<String, Object>) map.get(id);

        this.uuid = (String) results.get("uuid");
        this.rotationsCount = (Long) results.get("rotationsCount");
        this.viewsCount = (long) results.get("viewsCount");
        this.sortedCount = (long) results.get("sortedCount");
        this.transcribedCount = (long) results.get("transcribedCount");
        this.name = (String) results.get("name");
        this.url = (String) results.get("url");

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

    public LeaderboardData(String uuid, String name, String url){
        this.uuid = uuid;

        this.rotationsCount = 0;
        this.viewsCount = 0;
        this.sortedCount = 0;
        this.transcribedCount = 0;
        this.name = name;
        this.url = url;
    }

    public LeaderboardData (GoogleSignInAccount acct){
        this.uuid = acct.getId();
        this.rotationsCount = 0;
        this.viewsCount = 0;
        this.sortedCount = 0;
        this.transcribedCount = 0;
        this.name = acct.getDisplayName();
        this.url = acct.getPhotoUrl().toString();

    }

    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("uuid", uuid);
        result.put("name", name);
        result.put("url", uuid);
        result.put("rotationsCount", rotationsCount);
        result.put("viewsCount", viewsCount);
        result.put("sortedCount", sortedCount);
        result.put("transcribedCount", transcribedCount);

        return result;
    }

}
