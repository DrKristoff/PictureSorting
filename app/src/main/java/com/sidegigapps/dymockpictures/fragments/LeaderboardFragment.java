package com.sidegigapps.dymockpictures.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.sidegigapps.dymockpictures.MainActivity;
import com.sidegigapps.dymockpictures.R;
import com.sidegigapps.dymockpictures.models.Leaderboard;
import com.sidegigapps.dymockpictures.models.UserData;

import java.util.HashMap;

public class LeaderboardFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private HashMap<String, Leaderboard> leaderboardsMap;


    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    Button rotationsButton, viewsButton, downloadsButton;
    Spinner spinner;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public LeaderboardFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment LeaderboardFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static LeaderboardFragment newInstance(String param1, String param2) {
        LeaderboardFragment fragment = new LeaderboardFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public void setLeaderboardData(HashMap<String, Leaderboard> leaderboard){
        this.leaderboardsMap = leaderboard;
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
        View view = inflater.inflate(R.layout.fragment_leaderboard, container, false);

        mRecyclerView = view.findViewById(R.id.recyclerView);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);

        //rotationsButton = view.findViewById(R.id.rotationsButton);
        //viewsButton = view.findViewById(R.id.viewsButton);
        //downloadsButton = view.findViewById(R.id.downloadsButton);

        spinner = view.findViewById(R.id.spinner);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                getActivity(),
                R.layout.spinner_item,
                ((MainActivity)getActivity()).leaderboardNames
        );

        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String leaderboard = spinner.getSelectedItem().toString();
                showLeaderboard(leaderboard);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });



        showLeaderboard(MainActivity.LEADERBOARD_VIEWS);

        /*rotationsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLeaderboard(MainActivity.LEADERBOARD_ROTATIONS);
            }
        });

        viewsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLeaderboard(MainActivity.LEADERBOARD_VIEWS);
            }
        });

        downloadsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLeaderboard(MainActivity.LEADERBOARD_DOWNLOADS);
            }
        });*/

        return view;
    }

    private void showLeaderboard(String leaderboard) {
        Leaderboard rotationsLeaderboard = leaderboardsMap.get(leaderboard);
        mAdapter = new LeaderboardAdapter(rotationsLeaderboard);
        mRecyclerView.setAdapter(mAdapter);

    }

    private class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {
        private Leaderboard mLeaderboard;

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        public class ViewHolder extends RecyclerView.ViewHolder {
            // each data item is just a string in this case
            public TextView mRankTextView;
            public TextView mNameTextView;
            public ImageView mProfileImageView;
            public TextView mScoreTextView;


            public ViewHolder(View v) {
                super(v);
                mNameTextView = v.findViewById(R.id.textViewName);
                mProfileImageView = v.findViewById(R.id.profileImageView);
                mScoreTextView = v.findViewById(R.id.scoreTextView);
                mRankTextView = v.findViewById(R.id.textViewRanking);
            }
        }

        public LeaderboardAdapter(Leaderboard leaderboard) {
            mLeaderboard = leaderboard;
        }

        @Override
        public LeaderboardAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                                int viewType) {

            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.leaderboard_row_item, parent, false);
            ViewHolder vh = new ViewHolder(v);
            return vh;
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            String uuid = mLeaderboard.getUUIDByLeaderboardPosition(position);
            UserData userData = ((MainActivity)getActivity()).getUserDataByUUID(uuid);

            holder.mNameTextView.setText(userData.getName());
            holder.mRankTextView.setText(String.valueOf(position+1));
            //holder.mProfileImageView.set;
            holder.mScoreTextView.setText(String.valueOf(mLeaderboard.getScoreByUUID(uuid)));
        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return mLeaderboard.getSize();
        }
    }

}
