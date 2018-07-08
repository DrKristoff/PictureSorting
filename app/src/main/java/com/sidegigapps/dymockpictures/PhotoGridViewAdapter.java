package com.sidegigapps.dymockpictures;

import android.content.Context;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import java.util.ArrayList;


public class PhotoGridViewAdapter extends ArrayAdapter {

    private Context context;
    private LayoutInflater inflater;

    public ArrayList<String> imageUrls;

    public PhotoGridViewAdapter(Context context, ArrayList<String> imageUrls) {
        super(context, R.layout.photo_grid_imageview, imageUrls);

        this.context = context;
        this.imageUrls = imageUrls;

        inflater = LayoutInflater.from(context);
    }

    @Override
    public void add(@Nullable Object object) {
        super.add(object);
    }

    @Override
    public void clear() {
        super.clear();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (null == convertView) {
            convertView = inflater.inflate(R.layout.photo_grid_imageview, parent, false);
        }

        GlideApp
                .with(context)
                .load(getItem(position))
                .placeholder(R.drawable.progress_animation)
                .into((ImageView) convertView);

        return convertView;
    }
}
