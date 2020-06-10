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
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class DialogAskPassword extends DialogFragment {
    private String LOG_TAG = "DialogAskPassword_" + this.hashCode();
    AppState appState;
    Uri uri;
    EditText etPassword;
    // Use this instance of the interface to deliver action events
    private DialogAskPasswordListener mListener;

    public interface DialogAskPasswordListener {
        public void onDialogAskPasswordPositiveClick(DialogFragment dialog);
        public void onDialogAskPasswordNegativeClick(DialogFragment dialog);
    }

    String fileName = "";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log.v(LOG_TAG,"onCreateDialog was called");
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(inflater.inflate(R.layout.dialog_ask_passwd, null));
                // Add action buttons
        builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // Send the positive button event back to the host activity
                        mListener.onDialogAskPasswordPositiveClick(DialogAskPassword.this);
                    }
                });
        builder.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //LoginDialogFragment.this.getDialog().cancel();
                        // Send the negative button event back to the host activity
                        mListener.onDialogAskPasswordNegativeClick(DialogAskPassword.this);
                    }
                });
        Log.v(LOG_TAG, "onCreateDialog done");
        return builder.create();
    }

    @Override
    public void onStart() {
        Log.v(LOG_TAG, "onStart was called");
        super.onStart();
        // public EditText mEditText;
        etPassword = getDialog().findViewById(R.id.dap_pwd);
        TextView tvFileName = getDialog().findViewById(R.id.dap_title);
        tvFileName.setText(getString(R.string.password_for_file, fileName));
    }

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(@NonNull Activity activity) {
        super.onAttach(activity);
        Log.v(LOG_TAG, "onAttach activity=" + activity);

        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (DialogAskPasswordListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement DialogAskPasswordListener");
        }
    }

}
