package com.epam.itweek.layoutmanager.adapter;

import android.app.ActivityManager;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.epam.itweek.layoutmanager.R;
import com.squareup.picasso.LruCache;
import com.squareup.picasso.Picasso;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import hugo.weaving.DebugLog;

import static android.content.Context.ACTIVITY_SERVICE;
import static android.content.pm.ApplicationInfo.FLAG_LARGE_HEAP;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.HONEYCOMB;

public class FiveHundredPxRecyclerAdapter extends RecyclerView.Adapter<FiveHundredPxRecyclerAdapter.ViewHolder> {

    private List<FiveHundredPxService.Photo> photos;
    private Picasso picasso;

    public FiveHundredPxRecyclerAdapter(List<FiveHundredPxService.Photo> photos, Context ctx) {
        this.photos = photos;
        setHasStableIds(true);
        buildPicasso(ctx);
    }

    private void buildPicasso(Context ctx) {
        picasso = new Picasso.Builder(ctx)
                .indicatorsEnabled(true)
//                .loggingEnabled(true)
                .memoryCache(new LruCache(calculateMemoryCacheSize(ctx)))
                .build();
    }

    static int calculateMemoryCacheSize(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        boolean largeHeap = (context.getApplicationInfo().flags & FLAG_LARGE_HEAP) != 0;
        int memoryClass = am.getMemoryClass();
        if (largeHeap && SDK_INT >= HONEYCOMB) {
            memoryClass = am.getLargeMemoryClass();
        }
        // Target ~50% of the available heap.
        return 1024 * 1024 * memoryClass / 2;
    }

    @Override @DebugLog
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item, parent, false);
        return new ViewHolder(v);
    }

    @Override @DebugLog
    public void onBindViewHolder(ViewHolder holder, int position) {
        FiveHundredPxService.Photo photo = photos.get(position);
        picasso.load(photo.image_url)
                /*
                * By default Picasso can't center crop image to fit into target view - only scale which destroys aspect ratio.
                * Also we don't know actual target view size at binding time.
                * So we need a transformation that has access to a target view
                * and can deffer calculation of new image dimensions until we actually have both image and size of a target view.
                */
                .transform(new DeferredResizeTransformation(holder.image))
                .placeholder(R.color.primary).into(holder.image);
        holder.tapArea.setTag(photo);
    }

    @Override
    public long getItemId(int position) {
        return photos.get(position).id;
    }

    @Override
    public int getItemCount() {
        return photos.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        @InjectView(R.id.image) public ImageView image;
        @InjectView(R.id.tapArea) public View tapArea;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.inject(this, itemView);
        }

    }
}
