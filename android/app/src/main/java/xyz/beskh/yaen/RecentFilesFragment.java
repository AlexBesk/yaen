package xyz.beskh.yaen;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

public class RecentFilesFragment extends Fragment {
    private final String LOG_TAG = "RecentFiles_" + this.hashCode();
    private MyViewModel viewModel;
    FileInfoAdapter fileInfoAdapter;
    private RecentFilesInteraction recentFilesInteraction;

    public interface RecentFilesInteraction {
        void onRecentFileItemClick(Uri uri);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(LOG_TAG, "onCreateView container=" + container);
        if (container != null)
            Log.d(LOG_TAG, "onCreateView container.hashCode()=" + container.hashCode());
        View view = inflater.inflate(R.layout.recent_files, container, false);

        // получаем экземпляр элемента ListView
        ListView listView = view.findViewById(R.id.recentFilesListView);

        Log.d(LOG_TAG, "onCreateView viewModel=" + viewModel);
        Log.d(LOG_TAG, "onCreateView viewModel.fileInfos=" + (viewModel == null ? "nothing" : viewModel.fileInfos));

        // используем адаптер данных
        fileInfoAdapter = new FileInfoAdapter(this.getContext(), viewModel.fileInfos);

        Log.d(LOG_TAG, "onCreateView adapter=" + fileInfoAdapter);
        listView.setAdapter(fileInfoAdapter);

        listView.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View itemClicked, int position, long id) {
                        Log.d(LOG_TAG, "onItemClick position=" + position);
                        Uri uri = viewModel.fileInfos.get(position).getUri();
                        Log.d(LOG_TAG, "onItemClick uri=" + uri);
                        recentFilesInteraction.onRecentFileItemClick(uri);
                    }
                }
        );

        return view;
    }


    @Override
    public void onAttach(@NonNull Activity activity) {
        Log.d(LOG_TAG, "onAttach this.getId()=" + this.getId());
        super.onAttach(activity);
        try {
            recentFilesInteraction = (RecentFilesInteraction) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement RecentFilesInteraction");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(LOG_TAG, "onCreate this.getId()=" + this.getId());
        super.onCreate(savedInstanceState);

        Log.d(LOG_TAG, "onCreate requireActivity()=" + requireActivity());
        viewModel = new ViewModelProvider(requireActivity()).get(MyViewModel.class);

        Log.d(LOG_TAG, "onCreate viewModel=" + viewModel);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(LOG_TAG, "onActivityCreated");
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(LOG_TAG, "onStart");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(LOG_TAG, "onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(LOG_TAG, "onPause");
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
        super.onSaveInstanceState(outState);
        Log.d(LOG_TAG, "onSaveInstanceState");
    }

    public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        Log.d(LOG_TAG, "onRestoreInstanceState");
    }

}
