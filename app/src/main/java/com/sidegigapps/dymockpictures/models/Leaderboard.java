package com.sidegigapps.dymockpictures.models;

import java.util.HashMap;
import java.util.Map;

public class Leaderboard {

    public String title;
    public HashMap<String,Long> scores = new HashMap<>();

    public Leaderboard(){
        // Default constructor required for calls to DataSnapshot.getValue(Leaderboard.class)

    }

    public Leaderboard(String title, HashMap<String,Long> map){
        this.title = title;
        this.scores = map;
    }

    public Leaderboard(String title){
        this.title = title;
    }

    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("title", title);
        result.put("scores", scores);
        return result;
    }

    public long getRotationsByUUID(String uuid) {
        if(scores.containsKey(uuid)){
            return scores.get(uuid);
        } else {
            return 0;
        }
    }
}
