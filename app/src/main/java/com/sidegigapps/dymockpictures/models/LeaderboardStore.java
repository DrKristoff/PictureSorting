package com.sidegigapps.dymockpictures.models;

import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Objects;

public class LeaderboardStore {

    public static final String LEADERBOARD_VIEWS = "Views";
    public static final String LEADERBOARD_ROTATIONS = "Rotations";
    public static final String LEADERBOARD_SORTED = "Sorted";
    public static final String LEADERBOARD_TRANSCRIBED = "Transcribed";
    public static final String LEADERBOARD_DOWNLOADS = "Downloads";
    public static final String[] leaderboardNames = new String[]{
            LEADERBOARD_VIEWS,
            LEADERBOARD_ROTATIONS,
            LEADERBOARD_SORTED,
            //LEADERBOARD_TRANSCRIBED,
            LEADERBOARD_DOWNLOADS
    };
    private final GoogleSignInAccount googleAcct;

    private HashMap<String, Leaderboard> mLeaderboardsMap = new HashMap<>();
    private HashMap<String, UserData> mUsersMap = new HashMap<>();

    private DatabaseReference leaderboardsRef, usersRef;
    private UserData currUserData;
    private boolean mLeaderboardDataLoaded;
    private boolean mUserDataLoaded;

    public LeaderboardStore(DatabaseReference leaderboardsRef, DatabaseReference usersRef, GoogleSignInAccount acct){
        this.leaderboardsRef = leaderboardsRef;
        this.usersRef = usersRef;
        this.googleAcct = acct;

        loadUserData();
    }

    private void loadUserData() {
        Log.d("RCD", "loadUserData");
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.child(Objects.requireNonNull(googleAcct.getId())).exists()) {
                    Log.d("RCD", "account already exists: " + googleAcct.getId());
                    HashMap<String, Object> map = (HashMap<String, Object>) snapshot.getValue();
                    currUserData = new UserData(map, googleAcct.getId());
                } else {
                    Log.d("RCD", "account does not exist: " + googleAcct.getId());
                    currUserData = new UserData(googleAcct);
                    usersRef.child(currUserData.uuid).setValue(currUserData);
                    Log.d("RCD", "user created");
                }

                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    HashMap<String, Object> childMap = (HashMap<String, Object>) childSnapshot.getValue();
                    UserData userData = new UserData(childMap);
                    mUsersMap.put(userData.getUuid(), userData);
                }

                mUserDataLoaded = true;
                onUserDataLoaded();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

    }

    private void onUserDataLoaded() {
        Log.d("RCD", "onUserDataLoaded");
        setupLeaderboardListener();
    }

    private void setupLeaderboardListener() {
        Log.d("RCD", "setupLeaderboardListener");

        leaderboardsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (String leaderboardName : leaderboardNames) {
                    if (snapshot.child(leaderboardName).exists()) {
                        HashMap<String, Long> map = (HashMap<String, Long>) snapshot.child(leaderboardName).getValue();
                        Leaderboard newLeaderboard = new Leaderboard(leaderboardName, map);
                        mLeaderboardsMap.put(leaderboardName, newLeaderboard);
                    } else {
                        Log.d("RCD", "leaderboard does not exist: " + leaderboardName);
                        leaderboardsRef.child(leaderboardName).child(currUserData.uuid).setValue(0);  //race conditions, mUserData was still null
                        Leaderboard newLeaderboard = new Leaderboard(leaderboardName);
                        mLeaderboardsMap.put(leaderboardName, newLeaderboard);
                    }
                }

                if (!mLeaderboardDataLoaded) {
                    onLeaderboardDataLoaded();
                    mLeaderboardDataLoaded = true;
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

    }

    private void onLeaderboardDataLoaded() {

    }


    public void incrementScores(String leaderboardName) {
        Leaderboard leaderboard = mLeaderboardsMap.get(leaderboardName);
        long score = leaderboard.getScoreByUUID(currUserData.getUuid());
        score += 1;
        leaderboardsRef.child(leaderboardName).child(currUserData.getUuid()).setValue(score);
    }

    //GETTERS and SETTERS

    public HashMap<String,Leaderboard> getLeaderboardsMap() {
        return mLeaderboardsMap;
    }

    public UserData getUserDataByUUID(String uuid) {
        return mUsersMap.get(uuid);
    }

    public void incrementRotations(String uuid) {
        incrementScores(LEADERBOARD_ROTATIONS);
    }

    public void incrementViews(String uuid) {
        incrementScores(LEADERBOARD_VIEWS);
    }

    public void incrementSorts(String uuid) {
        incrementScores(LEADERBOARD_SORTED);
    }

    public void incrementTranscribes(String uuid) {
        incrementScores(LEADERBOARD_TRANSCRIBED);
    }

    public void incrementDownloads(String uuid) {
        incrementScores(LEADERBOARD_DOWNLOADS);
    }

}
