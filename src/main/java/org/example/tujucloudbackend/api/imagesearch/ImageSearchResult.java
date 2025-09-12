package org.example.tujucloudbackend.api.imagesearch;

import lombok.Data;

/**
 * @author : Yuan
 * @date :2025/9/11
 */
@Data
public class ImageSearchResult {
    /**
     * 缩略图地址
     */
    private String thumbUrl;

    /**
     * 来源地址
     */
    private String fromUrl;
}
