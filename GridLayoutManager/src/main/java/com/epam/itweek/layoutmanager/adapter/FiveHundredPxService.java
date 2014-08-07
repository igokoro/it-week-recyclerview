package com.epam.itweek.layoutmanager.adapter;

import java.util.List;

import retrofit.Callback;
import retrofit.http.GET;

public interface FiveHundredPxService {

    @GET("/v1/photos?feature=popular&sort=rating&image_size=4&rpp=99")
    void getPopularPhotos(Callback<PhotosResponse> callback);

    static class PhotosResponse {
        public List<Photo> photos;
    }

    static class Photo {
        public int id;
        public String image_url;
        public String name;
        public User user;
    }

    static class User {
        public String fullname;
    }
}
