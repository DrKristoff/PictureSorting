package com.sidegigapps.dymockpictures.fragments;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.DatePickerDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.jsibbold.zoomage.ZoomageView;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.sidegigapps.dymockpictures.GlideApp;
import com.sidegigapps.dymockpictures.MainActivity;
import com.sidegigapps.dymockpictures.R;
import com.sidegigapps.dymockpictures.models.FirebaseStore;
import com.sidegigapps.dymockpictures.models.GuidedSelectionHelper;
import com.sidegigapps.dymockpictures.utils.RotateTransformation;
import com.sidegigapps.dymockpictures.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;

public class SortPhotosFragment extends Fragment implements
        EraDialogFragment.OnEraSelectionListener,
        FirebaseStore.EraDownloadListener{

    private static final int REQUEST_CODE = 1;
    private StorageReference mStorageRef;
    private String targetFilenameURL;
    private ZoomageView targetImage, backsideImage;
    private float targetImageRotation = 0f;
    private String targetFilename;
    private FloatingActionButton fab_rotate, fab_new, fab_save, fab_exact;
    Button flip_button;
    private ProgressBar progressBar;
    private TextView eraTextView;

    GuidedSelectionHelper guideHelper;

    private boolean downloadRequested = false;

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private OnFragmentInteractionListener mListener;
    private FrameLayout bkg, guideLayout;
    private Button cancelButton, noButton, sameButton, yesButton;
    private ZoomageView guideTarget, guideComparison;
    private HashMap<String, String> anchorUrlMap = new HashMap<>();
    private boolean mHasBacksideText = false;
    FirebaseStore fbStore;

    MainActivity mActivity;
    private String mFileType;

    public SortPhotosFragment() {
        // Required empty public constructor
    }

    public static SortPhotosFragment newInstance(String param1, String param2) {
        SortPhotosFragment fragment = new SortPhotosFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mStorageRef = FirebaseStorage.getInstance().getReference();
        guideHelper = new GuidedSelectionHelper(
                fbStore.getAnchorMap(),
                getActivity().getResources().getStringArray(R.array.eras_array),
                getActivity().getResources().getStringArray(R.array.era_dates_array)
        );

        mFileType = mActivity.getResources().getString(R.string.fileType);

        anchorUrlMap = fbStore.getAnchorUrlMap();
    }

    public void onURLDownloaded(){
        Log.d("RCD","onURLDownloaded");

        if(mHasBacksideText){
            flip_button.setVisibility(View.VISIBLE);
        } else {
            flip_button.setVisibility(View.INVISIBLE);
        }
        progressBar.setVisibility(View.INVISIBLE);
        displayImage();
    }


    private void onBacksideUrlDownloaded(String url) {
        GlideApp.with(getActivity().getApplicationContext())
                .load(url)
                .placeholder(R.drawable.progress_animation)
                .into(backsideImage);

        backsideImage.setScaleType(ImageView.ScaleType.FIT_CENTER);

        flip_button.setVisibility(View.VISIBLE);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sort_photos, container, false);

        setupUI(view);

        if(this.getArguments()!=null) {
            String filename = this.getArguments().getString("filename");
            String filenameURL = this.getArguments().getString("filenameURL");

            if(filename==null){
                onSaveButtonPressed();
            } else {
                if(filename.endsWith("_b.jpg")){
                    mHasBacksideText = true;
                    filename = Utils.getFrontsideString(filename);
                } else{
                    mHasBacksideText = fbStore.filenames.contains(Utils.getBacksideString(filename));
                }

                loadUrl(filenameURL, filename);
            }
        }


        return view;

    }

    private void setupUI(View view){
        targetImage = view.findViewById(R.id.targetImage);
        backsideImage = view.findViewById(R.id.backsideImage);
        progressBar = view.findViewById(R.id.indeterminateBar);

        eraTextView = view.findViewById(R.id.dateTextView);

        fab_new = view.findViewById(R.id.fab_save_changes);
        fab_rotate = view.findViewById(R.id.fab_rotate);
        fab_save = view.findViewById(R.id.fab_download);
        fab_exact = view.findViewById(R.id.fab_exact);
        flip_button = view.findViewById(R.id.flip_button);

        bkg = view.findViewById(R.id.blackBackground);
        guideLayout = view.findViewById(R.id.guideFrameLayout);
        cancelButton = view.findViewById(R.id.cancelButton);
        yesButton = view.findViewById(R.id.yesButton);
        noButton = view.findViewById(R.id.noButton);
        sameButton = view.findViewById(R.id.sameButton);
        guideTarget = view.findViewById(R.id.guidedTargetImage);
        guideComparison = view.findViewById(R.id.guidedComparisonImage);

        View.OnClickListener guideListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch(v.getId()){
                    case R.id.noButton:
                        guideHelper.makeComparison(GuidedSelectionHelper.OLDER);
                        updateGuideUI();
                        break;
                    case R.id.yesButton:
                        guideHelper.makeComparison(GuidedSelectionHelper.NEWER);
                        updateGuideUI();
                        break;
                    case R.id.cancelButton:
                        hideGuideDialog();
                        guideHelper.reset();
                        break;
                    case R.id.sameButton:
                        String date = guideHelper.getDate();
                        onDateSelected(date);
                        hideGuideDialog();
                        guideHelper.reset();
                        break;
                }
            }
        };

        cancelButton.setOnClickListener(guideListener);
        noButton.setOnClickListener(guideListener);
        guideTarget.setOnClickListener(guideListener);
        guideComparison.setOnClickListener(guideListener);
        yesButton.setOnClickListener(guideListener);
        sameButton.setOnClickListener(guideListener);
        yesButton.setOnClickListener(guideListener);


        eraTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onEraSortPressed();
            }
        });

        setupFABs();

    }

    private void updateGuideUI() {
        if(guideHelper.isFinished()){
            String date = guideHelper.getDate();
            onDateSelected(date);
            guideHelper.reset();
            hideGuideDialog();
        } else {
            String comparisonFilename = guideHelper.getComparisonFilename();
            String comparisonURL = anchorUrlMap.get(comparisonFilename);

            GlideApp.with(getActivity().getApplicationContext())
                    .load(comparisonURL)
                    .placeholder(R.drawable.progress_animation)
                    .into(guideComparison);

            guideComparison.setScaleType(ImageView.ScaleType.FIT_CENTER);

        }

    }

    private void setupFABs() {

        View.OnClickListener fab_listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int id = v.getId();
                switch (id) {
                    case R.id.fab_save_changes:
                        onSaveButtonPressed();
                        break;
                    case R.id.fab_download:
                        onSaveFABPressed();
                        break;
                    case R.id.fab_rotate:
                        onRotateFabPressed();
                        break;
                    case R.id.fab_exact:
                        onExactSortPressed();
                        break;
                    case R.id.flip_button:
                        onFlipPressed();
                        break;
                }

            }
        };

        fab_new.setOnClickListener(fab_listener);
        fab_rotate.setOnClickListener(fab_listener);
        fab_save.setOnClickListener(fab_listener);
        fab_exact.setOnClickListener(fab_listener);
        flip_button.setOnClickListener(fab_listener);



        //fab_exact.setOnClickListener(fab_listener);
        //fab_era.setOnClickListener(fab_listener);
        //fab_guided.setOnClickListener(fab_listener);

    }

    private void onFlipPressed() {
        if(targetImage.getVisibility()==View.VISIBLE){
            flip_button.setText("FRONT");
            targetImage.animate()
                    .alpha(0.0f)
                    .setDuration(250)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            targetImage.setVisibility(View.INVISIBLE);
                        }
                    });

            backsideImage.setVisibility(View.VISIBLE);
            backsideImage.animate()
                    .alpha(1.0f)
                    .setDuration(250)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                        }
                    });
        } else {
            flip_button.setText("BACK");
            backsideImage.animate()
                .alpha(0.0f)
                .setDuration(250)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        backsideImage.setVisibility(View.INVISIBLE);
                    }
                });

            targetImage.setVisibility(View.VISIBLE);
            targetImage.animate()
                    .alpha(1.0f)
                    .setDuration(250)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                        }
                    });
        }
    }

    private void onGuidedSortPressed(){
        guideHelper.setFilename(targetFilename);
        showGuideDialog();
        HashMap<Integer,String> anchorMap = fbStore.getAnchorMap();
    }

    private void showGuideDialog(){
        GlideApp.with(getActivity().getApplicationContext())
                .load(targetFilenameURL)
                .placeholder(R.drawable.progress_animation)
                .transform(new RotateTransformation(getActivity(), targetImageRotation))
                .into(guideTarget);


        guideTarget.setScaleType(ImageView.ScaleType.FIT_CENTER);


        bkg.setVisibility(View.VISIBLE);

        bkg.animate()
                .alpha(1.0f)
                .setDuration(100)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        bkg.setVisibility(View.VISIBLE);
                    }
                });

        guideLayout.setVisibility(View.VISIBLE);
        guideLayout.animate()
                .alpha(1.0f)
                .setDuration(500)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        guideLayout.setVisibility(View.VISIBLE);
                    }
                });

    }

    private void hideGuideDialog(){
        bkg.animate()
                .alpha(0.0f)
                .setDuration(300)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        bkg.setVisibility(View.INVISIBLE);
                    }
                });

        guideLayout.animate()
                .alpha(0.0f)
                .setDuration(300)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        guideLayout.setVisibility(View.INVISIBLE);
                    }
                });

    }

    private void onEraSortPressed() {
        final String[] eraStrings = getActivity().getResources().getStringArray(R.array.eras_array);
        new AlertDialog.Builder(getActivity())
                .setSingleChoiceItems(eraStrings, 0, null)
                .setPositiveButton(R.string.ok_button_label, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        int selectedPosition = ((AlertDialog)dialog).getListView().getCheckedItemPosition();
                        onEraSelected(eraStrings[selectedPosition]);
                        dialog.dismiss();
                    }
                })
                .setNeutralButton(R.string.help_button,new DialogInterface.OnClickListener(){

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        onGuidedSortPressed();
                    }
                })
                .setNegativeButton(R.string.cancel,new DialogInterface.OnClickListener(){

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private void onExactSortPressed() {
        // Get Current Date
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(getActivity(),
                new DatePickerDialog.OnDateSetListener() {

                    @Override
                    public void onDateSet(DatePicker view, int year,
                                          int monthOfYear, int dayOfMonth) {
                        String formattedMonth = String.format(Locale.US,"%02d", (monthOfYear+1));
                        String formattedDay = String.format(Locale.US,"%02d", (dayOfMonth+1));
                        String formattedYear = String.valueOf(year);

                        String result = formattedMonth + "-" + formattedDay + "-" + formattedYear;

                        Log.d("RCD",result);
                        onDateSelected(result);
                    }
                }, year, month, day);
        datePickerDialog.show();
    }

    private void getNextDownloadLink(){
        Random random = new Random();

        int index = random.nextInt(fbStore.filenames.size());
        String filename = fbStore.filenames.get(index);

        if(filename.endsWith("_b.jpg")){
            mHasBacksideText = true;
            filename = Utils.getFrontsideString(filename);
        } else{
            mHasBacksideText = fbStore.filenames.contains(Utils.getBacksideString(filename));
        }

        getFirebaseDownloadURL(filename);
    }

    private void displayImage() {
        Log.d("RCD","displayImage");

        if(targetFilenameURL==null){
            Log.d("RCD","targetFilenameURL was null");
            return;
        }

        Log.d("RCD", targetFilenameURL);

        GlideApp.with(getActivity().getApplicationContext())
                .load(targetFilenameURL)
                .placeholder(R.drawable.progress_animation)
                .into(targetImage);


        progressBar.setVisibility(View.GONE);
        targetImage.setVisibility(View.VISIBLE);

        targetImage.setScaleType(ImageView.ScaleType.FIT_CENTER);

        incrementViews();
    }

    private void incrementViews() {
        fbStore.incrementViews();
    }

    private void incrementRotations() {
        fbStore.incrementRotations();
    }

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mActivity = (MainActivity)getActivity();

        fbStore = mActivity.getFbStore();
        fbStore.registerEraListener(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        fbStore.registerEraListener(null);
    }

    private void downloadTargetImage(final String filename, final String url) {
        if (getActivity().checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Log.v("RCD", "Permission is granted");
            Log.v("RCD", "Attemping download of " +url);


            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

            DownloadManager dm = (DownloadManager) getActivity().getSystemService(getActivity().DOWNLOAD_SERVICE);
            dm.enqueue(request);

        } else {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE);
        }
    }

    public void onSaveButtonPressed() {
        if (targetImageRotation != 0f) {
            //implies that the previous image was rotated.
            Toast.makeText(mActivity,"ATTEMPTING TO SAVE ROTATION",Toast.LENGTH_LONG).show();
            uploadBitmapToFirebase(targetFilename,targetFilenameURL,targetImageRotation);
            incrementRotations();
        }


        /*targetImageRotation = 0f;
        targetImage.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.VISIBLE);
        eraTextView.setText("Loading");*/

        //getNextDownloadLink();
    }

    public void onSaveFABPressed() {
        downloadRequested = true;
        uploadBitmapToFirebase(targetFilename,targetFilenameURL,targetImageRotation);
        targetImageRotation = 0f;
        Toast.makeText(getActivity(), "Saving Image", Toast.LENGTH_SHORT).show();
        fbStore.incrementDownloads();
    }

    public void onRotateFabPressed() {
        targetImageRotation = targetImageRotation - 90;
        if (targetImageRotation < 0)
            targetImageRotation = targetImageRotation + 360;
        rotateTargetImage();

    }

    private void rotateTargetImage() {
        Log.d("RCD", "Rotating:");
        Log.d("RCD", targetFilenameURL);
        GlideApp.with(this)
                .load(targetFilenameURL)
                .placeholder(R.drawable.progress_animation)
                .transform(new RotateTransformation(getActivity(), targetImageRotation))
                .into(targetImage);
        targetImage.setScaleType(ImageView.ScaleType.FIT_CENTER);


    }

    private void uploadBitmapToFirebase(final String filename, final String url, final float rotation) {

        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getActivity())
                .build();
        ImageLoader.getInstance().init(config);
        ImageLoader imageLoader = ImageLoader.getInstance();

        imageLoader.loadImage(url, new SimpleImageLoadingListener() {
            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                RotateTransformation transformation = new RotateTransformation(getActivity(), rotation);
                Bitmap rotated = transformation.transform(loadedImage);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                rotated.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                byte[] data = baos.toByteArray();

                StorageReference reference = mStorageRef.child(filename + mFileType);
                UploadTask uploadTask = reference.putBytes(data);
                uploadTask.addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        Toast.makeText(getActivity(), "Uh oh.  There was a problem.", Toast.LENGTH_LONG).show();
                    }
                }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Toast.makeText(getActivity(), "Uploaded Successfully: " + filename, Toast.LENGTH_LONG).show();
                        if(downloadRequested){
                            getFirebaseDownloadURL(filename);
                     }
                    }
                });
            }
        });
    }

    private void loadUrl(String url, String filename){
        Log.d("download","getFirebaseDownloadURL:");
        Log.d("download",url);
        targetFilename = filename;
        targetFilenameURL = url;
        getEraFromFireBase(targetFilename);
        onURLDownloaded();
        if(downloadRequested){
            downloadTargetImage(filename,targetFilenameURL);
            downloadRequested = false;
        }

        if(mHasBacksideText){
            String backsideString = filename.substring(0,filename.length()-4) + "_b.jpg";
            mStorageRef.child(backsideString).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    String url = uri.toString();
                    onBacksideUrlDownloaded(url);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    Log.d("download", "FAILED");
                    Log.d("download", exception.getMessage());
                }
            });

        }
    }

    private void getFirebaseDownloadURL(final String filename) {
        mStorageRef.child(filename).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                loadUrl(uri.toString(), filename);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.d("download", "FAILED");
                Log.d("download", exception.getMessage());
            }
        });

    }

    private void getEraFromFireBase(String targetFilename) {
        fbStore.fetchEra(targetFilename);

    }

    public void onEraReceivedFromFirebase(String era){
        eraTextView.setText(era);
    }

    @Override
    public void onFinishEditDialog(int selection) {
        String [] eras = getActivity().getResources().getStringArray(R.array.eras_array);
        onEraSelected(eras[selection]);
    }

    private void onEraSelected(String era) {
        fbStore.incrementSorts();
        String date = getDateFromEra(era,getActivity());
        eraTextView.setText(era);
        fbStore.updateImageDate(targetFilename,date);
        fbStore.updateImageEra(targetFilename,era);

        String backsideFilename = Utils.getBacksideString(targetFilename);
        if(mHasBacksideText){
            fbStore.updateImageDate(backsideFilename,date);
            fbStore.updateImageEra(backsideFilename,era);
        }

    }

    private void onDateSelected(String date){
        String era = getEraFromDate(date);
        eraTextView.setText(era);
        fbStore.updateImageDate(targetFilename,date);
        fbStore.updateImageEra(targetFilename,era);

        String backsideFilename = Utils.getBacksideString(targetFilename);
        if(mHasBacksideText){
            fbStore.updateImageDate(backsideFilename,date);
            fbStore.updateImageEra(backsideFilename,era);
        }
    }

    @Override
    public void onEraDownloaded(String era) {
        onEraReceivedFromFirebase(era);
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }

    public String getEraFromDate(String date) {
        //"mm/dd/yyyy" > "Era Name"

        int numericalDate = convertDateToInt(date);

        Log.d("RCD","Finding ERA from " + date);
        String [] era_dates = getActivity().getResources().getStringArray(R.array.era_dates_array);
        String [] era_strings = getActivity().getResources().getStringArray(R.array.eras_array);

        int chosen_era_index = 0;

        for(int i = 0; i<era_dates.length;i++) {
            int compareDate = convertDateToInt(era_dates[i]);
            if (compareDate < numericalDate) {
                chosen_era_index = i;
            }
        }

        Log.d("RCD","FOUND " + era_strings[chosen_era_index]);
        return era_strings[chosen_era_index];
    }

    public int convertDateToInt(String date){
        Log.d("RCD","attempting conversion of " + date);
        String[] dateParts = date.split("-");
        return Integer.parseInt(dateParts[2])*10000 +
                Integer.parseInt(dateParts[0])*100 +
                Integer.parseInt(dateParts[1]);
    }

    public String getDateFromEra(String era, Context context){
        String [] era_dates = context.getResources().getStringArray(R.array.era_dates_array);
        String [] era_strings = context.getResources().getStringArray(R.array.eras_array);

        for(int i=0;i<era_dates.length;i++){
            if(era.equals(era_strings[i])){
                return era_dates[i];
            }
        }

        return era_dates[0];

    }
}
