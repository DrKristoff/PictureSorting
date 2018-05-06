package com.sidegigapps.dymockpictures.fragments;

import android.Manifest;
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
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
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
import com.sidegigapps.dymockpictures.utils.RotateTransformation;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class ViewPhotosFragment extends Fragment {

    private static final int REQUEST_CODE = 1;
    private StorageReference mStorageRef;
    private HashMap<String, String> downloadLinksMap = new HashMap<>();
    private List<String> filenames = new ArrayList<>();
    private String targetFilenameURL;
    private ZoomageView targetImage;
    private float targetImageRotation = 0f;
    private String targetFilename;
    private FloatingActionButton fab_rotate, fab_new, fab_save;
    private ProgressBar progressBar;

    private boolean isLoading = true;

    HashMap<String,String> urlMap = new HashMap<>();
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

    private void loadFileNames() {
        Log.d("RCD","loadFileNames");
        String json = null;
        InputStream is = null;
        BufferedReader reader;
        try {
            is = getActivity().getAssets().open("filenames.txt");
            reader = new BufferedReader(new InputStreamReader(is));
            String filename = reader.readLine();
            while(filename != null){
                filenames.add(filename);
                filename = reader.readLine();
            }
            is.close();
            onNewImageRequested();
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        } finally {
            try {
                if (is != null)
                    is.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

        }
    }

    public void onURLsDownloaded(){
        Log.d("RCD","onURLsDownloaded");

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

        fab_new = view.findViewById(R.id.fab_new);
        fab_rotate = view.findViewById(R.id.fab_rotate);
        fab_save = view.findViewById(R.id.fab_save);

        setupFABs();

        loadFileNames();

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
                }

            }
        };

        fab_new.setOnClickListener(fab_listener);
        fab_rotate.setOnClickListener(fab_listener);
        fab_save.setOnClickListener(fab_listener);

    }

    private void getNextDownloadLink(){
        if(urlMap.size()>1) return;
        Random random = new Random();

        int index = random.nextInt(filenames.size());
        String filename = filenames.get(index);
        getFirebaseDownloadURL(filename);
    }

    private void displayImage() {
        Log.d("RCD","displayImage");

        //if (filenames.size() < 1) return;

        if(targetFilenameURL==null){
            Log.d("RCD","targetFilenameURL was null");
            //getNextDownloadLink();
            return;
        }

        Log.d("RCD", targetFilenameURL);

        GlideApp.with(getActivity().getApplicationContext())
                .load(targetFilenameURL)
                .placeholder(R.drawable.progress_animation)
                .into(targetImage);

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

    private void downloadTargetImage() {
        if (getActivity().checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Log.v("RCD", "Permission is granted");
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadLinksMap.get(targetFilename)));
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, targetFilename);
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
        GlideApp.with(getActivity().getApplicationContext())
                .load(R.drawable.progress_animation)
                .into(targetImage);

        getNextDownloadLink();
    }

    public void onSaveFABPressed() {
        downloadTargetImage();
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
                    }
                });
            }
        });


    }

    private void getFirebaseDownloadURL(final String filename) {
        mStorageRef.child(filename).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                //urlMap.put(filename,uri.toString());
                targetFilenameURL = uri.toString();
                targetFilename = filename;
                onURLsDownloaded();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.d("RCD", "FAILED");
                Log.d("RCD", exception.getMessage());
            }
        });
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }
}
