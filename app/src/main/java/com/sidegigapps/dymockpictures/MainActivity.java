package com.sidegigapps.dymockpictures;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

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
import com.google.firebase.storage.StreamDownloadTask;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;
import com.mikepenz.materialdrawer.util.AbstractDrawerImageLoader;
import com.mikepenz.materialdrawer.util.DrawerImageLoader;
import com.mikepenz.materialdrawer.util.DrawerUIUtils;
import com.sidegigapps.dymockpictures.fragments.AchievementsFragment;
import com.sidegigapps.dymockpictures.fragments.LeaderboardFragment;
import com.sidegigapps.dymockpictures.fragments.ViewPhotosFragment;
import com.sidegigapps.dymockpictures.models.Leaderboard;
import com.sidegigapps.dymockpictures.models.UserData;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity implements
        ViewPhotosFragment.OnFragmentInteractionListener {


    private static final String LEADERBOARDS = "leaderboards";
    private static final String USERS = "users";
    public static final String LEADERBOARD_VIEWS = "Views";
    public static final String LEADERBOARD_ROTATIONS = "Rotations";
    public static final String LEADERBOARD_SORTED ="Sorted";
    public static final String LEADERBOARD_TRANSCRIBED = "Transcribed";
    public static final String LEADERBOARD_DOWNLOADS = "Downloads";
    private GoogleSignInAccount mGoogleSignInAccount;
    private SharedPreferences prefs;
    private FirebaseAuth mAuth;
    private static final String TAG = "RCD";
    private Toolbar mToolbar;
    private ViewPhotosFragment viewPhotosFragment;
    private LeaderboardFragment leaderboardFragment;
    private AchievementsFragment achievementsFragment;
    private StorageReference mStorageRef;
    private DatabaseReference mDatabase;
    private DatabaseReference mUsersReference;
    private DatabaseReference mLeaderboardReference;
    private UserData mUserData;
    private HashMap<String,Leaderboard> mLeaderboardsMap = new HashMap<>();
    private HashMap<String,UserData> mUsersMap = new HashMap<>();
    public List<String> filenames = new ArrayList<>();

    public boolean mUserDataLoaded = false;
    public boolean mLeaderboardDataLoaded = false;
    private boolean mSetupComplete = false;

    public final String [] leaderboardNames = new String []{
            LEADERBOARD_VIEWS,
            LEADERBOARD_ROTATIONS,
            //LEADERBOARD_SORTED,
            //LEADERBOARD_TRANSCRIBED,
            LEADERBOARD_DOWNLOADS
    };

    Drawer drawer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("status",Context.MODE_PRIVATE);
        prefs.edit().putBoolean("signed_in", true).apply();

        mGoogleSignInAccount = getIntent().getParcelableExtra("ACCOUNT");
        onConnected(mGoogleSignInAccount);
        mAuth = FirebaseAuth.getInstance();

        mStorageRef = FirebaseStorage.getInstance().getReference();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        mUsersReference = mDatabase.child(USERS);
        mLeaderboardReference = mDatabase.child(LEADERBOARDS);

        loadImageData();

        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        setupDrawer();
    }

    private void loadImageData() {
        loadFilenames();

    }

    private void onFileNamesLoaded() {
        Log.d("RCD","onFileNamesLoaded");
        Log.d("RCD","Filenames loaded successfully");
        Log.d("RCD","There are " + String.valueOf(filenames.size()) + " files");
        loadUserData();
    }

    private void onUserDataLoaded(){
        Log.d("RCD","onUserDataLoaded");
        setupLeaderboardListener();
    }

    private void onLeaderboardDataLoaded(){
        Log.d("RCD","onLeaderboardDataLoaded");
        showPhotos();
    }

    public UserData getmUserData(){
        return mUserData;
    }

    private void setupLeaderboardListener(){
        Log.d("RCD","setupLeaderboardListener");

        mLeaderboardReference.addValueEventListener(new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot snapshot) {
            for(String leaderboardName: leaderboardNames){
                if (snapshot.child(leaderboardName).exists()) {
                    HashMap<String,Long> map = (HashMap<String, Long>) snapshot.child(leaderboardName).getValue();
                    Leaderboard newLeaderboard = new Leaderboard(leaderboardName,map);
                    mLeaderboardsMap.put(leaderboardName,newLeaderboard);
                }else{
                    Log.d("RCD","leaderboard does not exist: " + leaderboardName);
                    mLeaderboardReference.child(leaderboardName).child(mUserData.uuid).setValue(0);  //race conditions, mUserData was still null
                    Leaderboard newLeaderboard = new Leaderboard(leaderboardName);
                    mLeaderboardsMap.put(leaderboardName,newLeaderboard);
                }
            }

            if(!mLeaderboardDataLoaded){
                onLeaderboardDataLoaded();
                mLeaderboardDataLoaded = true;
            }

        }
        @Override
        public void onCancelled(DatabaseError databaseError) {
        }
    });

    }

    private void loadUserData() {
        Log.d("RCD","loadUserData");
        mUsersReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.child(mGoogleSignInAccount.getId()).exists()) {
                    Log.d("RCD","account already exists: " + mGoogleSignInAccount.getId());
                    HashMap<String,Object> map = (HashMap<String, Object>) snapshot.getValue();
                    mUserData = new UserData(map, mGoogleSignInAccount.getId());
                }else{
                    Log.d("RCD","account does not exist: " + mGoogleSignInAccount.getId());
                    mUserData = new UserData(mGoogleSignInAccount);
                    mUsersReference.child(mUserData.uuid).setValue(mUserData);
                    Log.d("RCD","user created");
                }

                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    HashMap<String,Object> childMap = (HashMap<String, Object>) childSnapshot.getValue();
                    UserData userData = new UserData(childMap);
                    mUsersMap.put(userData.getUuid(),userData);
                }

                mUserDataLoaded = true;
                onUserDataLoaded();
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

    }

    public UserData getUserDataByUUID(String uuid){
        return mUsersMap.get(uuid);
    }

    public void incrementRotations(){
        incrementScores(LEADERBOARD_ROTATIONS);
    }
    public void incrementViews(){
        incrementScores(LEADERBOARD_VIEWS);
    }
    public void incrementSorts(){
        incrementScores(LEADERBOARD_SORTED);
    }
    public void incrementTranscribes(){
        incrementScores(LEADERBOARD_TRANSCRIBED);
    }
    public void incrementDownloads(){
        incrementScores(LEADERBOARD_DOWNLOADS);
    }
    public void incrementScores(String leaderboardName){
        Leaderboard leaderboard = mLeaderboardsMap.get(leaderboardName);
        long score = leaderboard.getScoreByUUID(mUserData.uuid);
        score +=1;
        mLeaderboardReference.child(leaderboardName).child(mUserData.uuid).setValue(score);

        Log.d("RCD","UPDATING " + leaderboardName);
        Log.d("RCD","NOW " + String.valueOf(score));

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Sign Out?")
                .setMessage("Are you sure you want to sign out?")
                .setNegativeButton(android.R.string.no, null)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        boolean result = prefs.edit().putBoolean("signed_in", false).commit();
                        Log.d("RCD","boolean was " + String.valueOf(result));
                        boolean signed_in = prefs.getBoolean("signed_in", false);
                        Log.d("RCD","boolean is now " + String.valueOf(signed_in));
                        MainActivity.super.onBackPressed();
                    }
                }).create().show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sign_out:
                onBackPressed();

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
    }

    private void onConnected(GoogleSignInAccount acct) {
        Log.d(TAG, "onConnected(): connected to Google APIs");
        String uuid = acct.getId();
        String photoURL = acct.getPhotoUrl().toString();
        String name = acct.getDisplayName();

    }

    private void setupDrawer() {

        DrawerImageLoader.init(new AbstractDrawerImageLoader() {
            @Override
            public void set(ImageView imageView, Uri uri, Drawable placeholder, String tag) {
                GlideApp.with(imageView.getContext())
                        .load(uri)
                        .placeholder(placeholder)
                        .into(imageView);
            }

            @Override
            public void cancel(ImageView imageView) {
                GlideApp.with(getApplicationContext())
                        .clear(imageView);
            }

            @Override
            public Drawable placeholder(Context ctx, String tag) {
                //define different placeholders for different imageView targets
                //default tags are accessible via the DrawerImageLoader.Tags
                //custom ones can be checked via string. see the CustomUrlBasePrimaryDrawerItem LINE 111
                if (DrawerImageLoader.Tags.PROFILE.name().equals(tag)) {
                    return DrawerUIUtils.getPlaceHolder(ctx);
                } else if (DrawerImageLoader.Tags.ACCOUNT_HEADER.name().equals(tag)) {
                    return new IconicsDrawable(ctx).iconText(" ").backgroundColorRes(com.mikepenz.materialdrawer.R.color.primary).sizeDp(56);
                } else if ("customUrlItem".equals(tag)) {
                    return new IconicsDrawable(ctx).iconText(" ").backgroundColorRes(R.color.md_red_500).sizeDp(56);
                }

                //we use the default one for
                //DrawerImageLoader.Tags.PROFILE_DRAWER_ITEM.name()

                return super.placeholder(ctx, tag);
            }
        });

        // Create the AccountHeader

        String name = mGoogleSignInAccount.getDisplayName();
        String email = mGoogleSignInAccount.getEmail();
        Uri profileUri = mGoogleSignInAccount.getPhotoUrl();

        AccountHeader headerResult = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.drawable.header_background)
                .addProfiles(
                        new ProfileDrawerItem().withName(name).withEmail(email).withIcon(profileUri)
                )
                .withOnAccountHeaderListener(new AccountHeader.OnAccountHeaderListener() {
                    @Override
                    public boolean onProfileChanged(View view, IProfile profile, boolean currentProfile) {
                        return false;
                    }
                })
                .build();

        PrimaryDrawerItem viewPhotosItem = new PrimaryDrawerItem()
                .withName("View Photos");
        viewPhotosItem.withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
            @Override
            public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                showPhotos();
                drawer.closeDrawer();
                return true;
            }
        });

        PrimaryDrawerItem leaderboardItem = new PrimaryDrawerItem()
                .withName("Leaderboards");
        leaderboardItem.withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
            @Override
            public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                showLeaderboards();
                drawer.closeDrawer();
                return true;
            }
        });

/*        PrimaryDrawerItem achievementsItem = new PrimaryDrawerItem()
                .withName("Achievements");
        achievementsItem.withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
            @Override
            public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                showAchievements();
                drawer.closeDrawer();
                return true;
            }
        });*/

        drawer = new DrawerBuilder().withActivity(this)
                .withAccountHeader(headerResult)
                .withToolbar(mToolbar)
                .addDrawerItems(viewPhotosItem,leaderboardItem) //no achievements for now
                .build();

    }

    private void showPhotos(){
        Log.d("RCD","showPhotos");
        getSupportActionBar().setTitle("Family Picture Sorting");
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        viewPhotosFragment = new ViewPhotosFragment();

        ft.replace(R.id.fragment_layout, viewPhotosFragment);

        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.commit();
    }

    private void showAchievements() {
        getSupportActionBar().setTitle("Achievements");
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        achievementsFragment = new AchievementsFragment();
        ft.replace(R.id.fragment_layout, achievementsFragment);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.commit();
    }

    private void showLeaderboards() {
        getSupportActionBar().setTitle("Leaderboards");
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        leaderboardFragment = new LeaderboardFragment();
        leaderboardFragment.setLeaderboardData(mLeaderboardsMap);
        ft.replace(R.id.fragment_layout, leaderboardFragment);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.commit();

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
}
