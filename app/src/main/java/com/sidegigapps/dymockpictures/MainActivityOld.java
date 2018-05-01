package com.sidegigapps.dymockpictures;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.games.AchievementsClient;
import com.google.android.gms.games.EventsClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.LeaderboardsClient;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.PlayersClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.jsibbold.zoomage.ZoomageView;
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
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.sidegigapps.dymockpictures.utils.RotateTransformation;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class MainActivityOld extends AppCompatActivity {

    private StorageReference mStorageRef;
    private HashMap<String, String> downloadLinksMap = new HashMap<>();

    private static final String TAG = "RCD";

    String[] filenamesArray = new String[]{"1981_0002.jpg", "1981_0003.jpg", "1981_0002_b.jpg","1981_0002.jpg",
            "1981_0003_b.jpg","1981_0002.jpg", "1981_0004.jpg","1981_0002.jpg", "1981_0004_b.jpg","1981_0002.jpg", "secondarg",};

    private FirebaseAuth mAuth;
    private FirebaseUser mUser;

    private GoogleSignInAccount mGoogleSignInAccount;
    private AchievementsClient mAchievementsClient;
    private LeaderboardsClient mLeaderboardsClient;
    private EventsClient mEventsClient;
    private PlayersClient mPlayersClient;
    private GamesClient gamesClient;

    private static final int REQUEST_CODE = 1;
    private static final int RC_SIGN_IN = 9001;
    private static final int RC_FIREBASE_SIGN_IN = 123;
    private static final int RC_UNUSED = 5001;

    private List<String> filenames = new ArrayList<>();
    private ArrayList<Integer> mYearIds = new ArrayList<>();

    private String targetFilename;
    private float targetImageRotation = 0f;

    private HashMap<Integer, Integer> mYearImages = new HashMap<>();

    private boolean isFabOpen = false;
    private FloatingActionButton fab_rotate, fab_new, fab_save, fab_reset_zoom;
    private Animation fab_open, fab_close, rotate_forward, rotate_backward;
    private String targetFilenameURL;
    private ZoomageView targetImage;

    private SharedPreferences prefs;

    private final AccomplishmentsOutbox mOutbox = new AccomplishmentsOutbox();
    private Toolbar mToolbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_view_photos);
        prefs = getPreferences(Context.MODE_PRIVATE);

        mGoogleSignInAccount = getIntent().getParcelableExtra("ACCOUNT");
        onConnected(mGoogleSignInAccount);
        mAuth = FirebaseAuth.getInstance();

        gamesClient = Games.getGamesClient(MainActivityOld.this, mGoogleSignInAccount);
        gamesClient.setViewForPopups(findViewById(R.id.constraint_layout));

        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        setupDrawer();

        mStorageRef = FirebaseStorage.getInstance().getReference();

        loadJSONFromAsset();

        targetImage = findViewById(R.id.targetImage);
        fetchNewTargetImage();

        fab_new = findViewById(R.id.fab_new);
        fab_rotate = findViewById(R.id.fab_rotate);
        fab_save = findViewById(R.id.fab_save);
        fab_reset_zoom = findViewById(R.id.fab_reset_zoom);

        //fab_open = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_open);
        //fab_close = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.fab_close);
        //rotate_forward = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.rotate_forward);
        //rotate_backward = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.rotate_backward);

        View.OnClickListener fab_listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int id = v.getId();
                switch (id) {
                    case R.id.fab_new:
                        fetchNewTargetImage();
                        break;
                    case R.id.fab_save:
                        downloadTargetImage();
                        Toast.makeText(getApplicationContext(), "Saving Image", Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.fab_rotate:
                        targetImageRotation = targetImageRotation - 90;
                        if (targetImageRotation < 0)
                            targetImageRotation = targetImageRotation + 360;
                        rotateTargetImage();
                        Toast.makeText(getApplicationContext(), "Rotated Left", Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.fab_reset_zoom:
                        targetImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        break;
                }

            }
        };

        fab_new.setOnClickListener(fab_listener);
        fab_rotate.setOnClickListener(fab_listener);
        fab_save.setOnClickListener(fab_listener);
        fab_reset_zoom.setOnClickListener(fab_listener);


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
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
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Sign Out?")
                .setMessage("Are you sure you want to sign out?")
                .setNegativeButton(android.R.string.no, null)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MainActivityOld.super.onBackPressed();
                    }
                }).create().show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

    }

    private void downloadTargetImage() {
        if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Log.v("RCD", "Permission is granted");
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadLinksMap.get(targetFilename)));
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, targetFilename);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            dm.enqueue(request);
            mOutbox.mHasDownloaded = true;

        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE);
        }


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.v("RCD", "Permission: " + permissions[0] + "was " + grantResults[0]);
            downloadTargetImage();
        }
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

        PrimaryDrawerItem leaderboardItem = new PrimaryDrawerItem()
                .withName("Leaderboards");
        leaderboardItem.withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
            @Override
            public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                onShowLeaderboardsRequested();
                return true;
            }
        });

        PrimaryDrawerItem achievementsItem = new PrimaryDrawerItem()
                .withName("Achievements");
        achievementsItem.withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
            @Override
            public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                onShowAchievementsRequested();
                return true;
            }
        });

        new DrawerBuilder().withActivity(this)
                .withAccountHeader(headerResult)
                .withToolbar(mToolbar)
                .addDrawerItems(leaderboardItem,achievementsItem)
                .build();

    }

    private void rotateTargetImage() {
        Log.d("RCD", "Rotating:");
        Log.d("RCD", targetFilenameURL);
        GlideApp.with(this)
                .load(targetFilenameURL)
                .placeholder(R.drawable.progress_animation)
                .transform(new RotateTransformation(this, targetImageRotation))
                .into(targetImage);
        targetImage.setScaleType(ImageView.ScaleType.FIT_CENTER);

        uploadBitmapToFirebase();

    }

    private void fetchNewTargetImage() {

        if(targetImageRotation!=0f){
            updateRotationsAchievement();  //implies that the previous image was rotated.  Checking when fetching a new image so that you can't get unlimited rotations, it only checks when you are done
        }

        targetImageRotation = 0f;

        if (filenames.size() < 1) return;

        Random random = new Random();
        int index = random.nextInt(filenames.size());
        targetFilename = filenames.get(index);

        //FOR TESTING
        //targetFilename = "0001.JPG.";
        //downloadLinksMap.put(targetFilename,"https://firebasestorage.googleapis.com/v0/b/photo-organization-1e84f.appspot.com/o/0001.jpg?alt=media&token=0e5a75c8-9c95-4a62-b772-09e9d8534765");

        targetFilenameURL = downloadLinksMap.get(targetFilename);

        Log.d("RCD", targetFilenameURL);

        GlideApp.with(getApplicationContext())
                .load(targetFilenameURL)
                .placeholder(R.drawable.progress_animation)
                .error(R.drawable.thumb1983)
                .into(targetImage);

        targetImage.setScaleType(ImageView.ScaleType.FIT_CENTER);

        updateViewedAchievement();
        

    }

    private void updateYearCompareImage(int year) {
        int drawableID = mYearImages.get(year);
        ImageView image = findViewById(R.id.yearCompareImage);

        image.setImageResource(drawableID);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);

    }

    private void getUrlAsync(String filename) {
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        StorageReference reference = storageRef.child(filename);
        reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri downloadUrl) {
                targetFilenameURL = downloadUrl.toString();
            }
        });
    }

    public void loadJSONFromAsset() {
        String json = null;
        try {
            InputStream is = getAssets().open("downloads.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }

        try {
            JSONObject jsonObject = new JSONObject(json);
            Iterator<?> keys = jsonObject.keys();

            while (keys.hasNext()) {
                String key = (String) keys.next();
                downloadLinksMap.put(key, jsonObject.getString(key));
                filenames.add(key);
            }

            Log.d("RCD", "Finished filling map with size " + String.valueOf(downloadLinksMap.size()));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void initializeYears() {
        Resources res = this.getResources();

        for (int i = 1982; i < 2018; i++) {
            String imagename = "full" + String.valueOf(i);
            int resID = res.getIdentifier(imagename, "drawable", this.getPackageName());
            //mYears.add(String.valueOf(i));
            //mYearIds.add(resID);
            mYearImages.put(i, resID);
        }

    }

    private void uploadBitmapToFirebase() {

        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this)
                .build();
        ImageLoader.getInstance().init(config);
        ImageLoader imageLoader = ImageLoader.getInstance();

        imageLoader.loadImage(targetFilenameURL, new SimpleImageLoadingListener() {
            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                RotateTransformation transformation = new RotateTransformation(getApplicationContext(), targetImageRotation);
                Bitmap rotated = transformation.transform(loadedImage);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                rotated.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                byte[] data = baos.toByteArray();

                StorageReference reference = mStorageRef.child(targetFilename);
                UploadTask uploadTask = reference.putBytes(data);
                uploadTask.addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        Toast.makeText(getApplicationContext(), "Uh oh.  There was a problem.", Toast.LENGTH_SHORT).show();
                    }
                }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Toast.makeText(getApplicationContext(), "Uploaded Successfully", Toast.LENGTH_SHORT).show();
                        getFirebaseDownloadURL(targetFilename);
                    }
                });
            }
        });

    }

    private void updateRotationsAchievement() {
        int defaultValue = getResources().getInteger(R.integer.default_totals_key);
        int numRotations = prefs.getInt(getString(R.string.num_rotations_key), defaultValue);

        mOutbox.numRotations = numRotations + 1;

        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(getString(R.string.num_rotations_key), mOutbox.numRotations);
        editor.commit();
        Log.d("RCD","numRotations: " + String.valueOf(mOutbox.numRotations));
        pushAccomplishments();
    }

    private void updateViewedAchievement(){
        int defaultValue = getResources().getInteger(R.integer.default_totals_key);
        int numViewed = prefs.getInt(getString(R.string.num_viewed_key), defaultValue);
        mOutbox.numViewed = numViewed + 1;
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(getString(R.string.num_viewed_key), mOutbox.numViewed);
        editor.commit();
        Log.d("RCD","numViewed: " + String.valueOf(mOutbox.numViewed));
        pushAccomplishments();
    }

    private void getFirebaseDownloadURL(String targetFilename) {

        mStorageRef.child(targetFilename).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                Log.d("RCD", "SUCCESS");
                Log.d("RCD", uri.toString());
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.d("RCD", "FAILED");
                Log.d("RCD", exception.getMessage());
            }
        });
    }

    public void onShowAchievementsRequested() {
        mAchievementsClient.getAchievementsIntent()
                .addOnSuccessListener(new OnSuccessListener<Intent>() {
                    @Override
                    public void onSuccess(Intent intent) {
                        startActivityForResult(intent, RC_UNUSED);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        handleException(e, getString(R.string.achievements_exception));
                    }
                });
    }

    public void onShowLeaderboardsRequested() {

        mLeaderboardsClient.getAllLeaderboardsIntent()
                .addOnSuccessListener(new OnSuccessListener<Intent>() {
                    @Override
                    public void onSuccess(Intent intent) {
                        startActivityForResult(intent, RC_UNUSED);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        handleException(e, getString(R.string.leaderboards_exception));
                    }
                });
    }

    private void handleException(Exception e, String details) {
        int status = 0;

        if (e instanceof ApiException) {
            ApiException apiException = (ApiException) e;
            status = apiException.getStatusCode();
        }

        //String message = getString(R.string.status_exception_error, details, status, e);
        String message = "Error";

        new AlertDialog.Builder(MainActivityOld.this)
                .setMessage(message)
                .setNeutralButton(android.R.string.ok, null)
                .show();
    }

    private void pushAccomplishments() {

        //ROTATIONS
        if(mOutbox.numRotations>=1){
            mAchievementsClient.unlock(getString(R.string.achievement_finding_your_way_around__the_new_button));

        }
        if(mOutbox.numRotations>=10){
            mAchievementsClient.unlock(getString(R.string.achievement_10_rotations));

        }
        if(mOutbox.numRotations>=25){
            mAchievementsClient.unlock(getString(R.string.achievement_25_rotations));

        }
        if(mOutbox.numRotations>=100){
            mAchievementsClient.unlock(getString(R.string.achievement_100_rotations));

        }
        if(mOutbox.numRotations>=200){
            mAchievementsClient.unlock(getString(R.string.achievement_200_rotations));

        }
        if(mOutbox.numRotations>=500){
            mAchievementsClient.unlock(getString(R.string.achievement_500_rotations));

        }
        if(mOutbox.numRotations>=1000){
            mAchievementsClient.unlock(getString(R.string.achievement_tasmanian_devil));

        }

        //VIEWED
        if(mOutbox.numViewed>=1) {
            mAchievementsClient.unlock(getString(R.string.achievement_finding_your_way_around__the_new_button));
        }
        if(mOutbox.numViewed>=10) {
            mAchievementsClient.unlock(getString(R.string.achievement_eye_spy_with_my_little_eye));
        }
        if(mOutbox.numViewed>=100) {
            mAchievementsClient.unlock(getString(R.string.achievement_wheres_waldo));
        }
        if(mOutbox.numViewed>=250) {
            mAchievementsClient.unlock(getString(R.string.achievement_nostalgia_seeker));
        }
        mLeaderboardsClient.submitScoreImmediate(getString(R.string.leaderboard_rotations),
                mOutbox.numRotations);
        mLeaderboardsClient.submitScoreImmediate(getString(R.string.leaderboard_viewed),
                mOutbox.numViewed);


    }

    private void onConnected(GoogleSignInAccount googleSignInAccount) {
        Log.d(TAG, "onConnected(): connected to Google APIs");

        mAchievementsClient = Games.getAchievementsClient(this, googleSignInAccount);
        mLeaderboardsClient = Games.getLeaderboardsClient(this, googleSignInAccount);
        mEventsClient = Games.getEventsClient(this, googleSignInAccount);
        mPlayersClient = Games.getPlayersClient(this, googleSignInAccount);

        // Set the greeting appropriately on main menu
        mPlayersClient.getCurrentPlayer()
                .addOnCompleteListener(new OnCompleteListener<Player>() {
                    @Override
                    public void onComplete(@NonNull Task<Player> task) {
                        String displayName;
                        if (task.isSuccessful()) {
                            displayName = task.getResult().getDisplayName();
                        } else {
                            Exception e = task.getException();
                            handleException(e, getString(R.string.players_exception));
                            displayName = "???";
                        }
                        //mMainMenuFragment.setGreeting("Hello, " + displayName);
                    }
                });

    }

    private void onDisconnected(){
        Log.d(TAG, "onDisconnected()");

        mAchievementsClient = null;
        mLeaderboardsClient = null;
        mPlayersClient = null;

    }

    class AccomplishmentsOutbox {
        boolean mHasDownloaded = false;
        public int numViewed;
        public int numRotations;

    }
}
