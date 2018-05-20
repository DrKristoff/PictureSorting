package com.sidegigapps.dymockpictures.fragments;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import com.sidegigapps.dymockpictures.R;

public class EraDialogFragment extends DialogFragment {

    String[] eras;
    int selection = 0;

    private EraDialogListener listener;

    public interface EraDialogListener {
        void onEraSelected(DialogFragment dialog);
    }

    public interface OnItemSelectedListener {
        void onItemSelected(EraDialogFragment fragment, int index);
    }

    public static final String SELECTED = "selected";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        eras = getResources().getStringArray(R.array.eras_array);

        AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());

        dialog.setTitle("Select Era");
        dialog.setPositiveButton("Cancel", new CancelClickListener());

        int position =0;

        dialog.setSingleChoiceItems(eras, position, selectItemListener);

        return dialog.create();
    }

    class CancelClickListener implements DialogInterface.OnClickListener
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
            selection = which;
            //onEraSelected(which);
            dialog.dismiss();
        }

    };

    public interface OnEraSelectionListener{
        void onFinishEditDialog(int selection);
    }
}
