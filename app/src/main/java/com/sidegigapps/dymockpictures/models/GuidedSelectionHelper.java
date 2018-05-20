package com.sidegigapps.dymockpictures.models;

import android.support.v7.widget.CardView;
import android.util.Log;

import java.util.HashMap;

public class GuidedSelectionHelper {

    public static final String OLDER = "older";
    public static final String NEWER = "newer";

    private String mFilename;
    private String[] mErasArray;
    private String[] mDatesArray;
    private HashMap<Integer,String> anchorsMap;
    private int lowIndex, highIndex, comparisonIndex;
    private boolean isFinished = false;

    public GuidedSelectionHelper(HashMap<Integer,String> map,
                                 String[]erasArray,
                                 String[]datesArray){
        this.anchorsMap = map;
        this.mErasArray = erasArray;
        this.mDatesArray = datesArray;
        this.lowIndex = 0;
        this.highIndex = anchorsMap.size();
        updateComparisonIndex();
    }

    public void setFilename(String filename){
        this.mFilename = filename;
    }

    public String getDate(){
        String prefix = "01-01-";
        int year = 1982 + comparisonIndex;
        return prefix + String.valueOf(year);
    }

    private void updateComparisonIndex() {
        comparisonIndex = (lowIndex+highIndex)/2;
    }

    public void makeComparison(String result){
        boolean finalComparison = false;

        if(highIndex-lowIndex <=3){
            finalComparison = true;
        }

        if(result.equals(NEWER)){
            Log.d("RCD","reported newer");
            lowIndex = comparisonIndex;
            updateComparisonIndex();

            Log.d("RCD","new high and low " + String.valueOf(lowIndex) + " " + String.valueOf(highIndex));
            Log.d("RCD","TARGET: " + String.valueOf(comparisonIndex));
        } else {
            Log.d("RCD","reported older");
            highIndex = comparisonIndex;
            updateComparisonIndex();
            Log.d("RCD","new high and low " + String.valueOf(lowIndex) + " " + String.valueOf(highIndex));
            Log.d("RCD","TARGET: " + String.valueOf(comparisonIndex));
        }

        if(finalComparison){
            isFinished = true;

        }

    }

    public void reset(){
        this.lowIndex = 0;
        this.highIndex = anchorsMap.size();
        isFinished = false;
        mFilename = null;
        updateComparisonIndex();

    }

    public String getComparisonFilename() {
        return anchorsMap.get(comparisonIndex);
    }

    public boolean isFinished() {
        return isFinished;
    }
}
