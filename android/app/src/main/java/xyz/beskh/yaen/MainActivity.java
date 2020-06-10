package xyz.beskh.yaen;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager.widget.ViewPager;
import androidx.preference.PreferenceManager;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

import enotes.doc.Doc;
import enotes.doc.DocMetadata;

public class MainActivity extends AppCompatActivity
        implements EditTextFragment.EditTextInteraction,
        DialogAskPassword.DialogAskPasswordListener,
        RecentFilesFragment.RecentFilesInteraction,
        DialogAskFilenameAndPassword.DialogAskFilenameAndPasswordListener {

    private static final String version = "2.0";

    private static final String RECENT_FILES_KEY = "recent_files_key";
    private static final int RECENT_FILES_LIMIT = 10;
    final String LOG_TAG = "MainActivity_" + this.hashCode();
    MyViewModel viewModel;
    Uri uriFileToOpen;

    public static final String SETTINGS_PARANOID_SWITCH = "settings_paranoid_switch";
    public static final String SETTINGS_AUTOSAVE_SWITCH = "settings_autosave_switch";
    public static final String SETTINGS_RECENT_FILES_SWITCH = "settings_recent_files_switch";
    MyPageAdapter adapter;
    ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //super.onCreate(savedInstanceState);
        Log.d(LOG_TAG, "onCreate this.hashCode()=" + this.hashCode());
        setContentView(R.layout.activity_main);

        viewModel = new ViewModelProvider(this).get(MyViewModel.class);
        Log.d(LOG_TAG, "onCreate viewModel=" + viewModel);

/*        viewModel.onRecentFileListener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View itemClicked, int position, long id) {
                Log.d(LOG_TAG, "onItemClick position=" + position);
                TextView tv = findViewById(R.id.main_text);
                Log.d(LOG_TAG, "onItemClick tv=" + tv);
                Log.d(LOG_TAG, "onItemClick viewModel.fileInfos=" + viewModel.fileInfos);
                tv.setText("Выбран файл " + viewModel.fileInfos.get(position).getFullFileName());
                ViewPager vp = findViewById(R.id.viewpager);
                Log.d(LOG_TAG, "onItemClick vp.hashCode()=" + vp.hashCode());
                vp.setCurrentItem(1); // выводим второй экран
            }
        };*/

        adapter = new MyPageAdapter(getSupportFragmentManager(), FragmentPagerAdapter.BEHAVIOR_SET_USER_VISIBLE_HINT);
        Log.d(LOG_TAG, "onCreate adapter.hashCode()=" + adapter.hashCode());
        viewPager = findViewById(R.id.viewpager);
        Log.d(LOG_TAG, "onCreate viewPager.hashCode()=" + viewPager.hashCode());
        viewPager.setAdapter(adapter); // устанавливаем адаптер

        Log.d(LOG_TAG, "onCreate savedInstanceState=" + savedInstanceState);
        if (savedInstanceState == null){

            viewModel.fileInfos = new ArrayList<FileInfo>();
            Log.d(LOG_TAG, "onCreate viewModel.fileInfos=" + viewModel.fileInfos);

            // Open file on startup
            Intent intent = getIntent();
            String action = intent.getAction();
            Log.d(LOG_TAG, "onCreate intent.getAction()=" + action);
            if(Intent.ACTION_VIEW.equals(action)){
                uriFileToOpen = intent.getData();
                showEditTextPage();
            }
            else {
                readPreferences();
                if (viewModel.pref_recent_files) {
                    showRecentFilesPage();
                } else {
                    showEditTextPage();
                }
            }
            Log.d(LOG_TAG,"onCreate fileFileToOpenUri=" + uriFileToOpen);
        }
        else {
            Log.d(LOG_TAG, "onCreate viewModel.fileInfos=" + viewModel.fileInfos);
        }

        Log.d(LOG_TAG, "onCreate before super.onCreate call");
        super.onCreate(savedInstanceState);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

        Log.d(LOG_TAG, "onCreate after super.onCreate call");

    }

    //
    // EditTextInteraction interface implementation
    //
    @Override
    public Uri getFileToOpenUri(boolean purge) {
        Uri uri = this.uriFileToOpen;
        if (purge)
            this.uriFileToOpen = null;
        return uri;
    }

    /* Updates app title based on open file */
    public void updateTitle(DocMetadata docMetadata) {
        String fname;
        if (docMetadata.filename == null){
            fname = getResources().getString(R.string.new_document);
            Log.d(LOG_TAG,"updateTitle docMetadata.filename = null");
        }
        else {
            Log.d(LOG_TAG,"updateTitle docMetadata.filename = " + docMetadata.filename);
            fname = docMetadata.filename;
            if (docMetadata.modified) {
                fname = "* " + fname;
            }
            Log.d(LOG_TAG,"updateTitle fname = " + fname);
        }
        setTitle(getResources().getString(R.string.app_title, fname));
        invalidateOptionsMenu();
    }

    private void createTestFiles(ArrayList<FileInfo> list, int count ){
        Log.d(LOG_TAG, "createTestFiles");
        FileInfo fi;
        Uri uri;

        list.clear();
        for(int i = 0; i <= count -1; i++){
            fi = new FileInfo();
            uri = Uri.parse(Uri.decode("content://com.android.externalstorage.documents/path/to/File_" + i));;
            fi.setUri(uri, getContentResolver());
            list.add(fi);
        }
    }

    private boolean isNoRecentFileUri(Uri uri){
        String suri = uri.toString();
        Log.d(LOG_TAG, "noRecentFileUri suri=" + suri);
        boolean b = suri.equals(getString(R.string.no_recent_files));
        Log.d(LOG_TAG, "noRecentFileUri isNoRecentFileUri=" + b);
        return  b;
    }

    private void saveRecentFiles(@NonNull ArrayList<FileInfo> list) {
        Log.d(LOG_TAG, "saveRecentFiles");
        String suri;

        Log.d(LOG_TAG, "saveRecentFiles viewModel.pref_recent_files=" + viewModel.pref_recent_files);
        Set<String> lhs =  new LinkedHashSet<String>();
        if (viewModel.pref_recent_files) {
            for (FileInfo fi : list) {
                if (!isNoRecentFileUri(fi.getUri())) {
                    suri = fi.getUri().toString();
                    lhs.add(suri);
                }
            }
        }
        Log.d(LOG_TAG, "hs.size()=" + lhs.size());

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sp.edit();
        editor.putStringSet(RECENT_FILES_KEY, lhs);
        editor.apply();
    }

    private void readRecentFiles(@NonNull ArrayList<FileInfo> list){
        Log.d(LOG_TAG, "readRecentFiles");
        FileInfo fi;
        Uri uri;

        list.clear();
        ContentResolver cr = getContentResolver();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);

        Set<String> recentFiles = sp.getStringSet(RECENT_FILES_KEY, new LinkedHashSet<String>());

        for (String suri : recentFiles) {
            try {
                Log.d(LOG_TAG, "suri=" + suri);
                uri = Uri.parse(suri);
                Log.d(LOG_TAG, "uri=" + uri);
                fi = new FileInfo(uri, cr);
                Log.d(LOG_TAG, "add new FileInfo");
                list.add(fi);
            } catch(Exception e){
                Log.e(LOG_TAG, e.getMessage());
                e.printStackTrace();
            }
        }
        Log.d(LOG_TAG, "list.size()=" + list.size());
    }

    private boolean isUriInList(Uri uri, ArrayList<FileInfo> list){
        boolean b = false;
        Log.d(LOG_TAG, "isUriInList uri=" + uri);
        for (FileInfo fi : list) {
            b = fi.getUri().equals(uri);
            if (b)
                break;
        }
        Log.d(LOG_TAG, "isUriInList=" + b);
        return b;
    }

    private void removeUriFromList(Uri uri, ArrayList<FileInfo> list){
        boolean b;
        Log.d(LOG_TAG, "isUriInList uri=" + uri);
        for (FileInfo fi : list) {
            b = fi.getUri().equals(uri);
            if (b) {
                Log.d(LOG_TAG, "removeUriFromList uri was found. remove it");
                list.remove(fi);
                break;
            }
        }
    }

    private void addUriToListFirst(Uri uri, ArrayList<FileInfo> list){
        Log.d(LOG_TAG, "addUriToListFirst uri=" + uri);
        if (list.size() == 1 && isNoRecentFileUri(list.get(0).getUri()))
            list.remove(0);
        removeUriFromList(uri, list);
        list.add(0, new FileInfo(uri, getContentResolver()));
        while (list.size() > RECENT_FILES_LIMIT){
            list.remove(list.size() - 1);
        }
    }

    private void addUriNoRecentFiles(@NonNull ArrayList<FileInfo> list) {
        Log.d(LOG_TAG, "addNoRecentFilesFile");
        try {
            Uri uri = Uri.parse(getString(R.string.no_recent_files));
            addUriToListFirst(uri, list);
/*
            Log.d(LOG_TAG, "uri=" + uri);
            FileInfo fi = new FileInfo(uri, null);
            Log.d(LOG_TAG, "add new FileInfo");
            list.add(fi);
*/
        } catch(Exception e){
            Log.e(LOG_TAG, e.getMessage());
            e.printStackTrace();
        }
    }

    private void readPreferences(){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        viewModel.pref_recent_files = sp.getBoolean(SETTINGS_RECENT_FILES_SWITCH, true);
        Log.d(LOG_TAG, "readPreferences pref_recent_files=" + viewModel.pref_recent_files);

        viewModel.pref_autosave = sp.getBoolean(SETTINGS_AUTOSAVE_SWITCH, true);
        Log.d(LOG_TAG, "readPreferences pref_autosave=" + viewModel.pref_autosave);

        viewModel.pref_paranoid = sp.getBoolean(SETTINGS_PARANOID_SWITCH, false);
        Log.d(LOG_TAG, "readPreferences pref_paranoid=" + viewModel.pref_paranoid);
    }

        @Override
    protected void onStart() {
        super.onStart();
        Log.d(LOG_TAG, "onStart");
        // get from preferences
        readPreferences();
        invalidateOptionsMenu();
        readRecentFiles(viewModel.fileInfos);
        if (viewModel.fileInfos.size() == 0){
                addUriNoRecentFiles(viewModel.fileInfos);
            }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(LOG_TAG, "onResume");
    }

    @Override
    protected void onPause() {
        Log.d(LOG_TAG, "onPause");
        if (adapter != null && adapter.editTextFragment != null)
            adapter.editTextFragment.saveIfAutosave();
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(LOG_TAG, "onStop");
        readPreferences();
        saveRecentFiles(viewModel.fileInfos);
    }

    @Override
    public void onBackPressed() {
        Log.d(LOG_TAG,"onBackPressed was called");
        adapter.editTextFragment.onBackPressedImpl();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG, "onDestroy");
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {

        Log.d(LOG_TAG, "onSaveInstanceState");
        //outState.putParcelable("state_adapter", adapter.saveState());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        Log.d(LOG_TAG, "onRestoreInstanceState");
        super.onRestoreInstanceState(savedInstanceState);
        //adapter.restoreState(savedInstanceState.getParcelable("state_adapter"), getClassLoader());
    }

    /* # Create options menu */
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(LOG_TAG, "onCreateOptionsMenu was called");
        getMenuInflater().inflate(R.menu.menu_edit, menu);
        return true;
    }

    private void prepareMI(MenuItem item, Boolean enabled){
        Log.d(LOG_TAG, "prepareMI " + item.toString() + (enabled ? " Enabled" : " Disabled"));
        if (enabled){
            item.setEnabled(true);
            item.getIcon().setAlpha(255);
        }
        else{
            item.setEnabled(false);
            item.getIcon().setAlpha(130);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu (Menu menu) {
        prepareMI(menu.findItem(R.id.menu_undo), adapter.editTextFragment.textViewUndoRedo.getCanUndo());
        prepareMI(menu.findItem(R.id.menu_redo), adapter.editTextFragment.textViewUndoRedo.getCanRedo());

        MenuItem m = menu.findItem(R.id.menu_save);
        if (viewModel.pref_autosave) {
            m.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
            m.setTitle(R.string.menu_save_ason);
        }
        else {
            m.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            m.setTitle(R.string.menu_save_asoff);
        }
        prepareMI(m, adapter.editTextFragment.doc_metadata.modified);

        // cant call save_as in the paranoid mode
        menu.findItem(R.id.menu_save_as).setEnabled(!viewModel.pref_paranoid);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_new:
                Log.d(LOG_TAG, "onOptionsItemSelected menu_new");
                menuNewFile();
                return true;
            case R.id.menu_open:
                Log.d(LOG_TAG, "onOptionsItemSelected menu_open");
                menuOpenFile();
                //destroyStateFile();
                return true;
            case R.id.menu_save:
                Log.d(LOG_TAG, "onOptionsItemSelected menu_save");
                menuSaveFile();
                //destroyStateFile();
                return true;
            case R.id.menu_save_as:
                Log.d(LOG_TAG, "onOptionsItemSelected menu_save_as");
                menuSaveAsFile();
                //destroyStateFile();
                return true;
            case R.id.menu_settings:
                Log.d(LOG_TAG, "onOptionsItemSelected menu_settings");
                menuSettings();
                return true;
            case R.id.menu_undo:
                Log.d(LOG_TAG, "onOptionsItemSelected menu_undo");
                menuUndo();
                return true;
            case R.id.menu_redo:
                Log.d(LOG_TAG, "onOptionsItemSelected menu_redo");
                menuRedo();
                return true;
            case R.id.menu_info:
                Log.d(LOG_TAG, "onOptionsItemSelected menu_info");
                menuInfo();
                return true;
            case R.id.menu_close:
                Log.d(LOG_TAG, "onOptionsItemSelected menu_close");
                finish();
                return true;
        }
        return false;
    }

    private void menuNewFile(){
        Log.d(LOG_TAG,"menuNewFile was called");
        showEditTextPage();
        adapter.editTextFragment.menuNewFile();
        Log.d(LOG_TAG,"menuNewFile done");
    }

    private void menuSaveFile() {
        Log.d(LOG_TAG,"menuSaveFile was called");
        adapter.editTextFragment.menuSaveFile();
    }

    private void menuSaveAsFile() {
        Log.d(LOG_TAG,"menuSaveFile was called");
        adapter.editTextFragment.menuSaveAsFile();
    }

    private void menuOpenFile() {
        Log.d(LOG_TAG,"menuOpenFile was called");
        adapter.editTextFragment.menuOpenFile();
        Log.d(LOG_TAG,"menuOpenFile done");
    }

    private void menuSettings() {
        Log.d(LOG_TAG,"menuSettings was called");

        if (viewModel.pref_paranoid && adapter.editTextFragment.needToSave() && !viewModel.pref_autosave) {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    //.setTitle(R.string.savefile_title)
                    .setTitle(R.string.warning)
                    .setMessage(R.string.lose_cahnges)
                    .setPositiveButton(R.string.Yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.d(LOG_TAG,"menuSettingsWarning clicked Yes");
                            Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
                            startActivity(intent);
                        }
                    })
                    .setNegativeButton(R.string.No, null)
                    .show();
        }
        else{
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        }
    }

    private void menuUndo() {
        Log.d(LOG_TAG,"menuUndo was called");
        adapter.editTextFragment.menuUndo();
        invalidateOptionsMenu();
    }

    private void menuRedo() {
        Log.d(LOG_TAG,"menuRedo was called");
        adapter.editTextFragment.menuRedo();
        invalidateOptionsMenu();
    }

    private void menuInfo() {
        Log.d(LOG_TAG,"menuInfo was called");
        String msg;
        if (adapter.editTextFragment.doc_metadata.filename != null)
            msg = adapter.editTextFragment.doc_metadata.filename;
        else
            msg = "no file";
        if (viewModel.uriFull != null)
            msg = msg + "\nFull path:\n" + viewModel.uriFull;
        msg = getResources().getString(R.string.about_text, version, Doc.CRYPTO_MODE, msg);

        new AlertDialog.Builder(this)
                .setIcon(R.mipmap.ic_launcher)
                .setTitle(R.string.app_name)
                .setMessage(msg)
                .setPositiveButton("OK", null)
                .show();

    }

    // The dialog fragment receives a reference to this Activity through the
    // Fragment.onAttach() callback, which it uses to call the following methods
    // defined by the NoticeDialogFragment.NoticeDialogListener interface

    @Override
    public void onDialogAskPasswordPositiveClick(DialogFragment dialog) {
        Log.d(LOG_TAG,"onDialogAskPasswordPositiveClick was called");
        assert dialog instanceof DialogAskPassword;
        if (viewModel.pref_recent_files) {
            addUriToListFirst(((DialogAskPassword) dialog).uri, viewModel.fileInfos);
            adapter.recentFiles.fileInfoAdapter.notifyDataSetChanged();
        }
        if (adapter.editTextFragment.onDialogAskPasswordPositiveClick(dialog)) {
            Log.d(LOG_TAG,"adapter.editTextFragment.onDialogAskPasswordPositiveClick=true");
        }
        else {
            Log.d(LOG_TAG,"adapter.editTextFragment.onDialogAskPasswordPositiveClick=true");
        }
        showEditTextPage();
    }

    @Override
    public void onDialogAskPasswordNegativeClick(DialogFragment dialog) {
        Log.d(LOG_TAG,"onDialogAskPasswordNegativeClick was called");
        adapter.editTextFragment.onDialogAskPasswordNegativeClick(dialog);
    }

    public void onDialogAskFilenameAndPasswordPositiveClick(DialogFragment dialog){
        Uri uri;
        Log.d(LOG_TAG,"onDialogAskFilenameAndPasswordPositiveClick was called");
        assert dialog instanceof DialogAskFilenameAndPassword;
        uri = adapter.editTextFragment.onDialogAskFilenameAndPasswordPositiveClick(dialog);
        if (viewModel.pref_recent_files && uri != null ) {
            addUriToListFirst(uri, viewModel.fileInfos);
            adapter.recentFiles.fileInfoAdapter.notifyDataSetChanged();
        }
        showEditTextPage();
    }

    public void onDialogAskFilenameAndPasswordNegativeClick(DialogFragment dialog){
        Log.d(LOG_TAG,"onDialogAskFilenameAndPasswordNegativeClick was called");
        adapter.editTextFragment.onDialogAskFilenameAndPasswordNegativeClick(dialog);
    }

    /* Show a simple message dialog */
    private void showMessage(String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(msg).setCancelable(true).setPositiveButton("Ok",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog dlg = builder.create();
        dlg.show();
    }

    private void showRecentFilesPage(){
        if (viewPager != null)
            viewPager.setCurrentItem(0); // выводим первый экран
    }

    private void showEditTextPage(){
        if (viewPager != null)
            viewPager.setCurrentItem(1); // выводим второй экран
    }

    @Override
    public void onRecentFileItemClick(Uri uri) {
        Log.d(LOG_TAG,"onRecentFileItemClick uri=" + uri);
        if (uri != null) {
            if (!isNoRecentFileUri(uri))
                adapter.editTextFragment.onRecentFileItemClickImpl(uri);
            showEditTextPage();
        }
    }

}
