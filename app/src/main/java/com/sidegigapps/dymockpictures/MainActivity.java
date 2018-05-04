package com.sidegigapps.dymockpictures;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
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

import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements
        ViewPhotosFragment.OnFragmentInteractionListener {


    private static final String LEADERBOARDS = "leaderboards";
    private static final String USERS = "users";
    private static final String LEADERBOARD_VIEWS = "views";
    private static final String LEADERBOARD_ROTATIONS = "rotations";
    private static final String LEADERBOARD_SORTED ="sorted";
    private static final String LEADERBOARD_TRANSCRIBED = "transcribed";
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

    public boolean mUserDataLoaded = false;
    public boolean mLeaderboardDataLoaded = false;
    private boolean mSetupComplete = false;

    final String [] leaderboards = new String []{
            LEADERBOARD_ROTATIONS,
            LEADERBOARD_VIEWS,
            LEADERBOARD_SORTED,
            LEADERBOARD_TRANSCRIBED
    };

    Drawer drawer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getPreferences(Context.MODE_PRIVATE);

        mGoogleSignInAccount = getIntent().getParcelableExtra("ACCOUNT");
        onConnected(mGoogleSignInAccount);
        mAuth = FirebaseAuth.getInstance();

        mStorageRef = FirebaseStorage.getInstance().getReference();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        mUsersReference = mDatabase.child(USERS);
        mLeaderboardReference = mDatabase.child(LEADERBOARDS);

        loadLeaderboardData();

        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        setupDrawer();
    }

    private void onLeaderboardDataLoaded(){
        if(!(mUserDataLoaded && mLeaderboardDataLoaded)) return;

        if(mSetupComplete){
            return;
        } else {
            mSetupComplete = true;
            showPhotos();
        }

    }

    private void loadLeaderboardData() {
        mUsersReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.child(mGoogleSignInAccount.getId()).exists()) {
                    Log.d("RCD","account already exists: " + mGoogleSignInAccount.getId());
                    HashMap<String,Object> map = (HashMap<String, Object>) snapshot.getValue();
                    mUserData = new UserData(map, mGoogleSignInAccount.getId());
                }else{
                    Log.d("RCD","account does not exist: " + mGoogleSignInAccount.getId());
                    UserData data = new UserData(mGoogleSignInAccount);
                    mUsersReference.child(data.uuid).setValue(data);
                    Log.d("RCD","user created");
                }

                mUserDataLoaded = true;
                onLeaderboardDataLoaded();
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

        mLeaderboardReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                mLeaderboardDataLoaded = false;
                for(String leaderboardName: leaderboards){
                    if (snapshot.child(leaderboardName).exists()) {
                        HashMap<String,Long> map = (HashMap<String, Long>) snapshot.child(leaderboardName).getValue();
                        Leaderboard newLeaderboard = new Leaderboard(leaderboardName,map);
                        mLeaderboardsMap.put(leaderboardName,newLeaderboard);
                    }else{
                        Log.d("RCD","leaderboard does not exist: " + leaderboardName);
                        mLeaderboardReference.child(leaderboardName).child(mUserData.uuid).setValue(0);
                        Leaderboard newLeaderboard = new Leaderboard(leaderboardName);
                        mLeaderboardsMap.put(leaderboardName,newLeaderboard);
                    }
                }

                mLeaderboardDataLoaded = true;
                onLeaderboardDataLoaded();

            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

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

    public void incrementScores(String leaderboardName){
        Leaderboard leaderboard = mLeaderboardsMap.get(leaderboardName);
        long score = leaderboard.getRotationsByUUID(mUserData.uuid);
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
                        MainActivity    .super.onBackPressed();
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
        ft.replace(R.id.fragment_layout, leaderboardFragment);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.commit();

    }

}
