package org.example.tujucloudbackend.api.aliyun;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.example.tujucloudbackend.api.aliyun.model.CreateOutPaintingTaskRequest;
import org.example.tujucloudbackend.api.aliyun.model.CreateOutTaskResponse;
import org.example.tujucloudbackend.api.aliyun.model.GetOutPaintingTaskResponse;
import org.example.tujucloudbackend.exception.BusinessException;
import org.example.tujucloudbackend.exception.ErrorCode;
import org.example.tujucloudbackend.exception.ThrowUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author : Yuan
 * @date :2025/9/15
 */
@Slf4j
@Component
public class AliyunAiApi {

    @Value("${aliyunAi.apikey}")
    private String  aliyunApiKey;

    //创建任务地址
    private String createTaskUrl = "https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/out-painting";
    //查询任务地址
    private String queryTaskUrl = "https://dashscope.aliyuncs.com/api/v1/tasks/%s";

    // 最大重试次数
    private static final int MAX_RETRIES = 3;
    // 重试间隔时间（毫秒）
    private static final long RETRY_INTERVAL = 1000;


    /**
     * 创建任务
     *
     * @param request
     * @return
     */
    public CreateOutTaskResponse createOutTask(CreateOutPaintingTaskRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        
        //发送请求
        HttpRequest httpRequest = HttpRequest.post(createTaskUrl)
                .header("Authorization", "Bearer " + aliyunApiKey)
                .header("X-DashScope-Async", "enable")
                .header("Content-Type", "application/json")
                .body(JSONUtil.toJsonStr(request));
                
        //处理响应（带重试机制）
        for (int i = 0; i <= MAX_RETRIES; i++) {
            try(HttpResponse httpResponse = httpRequest.execute()){
                if(httpResponse.isOk()){
                    CreateOutTaskResponse response = JSONUtil.toBean(httpResponse.body(), CreateOutTaskResponse.class);
                    if(response.getCode() == null){
                        return response;
                    } else {
                        log.error("请求异常：{}", response.getMessage());
                        if (i == MAX_RETRIES) {
                            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Ai扩图失败" + response.getMessage());
                        }
                    }
                } else {
                    log.error("请求异常：{}", httpResponse.body());
                    if (i == MAX_RETRIES) {
                        throw new BusinessException(ErrorCode.OPERATION_ERROR, "Ai扩图失败");
                    }
                }
            } catch (Exception e) {
                log.error("请求出现异常：{}", e.getMessage());
                if (i == MAX_RETRIES) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "Ai扩图失败");
                }
            }
            
            // 如果不是最后一次尝试，则等待后重试
            if (i < MAX_RETRIES) {
                try {
                    Thread.sleep(RETRY_INTERVAL);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "线程中断");
                }
            }
        }
        throw new BusinessException(ErrorCode.OPERATION_ERROR, "Ai扩图失败");

    }

    //查询结果
    public GetOutPaintingTaskResponse getOutTask(String taskId) {
        if (StrUtil.isBlank(taskId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "任务 ID 不能为空");
        }
        String getInfoUrl = String.format(queryTaskUrl, taskId);
        
        // 带重试机制的查询
        for (int i = 0; i <= MAX_RETRIES; i++) {
            try(HttpResponse response = HttpRequest.get(getInfoUrl)
                    .header("Authorization", "Bearer " + aliyunApiKey)
                    .execute()){
                if (response.isOk()) {
                    return JSONUtil.toBean(response.body(), GetOutPaintingTaskResponse.class);
                } else {
                    log.error("请求异常：{}", response.body());
                    if (i == MAX_RETRIES) {
                        throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取任务结果失败");
                    }
                }
            } catch (Exception e) {
                log.error("请求出现异常：{}", e.getMessage());
                if (i == MAX_RETRIES) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取任务结果失败");
                }
            }
            
            // 如果不是最后一次尝试，则等待后重试
            if (i < MAX_RETRIES) {
                try {
                    Thread.sleep(RETRY_INTERVAL);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "线程中断");
                }
            }
        }
        throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取任务结果失败");
    }
}
