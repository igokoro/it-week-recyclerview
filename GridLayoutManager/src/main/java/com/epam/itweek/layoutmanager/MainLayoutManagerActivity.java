package com.epam.itweek.layoutmanager;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewTreeObserver;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.epam.itweek.layoutmanager.adapter.Config;
import com.epam.itweek.layoutmanager.adapter.FiveHundredPxRecyclerAdapter;
import com.epam.itweek.layoutmanager.adapter.FiveHundredPxService;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;


public class MainLayoutManagerActivity extends Activity {

    public static final String URL_FIVE_HUNDRED_PX = "https://api.500px.com";
    public static final String PARAM_CONSUMER_KEY = "consumer_key";
    private static final String TAG = MainLayoutManagerActivity.class.getSimpleName();
    public static final int NUMBER_OF_COLUMNS = 3;
    public static final int REVEAL_DURATION = 500;
    // very quick and simple and dirty caching across app/screen restarts
    // we can't do this in production of course
    private static List<FiveHundredPxService.Photo> cachedPhotos;

    @InjectView(android.R.id.list) RecyclerView recyclerView;

    @InjectView(android.R.id.progress) ProgressBar progressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_layout_manager);
        ButterKnife.inject(this);
        recyclerView.setHasFixedSize(true);
        // we always recycle at least one row, so keep the last recycled row in the first level cache
        recyclerView.setItemViewCacheSize(NUMBER_OF_COLUMNS);
        recyclerView.setLayoutManager(new GridLayoutManager(NUMBER_OF_COLUMNS));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        if (cachedPhotos == null) {
            fetchPhotos();
        } else {
            setup500pxAdapter(cachedPhotos);
        }
    }

    private void fetchPhotos() {
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(URL_FIVE_HUNDRED_PX)
                .setRequestInterceptor(new RequestInterceptor() {
                    @Override
                    public void intercept(RequestFacade request) {
                        request.addQueryParam(PARAM_CONSUMER_KEY, Config.FIVE_HUNDRED_PX_CONSUMER_KEY);
                    }
                }).build();
        FiveHundredPxService service = restAdapter.create(FiveHundredPxService.class);
        service.getPopularPhotos(new Callback<FiveHundredPxService.PhotosResponse>() {
            @Override
            public void success(FiveHundredPxService.PhotosResponse photosResponse, Response response) {
                cachedPhotos = photosResponse.photos;
                if (!isFinishing()) {
                    setup500pxAdapter(photosResponse.photos);
                }
            }

            @Override
            public void failure(RetrofitError error) {
                Log.w(TAG, "failed to load photos list from 500px", error);
                Toast.makeText(MainLayoutManagerActivity.this, "Failed to get data from the 500px. Please check internet connection and restart demo.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setup500pxAdapter(List<FiveHundredPxService.Photo> photos) {
        final FiveHundredPxRecyclerAdapter adapter = new FiveHundredPxRecyclerAdapter(photos, this);
        recyclerView.setAdapter(adapter);

        showRecyclerView();
    }

    private void showRecyclerView() {
        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        recyclerView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                int radius = Math.max(recyclerView.getWidth(), recyclerView.getHeight());
                ValueAnimator reveal = ViewAnimationUtils.createCircularReveal(recyclerView,
                        progressBar.getWidth() / 2 + progressBar.getLeft(),
                        progressBar.getTop() + progressBar.getHeight() / 2, 0, radius);
                reveal.setDuration(REVEAL_DURATION);
                reveal.start();
                recyclerView.getViewTreeObserver().removeOnPreDrawListener(this);
                return true;
            }
        });

    }

}
