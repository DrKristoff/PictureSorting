package com.sidegigapps.dymockpictures.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.sidegigapps.dymockpictures.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class Utils {

    public static int convertDateToInt(String date) {
        Log.d("RCD", "attempting conversion of " + date);
        String[] dateParts = date.split("-");
        return Integer.parseInt(dateParts[2]) * 10000 +
                Integer.parseInt(dateParts[0]) * 100 +
                Integer.parseInt(dateParts[1]);
    }

    public static String getEraFromDate(String date, Context context) {
        //"mm/dd/yyyy" > "Era Name"

        int numericalDate = Utils.convertDateToInt(date);

        Log.d("RCD", "Finding ERA from " + date);
        String[] era_dates = context.getResources().getStringArray(R.array.era_dates_array);
        String[] era_strings = context.getResources().getStringArray(R.array.eras_array);

        int chosen_era_index = 0;

        for (int i = 0; i < era_dates.length; i++) {
            int compareDate = Utils.convertDateToInt(era_dates[i]);
            if (compareDate < numericalDate) {
                chosen_era_index = i;
            }
        }

        Log.d("RCD", "FOUND " + era_strings[chosen_era_index]);
        return era_strings[chosen_era_index];
    }

    public static String getBacksideString(String filename) {
        return filename.substring(0, filename.length() - 4) + "_b.jpg";
    }

    public static String getFrontsideString(String filename) {
        return filename.substring(0, filename.length() - 6) + ".jpg";
    }

    public static String removeFileType(String filename) {
        return filename.substring(0, filename.length() - 4);
    }

    public static String addFileTypeIfMissing(String filename) {
        if(!filename.endsWith(".jpg")) {
            return filename + ".jpg";
        } else {
            return filename;
        }

    }

    public static String getDateFromEra(String era, Context context) {
        String[] era_dates = context.getResources().getStringArray(R.array.era_dates_array);
        String[] era_strings = context.getResources().getStringArray(R.array.eras_array);

        for (int i = 0; i < era_dates.length; i++) {
            if (era.equals(era_strings[i])) {
                return era_dates[i];
            }
        }

        return era_dates[0];
    }

    public static int getEraArrayPositionFromString(String era, Context context) {
        String[] era_strings = context.getResources().getStringArray(R.array.eras_array);
        return Arrays.asList(era_strings).indexOf(era);
    }
}
