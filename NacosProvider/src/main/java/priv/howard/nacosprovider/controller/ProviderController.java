package priv.howard.nacosprovider.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

//添加@RefreshScope使得通过@Value获得的配置值会动态刷新
@RefreshScope
@RestController
@RequestMapping("/test")
public class ProviderController {
    /**
     * @Description 服务提供方Controller
     */

    @Value("${server.port}")
    private String port;

//    测试Nacos配置热部署
    @Value("${user.name}")
    private String userName;

    @GetMapping
    public String sayHello() {
        return "Hello Nacos!";
    }

    @GetMapping("/port")
    public String getPort() {
        return this.port;
    }

    @GetMapping("/username")
    public String getUserName() {
        return this.userName;
    }
}
