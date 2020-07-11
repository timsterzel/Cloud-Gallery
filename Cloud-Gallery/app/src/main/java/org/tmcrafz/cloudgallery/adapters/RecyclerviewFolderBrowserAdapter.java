package org.tmcrafz.cloudgallery.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.tmcrafz.cloudgallery.R;
import org.tmcrafz.cloudgallery.ui.ShowPicturesActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

public class RecyclerviewFolderBrowserAdapter extends RecyclerView.Adapter {

    public interface OnLoadFolderData {
        void onLoadPathData(String path);
    }

    private static String TAG = RecyclerviewFolderBrowserAdapter.class.getCanonicalName();

    private OnLoadFolderData mContext;
    private ArrayList<AdapterItem> mData;


    public RecyclerviewFolderBrowserAdapter(OnLoadFolderData context, ArrayList<AdapterItem> pathData) {
        mContext = context;
        mData = pathData;
    }

    @Override
    public int getItemViewType(int position) {
        return mData.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        switch (viewType) {
            case AdapterItem.TYPE_FOLDER:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_folder_entry_line, parent, false);
                return new FolderTypeViewHolder(view);
            case AdapterItem.TYPE_IMAGE:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_image_entry_line, parent, false);
                return new ImageTypeViewHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        AdapterItem data = mData.get(position);
        switch (data.type) {
            case AdapterItem.TYPE_FOLDER:
                AdapterItem.FolderItem folderData = (AdapterItem.FolderItem) data;
                ((FolderTypeViewHolder) holder).mTextFolderName.setText(folderData.name);
                final String folderPath = folderData.path;
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                                                   @Override
                                                   public void onClick(View v) {
                                                       ((OnLoadFolderData) mContext).onLoadPathData(folderPath); }
                                               }
                );
                ((FolderTypeViewHolder) holder).mButtonShow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Context context = v.getContext();
                    Intent intent = new Intent(context, ShowPicturesActivity.class);
                    intent.putExtra(ShowPicturesActivity.EXTRA_PATH_TO_SHOW, folderPath);
                    context.startActivity(intent);
                    }
                });
                break;
            case AdapterItem.TYPE_IMAGE:
                AdapterItem.ImageItem imageData = (AdapterItem.ImageItem) data;
                String imagePath = imageData.localFilePath;
                File file = new File(imagePath);

                // Show when media is not already loaded in view and file is downloaded
                //if (!holder.mIsLoaded && mMediaPaths.getItem(position).isLocalFileDownloaded) {
                if (file.exists() && file.isFile()) {
                    String imagename = file.getName();
                    ((ImageTypeViewHolder) holder).mTextView.setText(imagename);
                    ((ImageTypeViewHolder) holder).mImagePreview.setImageBitmap(BitmapFactory.decodeFile(imagePath));
//            Glide.with(mContext)
//                    .load(new File(path)) // Uri of the picture
//                    .into(holder.mImagePreview);
                }
                // Else show placeholder
                else {
                    ((ImageTypeViewHolder) holder).mImagePreview.setImageResource(R.drawable.ic_launcher_foreground);
//            Glide.with(mContext)
//                    .load(R.drawable.ic_launcher_foreground) // Uri of the picture
//                    .into(holder.mImagePreview);
                }
                break;
        }
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public static class FolderTypeViewHolder extends RecyclerView.ViewHolder {
        //public ImageView mImagePreview;
        public TextView mTextFolderName;
        public Button mButtonShow;
        //public boolean mIsLoaded = false;

        public FolderTypeViewHolder(@NonNull View itemView) {
            super(itemView);
            mTextFolderName = itemView.findViewById(R.id.textView_image_name);
            mButtonShow = itemView.findViewById(R.id.button_show);
        }
    }

    public static class ImageTypeViewHolder extends RecyclerView.ViewHolder {

        public ImageView mImagePreview;
        public TextView mTextView;
        //public boolean mIsLoaded = false;

        public ImageTypeViewHolder(@NonNull View itemView) {
            super(itemView);
            mImagePreview = itemView.findViewById(R.id.imageView_preview);
            mTextView = itemView.findViewById(R.id.textView_image_name);
        }
    }

    // ToDo: Encapsulate from Adapter
    public static class AdapterItem {
        public static class ImageItem extends AdapterItem{
            // The directory where the downloaded item is stored
            public String localDirectory;
            public String localFilePath;
            public String remotePath;
            public boolean isLocalFileDownloaded;

            public ImageItem(int type, String localDirectory, String localFilePath, String remotePath, boolean isLocalFileDownloaded) {
                super(type);
                this.localDirectory = localDirectory;
                this.localFilePath = localFilePath;
                this.remotePath = remotePath;
                this.isLocalFileDownloaded = isLocalFileDownloaded;
            }

            // ToDo: encapsulate all the static methods
            public static boolean updateDownloadStatusByLocalFilePath(ArrayList<AdapterItem> items, String localFilePath, boolean isLocalFileDownloaded) {
                for (AdapterItem item : items) {
                    if (item.type != AdapterItem.TYPE_IMAGE) {
                        continue;
                    }
                    ImageItem imageItem = (ImageItem) item;
                    if (imageItem.localFilePath.equals(localFilePath)) {
                        imageItem.isLocalFileDownloaded = isLocalFileDownloaded;
                        return true;
                    }
                }
                return false;
            }

            public static int getPositionByLocalFilePath(ArrayList<AdapterItem> items, String localFilePath) {
                for (int i = 0; i < items.size(); i++) {
                    AdapterItem item = items.get(i);
                    if (item == null) {
                        continue;
                    }
                    if (item.type != AdapterItem.TYPE_IMAGE) {
                        continue;
                    }
                    ImageItem imageItem = (ImageItem) item;
                    if (imageItem.localFilePath.equals(localFilePath)) {
                        return i;
                    }
                }
                return -1;
            }


            public static void removeByLocalFilePath(ArrayList<AdapterItem> items, String localFilePath) {
                Iterator<AdapterItem> itemIterator = items.iterator();
                while (itemIterator.hasNext()) {
                    AdapterItem item = itemIterator.next();
                    if (item.type != AdapterItem.TYPE_IMAGE) {
                        continue;
                    }
                    ImageItem imageItem = (ImageItem) item;
                    if (imageItem.localFilePath.equals(localFilePath)) {
                        itemIterator.remove();
                    }
                }
            }

            public static void removeLocalFilePaths(ArrayList<AdapterItem> items, HashSet<String> localFilePaths) {
                Iterator<AdapterItem> itemIterator = items.iterator();
                while (itemIterator.hasNext()) {
                    AdapterItem item = itemIterator.next();
                    if (item.type != AdapterItem.TYPE_IMAGE) {
                        continue;
                    }
                    ImageItem imageItem = (ImageItem) item;
                    if (localFilePaths.contains(imageItem.localFilePath)) {
                        itemIterator.remove();
                    }
                }
            }
        }

        public static class FolderItem extends AdapterItem {
            public String name;
            public String path;

            public FolderItem(int type, String name, String path) {
                super(type);
                this.name = name;
                this.path = path;
            }
        }
        public static final int TYPE_FOLDER = 0;
        public static final int TYPE_IMAGE = 1;

        public int type;

        public AdapterItem(int type) {
            this.type = type;
        }
    }

}
