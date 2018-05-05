package com.sidegigapps.dymockpictures.models;

import android.support.annotation.NonNull;

public class UserScore implements Comparable<UserScore>{
    private long score;
    private String uuid;

    public UserScore(){

    }

    public UserScore(String uuid, long score){
        this.score = score;
        this.uuid = uuid;

    }

    public long getScore(){
        return score;
    }

    @Override
    public int compareTo(@NonNull UserScore userScore) {
        int count1 =  (int)userScore.getScore();
        return (int)score - count1;
    }
}
