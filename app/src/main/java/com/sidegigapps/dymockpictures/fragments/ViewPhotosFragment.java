package com.sidegigapps.dymockpictures.fragments;

import android.content.Context;
import android.net.Uri;
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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

public class ViewPhotosFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    MainActivity mActivity;
    Spinner mSpinner;
    GridView mGridview;
    String mEra;

    private String [] eras;

    public static ArrayList<String> eatFoodyImages = new ArrayList<>();
    private ArrayList<String> imageURLs = new ArrayList<>();

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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_photo_gallery, container, false);

        SwipeRefreshLayout swipeLayout = view.findViewById(R.id.swipeLayout);
        swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadMorePhotos();
            }
        });

        eatFoodyImages.add("http://i.imgur.com/rFLNqWI.jpg");
        eatFoodyImages.add("http://i.imgur.com/C9pBVt7.jpg");
        eatFoodyImages.add("http://i.imgur.com/aIy5R2k.jpg");
        eatFoodyImages.add("http://i.imgur.com/MoJs9pT.jpg");
        eatFoodyImages.add("http://i.imgur.com/S963yEM.jpg");
        eatFoodyImages.add("http://i.imgur.com/rLR2cyc.jpg");
        eatFoodyImages.add("http://i.imgur.com/SEPdUIx.jpg");
        eatFoodyImages.add("http://i.imgur.com/aC9OjaM.jpg");
        eatFoodyImages.add("http://i.imgur.com/76Jfv9b.jpg");
        eatFoodyImages.add("http://i.imgur.com/fUX7EIB.jpg");
        eatFoodyImages.add("http://i.imgur.com/syELajx.jpg");
        eatFoodyImages.add("http://i.imgur.com/COzBnru.jpg");
        eatFoodyImages.add("http://i.imgur.com/Z3QjilA.jpg");

        //eras = ((MainActivity)getActivity()).getEraNames();
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
                mActivity.setCurrentEra(mEra);
                clearAdapter();
                loadMorePhotos();

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        mActivity.setCurrentEra(mEra);
        loadMorePhotos();

        return view;
    }

    private void loadMorePhotos() {
        mActivity.loadMoreImages();
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

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
