package com.yihai.wu.util;

import com.yihai.wu.entity.BannerEntity;

import retrofit2.Call;
import retrofit2.http.GET;

/**
 * Created by ${Wu} on 2017/7/12.
 */

public interface RetrofitService {

    @GET(Constant.BANNER_IMAGES)
    Call<BannerEntity> getBannerImages();


}
