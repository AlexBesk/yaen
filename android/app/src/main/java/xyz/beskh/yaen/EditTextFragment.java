package xyz.beskh.yaen;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import enotes.doc.Doc;
import enotes.doc.DocException;
import enotes.doc.DocMetadata;

public class EditTextFragment extends Fragment {
    private static final String SAVE_UNDO_PREFIX = "undo_redo";
    private EditText editText;
    private final String LOG_TAG = "EditTextFragment_" + this.hashCode();
    private EditTextInteraction editTextInteraction;
    private MyViewModel viewModel;

    DocMetadata doc_metadata;
    TextViewUndoRedo textViewUndoRedo;
    private String pswd = "";
    private static final String fileExt = ".etxt";
    private SharedPreferences sharedPreferences;
    private TextWatcher textWatcher;
    private Uri tmp_uri;
    private String tmp_pswd;


    public interface EditTextInteraction {
        Uri getFileToOpenUri(boolean purge);
        void updateTitle(DocMetadata docMetadata);
        void onDialogAskPasswordPositiveClick(DialogFragment dialog);
        void onDialogAskPasswordNegativeClick(DialogFragment dialog);
        void onDialogAskFilenameAndPasswordPositiveClick(DialogFragment dialog);
        void onDialogAskFilenameAndPasswordNegativeClick(DialogFragment dialog);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (container != null)
            Log.d(LOG_TAG, "onCreateView container.hashCode()=" + container.hashCode());

        View view = inflater.inflate(R.layout.edit_text, container, false);

        editText = view.findViewById(R.id.main_text);

        textWatcher = new TextWatcher() {
            public void afterTextChanged(Editable s) {
                Log.d(LOG_TAG,"afterTextChanged was called");
                if (!doc_metadata.modified) {
                    Log.d(LOG_TAG,"afterTextChanged switch doc_metadata.modified from false to true");
                    doc_metadata.modified = true;
                }
                editTextInteraction.updateTitle(doc_metadata);
            }
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
            }
        };

        textViewUndoRedo = new TextViewUndoRedo(editText);
        textViewUndoRedo.disconnect();

        if (savedInstanceState == null){
            doc_metadata = new DocMetadata();
        }
        else {
            //restore prev state
            if (viewModel.pref_paranoid) {
                doc_metadata = new DocMetadata();
            } else {
                doc_metadata = (DocMetadata) savedInstanceState.getSerializable("metadata");
                Log.d(LOG_TAG, "onCreateView doc_metadata=" + doc_metadata);

                pswd = savedInstanceState.getString("pswd");

                // UndoRedo
                Activity a = getActivity();
                if (a == null)
                    Log.d(LOG_TAG, "onCreateView cant find context. Cant load Undo/Redo state");
                else {
                    editText.setText(savedInstanceState.getString("text"));
                    textViewUndoRedo.restorePersistentState(sharedPreferences, SAVE_UNDO_PREFIX);
                    Log.d(LOG_TAG, "onCreateView textViewUndoRedo restored");
                    Log.d(LOG_TAG, "onCreateView textViewUndoRedo.getCanUndo()=" + textViewUndoRedo.getCanUndo());

                }
            }
            editTextInteraction.updateTitle(doc_metadata);
        }

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        Log.d(LOG_TAG, "onAttach activity.hashCode()=" + activity.hashCode());
        super.onAttach(activity);

        sharedPreferences = activity.getPreferences(Context.MODE_PRIVATE);

        try {
            editTextInteraction = (EditTextInteraction) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement EditTextInteraction");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(LOG_TAG, "onCreate");
        super.onCreate(savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(MyViewModel.class);
        Log.d(LOG_TAG, "onCreate viewModel=" + viewModel);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(LOG_TAG, "onActivityCreated");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(LOG_TAG, "onResume");
        textViewUndoRedo.connect();
        editText.addTextChangedListener(textWatcher);
        //editTextInteraction.updateTitle(doc_metadata);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(LOG_TAG, "onPause");
        textViewUndoRedo.disconnect();
        editText.removeTextChangedListener(textWatcher);
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(LOG_TAG, "onStop");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(LOG_TAG, "onDestroyView");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG, "onDestroy");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(LOG_TAG, "onDetach");
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        Log.d(LOG_TAG, "onSaveInstanceState");
        super.onSaveInstanceState(outState);

        if (viewModel.pref_paranoid) {
            Log.d(LOG_TAG, "onSaveInstanceState paranoid mode detected !!!");
            resetFile();
        }
        else {
            doc_metadata.caretPosition = editText.getSelectionStart();
            outState.putString("text", editText.getText().toString());
            outState.putString("pswd", pswd);
            outState.putSerializable("metadata", doc_metadata);

            // UndoRedo
            Activity a = getActivity();
            if (a == null)
                Log.d(LOG_TAG, "onSaveInstanceState cant find context. Cant save Undo/Redo state");
            else {
                SharedPreferences.Editor ed = sharedPreferences.edit();
                textViewUndoRedo.storePersistentState(ed, SAVE_UNDO_PREFIX);
                ed.apply();
                Log.d(LOG_TAG, "onSaveInstanceState textViewUndoRedo.storePersistentState getCanUndo()=" + textViewUndoRedo.getCanUndo());

            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(LOG_TAG, "onStart");
        Uri uri = editTextInteraction.getFileToOpenUri(true);
        Log.d(LOG_TAG, "onStart getFileToOpenUri=" + uri);
        if (uri != null)
            // open file on startup
            askPassword(AppState.appFileOpen, uri);
    }

/*    public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        Log.d(LOG_TAG, "onRestoreInstanceState");
    }*/

    private void setUris(Uri uri){
        if (uri == null) {
            viewModel.uriFull = null;
            doc_metadata.filename = null;
            Log.d(LOG_TAG, "setUris uriFull=null");
            Log.d(LOG_TAG, "setUris fileName=null");
        }
        else {
            viewModel.uriFull = uri;
            Log.d(LOG_TAG, "setUris uriFull=" + viewModel.uriFull.toString());
            try {
                doc_metadata.filename = FileInfo.getFileName(viewModel.uriFull, getActivity().getContentResolver());
            } catch (Exception e) {
                doc_metadata.filename = null;
                e.printStackTrace();
                Log.e(LOG_TAG, e.toString());
            }
            Log.d(LOG_TAG, "setUris fileName=" + doc_metadata.filename);
        }
    }

    void menuNewFile() {
        Log.d(LOG_TAG,"menuNewFile was called");

        saveIfAutosave();
        if (needToSave()) {
            if (doc_metadata.filename == null)
                saveToNewFileAndResetCurrent();
            else
                saveToOldFileAndResetCurrent(doc_metadata.filename);
        }
        else
            resetFile();
    }

    void menuOpenFile() {
        Log.d(LOG_TAG,"menuOpenFile was called");
        saveIfAutosave();
        if (needToSave()) {
            if (doc_metadata.filename == null){
                saveToNewFileAndOpenAnother();
            }
            else {
                saveToOldFileAndOpenAnother(doc_metadata.filename);
            }
        }
        else
            // ask for filename
            askSelectFile(AppState.appFileOpen);
        Log.d(LOG_TAG,"menuOpenFile done");
    }

    void menuSaveFile(){
        Log.d(LOG_TAG,"menuSaveFile was called");
        if (doc_metadata.filename != null) {
            saveEditText(true, viewModel.uriFull);
        } else {
            askDirectory(AppState.appFileSave);
        }
    }

    void menuSaveAsFile(){
        Log.d(LOG_TAG,"menuSaveAsFile was called");
        askDirectory(AppState.appFileSave);
    }


    void menuUndo() {
        Log.d(LOG_TAG,"menuUndo was called");
        if (textViewUndoRedo.getCanUndo())
            textViewUndoRedo.undo();
    }

    void menuRedo() {
        Log.d(LOG_TAG,"menuRedo was called");
        if (textViewUndoRedo.getCanRedo())
            textViewUndoRedo.redo();
    }

    public void resetFile(){
        Log.d(LOG_TAG,"resetFile was called");
        setUris(null);
        pswd = "";
        editText.setText("");
        textViewUndoRedo.clearHistory();
        doc_metadata.caretPosition = 0;
        doc_metadata.modified = false;
        editTextInteraction.updateTitle(doc_metadata);
        Log.d(LOG_TAG,"resetFile done");
    }

    private void askForSave(String fileName,
                            DialogInterface.OnClickListener onYesClickListener,
                            DialogInterface.OnClickListener onNoClickListener,
                            DialogInterface.OnClickListener onCancelClickListener){
        Log.d(LOG_TAG,"askForSave was called. fileName=" + fileName);
        new AlertDialog.Builder(getActivity())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.savefile_title)
                .setMessage(getResources().getString(R.string.savefile_message, fileName))
                .setPositiveButton(R.string.Yes, onYesClickListener)
                .setNegativeButton(R.string.No, onNoClickListener)
                .setNeutralButton(R.string.Cancel, onCancelClickListener)
                .show();
        Log.d(LOG_TAG,"askForSave done");
    }

    private void saveToNewFileAndFinish(){
        Log.d(LOG_TAG,"saveToNewFileAndExit was called");
        askForSave("",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(LOG_TAG,"saveToNewFileAndExit Yes");
                        askDirectory(AppState.appFinish);
                    }
                },
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(LOG_TAG,"saveToNewFileAndExit No");
                        getActivity().finish();
                    }
                },
                null
        );
        Log.d(LOG_TAG,"saveToNewFileAndExit done");
    }

    private void saveToOldFileAndFinish(String fileName){
        Log.d(LOG_TAG,"saveToOldFileAndFinish was called");
        askForSave(fileName,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(LOG_TAG,"saveToOldFileAndFinish Yes");
                        saveEditText(true, viewModel.uriFull);
                        getActivity().finish();;
                    }
                },
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(LOG_TAG,"saveToOldFileAndFinish No");
                        getActivity().finish();
                    }
                },
                null
        );
        Log.d(LOG_TAG,"saveToOldFileAndFinish done");
    }

    private void saveToNewFileAndOpenUri(final Uri uri){
        Log.d(LOG_TAG,"saveToNewFileAndOpenUri was called");
        askForSave("",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(LOG_TAG,"saveToNewFileAndOpenAnother Yes");
                        askDirectory(AppState.appFileSaveAndOpen);
                    }
                },
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(LOG_TAG,"saveToNewFileAndOpenAnother No");
                        resetFile();
                        askPassword(AppState.appFileOpen, uri);
                    }
                },
                null
        );
        Log.d(LOG_TAG,"saveToNewFileAndOpenUri done");
    }

    private void saveToOldFileAndOpenUri(String fileName, final Uri uri){
        Log.d(LOG_TAG,"saveToOldFileAndOpenUri was called");
        askForSave(fileName,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(LOG_TAG,"saveToOldFileAndOpenUri Yes");
                        saveEditText(true, viewModel.uriFull);
                        resetFile();
                        askPassword(AppState.appFileOpen, uri);
                    }
                },
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(LOG_TAG,"saveToOldFileAndOpenUri No");
                        resetFile();
                        askPassword(AppState.appFileOpen, uri);
                    }
                },
                null
        );
        Log.d(LOG_TAG,"saveToOldFileAndOpenUri done");
    }

    private void saveToNewFileAndOpenAnother(){
        Log.d(LOG_TAG,"saveToNewFileAndOpenAnother was called");
        askForSave("",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(LOG_TAG,"saveToNewFileAndOpenAnother Yes");
                        askDirectory(AppState.appFileSaveAndOpen);
                    }
                },
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(LOG_TAG,"saveToNewFileAndOpenAnother No");
                        resetFile();
                        askSelectFile(AppState.appFileOpen);
                    }
                },
                null
        );
        Log.d(LOG_TAG,"saveToNewFileAndOpenAnother done");
    }

    private void saveToOldFileAndOpenAnother(String fileName){
        Log.d(LOG_TAG,"saveToOldFileAndOpenAnother was called");
        askForSave(fileName,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(LOG_TAG,"saveToOldFileAndOpenAnother Yes");
                        saveEditText(true, viewModel.uriFull);
                        resetFile();
                        askSelectFile(AppState.appFileOpen);
                    }
                },
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(LOG_TAG,"saveToOldFileAndOpenAnother No");
                        resetFile();
                        askSelectFile(AppState.appFileOpen);
                    }
                },
                null
        );
        Log.d(LOG_TAG,"saveToOldFileAndOpenAnother done");
    }

    private void saveToNewFileAndResetCurrent(){
        Log.d(LOG_TAG,"saveToNewFileResetCurrent was called");
        askForSave("",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(LOG_TAG,"saveToNewFileResetCurrent Yes");
                        askDirectory(AppState.appFileReset);
                    }
                },
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(LOG_TAG,"saveToNewFileResetCurrent No");
                        resetFile();
                    }
                },
                null
        );
        Log.d(LOG_TAG,"saveToNewFileResetCurrent done");
    }

    private void saveToOldFileAndResetCurrent(String fileName){
        Log.d(LOG_TAG,"saveToOldFileAndResetCurrent was called");
        askForSave(fileName,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(LOG_TAG,"saveToOldFileAndResetCurrent Yes");
                        saveEditText(true, viewModel.uriFull);
                        resetFile();
                    }
                },
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(LOG_TAG,"saveToOldFileAndResetCurrent No");
                        resetFile();
                    }
                },
                null
        );
        Log.d(LOG_TAG,"saveToOldFileAndResetCurrent done");
    }

    private String editTextToString(){
        return editText.getText().toString();
    }

    private void refreshMetadata(DocMetadata docMetadata){
        docMetadata.caretPosition = editText.getSelectionStart();
        docMetadata.setKey(pswd);
    }

    private void saveMetadataTextAndOutputStream(DocMetadata docMetadata, String text, OutputStream outputStream) {
        Log.d(LOG_TAG,"saveMetadataTextAndOutputStream Start");

        Doc d = new Doc(text, docMetadata);
        try {
            d.doSaveFOS(outputStream);
        } catch (Exception e) {
            e.printStackTrace();
            showMessage("Error saving \"" + docMetadata.filename+"\":\n" + e.getMessage());
            return;
        }
        docMetadata.modified = false;

        Log.d(LOG_TAG, "saveMetadataTextAndOutputStream Done");
    }

    private void saveEditText(boolean interactive, Uri uriToSave) {
        String text;

        Log.d(LOG_TAG,"saveEditText Start");

        text = editTextToString();
        refreshMetadata(doc_metadata);

        Log.d(LOG_TAG,"saveEditText Create OutputStream for Uri=" + uriToSave.toString());
        try {
            OutputStream outputStream = getActivity().getContentResolver().openOutputStream(uriToSave);
            assert outputStream != null;
            saveMetadataTextAndOutputStream(doc_metadata, text, outputStream);
            outputStream.close();
            if (interactive) {
                showLongToast(getResources().getString(R.string.file_saved, doc_metadata.filename));
            }
        } catch (IOException e) {
            e.printStackTrace();
            showMessage(e.getMessage());
        }
        editTextInteraction.updateTitle(doc_metadata);
        Log.d(LOG_TAG, "saveEditText Done");
    }

    void saveIfAutosave() {
        Log.d(LOG_TAG,"saveIfAutosave was called");
        Log.d(LOG_TAG, viewModel.pref_autosave ? "saveIfAutosave pref_autosave=true" : "saveIfAutosave pref_autosave=false");
        Log.d(LOG_TAG, doc_metadata.modified ? "saveIfAutosave doc_metadata.modified=true" : "saveIfAutosave doc_metadata.modified=false");
        Log.d(LOG_TAG, (doc_metadata.filename == null) ? "saveIfAutosave doc_metadata.filename==null" : "saveIfAutosave doc_metadata.filename!=null");
        if (viewModel.pref_autosave && doc_metadata.modified && doc_metadata.filename != null) {
            Log.d(LOG_TAG,"saveIfAutosave - save filename=" + doc_metadata.filename);
            saveEditText(true, viewModel.uriFull);
        }
        else
            Log.d(LOG_TAG,"saveIfAutosave not needed");
        Log.d(LOG_TAG,"saveIfAutosave done");
    }

    public boolean needToSave(){
        return doc_metadata.modified && textViewUndoRedo.getCanUndo();
    }

    private void askFilenameAndPassword(Uri uriPath, AppState appState){
        Log.d(LOG_TAG,"askFilenameAndPassword Call showDialogAskPassword");
        Log.v(LOG_TAG, "askFilenameAndPassword appState=" + appState);

        // Create an instance of the dialog fragment and show it
        DialogAskFilenameAndPassword dialog = new DialogAskFilenameAndPassword();
        dialog.appState = appState;
        dialog.mUriPath = uriPath;
        dialog.show(getActivity().getSupportFragmentManager(), "DialogAskFilenameAndPassword");

        Log.d(LOG_TAG,"askFilenameAndPassword Call DialogAskFilenameAndPassword done");
    }


    private String normFileExt(String fileName){
        String result;
        if (fileName.endsWith(fileExt))
            result = fileName;
        else
            result = fileName + fileExt;
        return result;
    }

    private Uri createFile(Uri directoryUri, String fileName) {
        String text;
        String mimeType = "application/octet-stream";
        Uri outputUri = null;

        ContentResolver cr = getActivity().getContentResolver();
        assert cr != null;
        assert getContext() != null;
        DocumentFile dirFile = DocumentFile.fromTreeUri(getContext(), directoryUri);
        if (dirFile != null) {
            Log.d(LOG_TAG,"createFile dirFile=" + dirFile.toString());
            Log.d(LOG_TAG,"createFile dirFile.canWrite()=" + dirFile.canWrite());

            DocumentFile file = dirFile.createFile(mimeType, fileName);
            if (file != null) {
                Log.d(LOG_TAG,"createFile file=" + file.toString());
                outputUri = file.getUri();
                Log.d(LOG_TAG,"createFile outputUri=" + outputUri);
                Log.d(LOG_TAG,"createFile fileName=" + FileInfo.getFileName(outputUri, cr));
                //TODO: revise
                // remember outputUri for future use and fill doc_metadata.filename
                setUris(outputUri);
                try (ParcelFileDescriptor fd = cr.openFileDescriptor(outputUri, "w")) {
                    if (fd != null) {
                        text = editTextToString();
                        refreshMetadata(doc_metadata);

                        Log.d(LOG_TAG,"createFile outputStream");
                        FileOutputStream outputStream = new FileOutputStream(fd.getFileDescriptor());
                        saveMetadataTextAndOutputStream(doc_metadata, text, outputStream);
                        outputStream.close();
                        showLongToast(getResources().getString(R.string.file_saved, FileInfo.getFileName(outputUri, cr)));
                        Log.d(LOG_TAG,"createFile outputStream was saved");
                    }
                } catch (Throwable th) {
                    th.printStackTrace();
                    showMessage(th.toString() + " : " + th.getMessage());
                }
            }
            else
                Log.d(LOG_TAG,"createFile Ops! file is null" + dirFile.toString());
        }
        else
            Log.d(LOG_TAG,"createFile Ops! dirFile is null");
        return outputUri;
    }

    public Uri onDialogAskFilenameAndPasswordPositiveClick(DialogFragment dialog){
        // User touched the dialog's positive button
        Uri realUri = null;
        String fileName;

        Log.d(LOG_TAG,"onDialogAskFilenameAndPasswordPositiveClick was called");
        // Enter new fileName and Password
        assert dialog instanceof DialogAskFilenameAndPassword;
        try {
            DialogAskFilenameAndPassword dialogAFAP = (DialogAskFilenameAndPassword) dialog;
            EditText et = dialogAFAP.getDialog().findViewById(R.id.dafap_pwd);
            pswd = et.getText().toString();
            Log.d(LOG_TAG,"Now pswd=" + pswd);

            et = dialogAFAP.getDialog().findViewById(R.id.dafap_filename);
            fileName = normFileExt(et.getText().toString());
            Log.d(LOG_TAG,"Now fileName=" + fileName);
            Log.d(LOG_TAG,"Call createFile");
            realUri = createFile(dialogAFAP.mUriPath , fileName);
            //TODO: revise
            // if all OK - update uri
            setUris(realUri);
            // next step
            Log.d(LOG_TAG,"onDialogAskFilenameAndPasswordPositiveClick dialogAFAP.appState=" + dialogAFAP.appState);

            switch (dialogAFAP.appState) {
                // back pressed, on the way to exit
                case appFinish:
                    getActivity().finish();
                    break;
                // new File
                case appFileReset:
                    resetFile();
                    break;
                case appFileOpen:
                    showMessage("onDialogAskFilenameAndPasswordPositiveClick\nImposible situation appFileOpen");
                    //askSelectFile(AppState.appFileOpen);
                    break;
                // save file
                case appFileSave:
                    editTextInteraction.updateTitle(doc_metadata);
                    break;
                // file saved, now open another file
                case appFileSaveAndOpen:
                    editTextInteraction.updateTitle(doc_metadata);
                    askSelectFile(AppState.appFileOpen);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            showMessage(e.getMessage());
        }
        return realUri;
    }

    public void onDialogAskFilenameAndPasswordNegativeClick(DialogFragment dialog){
        Log.d(LOG_TAG,"onDialogAskFilenameAndPasswordNegativeClick was called");
    }

    public void askPassword(AppState appState, Uri uri){
        Log.d(LOG_TAG,"askPassword Call showDialogAskPassword");

        // Create an instance of the dialog fragment and show it
        DialogAskPassword dialog = new DialogAskPassword();
        dialog.uri = uri;
        dialog.fileName = FileInfo.getFileName(uri, getActivity().getContentResolver());
        dialog.appState = appState;
        dialog.show(getActivity().getSupportFragmentManager(), "DialogAskPassword");

        Log.d(LOG_TAG,"askPassword Call showDialogAskPassword done.");
    }

    // called from MainActivity
    public boolean onDialogAskPasswordPositiveClick(DialogFragment dialog) {
        boolean openSuccessfully = false;
        String s;
        DialogAskPassword d;

        // Enter password
        Log.d(LOG_TAG,"onDialogAskPasswordPositiveClick was called");
        assert dialog instanceof DialogAskPassword;
        d = (DialogAskPassword) dialog;
        s = d.etPassword.getText().toString();
        if (d.appState == AppState.appFileOpen){
            try {
                Log.d(LOG_TAG, "Call openFile d.uri=" + d.uri);
                try {
                    openFile(d.uri, s);
                    openSuccessfully = true;
                    pswd = s;
                    viewModel.uriFull = d.uri;
                } catch (SecurityException se) {
                    Log.d(LOG_TAG, "SecurityException se=" + se);
                    showMessage("Cant open file.\n Try to open by menu->Open\nor use file manager and ->Open with...\n\n" + se.getMessage());
                }
            } catch (Exception e) {
                e.printStackTrace();
                showMessage(e.getMessage());
            }
        }
        else
            showMessage("onDialogAskPasswordPositiveClick called for " + d.appState);
        return openSuccessfully;
    }

    public void onDialogAskPasswordNegativeClick(DialogFragment dialog) {
        Log.d(LOG_TAG,"onDialogAskPasswordNegativeClick was called");
    }

    private void askDirectory(AppState appState){
        Log.v(LOG_TAG, "askDirectory was called appState=" + appState);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Log.v(LOG_TAG, "askDirectory appState=" + appState);
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            // don't work, so value stored as field
            //i.putExtra(extra_want_to_die_key, wantToDie);
            startActivityForResult(Intent.createChooser(i, "Choose directory"), appState.asInt());
        }
    }

    private void askSelectFile(AppState appState){
        Log.v(LOG_TAG, "askSelectFile was called appState=" + appState);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("*/*");
            startActivityForResult(Intent.createChooser(i, "Choose file"), appState.asInt());
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        AppState appState;
        Uri uri;

        super.onActivityResult(requestCode, resultCode, data);

        appState = AppState.fromInt(requestCode);
        Log.d(LOG_TAG, "onActivityResult from requestCode appState="+ appState);

        // Open file selection
        if (appState == AppState.appFileOpen && resultCode == Activity.RESULT_OK) {
            // read Uri
            assert data != null;
            uri = data.getData();
            Log.d(LOG_TAG, "onActivityResult1 uri=" + uri);
            Log.d(LOG_TAG, "onActivityResult1 Asking password");
            askPassword(appState, uri);

            // Save file directory selection
        } else if ((appState == AppState.appFileSave || appState == AppState.appFileReset || appState == AppState.appFileSaveAndOpen)
                && resultCode == Activity.RESULT_OK){
            try {
                // read directory Uri
                Uri dirUri = data.getData();
                if (dirUri == null)
                    return;
                // persist picked uri to be able to reuse it later
                getActivity().getContentResolver().takePersistableUriPermission(dirUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                askFilenameAndPassword(dirUri, appState);
            } catch (Exception e) {
                e.printStackTrace();
                showMessage(e.getMessage());
            }
        }
    }

    /* Show a simple message dialog */
    private void showMessage(String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMessage(msg).setCancelable(true).setPositiveButton("Ok",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog dlg = builder.create();
        dlg.show();
    }

    public void openFile(@NonNull Uri uriFile, String password) throws IOException, DocException, SecurityException {

        Log.d(LOG_TAG,"openFile Create inputStream from uriFile=" + uriFile.getEncodedPath());
        ContentResolver cr = getActivity().getContentResolver();
        Log.d(LOG_TAG,"ContentResolver=" + cr.toString());
        Log.d(LOG_TAG,"ContentResolver.getType(uriFile)=" + cr.getType(uriFile));
        InputStream inputStream = cr.openInputStream(uriFile);

        File fdir = new File(uriFile.getEncodedPath());
        Log.d(LOG_TAG,"fdir=" + fdir);
        Doc d = new Doc();
        Log.d(LOG_TAG,"openFile Call doOpenFIS");
        d.doOpenFIS(inputStream, password, fdir);

        Log.d(LOG_TAG,"openFile Show result in the main_text");
        doc_metadata = d.getDocMetadata();
        doc_metadata.filename = FileInfo.getFileName(uriFile, cr);

        editText.setText(d.getText());
        editText.setSelection(doc_metadata.caretPosition, doc_metadata.caretPosition);
        editText.requestFocus();

        doc_metadata.modified = false;
        textViewUndoRedo.clearHistory();
        editTextInteraction.updateTitle(doc_metadata);
    }

    private void showToast(String message, int teastLength){
        Toast toast = Toast.makeText(getActivity().getApplicationContext(), message, teastLength);
        toast.show();
    }

    private void showShortToast(String message){
        showToast(message, Toast.LENGTH_SHORT);
    }

    private void showLongToast(String message){
        showToast(message, Toast.LENGTH_LONG);
    }

    public void onBackPressedImpl() {
        String fn;

        Log.d(LOG_TAG,"onBackPressedImpl was called");

        saveIfAutosave();
        // if still not saved, so or not autosave or no filename
        if (needToSave()) {
            if (doc_metadata.filename == null)
                saveToNewFileAndFinish();
            else
                saveToOldFileAndFinish(doc_metadata.filename);
        }
        else
            getActivity().finish();

        Log.d(LOG_TAG,"onBackPressedImpl done");
    }


    void onRecentFileItemClickImpl(Uri uri) {
        Log.d(LOG_TAG,"onRecentFileItemClickImpl was called");
        saveIfAutosave();
        if (needToSave()) {
            if (doc_metadata.filename == null){
                saveToNewFileAndOpenUri(uri);
            }
            else {
                saveToOldFileAndOpenUri(doc_metadata.filename, uri);
            }
        }
        else
            askPassword(AppState.appFileOpen, uri);
        Log.d(LOG_TAG,"onRecentFileItemClickImpl done");
    }

}
