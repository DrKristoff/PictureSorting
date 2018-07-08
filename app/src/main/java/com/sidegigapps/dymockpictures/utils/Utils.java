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

    public static String getBacksideString(String filename){
        return filename.substring(0,filename.length()-4) + "_b.jpg";
    }

    public static String getFrontsideString(String filename){
        return filename.substring(0,filename.length()-6) + ".jpg";
    }
}
