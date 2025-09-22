package org.example.tujucloudbackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.tujucloudbackend.exception.ErrorCode;
import org.example.tujucloudbackend.exception.ThrowUtils;
import org.example.tujucloudbackend.mapper.SpaceUserMapper;
import org.example.tujucloudbackend.model.dto.spaceuser.SpaceUserAddRequest;
import org.example.tujucloudbackend.model.dto.spaceuser.SpaceUserQueryRequest;
import org.example.tujucloudbackend.model.entity.Space;
import org.example.tujucloudbackend.model.entity.SpaceUser;
import org.example.tujucloudbackend.model.entity.User;
import org.example.tujucloudbackend.model.enums.SpaceRoleEnum;
import org.example.tujucloudbackend.model.vo.SpaceUserVO;
import org.example.tujucloudbackend.model.vo.SpaceVO;
import org.example.tujucloudbackend.model.vo.UserVO;
import org.example.tujucloudbackend.service.SpaceService;
import org.example.tujucloudbackend.service.SpaceUserService;
import org.example.tujucloudbackend.service.UserService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 行者
 * @description 针对表【space_user(空间用户关联)】的数据库操作Service实现
 * @createDate 2025-09-18 17:59:59
 */
@Service
public class SpaceUserServiceImpl extends ServiceImpl<SpaceUserMapper, SpaceUser>
        implements SpaceUserService {
    @Resource
    private UserService userService;

    @Lazy
    @Resource
    private SpaceService spaceService;


    @Override
    public void validSpaceUser(SpaceUser spaceUser, boolean add) {
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.PARAMS_ERROR);

        if (add) {
            ThrowUtils.throwIf(spaceUser.getSpaceId() == null, ErrorCode.PARAMS_ERROR);
            ThrowUtils.throwIf(spaceUser.getUserId() == null, ErrorCode.PARAMS_ERROR);
            User user = userService.getById(spaceUser.getUserId());
            ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
            Space space = spaceService.getById(spaceUser.getSpaceId());
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        }
        SpaceRoleEnum enumByValue = SpaceRoleEnum.getEnumByValue(spaceUser.getSpaceRole());
        ThrowUtils.throwIf(spaceUser.getSpaceRole() != null && enumByValue == null, ErrorCode.PARAMS_ERROR, "空间角色不存在");
    }

    @Override
    public long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest) {
        ThrowUtils.throwIf(spaceUserAddRequest == null, ErrorCode.PARAMS_ERROR);
        SpaceUser spaceUser = new SpaceUser();
        BeanUtil.copyProperties(spaceUserAddRequest, spaceUser);
        validSpaceUser(spaceUser, true);
        spaceUser.setCreateTime(new Date());
        boolean save = this.save(spaceUser);
        ThrowUtils.throwIf(!save, ErrorCode.OPERATION_ERROR, "新增空间成员失败");
        return spaceUser.getId();
    }

    @Override
    public SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request) {

        ThrowUtils.throwIf(spaceUser == null, ErrorCode.NOT_FOUND_ERROR);
        SpaceUserVO spaceUserVO = SpaceUserVO.objToVo(spaceUser);
        Long userId = spaceUser.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
            UserVO userVO = userService.getUserVO(user);
            spaceUserVO.setUser(userVO);
        }
        Long spaceId = spaceUser.getSpaceId();
        if (spaceId != null && spaceId > 0) {
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
            SpaceVO spaceVO = spaceService.getSpaceVO(space, request);
            spaceUserVO.setSpace(spaceVO);
        }
        return spaceUserVO;
    }

    @Override
    public List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList) {
        if (CollUtil.isEmpty(spaceUserList)) {
            return new ArrayList<>();
        }
        List<SpaceUserVO> spaceUserVOList = spaceUserList.stream().map(SpaceUserVO::objToVo).collect(Collectors.toList());

        Set<Long> userIdList = spaceUserList.stream().map(SpaceUser::getUserId).collect(Collectors.toSet());
        Set<Long> spaceIdList = spaceUserList.stream().map(SpaceUser::getSpaceId).collect(Collectors.toSet());

        Map<Long, List<User>> userIdListMap = userService.listByIds(userIdList).stream().collect(Collectors.groupingBy(User::getId));
        Map<Long, List<Space>> spaceIdListMap = spaceService.listByIds(spaceIdList).stream().collect(Collectors.groupingBy(Space::getId));

        spaceUserVOList.forEach(spaceUserVO -> {
            Long userId = spaceUserVO.getUserId();
            Long spaceId = spaceUserVO.getSpaceId();
            User user = null;
            if (userIdListMap.containsKey(userId)) {
                user = userIdListMap.get(userId).get(0);
                spaceUserVO.setUser(userService.getUserVO(user));
            }
            Space space = null;
            if (spaceIdListMap.containsKey(spaceId)) {
                space = spaceIdListMap.get(spaceId).get(0);
                spaceUserVO.setSpace(spaceService.getSpaceVO(space, null));
            }
        });

        return spaceUserVOList;

    }


    @Override
    public QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest) {
        QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
        if (spaceUserQueryRequest == null) {
            return queryWrapper;
        }
        queryWrapper.eq(spaceUserQueryRequest.getId() != null, "id", spaceUserQueryRequest.getId());
        queryWrapper.eq(spaceUserQueryRequest.getSpaceId() != null, "spaceId", spaceUserQueryRequest.getSpaceId());
        queryWrapper.eq(spaceUserQueryRequest.getUserId() != null, "userId", spaceUserQueryRequest.getUserId());
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceUserQueryRequest.getSpaceRole()), "spaceRole", spaceUserQueryRequest.getSpaceRole());
        return queryWrapper;
    }
}




