package com.sidegigapps.dymockpictures.models;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

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
import com.google.firebase.storage.UploadTask;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.sidegigapps.dymockpictures.utils.RotateTransformation;

import org.apache.commons.io.FilenameUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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


    private static final int REQUEST_CODE = 1;

    private String mFileType = ".jpg";
    private String backsideSuffic = "_b";
    private static final String LEADERBOARDS = "leaderboards";
    private static final String USERS = "users";
    private static final String IMAGES = "images";
    private DatabaseReference mUsersReference, mLeaderboardReference, mImagesReference;

    private HashMap<Integer,String> anchorMap = new HashMap<>();
    private HashMap<String,String> anchorUrlMap = new HashMap<>();
    public HashMap<String, ArrayList<String>> mEraFilenames = new HashMap<>();
    public List<String> filenames = new ArrayList<>();
    private HashMap<String,String> filenamesUrlMap = new HashMap<>();


    public FirebaseStore(GoogleSignInAccount acct, InitializationListener initListener){
        this.mGoogleSignInAccount = acct;
        this.initializationListener = initListener;
        initDatabase();
        initLeaderboards();
        loadImageData();

    }

    private void initDatabase(){
        mAuth = FirebaseAuth.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mUsersReference = mDatabase.child(USERS);
        mLeaderboardReference = mDatabase.child(LEADERBOARDS);
        mImagesReference = mDatabase.child(IMAGES);
    }

    LeaderboardStore leaderboards;
    private void initLeaderboards(){
        leaderboards = new LeaderboardStore(mLeaderboardReference, mUsersReference, mGoogleSignInAccount);
    }


    public HashMap<String, Leaderboard> getmLeaderboardsMap() {
        return leaderboards.getLeaderboardsMap();
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
        return leaderboards.getUserDataByUUID(uuid);
    }

    public void incrementRotations() {
        leaderboards.incrementScores(leaderboards.LEADERBOARD_ROTATIONS);
    }

    public void incrementViews() {
        leaderboards.incrementScores(leaderboards.LEADERBOARD_VIEWS);
    }

    public void incrementSorts() {
        leaderboards.incrementScores(leaderboards.LEADERBOARD_SORTED);
    }

    public void incrementTranscribes() {
        leaderboards.incrementScores(leaderboards.LEADERBOARD_TRANSCRIBED);
    }

    public void incrementDownloads() {
        leaderboards.incrementScores(leaderboards.LEADERBOARD_DOWNLOADS);
    }


    private void onFileNamesLoaded() {
        Log.d("RCD", "onFileNamesLoaded");
        Log.d("RCD", "Filenames loaded successfully");
        Log.d("RCD", "There are " + String.valueOf(filenames.size()) + " files");
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
        Log.d("FirebaseStore","loadFilenamesUrls " + String.valueOf(imageFilenames.size()));
        for(final String filename : imageFilenames){
            Log.d("FirebaseStore",filename);
            final String fileWithType = filename + ".jpg";
            mStorageRef.child(fileWithType).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    String url = uri.toString();
                    Log.d("FirebaseStore","received: " + fileWithType);
                    if(mUrlListener!=null){
                        mUrlListener.onUrlReceived(filename,url);
                    }


                }
            });
        }

        mUrlListener.onAllUrlsReceived();
    }

    public ArrayList<String> getFilenamesByEra(String era, boolean includeBackside) {
        if (mEraFilenames.containsKey(era)) {
            if(includeBackside){
                return mEraFilenames.get(era);
            } else {
                ArrayList<String> results = new ArrayList<>();
                for(String filename : mEraFilenames.get(era)){
                    if(!filename.endsWith("_b")){
                        results.add(filename);
                    }
                }
                return results;
            }
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
                era = ((HashMap<String,String>)(entry.getValue())).get("era");
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

        mUrlListener.onEraSetupComplete();
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

    public void getFirebaseDownloadURL(final String filename) {
        mStorageRef.child(filename).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                mUpDownListener.onUrlReceived(uri.toString(), filename);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.d("download", "FAILED");
                Log.d("download", exception.getMessage());
            }
        });

    }


    public void downloadTargetImage(Activity activity, final String filename, final String url) {
        if (activity.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Log.v("RCD", "Permission is granted");
            Log.v("RCD", "Attemping download of " +url);


            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

            DownloadManager dm = (DownloadManager) activity.getSystemService(activity.DOWNLOAD_SERVICE);
            dm.enqueue(request);

        } else {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE);
        }
    }

    public void uploadBitmapToFirebase(final Activity activity, final String filename, final String url, final float rotation) {

        incrementRotations();

        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(activity)
                .build();
        ImageLoader.getInstance().init(config);
        ImageLoader imageLoader = ImageLoader.getInstance();

        imageLoader.loadImage(url, new SimpleImageLoadingListener() {
            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                RotateTransformation transformation = new RotateTransformation(activity, rotation);
                Bitmap rotated = transformation.transform(loadedImage);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                rotated.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                byte[] data = baos.toByteArray();

                StorageReference reference = mStorageRef.child(filename + mFileType);
                UploadTask uploadTask = reference.putBytes(data);
                uploadTask.addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        Toast.makeText(activity, "Uh oh.  There was a problem.", Toast.LENGTH_LONG).show();
                    }
                }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Toast.makeText(activity, "Uploaded Successfully: " + filename, Toast.LENGTH_LONG).show();

                        mUpDownListener.onUploadComplete(filename);
                    }
                });
            }
        });
    }

    /**
     * Interfaces and Listeners
     */


    InitializationListener initializationListener;
    UrlReceivedListener mUrlListener;
    UploadDownloadListener mUpDownListener;

    @Nullable
    public void registerUrlListener(UrlReceivedListener listener, UploadDownloadListener uploadDownloadListener) {
        mUrlListener = listener;
        mUpDownListener = uploadDownloadListener;
    }

    public void registerEraListener(EraDownloadListener listener) {
        mEraListener = listener;
    }

    public interface UrlReceivedListener {
        void onUrlReceived(String filename, String url);
        void onAllUrlsReceived();
        void onEraSetupComplete();
    }

    public interface UploadDownloadListener {
        void onUrlReceived(String url, String filename);
        void onUploadComplete(String filename);
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
