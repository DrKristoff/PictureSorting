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
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.auth.FirebaseAuth;
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
import com.sidegigapps.dymockpictures.fragments.SortPhotosFragment;
import com.sidegigapps.dymockpictures.fragments.ViewPhotosFragment;
import com.sidegigapps.dymockpictures.models.FirebaseStore;

public class MainActivity extends AppCompatActivity implements
        FirebaseStore.InitializationListener {

    private static final String VIEW_PHOTOS_FRAG = "view_photos_fragment";
    private static final String SORT_PHOTOS_FRAG = "sort_photos_fragment";
    private static final String LEADERBOARD_FRAG = "leaderboard_fragment";
    private GoogleSignInAccount mGoogleSignInAccount;
    private SharedPreferences prefs;
    private FirebaseAuth mAuth;
    private static final String TAG = "RCD";
    private Toolbar mToolbar;
    private SortPhotosFragment sortPhotosFragment;
    private ViewPhotosFragment viewPhotosFragment;
    private LeaderboardFragment leaderboardFragment;
    private AchievementsFragment achievementsFragment;

    FirebaseStore mFbStore;

    Drawer drawer;
    private String currentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("status", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("signed_in", true).apply();

        mGoogleSignInAccount = getIntent().getParcelableExtra("ACCOUNT");
        onConnected(mGoogleSignInAccount);

        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        mFbStore = new FirebaseStore(mGoogleSignInAccount, this);

        setupDrawer();

        viewPhotos();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public void onBackPressed() {

        if(currentFragment.equals(SORT_PHOTOS_FRAG)){
            viewPhotos();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Sign Out?")
                    .setMessage("Are you sure you want to sign out?")
                    .setNegativeButton(android.R.string.no, null)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            boolean result = prefs.edit().putBoolean("signed_in", false).commit();
                            MainActivity.super.onBackPressed();
                        }
                    }).create().show();
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sign_out:
                onBackPressed();
                break;

            case R.id.show_about:
                showHelpDialog();
                break;

/*            case R.id.show_help:
                boolean checked = prefs.getBoolean("show_labels",false);
                if(checked){
                    item.setChecked(false);
                    displayFabLabels(false);
                    prefs.edit().putBoolean("show_labels",false).commit();
                } else{
                    item.setChecked(true);
                    prefs.edit().putBoolean("show_labels",true).commit();
                    displayFabLabels(true);
                }
                break;*/

            default:
                return super.onOptionsItemSelected(item);
        }
        return false;
    }

    private void displayFabLabels(boolean b) {
    }


    private void showHelpDialog() {
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Light_Dialog_Alert);
        builder.setTitle("So What's the Point?")
                .setMessage(getResources().getString(R.string.help_message))
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
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
                viewPhotos();
                drawer.closeDrawer();
                return true;
            }
        });

        PrimaryDrawerItem sortPhotosItem = new PrimaryDrawerItem()
                .withName("Sort Photos");
        sortPhotosItem.withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
            @Override
            public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                sortPhotos();
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

        drawer = new DrawerBuilder().withActivity(this)
                .withAccountHeader(headerResult)
                .withToolbar(mToolbar)
                .addDrawerItems(viewPhotosItem, sortPhotosItem, leaderboardItem) //no achievements for now
                .build();

    }

    private void viewPhotos() {
        Log.d("RCD", "viewPhotos");
        getSupportActionBar().setTitle("Family Pictures");

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        viewPhotosFragment = new ViewPhotosFragment();

        if(currentFragment==SORT_PHOTOS_FRAG){
            ft.remove(sortPhotosFragment);
        }
        ft.replace(R.id.fragment_layout, viewPhotosFragment);
        currentFragment = VIEW_PHOTOS_FRAG;

        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.commit();
    }

    private void sortPhotos() {
        Log.d("RCD", "sortPhotos");
        getSupportActionBar().setTitle("Family Picture Sorting");
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        sortPhotosFragment = new SortPhotosFragment();

        ft.replace(R.id.fragment_layout, sortPhotosFragment);
        currentFragment = SORT_PHOTOS_FRAG;

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
        leaderboardFragment.setLeaderboardData(mFbStore.getmLeaderboardsMap());
        ft.replace(R.id.fragment_layout, leaderboardFragment);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.commit();

        currentFragment = LEADERBOARD_FRAG;

    }

    public FirebaseStore getFbStore() {
        return mFbStore;
    }

    @Override
    public void onInitializationComplete() {
        //viewPhotos();

        Toast.makeText(this, "LOADING COMPLETE", Toast.LENGTH_SHORT).show();
    }

    public void onPhotoSelected(String filename, String filenameURL) {
        Bundle bundle = new Bundle();
        bundle.putString("filename",filename);
        bundle.putString("filenameURL",filenameURL);

        getSupportActionBar().setTitle("Family Picture Sorting");
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        sortPhotosFragment = new SortPhotosFragment();
        sortPhotosFragment.setArguments(bundle);
        currentFragment = SORT_PHOTOS_FRAG;

        ft.replace(R.id.fragment_layout, sortPhotosFragment);

        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.commit();
    }
}
