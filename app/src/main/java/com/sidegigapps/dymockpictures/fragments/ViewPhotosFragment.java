package com.sidegigapps.dymockpictures.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.Spinner;

import com.sidegigapps.dymockpictures.MainActivity;
import com.sidegigapps.dymockpictures.PhotoGridViewAdapter;
import com.sidegigapps.dymockpictures.R;
import com.sidegigapps.dymockpictures.models.FirebaseStore;

import java.util.ArrayList;
import java.util.Arrays;

public class ViewPhotosFragment extends Fragment implements
        FirebaseStore.UrlDownloadListener{

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    MainActivity mActivity;
    Spinner mSpinner;
    GridView mGridview;
    String mEra;

    FirebaseStore fbStore;

    private String [] eras;

    int viewPhotoBatchSize = 25;
    int numBatchesToShow = 0;

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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_photo_gallery, container, false);

        fbStore = ((MainActivity)getActivity()).getFbStore();

        SwipeRefreshLayout swipeLayout = view.findViewById(R.id.swipeLayout);
        swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadMorePhotos();
            }
        });

        eras = getActivity().getResources().getStringArray(R.array.eras_array);
        mEra = eras[0];

        mGridview = view.findViewById(R.id.photo_gridview);
        mGridview.setAdapter(new PhotoGridViewAdapter(getActivity(),new ArrayList<String>()));

        mSpinner = view.findViewById(R.id.spinner2);
        ArrayAdapter<String> newAdapter = new ArrayAdapter<>(
                getActivity(), R.layout.spinner_item, Arrays.asList(eras));

        mSpinner.setAdapter(newAdapter);
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mEra = eras[position];
                resetBatchCount();
                clearAdapter();
                loadMorePhotos();

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        resetBatchCount();
        loadMorePhotos();

        return view;
    }

    private void loadMorePhotos() {
        fbStore.registerUrlListener(this);
        ArrayList<String> eraFilenames = fbStore.getFilenamesByEra(mEra);
        int maxImages = eraFilenames.size();  //calling size on null list, that era arraylist hasn't been filled yet
        numBatchesToShow++;

        int listSize = Math.min(maxImages,numBatchesToShow*viewPhotoBatchSize);
        ArrayList<String> result = new ArrayList<>(eraFilenames.subList(0,listSize));

        fbStore.loadFilenamesUrls(result);
    }

    @Override
    public void onUrlDownloaded(String url) {
        addImageURL(url);
    }

    //define callback interface
    private interface UrlCallback {

        void onUrlDownloaded(String result);
    }

    public void addImageURL(String url){
        PhotoGridViewAdapter adapter = (PhotoGridViewAdapter) mGridview.getAdapter();
        adapter.add(url);
        adapter.notifyDataSetChanged();
    }

    public void clearAdapter(){
        PhotoGridViewAdapter adapter = (PhotoGridViewAdapter) mGridview.getAdapter();
        adapter.clear();
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = (MainActivity)getActivity();
    }


    public void resetBatchCount() {
        numBatchesToShow = 0;
    }
}
