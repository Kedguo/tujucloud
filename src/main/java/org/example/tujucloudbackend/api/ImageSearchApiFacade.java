package org.example.tujucloudbackend.api;

import org.example.tujucloudbackend.api.imagesearch.ImageSearchResult;
import org.example.tujucloudbackend.api.sub.GetImageFirstPageApi;
import org.example.tujucloudbackend.api.sub.GetImageFirstUrlApi;
import org.example.tujucloudbackend.api.sub.GetImageListApi;

import java.util.List;

/**
 * @author : Yuan
 * @date :2025/9/12
 */
public class ImageSearchApiFacade {

    // 图片搜索
    public static List<ImageSearchResult> searchImage(String imageUrl) {
        String imagePageUrl = GetImageFirstPageApi.getImageFirstUrl(imageUrl);
        String imageFirstUrl = GetImageFirstUrlApi.getImageFirstUrl(imagePageUrl);
        List<ImageSearchResult> imageSearchResults = GetImageListApi.getImageList(imageFirstUrl);
        return (imageSearchResults);
    }


    public static void main(String[] args) {
        List<ImageSearchResult> imageList = searchImage("xxxx");
        System.out.println("结果列表" + imageList);
    }

}
