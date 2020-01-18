package priv.howard.feignconsumer.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import priv.howard.feignconsumer.service.ProviderService;

@RestController
@RequestMapping("/test")
public class ConsumerController {
    /**
     * @Description 通过Feign服务接口来访问远程服务
     */

    @Autowired
    private ProviderService providerService;

    @GetMapping
    public String getHello() {
        return providerService.sayHello();
    }

    @GetMapping("/port")
    public String getPort() {
        return providerService.getPort();
    }
}
