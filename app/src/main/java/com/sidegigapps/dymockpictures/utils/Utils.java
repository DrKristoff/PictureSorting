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
import java.util.Date;

public class Utils {

    public static String getBacksideSuffix(String filename){

        return null;
    }

    public static Bitmap drawableToBitmap (Drawable drawable) {
        Bitmap bitmap = null;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if(bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public static String getEraFromDate(String date, Context context) {
        //"mm/dd/yyyy" > "Era Name"

        String [] era_dates = context.getResources().getStringArray(R.array.era_dates_array);
        String [] era_strings = context.getResources().getStringArray(R.array.eras_array);
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-YYYY");

        Date d;

        int chosen_era = 0;

        try{
            d = sdf.parse(date);
            for(int i = 0; i<era_dates.length;i++){
                Date e = sdf.parse(era_dates[i]);
                if(d.compareTo(e)>0){
                    chosen_era = i;
                }
            }

        } catch (ParseException e){
            Log.d("RCD",e.getMessage());
        }

        return era_strings[chosen_era];
    }

    public static String getDateFromEra(String era, Context context){
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
