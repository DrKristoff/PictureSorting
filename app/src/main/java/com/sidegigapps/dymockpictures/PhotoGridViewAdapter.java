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

    private ArrayList<String> filenames = new ArrayList<>();
    private ArrayList<String> urls = new ArrayList<>();

    public PhotoGridViewAdapter(Context context, ArrayList<String> imageUrls) {
        super(context, R.layout.photo_grid_imageview, imageUrls);

        this.context = context;

        inflater = LayoutInflater.from(context);
    }

    public String getFilename(int position){
        return filenames.get(position);
    }

    public void addFilename(String filename){
        filenames.add(filename);
    }

    public ArrayList<String> getFilenames() {
        return filenames;
    }

    public ArrayList<String> getUrls() {
        return urls;
    }
    @Override
    public void add(@Nullable Object object) {
        super.add(object);
        urls.add((String)object);
    }

    @Override
    public void clear() {
        super.clear();
        filenames.clear();
        urls.clear();
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
