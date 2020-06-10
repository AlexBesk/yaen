package xyz.beskh.yaen;

import android.util.Log;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

public class MyPageAdapter extends FragmentPagerAdapter {
    String LOG_TAG = "MyPageAdapter_"+ this.hashCode();
    EditTextFragment editTextFragment;
    RecentFilesFragment recentFiles;

    public MyPageAdapter(@NonNull FragmentManager fm, int behavior) {
        super(fm, behavior);
    }

    @Override
    public int getCount() {
        return 2;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                RecentFilesFragment rf = new RecentFilesFragment();
                Log.d(LOG_TAG, "getItem recentFiles.hashCode()=" + rf.hashCode());
                return rf;
            case 1:
                EditTextFragment et = new EditTextFragment();
                Log.d(LOG_TAG, "getItem editText.hashCode()=" + et.hashCode());
                return et;
            default:
                return null;
        }
    }

    // Here we can finally safely save a reference to the created
    // Fragment, no matter where it came from (either getItem() or
    // FragmentManger). Simply save the returned Fragment from
    // super.instantiateItem() into an appropriate reference depending
    // on the ViewPager position.
    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        Fragment createdFragment = (Fragment) super.instantiateItem(container, position);
        // save the appropriate reference depending on position
        switch (position) {
            case 0:
                recentFiles = (RecentFilesFragment) createdFragment;
                Log.d(LOG_TAG, "instantiateItem recentFiles.hashCode()=" + recentFiles.hashCode());
                break;
            case 1:
                editTextFragment = (EditTextFragment) createdFragment;
                Log.d(LOG_TAG, "instantiateItem editText.hashCode()=" + editTextFragment.hashCode());
                break;
        }
        return createdFragment;
    }
}
