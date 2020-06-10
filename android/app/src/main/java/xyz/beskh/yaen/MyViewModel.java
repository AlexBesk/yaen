package xyz.beskh.yaen;

import android.net.Uri;
import android.widget.AdapterView;

import androidx.lifecycle.ViewModel;

import java.util.ArrayList;

public class MyViewModel extends ViewModel {
    ArrayList<FileInfo> fileInfos;
    //AdapterView.OnItemClickListener onRecentFileListener;
    boolean pref_paranoid;
    boolean pref_autosave;
    boolean pref_recent_files;
    Uri uriFull;
}
