package com.sidegigapps.dymockpictures.models;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.sidegigapps.dymockpictures.fragments.SortPhotosFragment;
import com.sidegigapps.dymockpictures.fragments.ViewPhotosFragment;

import org.apache.commons.io.FilenameUtils;
import org.threeten.bp.chrono.Era;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FirebaseStore {
    private FirebaseAuth mAuth;
    private StorageReference mStorageRef;
    private DatabaseReference mDatabase;
    private GoogleSignInAccount mGoogleSignInAccount;

    private static final String LEADERBOARDS = "leaderboards";
    private static final String USERS = "users";
    private static final String IMAGES = "images";
    private DatabaseReference mUsersReference, mLeaderboardReference, mImagesReference;

    private HashMap<String, Leaderboard> mLeaderboardsMap = new HashMap<>();
    private HashMap<String, UserData> mUsersMap = new HashMap<>();

    private UserData mUserData;
    public boolean mUserDataLoaded = false;
    public boolean mLeaderboardDataLoaded = false;
    private boolean mSetupComplete = false;

    public static final String LEADERBOARD_VIEWS = "Views";
    public static final String LEADERBOARD_ROTATIONS = "Rotations";
    public static final String LEADERBOARD_SORTED = "Sorted";
    public static final String LEADERBOARD_TRANSCRIBED = "Transcribed";
    public static final String LEADERBOARD_DOWNLOADS = "Downloads";
    public final String[] leaderboardNames = new String[]{
            LEADERBOARD_VIEWS,
            LEADERBOARD_ROTATIONS,
            LEADERBOARD_SORTED,
            //LEADERBOARD_TRANSCRIBED,
            LEADERBOARD_DOWNLOADS
    };

    private HashMap<Integer,String> anchorMap = new HashMap<>();
    private HashMap<String,String> anchorUrlMap = new HashMap<>();
    public HashMap<String, ArrayList<String>> mEraFilenames = new HashMap<>();
    public List<String> filenames = new ArrayList<>();
    private HashMap<String,String> filenamesUrlMap = new HashMap<>();


    InitializationListener initializationListener;
    UrlDownloadListener mUrlListener;

    public FirebaseStore(GoogleSignInAccount acct, InitializationListener initListener){
        this.mGoogleSignInAccount = acct;
        this.initializationListener = initListener;
        initializeDatabase();
        loadImageData();

    }

    private void initializeDatabase(){
        mAuth = FirebaseAuth.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mUsersReference = mDatabase.child(USERS);
        mLeaderboardReference = mDatabase.child(LEADERBOARDS);
        mImagesReference = mDatabase.child(IMAGES);
    }

    private void loadUserData() {
        Log.d("RCD", "loadUserData");
        mUsersReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.child(mGoogleSignInAccount.getId()).exists()) {
                    Log.d("RCD", "account already exists: " + mGoogleSignInAccount.getId());
                    HashMap<String, Object> map = (HashMap<String, Object>) snapshot.getValue();
                    mUserData = new UserData(map, mGoogleSignInAccount.getId());
                } else {
                    Log.d("RCD", "account does not exist: " + mGoogleSignInAccount.getId());
                    mUserData = new UserData(mGoogleSignInAccount);
                    mUsersReference.child(mUserData.uuid).setValue(mUserData);
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

    public HashMap<String, Leaderboard> getmLeaderboardsMap() {
        return mLeaderboardsMap;
    }

    private void onUserDataLoaded() {
        Log.d("RCD", "onUserDataLoaded");
        setupLeaderboardListener();
    }

    private void setupLeaderboardListener() {
        Log.d("RCD", "setupLeaderboardListener");

        mLeaderboardReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (String leaderboardName : leaderboardNames) {
                    if (snapshot.child(leaderboardName).exists()) {
                        HashMap<String, Long> map = (HashMap<String, Long>) snapshot.child(leaderboardName).getValue();
                        Leaderboard newLeaderboard = new Leaderboard(leaderboardName, map);
                        mLeaderboardsMap.put(leaderboardName, newLeaderboard);
                    } else {
                        Log.d("RCD", "leaderboard does not exist: " + leaderboardName);
                        mLeaderboardReference.child(leaderboardName).child(mUserData.uuid).setValue(0);  //race conditions, mUserData was still null
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


    public ArrayList<String> getImageUrlsByEra(String era){
        ArrayList<String> imageNames = getImageNamesByEra(era);
        ArrayList<String> result = new ArrayList<>();
        for(String imageName : imageNames){
            if(filenamesUrlMap.containsKey(imageName))
                result.add(filenamesUrlMap.get(imageName));
        }
        return result;
    }

    public ArrayList<String> getImageNamesByEra(String era){
        if(!mEraFilenames.containsKey(era)){
            return new ArrayList<>();
        } else {
            return mEraFilenames.get(era);
        }
    }

    private void onLeaderboardDataLoaded() {
        Log.d("RCD", "onLeaderboardDataLoaded");
    }

    public UserData getUserDataByUUID(String uuid) {
        return mUsersMap.get(uuid);
    }

    public void incrementRotations() {
        incrementScores(LEADERBOARD_ROTATIONS);
    }

    public void incrementViews() {
        incrementScores(LEADERBOARD_VIEWS);
    }

    public void incrementSorts() {
        incrementScores(LEADERBOARD_SORTED);
    }

    public void incrementTranscribes() {
        incrementScores(LEADERBOARD_TRANSCRIBED);
    }

    public void incrementDownloads() {
        incrementScores(LEADERBOARD_DOWNLOADS);
    }

    public void incrementScores(String leaderboardName) {
        Leaderboard leaderboard = mLeaderboardsMap.get(leaderboardName);
        long score = leaderboard.getScoreByUUID(mUserData.uuid);
        score += 1;
        mLeaderboardReference.child(leaderboardName).child(mUserData.uuid).setValue(score);

        Log.d("RCD", "UPDATING " + leaderboardName);
        Log.d("RCD", "NOW " + String.valueOf(score));

    }


    private void onFileNamesLoaded() {
        Log.d("RCD", "onFileNamesLoaded");
        Log.d("RCD", "Filenames loaded successfully");
        Log.d("RCD", "There are " + String.valueOf(filenames.size()) + " files");
        loadUserData();
        loadAnchorData();
    }

    private void loadAnchorData() {
        Log.d("RCD", "loadAnchorData");
        DatabaseReference reference = mDatabase.child("images").child("anchors");
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                ArrayList<String> anchorsList = (ArrayList<String>)snapshot.getValue();
                for (int i = 0; i < anchorsList.size(); i ++) {
                    String anchor = anchorsList.get(i);
                    anchorMap.put(i,anchor);
                }

                fetchAnchorDownloadURLs();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

    }

    private void fetchAnchorDownloadURLs() {
        for(Map.Entry<Integer,String> entry : anchorMap.entrySet()){
            final String filename = entry.getValue();

            mStorageRef.child(filename).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    String url = uri.toString();
                    anchorUrlMap.put(filename,url);
                    Log.d("ANCHOR","filename: " + filename);
                    Log.d("ANCHOR","URL: " + url);
                }
            });
        }
    }


    public ArrayList<String> getEraNamesArrayList(){
        Set<String> keys = mEraFilenames.keySet();
        return new ArrayList(keys);
    }

    public String[] getEraNames(){
        Set<String> keys = mEraFilenames.keySet();
        ArrayList<String> arrayList = new ArrayList(keys);
        return arrayList.toArray(new String[arrayList.size()]);
    }

    public void loadFilenamesUrls(ArrayList<String> imageFilenames){
        for(String filename : imageFilenames){
            Log.d("RCD",filename);
            final String fileWithType = filename + ".jpg";
            mStorageRef.child(fileWithType).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    String url = uri.toString();
                    Log.d("RCD","received");
                    if(mUrlListener!=null){
                        mUrlListener.onUrlDownloaded(url);
                    }


                }
            });
        }
    }

    public void registerUrlListener(UrlDownloadListener listener) {
        mUrlListener = listener;
    }

    public void registerEraListener(EraDownloadListener listener) {
        mEraListener = listener;
    }

    public interface UrlDownloadListener{
        void onUrlDownloaded(String url);
    }

    public ArrayList<String> getFilenamesByEra(String era) {
        if (mEraFilenames.containsKey(era)) {
            return mEraFilenames.get(era);
        } else {
            return new ArrayList<>();
        }
    }

    private void loadFilenames() {
        Log.d("RCD","loadFilenames");

        StorageReference filenamesRef = mStorageRef.child("filenames.txt");

        final long ONE_MEGABYTE = 1024 * 1024;
        filenamesRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                loadFilenamesFromByteString(bytes);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle any errors
            }
        });
    }

    private void loadImageEras() {
        Log.d("RCD","loadImageEras");

        mImagesReference.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        //Get map of users in datasnapshot
                        collectImageEras((Map<String,Object>) dataSnapshot.getValue());
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        //handle databaseError
                    }
                });
    }

    private void loadImageData() {
        loadFilenames();
        loadImageEras();
        initializationListener.onInitializationComplete();
    }

    private void collectImageEras(Map<String, Object> users) {
        mEraFilenames = new HashMap<>();
        for (Map.Entry<String, Object> entry : users.entrySet()){
            Log.d("RCD","test");
            String imageName = entry.getKey();
            String era = null;
            try {
                era = (String) ((HashMap)(entry.getValue())).get("era");
            } catch (ClassCastException e) {
                e.printStackTrace();
                continue;
            }
            if(mEraFilenames.containsKey(era)){
                ArrayList<String> arrayList = mEraFilenames.get(era);
                arrayList.add(imageName);
            } else {
                ArrayList<String> arrayList = new ArrayList<>();
                arrayList.add(imageName);
                mEraFilenames.put(era,arrayList);
            }
        }
    }

    private void loadFilenamesFromByteString(byte[] bytes){
        Log.d("RCD","loadFilenamesFromByteString");
        InputStream is = new ByteArrayInputStream(bytes);

        BufferedReader reader;
        try {
            reader = new BufferedReader(new InputStreamReader(is));
            String filename = reader.readLine();
            while(filename != null){
                filenames.add(filename);
                filename = reader.readLine();
            }
            is.close();
            onFileNamesLoaded();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (is != null)
                    is.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void updateImageEra(String filename, String era) {
        String name = FilenameUtils.removeExtension(filename);
        mImagesReference.child(name).child("era").setValue(era);
    }

    public void updateImageDate(String filename, String date) {
        String name = FilenameUtils.removeExtension(filename);
        mImagesReference.child(name).child("date").setValue(date);
    }

    public void fetchEra(String targetFilename) {
        String formatted = FilenameUtils.removeExtension(targetFilename);
        mImagesReference.child(formatted).child("era").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Log.d("RCD",snapshot.toString());
                if(snapshot.getValue()!=null){
                    mEraListener.onEraDownloaded((String)snapshot.getValue());
                } else {
                    mEraListener.onEraDownloaded("Unknown Era");
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    EraDownloadListener mEraListener;
    public interface EraDownloadListener {
        void onEraDownloaded(String era);
    }

    public HashMap<Integer,String> getAnchorMap() {
        return anchorMap;
    }

    public HashMap<String,String> getAnchorUrlMap() {
        return anchorUrlMap;
    }

    public interface InitializationListener {
        void onInitializationComplete();
    }
}
