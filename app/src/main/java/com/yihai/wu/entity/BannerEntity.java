package com.yihai.wu.entity;

import java.util.List;

/**
 * Created by ${Wu} on 2017/7/12.
 */

public class BannerEntity {


    private List<ImagesUrlBean> images_url;

    public List<ImagesUrlBean> getImages_url() {
        return images_url;
    }

    public void setImages_url(List<ImagesUrlBean> images_url) {
        this.images_url = images_url;
    }

    public static class ImagesUrlBean {
        /**
         * link : https://sxlogin.oss-cn-shenzhen.aliyuncs.com/home_images/2.jpg
         * connect_url : http://www.yihisxmini.com/productview/61.html
         */

        private String link;
        private String connect_url;

        public String getLink() {
            return link;
        }

        public void setLink(String link) {
            this.link = link;
        }

        public String getConnect_url() {
            return connect_url;
        }

        public void setConnect_url(String connect_url) {
            this.connect_url = connect_url;
        }
    }
}
