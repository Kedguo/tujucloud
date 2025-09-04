package org.example.tujucloudbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.tujucloudbackend.model.dto.picture.PictureQueryRequest;
import org.example.tujucloudbackend.model.dto.picture.PictureUploadRequest;
import org.example.tujucloudbackend.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import org.example.tujucloudbackend.model.entity.User;
import org.example.tujucloudbackend.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

/**
 * @author 行者
 * @description 针对表【picture(图片)】的数据库操作Service
 * @createDate 2025-09-03 01:07:20
 */
public interface PictureService extends IService<Picture> {

    //region 通用方法

    /**
     * 上传图片
     *
     * @param multipartFile        文件输入源
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    PictureVO uploadPicture(MultipartFile multipartFile,
                            PictureUploadRequest pictureUploadRequest,
                            User loginUser);

    /**
     * 获取查询对象
     *
     * @param pictureQueryRequest
     * @return
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /**
     * 获取图片包装类（单条）
     *
     * @param picture
     * @param request
     * @return
     */
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    /**
     * 获取图片包装类（分页）
     *
     * @param picturePage
     * @param request
     * @return
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    /**
     * 校验图片
     *
     * @param picture
     */
    void validPicture(Picture picture);

    //endregion





}
