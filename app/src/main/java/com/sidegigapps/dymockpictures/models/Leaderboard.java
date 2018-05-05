package com.sidegigapps.dymockpictures.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

public class Leaderboard {

    public String title;
    public HashMap<String,Long> scores = new HashMap<>();

    Comparator<Entry<String, Long>> valueComparator = new Comparator<Entry<String,Long>>() {

        @Override
        public int compare(Entry<String, Long> e1, Entry<String, Long> e2) {
            long score1 = e1.getValue();
            long score2 = e2.getValue();
            return (int)(score1 - score2);
        }
    };

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

    public long getScoreByUUID(String uuid) {
        if(scores.containsKey(uuid)){
            return scores.get(uuid);
        } else {
            return 0;
        }
    }

    public String getUUIDByLeaderboardPosition(int position){
        Set<Entry<String, Long>> entries = scores.entrySet();
        List<Entry<String, Long>> scoresList = new ArrayList<Entry<String, Long>>(entries);
        Collections.sort(scoresList, valueComparator);
        return scoresList.get(position).getKey();
    }

    public int getSize(){
        return scores.size();
    }
}
