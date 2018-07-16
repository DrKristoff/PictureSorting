package com.sidegigapps.dymockpictures.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
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
import com.jsibbold.zoomage.ZoomageView;
import com.sidegigapps.dymockpictures.GlideApp;
import com.sidegigapps.dymockpictures.MainActivity;
import com.sidegigapps.dymockpictures.R;
import com.sidegigapps.dymockpictures.models.FirebaseStore;
import com.sidegigapps.dymockpictures.models.GuidedSelectionHelper;
import com.sidegigapps.dymockpictures.utils.RotateTransformation;
import com.sidegigapps.dymockpictures.utils.Utils;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

public class SortPhotosFragment extends Fragment implements
        EraDialogFragment.OnEraSelectionListener,
        FirebaseStore.EraDownloadListener,
FirebaseStore.UploadDownloadListener{

    private StorageReference mStorageRef;
    private ZoomageView targetImage, backsideImage;
    private float targetImageRotation = 0f;
    private FloatingActionButton fab_rotate, fab_new, fab_save, fab_exact;
    Button flip_button;
    private ProgressBar progressBar;
    private TextView eraTextView;

    GuidedSelectionHelper guideHelper;

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private FrameLayout bkg, guideLayout;
    private Button cancelButton, noButton, sameButton, yesButton;
    private ZoomageView guideTarget, guideComparison;
    private HashMap<String, String> anchorUrlMap = new HashMap<>();
    private boolean mHasBacksideText = false;
    FirebaseStore fbStore;

    MainActivity mActivity;
    private String mFilename;
    private String mFilenameURL;

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

        anchorUrlMap = fbStore.getAnchorUrlMap();
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
            mFilename = this.getArguments().getString("filename");
            mFilenameURL = this.getArguments().getString("filenameURL");

            if(mFilename==null){

            } else {
                if(mFilename.endsWith("_b.jpg")){
                    mHasBacksideText = true;
                    mFilename = Utils.getFrontsideString(mFilename);
                } else{
                    mHasBacksideText = fbStore.filenames.contains(Utils.getBacksideString(mFilename));
                }

                loadUrl(mFilenameURL, mFilename);
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
                        onCheckButtonPressed();
                        break;
                    case R.id.fab_download:
                        onDownloadFABPressed();
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
        guideHelper.setFilename(mFilename);
        showGuideDialog();
        HashMap<Integer,String> anchorMap = fbStore.getAnchorMap();
    }

    private void showGuideDialog(){
        GlideApp.with(getActivity().getApplicationContext())
                .load(mFilenameURL)
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

        DatePickerDialog datePickerDialog = new DatePickerDialog(mActivity,
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

    private void displayImage() {
        Log.d("RCD","displayImage");

        if(mFilenameURL==null){
            Log.d("RCD","targetFilenameURL was null");
            return;
        }

        Log.d("RCD", mFilenameURL);

        GlideApp.with(getActivity().getApplicationContext())
                .load(mFilenameURL)
                .placeholder(R.drawable.progress_animation)
                .into(targetImage);


        progressBar.setVisibility(View.GONE);
        targetImage.setVisibility(View.VISIBLE);

        targetImage.setScaleType(ImageView.ScaleType.FIT_CENTER);

        fbStore.incrementViews();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mActivity = (MainActivity)getActivity();

        fbStore = mActivity.getFbStore();
        fbStore.registerEraListener(this);
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


    @Override
    public void onDetach() {
        super.onDetach();
        fbStore.registerEraListener(null);
    }

    public void onCheckButtonPressed() {
        if (targetImageRotation != 0f) {
            //implies that the image was rotated.
            Toast.makeText(mActivity,"Saving Rotation",Toast.LENGTH_LONG).show();
            fbStore.uploadBitmapToFirebase(getActivity(), mFilename,mFilenameURL,targetImageRotation);
        }

        mActivity.sortFinished();
    }

    public void onDownloadFABPressed() {
        if (targetImageRotation != 0f) {
            //fbStore.uploadBitmapToFirebase(getActivity(), targetFilename,targetFilenameURL,targetImageRotation);
            //can't press download if rotated
        } else {
            fbStore.downloadTargetImage(mActivity,mFilename,mFilenameURL);
        }

        targetImageRotation = 0f;
        Toast.makeText(getActivity(), "Saving Image", Toast.LENGTH_SHORT).show();
        fbStore.incrementDownloads();
    }

    private void disableSave(){
        fab_save.setEnabled(false);
        fab_save.setAlpha(.3f);

    }

    private void reEnableSave() {
        fab_save.setEnabled(true);
        fab_save.setAlpha(1.0f);

    }

    public void onRotateFabPressed() {
        disableSave();
        targetImageRotation = targetImageRotation - 90;
        if (targetImageRotation < 0)
            targetImageRotation = targetImageRotation + 360;
        rotateTargetImage();
        if(targetImageRotation==0) {
            reEnableSave();
        }

    }

    private void rotateTargetImage() {
        Log.d("RCD", "Rotating:");
        Log.d("RCD", mFilenameURL);
        GlideApp.with(this)
                .load(mFilenameURL)
                .placeholder(R.drawable.progress_animation)
                .transform(new RotateTransformation(getActivity(), targetImageRotation))
                .into(targetImage);
        targetImage.setScaleType(ImageView.ScaleType.FIT_CENTER);


    }

    private void loadUrl(String url, String filename){
        Log.d("download","getFirebaseDownloadURL:");
        Log.d("download",url);
        getEraFromFireBase(filename);
        onURLDownloaded();

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
        String date = Utils.getDateFromEra(era,getActivity());
        eraTextView.setText(era);
        Log.d("RCD",String.format("Era updated to {}",era));
        fbStore.updateImageDate(mFilename,date);
        fbStore.updateImageEra(mFilename,era);

        String backsideFilename = Utils.getBacksideString(mFilename);
        if(mHasBacksideText){
            fbStore.updateImageDate(backsideFilename,date);
            fbStore.updateImageEra(backsideFilename,era);
        }

        mActivity.setCurrentEra(era);

    }

    private void onDateSelected(String date){
        String era = Utils.getEraFromDate(date, mActivity);
        eraTextView.setText(era);
        fbStore.updateImageDate(mFilename,date);
        fbStore.updateImageEra(mFilename,era);

        String backsideFilename = Utils.getBacksideString(mFilename);
        if(mHasBacksideText){
            fbStore.updateImageDate(backsideFilename,date);
            fbStore.updateImageEra(backsideFilename,era);
        }
    }

    @Override
    public void onEraDownloaded(String era) {
        onEraReceivedFromFirebase(era);
    }

    @Override
    public void onDownloadUrlReceived(String url, String filename) {
        loadUrl(url, filename);
    }

    @Override
    public void onUploadComplete(String filename) {


    }

}
