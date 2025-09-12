package org.example.tujucloudbackend.api.sub;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.example.tujucloudbackend.exception.BusinessException;
import org.example.tujucloudbackend.exception.ErrorCode;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author : Yuan
 * @date :2025/9/11
 * 获取以图搜图页面地址（step 1）
 */
@Slf4j
public class GetImageFirstPageApi {

    // 常量配置（从curl命令中提取）
    private static final String API_URL = "https://graph.baidu.com/upload";
    private static final String URL_REGEX = "^(https?|ftp)://.*$";
    private static final Pattern URL_PATTERN = Pattern.compile(URL_REGEX);
    private static final int TIMEOUT = 10000; // 延长超时时间
    private static final int MAX_RETRY = 2;

    // 从curl中提取的关键头信息和Cookie
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36";
    private static final String REFERER = "https://graph.baidu.com/s?card_key=&entrance=GENERAL&extUiData%5BisLogoShow%5D=1&f=all&isLogoShow=1&session_id=9569122288169288930&sign=1265aab6da845bbd7887201757581585&tpl_from=pc";
    // Cookie从curl的-b参数中复制（注意保留原样，包括分号和空格）
    private static final String COOKIE = "BDUSS=WlheVR-dXdZeX53NElLUkpCMzFsU3BSQk4xZmh0RkRONUpmODRBUGFuYlJWb1JvSUFBQUFBJCQAAAAAAAAAAAEAAAAKCijXS2VkZ3VvAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAANHJXGjRyVxoO; BDUSS_BFESS=WlheVR-dXdZeX53NElLUkpCMzFsU3BSQk4xZmh0RkRONUpmODRBUGFuYlJWb1JvSUFBQUFBJCQAAAAAAAAAAAEAAAAKCijXS2VkZ3VvAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAANHJXGjRyVxoO; PSTM=1756137965; BIDUPSID=888C0DBC38322C6B369DD75B924432B1; BDORZ=B490B5EBF6F3CD402E515D22BCDA1598; BAIDUID=84DA408A6FBEBBF8A68269C5E1265AF5:FG=1; BAIDUID_BFESS=84DA408A6FBEBBF8A68269C5E1265AF5:FG=1; BA_HECTOR=a0212h8k0k0h2ga12504al810ka1801kc4kfv25; ZFY=KokIURYmiRqdkvtIqazOhom7Jz1P00:BviUiwWJu8TSE:C; H_PS_PSSID=63146_63324_63948_64450_64642_64820_64813_64817_64867_64876_64837_64901_64919_64953_64940_64966_64987_65004_65003_65078_65083_65122_65142_65139_65137_65186_65204_65235_65240; BDRCVFR[feWj1Vr5u3D]=I67x6TjHwwYf0; PSINO=7; delPer=0; antispam_key_id=23; BCLID=7230415358982653446; BCLID_BFESS=7230415358982653446; BDSFRCVID=Zp-OJeC627qSqgRskAxbEHtYop7IYE6TH6aohXM9IETfj3aLkyE7EG0nfM8g0Ku-S2-vogKK3gOTH4HILm-2tncgogNOM8Tw0h8ftf8g0x5; BDSFRCVID_BFESS=Zp-OJeC627qSqgRskAxbEHtYop7IYE6TH6aohXM9IETfj3aLkyE7EG0nfM8g0Ku-S2-vogKK3gOTH4HILm-2tncgogNOM8Tw0h8ftf8g0x5; H_BDCLCKID_SF=tRAOoC8ytDvjDb7GbKTD-tFO5eT22-usyj4L2hcH0KLKMPjbKtJCKPPF2M54Bn3ka54faxTNJfb1MRjvMh7YefFk3MjBJ-or2554_h5Ttnrt8DnTDMRhqqJXqqjyKMnitKv9-pP2WpTdDjC45-TjbP4sX4nuqp3ftI-O0tbDfn02JKKu-n5jHjj3jG_H3H; H_BDCLCKID_SF_BFESS=tRAOoC8ytDvjDb7GbKTD-tFO5eT22-usyj4L2hcH0KLKMPjbKtJCKPPF2M54Bn3ka54faxTNJfb1MRjvMh7YefFk3MjBJ-or2554_h5Ttnrt8DnTDMRhqqJXqqjyKMnitKv9-pP2WpTdDjC45-TjbP4sX4nuqp3ftI-O0tbDfn02JKKu-n5jHjj3jG_H3H; RT=\"z=1&dm=baidu.com&si=32aeaf5d-3642-4256-a881-7a8ae84ff768&ss=mff4h6nv&sl=0&tt=0&bcn=https%3A%2F%2Ffclog.baidu.com%2Flog%2Fweirwood%3Ftype%3Dperf&ul=2cb&hd=2cn\"; H_WISE_SIDS=63146_63324_63948_64450_64642_64820_64813_64817_64867_64876_64837_64901_64919_64953_64940_64966_64987_65004_65003_65078_65083_65122_65142_65139_65137_65186_65204_65235_65240; antispam_data=f89bd4e2d90e02e88241569ee8d562d00d4a3136f2c7706406551308ae9820cce05cf731f5284bf951a3ec4e36e9ed8ae4a97ceab8bccad34d207a2b22b4cc47430242cbd2bb9c6886e720ae9e9a578614dd1cf93f26dce615dd47cfa6dc3f95";
    // 从curl的-H参数中提取acs-token（注意时效性，若失效需从浏览器重新获取）
    private static final String ACS_TOKEN = "1757564385541_1757596680852_/a5Fnv7jisPEKbCaT/YEkGZSw+Zkx7MHVGvFntLq8OpTLrm2jz7tTGw8VDgB49vDwsTRSg4b9Ph9lEReOYA1UEFTQvPaOyzT3INJeFQc43J4K5XZGXGKFTtonpTWbWoYvYsgqDF5WVU5uRR71q6KzcuJTNfxrVAbxoIQWW4U5QFHLPPDyKMegljORyk2AEuRhaiqq0uyThjMheeSn8XT6IgMQ8/wNCw8hlvhFYGBOUhYUFkeqS9QFiQ02sNzLXW9oQpzofBzGJl6e3WcUbn9myqfe6/7mFoQeh9d+iEGhGGjAVTTz21g6xR/eQ7Ec2HiiZKHBFzZGHMdJ4qdhv9kZLQMM8Vh9KSpP6kab4pMwa5GNG1yf6LHzt/jcBCEzmGOtcR6+olfBm8D9I3AJ2fIfPDjmHvVLeByTQxqspPh6+XJDb5xhDr0b7s1UJAOAn1Y9GUPldcdRDJN3sV2GzgORg==";

    public static String getImageFirstUrl(String imageUrl) {
        // 1. 参数校验
        if (StrUtil.isBlank(imageUrl) || !URL_PATTERN.matcher(imageUrl).matches()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无效的图片URL: " + imageUrl);
        }

        // 2. 准备请求参数（包含curl中的所有表单字段，尤其是sdkParams）
        long upTime = System.currentTimeMillis();
        String requestUrl = API_URL + "?uptime=" + upTime;

        Map<String, Object> formData = new HashMap<>();
        formData.put("image", imageUrl);       // 图片URL
        formData.put("tn", "pc");              // 固定参数
        formData.put("from", "pc");            // 固定参数
        formData.put("image_source", "PC_UPLOAD_URL"); // 固定参数
        // 关键：添加sdkParams（从curl的--data-raw中复制，保持原样）
        formData.put("sdkParams", "{\"data\":\"f89bd4e2d90e02e88241569ee8d562d00d4a3136f2c7706406551308ae9820cce05cf731f5284bf951a3ec4e36e9ed8ae4a97ceab8bccad34d207a2b22b4cc47430242cbd2bb9c6886e720ae9e9a578614dd1cf93f26dce615dd47cfa6dc3f95\",\"key_id\":\"23\",\"sign\":\"95ab68ae\"}");

        // 3. 发起请求（带重试机制）
        int retryCount = 0;
        while (retryCount <= MAX_RETRY) {
            try {
                HttpResponse response = HttpRequest.post(requestUrl)
                        // 添加关键请求头（模拟浏览器）
                        .header("User-Agent", USER_AGENT)
                        .header("Referer", REFERER)
                        .header("acs-token", ACS_TOKEN)
                        .header("accept", "*/*")
                        .header("accept-language", "zh-CN,zh;q=0.9")
                        .header("x-requested-with", "XMLHttpRequest")
                        .header("sec-ch-ua", "\"Chromium\";v=\"140\", \"Not=A?Brand\";v=\"24\", \"Google Chrome\";v=\"140\"")
                        .header("sec-ch-ua-mobile", "?0")
                        .header("sec-ch-ua-platform", "\"Windows\"")
                        .header("sec-fetch-dest", "empty")
                        .header("sec-fetch-mode", "cors")
                        .header("sec-fetch-site", "same-origin")
                        // 携带Cookie（关键）
                        .cookie(COOKIE)
                        // 表单数据
                        .form(formData)
                        // 超时设置
                        .timeout(TIMEOUT)
                        .execute();

                log.info("请求URL: {}，响应状态码: {}", requestUrl, response.getStatus());

                // 4. 响应处理
                if (response.getStatus() != HttpStatus.HTTP_OK) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR,
                            "接口调用失败，状态码: " + response.getStatus() + "，响应内容: " + response.body());
                }

                String body = response.body();
                if (StrUtil.isBlank(body)) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口返回空内容");
                }

                JSONObject resultJson = JSONUtil.parseObj(body);
                if (resultJson == null) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "解析响应失败，内容: " + body);
                }

                int status = resultJson.getInt("status", -1);
                if (status != 0) {
                    String msg = resultJson.getStr("msg", "未知错误");
                    throw new BusinessException(ErrorCode.OPERATION_ERROR,
                            "接口返回错误，状态: " + status + "，信息: " + msg);
                }

                JSONObject dataJson = resultJson.getJSONObject("data");
                if (dataJson == null) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口未返回data字段");
                }

                String rawUrl = dataJson.getStr("url");
                if (StrUtil.isBlank(rawUrl)) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口未返回有效url");
                }
                return URLUtil.decode(rawUrl);

            } catch (BusinessException e) {
                // 业务异常直接抛出
                throw e;
            } catch (Exception e) {
                retryCount++;
                log.error("第{}次请求失败，原因: {}", retryCount, e.getMessage(), e);
                if (retryCount > MAX_RETRY) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR,
                            "达到最大重试次数(" + MAX_RETRY + ")，请求失败: " + e.getMessage());
                }
                // 重试间隔（指数退避）
                try {
                    Thread.sleep(1000 * retryCount);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        throw new BusinessException(ErrorCode.OPERATION_ERROR, "未获取到有效结果");
    }

    public static void main(String[] args) {
        String imageUrl = "xxx";
        String resultUrl = getImageFirstUrl(imageUrl);
        System.out.println("返回的结果Url:" + resultUrl);

    }

}
