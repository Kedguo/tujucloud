package org.example.tujucloudbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.example.tujucloudbackend.model.dto.space.SpaceAddRequest;
import org.example.tujucloudbackend.model.dto.space.SpaceQueryRequest;
import org.example.tujucloudbackend.model.entity.Space;
import org.example.tujucloudbackend.model.entity.User;
import org.example.tujucloudbackend.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;


/**
 * @author 行者
 * @deScription 针对表【Space(空间)】的数据库操作Service
 * @createDate 2025-09-08 10:33:14
 */
public interface SpaceService extends IService<Space> {

    //region 通用方法

    /**
     * 校验空间
     */
    void validSpace(Space space, Boolean add);

    /**
     * 获取查询对象
     *
     * @param SpaceQueryRequest
     * @return
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest SpaceQueryRequest);

    /**
     * 获取空间包装类（单条）
     *
     * @param space
     * @param request
     * @return
     */
    SpaceVO getSpaceVO(Space space, HttpServletRequest request);

    /**
     * 获取空间包装类（分页）
     *
     * @param spacePage
     * @param request
     * @return
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);

    /**
     * 根据空间级别填充空间对象
     *
     * @param space
     */
    void fillSpaceBySpaceLevel(Space space);
    //endregion

    /**
     * 创建空间
     *
     * @param spaceAddRequest
     * @param loginUser
     */
    long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);


    void checkSpaceAuth(User loginUser, Space space);
}
