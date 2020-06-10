package xyz.beskh.yaen;

//import android.app.Dialog;
//import android.app.DialogFragment;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class DialogAskFilenameAndPassword extends DialogFragment {
    private static final String TAG = "DialogAskFnAndPwd";
    AppState appState;
    Uri mUriPath;

    public interface DialogAskFilenameAndPasswordListener {
        public void onDialogAskFilenameAndPasswordPositiveClick(DialogFragment dialog);
        public void onDialogAskFilenameAndPasswordNegativeClick(DialogFragment dialog);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log.v(TAG,"onCreateDialog was called");
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(inflater.inflate(R.layout.dialog_ask_filename_and_passwd, null));
                // Add action buttons
        builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // Send the positive button event back to the host activity
                        mListener.onDialogAskFilenameAndPasswordPositiveClick(DialogAskFilenameAndPassword.this);
                    }
                });
        builder.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //LoginDialogFragment.this.getDialog().cancel();
                        // Send the negative button event back to the host activity
                        mListener.onDialogAskFilenameAndPasswordNegativeClick(DialogAskFilenameAndPassword.this);
                    }
                });
        Log.v(TAG, "onCreateDialog done");
        return builder.create();
    }

    @Override
    public void onStart() {
        Log.v(TAG, "onStart was called");
        super.onStart();
        // public EditText mEditText;
        TextView tvTitle = (TextView) getDialog().findViewById(R.id.dafap_title);
        tvTitle.setText(getString(R.string.directory_is, mUriPath.getEncodedPath()));
        Log.v(TAG, "onStart appState=" + appState);
    }

    // Use this instance of the interface to deliver action events
    DialogAskFilenameAndPasswordListener mListener;

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (DialogAskFilenameAndPasswordListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement DialogAskFilenameAndPasswordListener");
        }
    }

}
