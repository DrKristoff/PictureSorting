package com.sidegigapps.dymockpictures.fragments;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.jsibbold.zoomage.ZoomageView;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.sidegigapps.dymockpictures.GlideApp;
import com.sidegigapps.dymockpictures.MainActivity;
import com.sidegigapps.dymockpictures.R;
import com.sidegigapps.dymockpictures.utils.RotateTransformation;
import com.sidegigapps.dymockpictures.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class ViewPhotosFragment extends Fragment implements EraDialogFragment.OnEraSelectionListener {

    private static final int REQUEST_CODE = 1;
    private StorageReference mStorageRef;
    private HashMap<String, String> downloadLinksMap = new HashMap<>();
    private String targetFilenameURL;
    private ZoomageView targetImage;
    private float targetImageRotation = 0f;
    private String targetFilename;
    private FloatingActionButton fab_rotate, fab_new, fab_save;
    private com.github.clans.fab.FloatingActionButton fab_exact, fab_era, fab_guided;
    private ProgressBar progressBar;
    private TextView dateTextView;

    private boolean isLoading = true;
    private boolean downloadRequested = false;

    LinkedList<String> queuedFileNames = new LinkedList<>();

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    public ViewPhotosFragment() {
        // Required empty public constructor
    }

    public static ViewPhotosFragment newInstance(String param1, String param2) {
        ViewPhotosFragment fragment = new ViewPhotosFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        mStorageRef = FirebaseStorage.getInstance().getReference();
    }

    public void onURLDownloaded(){
        Log.d("RCD","onURLDownloaded");

        //targetImage.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.INVISIBLE);
        displayImage();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_view_photos, container, false);

        targetImage = view.findViewById(R.id.targetImage);
        progressBar = view.findViewById(R.id.indeterminateBar);

        dateTextView = view.findViewById(R.id.dateTextView);

        fab_new = view.findViewById(R.id.fab_new);
        fab_rotate = view.findViewById(R.id.fab_rotate);
        fab_save = view.findViewById(R.id.fab_save);

        fab_exact = view.findViewById(R.id.exact_sort_fab);
        fab_era = view.findViewById(R.id.era_sort_fab);
        fab_guided = view.findViewById(R.id.guided_sort_fab);


        setupFABs();
        onNewImageRequested();

        return view;

    }


    private void setupFABs() {

        View.OnClickListener fab_listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int id = v.getId();
                switch (id) {
                    case R.id.fab_new:
                        onNewImageRequested();
                        break;
                    case R.id.fab_save:
                        onSaveFABPressed();
                        break;
                    case R.id.fab_rotate:
                        onRotateFabPressed();
                        break;
                    case R.id.exact_sort_fab:
                        onExactSortPressed();
                        break;
                    case R.id.era_sort_fab:
                        onEraSortPressed();
                        break;
                    case R.id.guided_sort_fab:
                        onGuidedSortPressed();
                        break;
                }

            }
        };

        fab_new.setOnClickListener(fab_listener);
        fab_rotate.setOnClickListener(fab_listener);
        fab_save.setOnClickListener(fab_listener);


        fab_exact.setOnClickListener(fab_listener);
        fab_era.setOnClickListener(fab_listener);
        fab_guided.setOnClickListener(fab_listener);

    }

    private void onGuidedSortPressed(){
        Toast.makeText(getActivity(),"Coming Soon",Toast.LENGTH_SHORT).show();

    }

    private void onEraSortPressed() {
        EraDialogFragment dialog = new EraDialogFragment();
        dialog.show(getFragmentManager(),"EraDialogFragment");

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

                        //txtDate.setText(dayOfMonth + "-" + (monthOfYear + 1) + "-" + year);

                    }
                }, year, month, day);
        datePickerDialog.show();
    }

    private void getNextDownloadLink(){
        Random random = new Random();

        List<String> filenames = ((MainActivity)getActivity()).filenames;

        int index = random.nextInt(filenames.size());
        String filename = filenames.get(index);
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
        ((MainActivity)getActivity()).incrementViews();
    }

    private void incrementRotations() {
        ((MainActivity)getActivity()).incrementRotations();
    }

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private void downloadTargetImage(final String filename, final String url) {
        if (getActivity().checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Log.v("RCD", "Permission is granted");
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

            DownloadManager dm = (DownloadManager) getActivity().getSystemService(getActivity().DOWNLOAD_SERVICE);
            dm.enqueue(request);

        } else {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE);
        }

    }

    public void onNewImageRequested() {
        if (targetImageRotation != 0f) {
            //implies that the previous image was rotated.
            uploadBitmapToFirebase(targetFilename,targetFilenameURL,targetImageRotation);
            incrementRotations();
        }
        targetImageRotation = 0f;
        targetImage.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.VISIBLE);
/*        GlideApp.with(getActivity().getApplicationContext())
                .load(R.drawable.progress_animation)
                .into(targetImage);*/

        getNextDownloadLink();
    }

    public void onSaveFABPressed() {
        downloadRequested = true;
        uploadBitmapToFirebase(targetFilename,targetFilenameURL,targetImageRotation);
        Toast.makeText(getActivity(), "Saving Image", Toast.LENGTH_SHORT).show();
        ((MainActivity)getActivity()).incrementDownloads();
    }

    public void onRotateFabPressed() {
        targetImageRotation = targetImageRotation - 90;
        if (targetImageRotation < 0)
            targetImageRotation = targetImageRotation + 360;
        rotateTargetImage();
        Toast.makeText(getActivity(), "Rotated Left", Toast.LENGTH_SHORT).show();

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

                StorageReference reference = mStorageRef.child(filename);
                UploadTask uploadTask = reference.putBytes(data);
                uploadTask.addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        Toast.makeText(getActivity(), "Uh oh.  There was a problem.", Toast.LENGTH_SHORT).show();
                    }
                }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Toast.makeText(getActivity(), "Uploaded Successfully", Toast.LENGTH_SHORT).show();  //TODO: remove this after testing
                        if(downloadRequested){
                            downloadTargetImage(filename,url);
                            downloadRequested = false;
                        }
                    }
                });
            }
        });


    }

    private void getFirebaseDownloadURL(final String filename) {
        mStorageRef.child(filename).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                targetFilenameURL = uri.toString();
                targetFilename = filename;
                onURLDownloaded();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.d("RCD", "FAILED");
                Log.d("RCD", exception.getMessage());
            }
        });

        mStorageRef.child(filename).getMetadata().addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
            @Override
            public void onSuccess(StorageMetadata storageMetadata) {
                // Metadata now contains the metadata for 'images/forest.jpg'
                Log.d("RCD","success");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Uh-oh, an error occurred!
            }
        });
    }

    @Override
    public void onFinishEditDialog(int selection) {
        String [] eras = getActivity().getResources().getStringArray(R.array.eras_array);
        onEraSelected(eras[selection]);
    }

    private void onEraSelected(String era) {
        dateTextView.setText(era);
        ((MainActivity)getActivity()).updateImageEra(targetFilename,era);

    }

    private void onDateSelected(String date) {
        dateTextView.setText(date);
        ((MainActivity)getActivity()).updateImageDate(targetFilename,date);
        String era = Utils.getEraFromDate(date, getActivity());
        ((MainActivity)getActivity()).updateImageEra(targetFilename,era);

    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }
}
