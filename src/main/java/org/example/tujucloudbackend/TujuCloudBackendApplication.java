package org.example.tujucloudbackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@MapperScan("org.example.tujucloudbackend.mapper")
@EnableAsync
@EnableAspectJAutoProxy(exposeProxy = true) //暴露当前代理对象
public class TujuCloudBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(TujuCloudBackendApplication.class, args);

    }

}
