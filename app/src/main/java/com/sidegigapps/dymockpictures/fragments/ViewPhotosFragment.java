package com.sidegigapps.dymockpictures.fragments;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.Spinner;

import com.sidegigapps.dymockpictures.MainActivity;
import com.sidegigapps.dymockpictures.PhotoGridViewAdapter;
import com.sidegigapps.dymockpictures.R;
import com.sidegigapps.dymockpictures.models.FirebaseStore;
import com.sidegigapps.dymockpictures.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;

public class ViewPhotosFragment extends Fragment implements
        FirebaseStore.UrlReceivedListener,
        FirebaseStore.UploadDownloadListener{

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    MainActivity mActivity;
    Spinner mSpinner;
    public GridView mGridview;
    PhotoGridViewAdapter mAdapter;

    FirebaseStore fbStore;

    private String[] eras;

    int viewPhotoBatchSize = 24;
    int numBatchesToShow = 0;
    private boolean areDownloadsComplete = true;
    private Button mButton;

    public ViewPhotosFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        eras = getActivity().getResources().getStringArray(R.array.eras_array);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_photo_gallery, container, false);

        mGridview = view.findViewById(R.id.photo_gridview);
        mAdapter = new PhotoGridViewAdapter(mActivity,new ArrayList<String>());
        mGridview.setAdapter(mAdapter);

        mGridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ClipboardManager clipboard = (ClipboardManager) mActivity.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("photo", (String) mAdapter.getItem(position));
                clipboard.setPrimaryClip(clip);

                onPhotoSelected(mAdapter.getFilename(position), (String)mAdapter.getItem(position));
            }
        });

        mButton = view.findViewById(R.id.buttonLoadMore);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadMorePhotos();
            }
        });

        mGridview.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (firstVisibleItem + visibleItemCount >= totalItemCount) {
                    mButton.setVisibility(View.VISIBLE);
                } else {
                    mButton.setVisibility(View.GONE);
                }

            }
        });

        mSpinner = view.findViewById(R.id.spinner2);
        ArrayAdapter<String> newAdapter = new ArrayAdapter<>(
                getActivity(), R.layout.spinner_item, Arrays.asList(eras));


        mSpinner.setAdapter(newAdapter);
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mActivity.setCurrentEra(eras[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        String currentEra = mActivity.getCurrentEra();
        mSpinner.setSelection(Utils.getEraArrayPositionFromString(currentEra, mActivity));

        mAdapter.clear();
        if(getArguments()!=null){
            ArrayList<String> filenames = getArguments().getStringArrayList("filenames");
            ArrayList<String> urls = getArguments().getStringArrayList("urls");
            if ((filenames.size() != urls.size())) throw new AssertionError();
            for (int i = 0; i < filenames.size(); i++){
                onUrlReceived(filenames.get(i),urls.get(i),mActivity.getCurrentEra());
            }
            resetBatchCount(filenames.size());


        } else {
            resetBatchCount(0);
            loadMorePhotos();
        }

        return view;
    }

    private void onPhotoSelected(String filename, String position) {
        mActivity.onPhotoSelected(filename, position, mAdapter.getFilenames(), mAdapter.getUrls());

    }

    private void loadMorePhotos() {
        int count = mAdapter.getCount();
        ArrayList<String> eraFilenames = fbStore.getFilenamesByEra(mActivity.getCurrentEra(), false);
        int maxImages = eraFilenames.size();
        Log.d("RCD", String.format("Currently showing %01d images, total in this era %01d", count, maxImages));
        if (count >= maxImages) {
            Snackbar.make(mActivity.findViewById(android.R.id.content),
                    "No more photos to load",
                    Snackbar.LENGTH_SHORT).show();
            return;
        }

        areDownloadsComplete = false;
        numBatchesToShow++;

        Log.d("RCD", "Requesting: " + String.valueOf(numBatchesToShow * viewPhotoBatchSize));

        int listSize = Math.min(maxImages, numBatchesToShow * viewPhotoBatchSize);

        Log.d("RCD", "Found: " + String.valueOf(listSize));
        ArrayList<String> result = new ArrayList<>(eraFilenames.subList(0, listSize));

        fbStore.loadFilenamesUrls(result, mActivity.getCurrentEra());
    }

    @Override
    public void onUrlReceived(String filename, String url, String era) {
        if(mActivity.getCurrentEra().equals(era)) {
            if (!filename.endsWith("_b")) {
                addImageURL(url, filename);
            }
        }
    }

    @Override
    public void onDownloadUrlReceived(String url, String filename) {

    }

    @Override
    public void onUploadComplete(String filename) {

    }

    @Override
    public void onAllUrlsReceived() {
        areDownloadsComplete = true;
    }

    @Override
    public void onEraSetupComplete() {
        if (mAdapter.getCount() == 0) {
            loadMorePhotos();
        }
    }

    public void addImageURL(String url, String filename) {
        Log.d("DYMOCK", url);
        mAdapter = (PhotoGridViewAdapter) mGridview.getAdapter();
        mAdapter.add(url);
        mAdapter.addFilename(filename);
        mAdapter.notifyDataSetChanged();
        Log.d("RCD","COUNTS:");
        Log.d("RCD",String.valueOf(mAdapter.getFilenames().size()));
        Log.d("RCD",String.valueOf(mAdapter.getUrls().size()));
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = (MainActivity) getActivity();

        fbStore = mActivity.getFbStore();
        fbStore.registerUrlListener(this, this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        fbStore.registerUrlListener(null, null);
    }

    public void resetBatchCount(int currentNumberShowing) {
        int result = currentNumberShowing / viewPhotoBatchSize;
        numBatchesToShow = result + 1;
    }

    public void onNewEraSelected(String era) {
        mAdapter.clear();
        mAdapter.notifyDataSetChanged();
        resetBatchCount(0);
        loadMorePhotos();


    }
}
