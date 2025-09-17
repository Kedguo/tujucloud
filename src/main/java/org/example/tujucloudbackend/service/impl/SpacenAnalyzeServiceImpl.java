package org.example.tujucloudbackend.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.tujucloudbackend.exception.BusinessException;
import org.example.tujucloudbackend.exception.ErrorCode;
import org.example.tujucloudbackend.exception.ThrowUtils;
import org.example.tujucloudbackend.mapper.SpaceMapper;
import org.example.tujucloudbackend.model.dto.space.analyze.*;
import org.example.tujucloudbackend.model.entity.Picture;
import org.example.tujucloudbackend.model.entity.Space;
import org.example.tujucloudbackend.model.entity.User;
import org.example.tujucloudbackend.model.vo.space.analyze.*;
import org.example.tujucloudbackend.service.PictureService;
import org.example.tujucloudbackend.service.SpaceAnalyzeService;
import org.example.tujucloudbackend.service.SpaceService;
import org.example.tujucloudbackend.service.UserService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author 行者
 * @description 针对表【space(空间)】的数据的分析
 */
@Service
public class SpacenAnalyzeServiceImpl extends ServiceImpl<SpaceMapper, Space> implements SpaceAnalyzeService {

    @Resource
    private SpaceService spaceService;

    @Resource
    private PictureService pictureService;

    @Resource
    private UserService userService;

    @Override
    public SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceUsageAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        //管理员可查看所有图库和空间
        if (spaceUsageAnalyzeRequest.isQueryPublic() || spaceUsageAnalyzeRequest.isQueryAll()) {
            checkSpaceAnalyzeAuth(spaceUsageAnalyzeRequest, loginUser);
            //查看图库
            QueryWrapper<Picture> pictureQueryWrapper = new QueryWrapper<>();
            pictureQueryWrapper.select("picSize");
            this.fillAnalyzeQueryWrapper(spaceUsageAnalyzeRequest, pictureQueryWrapper);

            List<Object> pictureObject = pictureService.getBaseMapper().selectObjs(pictureQueryWrapper);
            long usedSize = pictureObject.stream().mapToLong(obj -> (Long) obj).sum();
            long usedCount = pictureObject.size();

            SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
            spaceUsageAnalyzeResponse.setUsedSize(usedSize);
            spaceUsageAnalyzeResponse.setUsedCount(usedCount);
            //公共图库（或者全部空间）无数量和容量限制、也没有比例
            spaceUsageAnalyzeResponse.setMaxSize(null);
            spaceUsageAnalyzeResponse.setSizeUsageRatio(null);
            spaceUsageAnalyzeResponse.setMaxCount(null);
            spaceUsageAnalyzeResponse.setCountUsageRatio(null);
            return spaceUsageAnalyzeResponse;
        } else {
            //用户只能查看特定的数据
            Long spaceId = spaceUsageAnalyzeRequest.getSpaceId();
            ThrowUtils.throwIf(spaceId == null || spaceId <= 0, ErrorCode.PARAMS_ERROR);
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            checkSpaceAnalyzeAuth(spaceUsageAnalyzeRequest, loginUser);

            SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
            spaceUsageAnalyzeResponse.setUsedSize(space.getTotalSize());
            spaceUsageAnalyzeResponse.setUsedCount(space.getTotalCount());
            spaceUsageAnalyzeResponse.setMaxSize(space.getMaxSize());
            spaceUsageAnalyzeResponse.setMaxCount(space.getMaxCount());

            double sizeUsageRatio = NumberUtil.round(space.getTotalCount() * 100.0 / space.getMaxCount(), 2).doubleValue();
            double countUsageRatio = NumberUtil.round(space.getTotalCount() * 100.0 / space.getMaxCount(), 2).doubleValue();
            spaceUsageAnalyzeResponse.setSizeUsageRatio(sizeUsageRatio);
            spaceUsageAnalyzeResponse.setCountUsageRatio(countUsageRatio);
            return spaceUsageAnalyzeResponse;
        }
    }

    @Override
    public List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser) {
        checkSpaceAnalyzeAuth(spaceCategoryAnalyzeRequest, loginUser);
        QueryWrapper<Picture> pictureQueryWrapper = new QueryWrapper<>();
        pictureQueryWrapper.select("category", "count(*) as count", "sum(picSize) as totalSize").groupBy("category");

        return pictureService.getBaseMapper().selectMaps(pictureQueryWrapper).stream().map(result -> {
            String category = (String) result.get("category");
            Long count = ((Number) result.get("count")).longValue();
            Long totalSize = ((Number) result.get("totalSize")).longValue();
            return new SpaceCategoryAnalyzeResponse(category, count, totalSize);
        }).collect(Collectors.toList());
    }

    @Override
    public List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceTagAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        checkSpaceAnalyzeAuth(spaceTagAnalyzeRequest, loginUser);

        QueryWrapper<Picture> pictureQueryWrapper = new QueryWrapper<>();
        this.fillAnalyzeQueryWrapper(spaceTagAnalyzeRequest, pictureQueryWrapper);

        pictureQueryWrapper.select("tags");
        // 查询所有符合条件的标签
        List<String> tagsJsonList = pictureService.getBaseMapper().selectObjs(pictureQueryWrapper).stream().filter(ObjUtil::isNotEmpty).map(Object::toString).collect(Collectors.toList());
        //解析标签的统计
        Map<String, Long> tagCountMap = tagsJsonList.stream().flatMap(tagsJson -> JSONUtil.toList(tagsJson, String.class).stream()).collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));

        // 转换为响应对象，按照使用次数进行排序
        return tagCountMap.entrySet().stream().map(entry -> new SpaceTagAnalyzeResponse(entry.getKey(), entry.getValue())).collect(Collectors.toList());

    }

    @Override
    public List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceSizeAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        checkSpaceAnalyzeAuth(spaceSizeAnalyzeRequest, loginUser);
        QueryWrapper<Picture> pictureQueryWrapper = new QueryWrapper<>();
        this.fillAnalyzeQueryWrapper(spaceSizeAnalyzeRequest, pictureQueryWrapper);


        pictureQueryWrapper.select("picSize");
        List<Long> pictureSizes = pictureService.getBaseMapper().selectObjs(pictureQueryWrapper).stream().filter(ObjUtil::isNotEmpty).map(size -> (Long) size).collect(Collectors.toList());

        LinkedHashMap<String, Long> sizeRanges = new LinkedHashMap<>();
        sizeRanges.put("<100KB", pictureSizes.stream().filter(size -> size < 100 * 1024).count());
        sizeRanges.put("100KB-500KB", pictureSizes.stream().filter(size -> size >= 100 * 1024 && size < 500 * 1024).count());
        sizeRanges.put("500KB-1MB", pictureSizes.stream().filter(size -> size >= 500 * 1024 && size < 1 * 1024 * 1024).count());
        sizeRanges.put(">1MB", pictureSizes.stream().filter(size -> size >= 1 * 1024 * 1024).count());

        return sizeRanges.entrySet().stream().map(entry -> new SpaceSizeAnalyzeResponse(entry.getKey(), entry.getValue())).collect(Collectors.toList());

    }

    @Override
    public List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceUserAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        checkSpaceAnalyzeAuth(spaceUserAnalyzeRequest, loginUser);
        QueryWrapper<Picture> pictureQueryWrapper = new QueryWrapper<>();
        this.fillAnalyzeQueryWrapper(spaceUserAnalyzeRequest, pictureQueryWrapper);
        if (spaceUserAnalyzeRequest.getSpaceId() != null) {
            Space space = spaceService.getById(spaceUserAnalyzeRequest.getSpaceId());
            Long userId = space.getUserId();
            pictureQueryWrapper.eq(ObjUtil.isNotEmpty("userId"), "userId", userId);
        }

        String timeDimension = spaceUserAnalyzeRequest.getTimeDimension();
        switch (timeDimension) {
            case "day":
                pictureQueryWrapper.select("DATE_FORMAT(createTime, '%Y-%m-%d') as period", "count(*) as count");
                break;
            case "week":
                pictureQueryWrapper.select("YEARWEEK(createTime) as period", "count(*) as count");
                break;
            case "month":
                pictureQueryWrapper.select("DATE_FORMAT(createTime, '%Y-%m') as period", "count(*) as count");
                break;
            case "year":
                pictureQueryWrapper.select("DATE_FORMAT(createTime, '%Y') as period", "count(*) as count");
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的时间维度");
        }
        pictureQueryWrapper.groupBy("period").orderByAsc("period");

        List<Map<String, Object>> queryResult = pictureService.getBaseMapper().selectMaps(pictureQueryWrapper);
        return queryResult.stream().map(result -> {
            String period = result.get("period").toString();
            Long count = ((Number) result.get("count")).longValue();
            return new SpaceUserAnalyzeResponse(period, count);
        }).collect(Collectors.toList());
    }

    @Override
    public List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceRankAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        QueryWrapper<Space> spaceQueryWrapper = new QueryWrapper<>();
        QueryWrapper<Space> queryResult = spaceQueryWrapper.select("id", "spaceName", "totalCount", "totalSize").orderByDesc("totalCount", "totalSize").last("LIMIT " + spaceRankAnalyzeRequest.getTopN());
        // 查询并封装结果
        return spaceService.list(queryResult);
    }


    /**
     * 校验空间权限
     *
     * @param spaceAnalyzeRequest
     * @param loginUser
     */
    public void checkSpaceAnalyzeAuth(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        //管理员可查看所有空间和公共图库

        boolean queryPublic = spaceAnalyzeRequest.isQueryPublic();
        boolean queryAll = spaceAnalyzeRequest.isQueryAll();
        if (queryAll || queryPublic) {
            ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR);
        }

        //个人用户只能查看头顶的空间
        Long spaceId = spaceAnalyzeRequest.getSpaceId();
        if (spaceId != null) {
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            spaceService.checkSpaceAuth(loginUser, space);
        }
    }

    /**
     * 填充查询条件
     */
    public void fillAnalyzeQueryWrapper(SpaceAnalyzeRequest spaceAnalyzeRequest, QueryWrapper<Picture> queryWrapper) {
        ThrowUtils.throwIf(spaceAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        Long spaceId = spaceAnalyzeRequest.getSpaceId();
        boolean queryPublic = spaceAnalyzeRequest.isQueryPublic();
        boolean queryAll = spaceAnalyzeRequest.isQueryAll();
        //管理员查询图库
        if (queryPublic) {
            queryWrapper.isNull("spaceId");
            return;
        }
        //管理员全空间分析
        if (queryAll) {
            return;
        }
        //个人用户进行查询
        if (spaceId != null) {
            queryWrapper.eq("spaceId", spaceId);
        }

    }
}




