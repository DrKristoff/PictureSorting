package com.sidegigapps.dymockpictures.fragments;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;

import com.sidegigapps.dymockpictures.R;

import java.util.List;

public class EraDialogFragment extends DialogFragment {

    String[] eras;

    public static final String SELECTED = "selected";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        eras = getResources().getStringArray(R.array.eras_array);

        AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());

        dialog.setTitle("Select Era");
        dialog.setPositiveButton("Cancel", new PositiveButtonClickListener());

        //List<String> list = (List<String>)bundle.get(DATA);
        //int position = bundle.getInt(SELECTED);
        int position =0;

        //CharSequence[] cs = list.toArray(new CharSequence[list.size()]);
        dialog.setSingleChoiceItems(eras, position, selectItemListener);

        return dialog.create();
    }

    class PositiveButtonClickListener implements DialogInterface.OnClickListener
    {
        @Override
        public void onClick(DialogInterface dialog, int which)
        {
            dialog.dismiss();
        }
    }

    DialogInterface.OnClickListener selectItemListener = new DialogInterface.OnClickListener()
    {

        @Override
        public void onClick(DialogInterface dialog, int which)
        {
            // process
            //which means position
            dialog.dismiss();
        }

    };
}
