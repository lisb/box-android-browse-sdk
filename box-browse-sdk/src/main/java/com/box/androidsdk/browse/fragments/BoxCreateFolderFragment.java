package com.box.androidsdk.browse.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.box.androidsdk.browse.R;
import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.utils.SdkUtils;

/**
 * A dialog fragment shown when user creates a new box folder
 */
public class BoxCreateFolderFragment extends DialogFragment implements DialogInterface.OnClickListener {

    public static String ARGS_PARENT_FOLDER_ID = "argsParentFolderId";

    protected String mFolderId;

    protected EditText mNameText;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

    }

    @Override
    public void onStart() {
        super.onStart();
        AlertDialog dialog = (AlertDialog) getDialog();
        final Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        positiveButton.setEnabled(false);

        mNameText = (EditText) dialog.findViewById(R.id.box_browsesdk_new_folder_name);
        mNameText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                boolean isDisabled = SdkUtils.isBlank(s.toString());
                positiveButton.setEnabled(!isDisabled);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity activity = getActivity();
        Bundle b = getArguments();
        mFolderId = b.getString(ARGS_PARENT_FOLDER_ID, null);
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
        View view = getActivity().getLayoutInflater().inflate(R.layout.box_browsesdk_fragment_create_folder, null);
        dialogBuilder.setView(view)
                .setPositiveButton(R.string.box_browsesdk_dialog_ok, this)
                .setNegativeButton(R.string.box_browsesdk_dialog_cancel, this);

        return dialogBuilder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        OnCreateFolderListener activity = (OnCreateFolderListener) getActivity();
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                activity.onCreateFolder(mNameText.getText().toString());
                break;
        }
    }

    /**
     * New instance box create folder fragment.
     *
     * @param folder  the folder
     * @return the box create folder fragment
     */
    public static BoxCreateFolderFragment newInstance(BoxFolder folder) {
        if (folder == null || SdkUtils.isBlank(folder.getId()))
            throw new IllegalArgumentException("A valid folder must be provided to browse");
        BoxCreateFolderFragment createFolderDialog = new BoxCreateFolderFragment();
        Bundle b = new Bundle();
        b.putString(ARGS_PARENT_FOLDER_ID, folder.getId());
        createFolderDialog.setArguments(b);
        return createFolderDialog;
    }

    /**
     * The interface On create folder listener.
     */
    public interface OnCreateFolderListener {
        /**
         * On create folder.
         *
         * @param name the name
         */
        void onCreateFolder(String name);
    }
}
