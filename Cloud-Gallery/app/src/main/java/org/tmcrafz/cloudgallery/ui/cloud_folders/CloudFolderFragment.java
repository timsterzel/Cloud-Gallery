package org.tmcrafz.cloudgallery.ui.cloud_folders;

import androidx.lifecycle.ViewModelProviders;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.owncloud.android.lib.resources.files.model.RemoteFile;

import org.tmcrafz.cloudgallery.R;
import org.tmcrafz.cloudgallery.adapters.RecyclerviewFolderBrowserAdapter;
import org.tmcrafz.cloudgallery.datahandling.StorageHandler;
import org.tmcrafz.cloudgallery.web.CloudFunctions;
import org.tmcrafz.cloudgallery.web.nextcloud.NextcloudOperationDownloadThumbnail;
import org.tmcrafz.cloudgallery.web.nextcloud.NextcloudOperationReadFolder;
import org.tmcrafz.cloudgallery.web.nextcloud.NextcloudWrapper;

import java.io.File;
import java.util.ArrayList;

public class CloudFolderFragment extends Fragment implements
        NextcloudOperationReadFolder.OnReadFolderFinishedListener,
        NextcloudOperationDownloadThumbnail.OnDownloadThumbnailFinishedListener,
        RecyclerviewFolderBrowserAdapter.OnLoadFolderData {
    private static String TAG = CloudFolderFragment.class.getCanonicalName();

    private ArrayList<RecyclerviewFolderBrowserAdapter.AdapterItem> mItemData = new ArrayList<RecyclerviewFolderBrowserAdapter.AdapterItem>();
    private final String ABSOLUTE_ROOT_PATH = "/";
    private String mCurrentPath = ABSOLUTE_ROOT_PATH;

    private CloudFolderViewModel mViewModel;
    private RecyclerView mRecyclerViewFolderBrowser;
    private LinearLayoutManager mLayoutManager;
    private RecyclerviewFolderBrowserAdapter mRecyclerViewFolderBrowserAdapter;

    private NextcloudWrapper mNextCloudWrapper;

    public static CloudFolderFragment newInstance() {
        return new CloudFolderFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getActivity().getSharedPreferences(getString(R.string.key_preference_file_key), 0);
        String serverUrl = prefs.getString(getString(R.string.key_preference_server_url), "");
        String username = prefs.getString(getString(R.string.key_preference_username), "");
        String password = prefs.getString(getString(R.string.key_preference_password), "");
        if (serverUrl != "" && username != "" && password != "") {
            mNextCloudWrapper = new NextcloudWrapper(serverUrl);
            mNextCloudWrapper.connect(username, password, getActivity());
        }
        else {
            Log.d(TAG, "mNextCloudWrapper Cannot connect to Nextcloud. Server, username or password is not set");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_cloud_folder, container, false);

        mRecyclerViewFolderBrowser = root.findViewById(R.id.recyclerView_gallery);
        mLayoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        mRecyclerViewFolderBrowser.setLayoutManager(mLayoutManager);

        mRecyclerViewFolderBrowserAdapter = new RecyclerviewFolderBrowserAdapter((RecyclerviewFolderBrowserAdapter.OnLoadFolderData) this, mItemData);
        mRecyclerViewFolderBrowser.setAdapter(mRecyclerViewFolderBrowserAdapter);
        // Turn off animation when item change
        //((SimpleItemAnimator) mRecyclerViewGallery.getItemAnimator()).setSupportsChangeAnimations(false);
        onLoadPathData(mCurrentPath);
        return root;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(CloudFolderViewModel.class);
        // TODO: Use the ViewModel
    }

    @Override
    public void onReadFolderFinished(String identifier, boolean isSuccesfull, ArrayList<Object> files) {
        if (isSuccesfull) {
            // We replace the new pathes with the old ones
            mItemData.clear();
            // ToDo: thumbnail size in settings and elsewhere (not hardcoded here)
            int thumbnailSizePixel = 1280;
            String localDirectoryPath = StorageHandler.getThumbnailDir(getContext());

            //mPathData.add(new RecyclerviewFolderBrowserAdapter.AdapterItem("Back", ""));
            // ToDo: Temporary add entry to come back. Add functionality with androids "back button" instead
            if (!mCurrentPath.equals(ABSOLUTE_ROOT_PATH)) {
                String parent = (new File(mCurrentPath)).getParent();
                mItemData.add(
                        new RecyclerviewFolderBrowserAdapter.AdapterItem.FolderItem(
                                RecyclerviewFolderBrowserAdapter.AdapterItem.TYPE_FOLDER, getString(R.string.text_folder_browser_back), parent));
            }
            for(Object fileTmp: files) {
                RemoteFile file = (RemoteFile)  fileTmp;
                String mimetype = file.getMimeType();
                String remotePath = file.getRemotePath();
                //Log.d(TAG, remotePath + ": " + mimetype);
                if (mimetype.equals("DIR")) {
                    String name = remotePath;
                    //Log.d(TAG, "remotePath Path: " + remotePath);
                    mItemData.add(
                            new RecyclerviewFolderBrowserAdapter.AdapterItem.FolderItem(
                                    RecyclerviewFolderBrowserAdapter.AdapterItem.TYPE_FOLDER, name, remotePath));
                    // ToDo: Ausnahme für aktuellen Ordner (bpen Eintrag, außerhalb von Adapter?)
                    //if (!remotePath.equals(identifier)) {
                    //    mNextCloudWrapper.startReadFolder(remotePath, remotePath, new Handler(), this);
                    //}
                }
                else if (CloudFunctions.isFileSupportedPicture(remotePath)) {
                    // Download Picture
                    // Create local file path (location on disk + absolute path
                    // For example:
                    // remote path: /Test1/test.png, location on disk: /storage/external/CloudGallery
                    // Local file path: /storage/external/CloudGallery/Test1/test.png
                    // The final path
                    String localFilePath = localDirectoryPath + remotePath;
                    Log.d(TAG, "->localFilePath before Add: " + localFilePath);
                    mItemData.add(
                            new RecyclerviewFolderBrowserAdapter.AdapterItem.ImageItem(
                                RecyclerviewFolderBrowserAdapter.AdapterItem.TYPE_IMAGE, localDirectoryPath, localFilePath, remotePath, false));
                }
            }
            // Show loaded path in recyclerview adapter
            mRecyclerViewFolderBrowserAdapter.notifyDataSetChanged();
        }
        else {
            Log.e(TAG, "Could not read remote folder with identifier: " + identifier);
        }
        mNextCloudWrapper.cleanOperations();
    }

    @Override
    public void onDownloadThumbnailFinished(String identifier, boolean isSuccessful) {
        if (isSuccessful) {
            String localFilePath = identifier;
            File file = new File(localFilePath);
            if (file.exists() && file.isFile()) {
                // We note that the file is available now
                RecyclerviewFolderBrowserAdapter.AdapterItem.ImageItem.updateDownloadStatusByLocalFilePath(mItemData, localFilePath, true);
                //mGalleryAdapter.notifyDataSetChanged();
                int updatePosition = RecyclerviewFolderBrowserAdapter.AdapterItem.ImageItem.getPositionByLocalFilePath(mItemData, localFilePath);
                mRecyclerViewFolderBrowserAdapter.notifyItemChanged(updatePosition, null);
                //mGalleryAdapter.notifyDataSetChanged();
            }
            else {
                Log.e(TAG, "Showing downloaded file with identifier '" + identifier +"' failed. File is not existing or directory");
                if (!file.exists()) {
                    Log.e(TAG, "-->File is not existing");
                }
                if (!file.isFile()) {
                    Log.e(TAG, "-->Not a file");
                }
            }
        }
        else {
            Log.e(TAG, "Download Thumbnail with identifier failed: " + identifier);
        }
        mNextCloudWrapper.cleanOperations();
    }

    @Override
    public void onLoadPathData(String path) {
        if (mNextCloudWrapper == null) {
            Log.e(TAG, "Can't load cloud path. Nextcloud Wrapper is null");
            return;
        }
        // The new current path will be the one we load
        mCurrentPath = path;
        mNextCloudWrapper.startReadFolder(path, path, new Handler(), this);
    }


}
