package priv.howard.feignconsumer.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import priv.howard.feignconsumer.fallback.ServiceFallback;

@FeignClient(value = "NacosProvider", fallback = ServiceFallback.class)
public interface ProviderService {
    /**
     * @Description Feign服务接口，实现在服务NacosProvider中
     */
    @GetMapping("/test")
    String sayHello();

    @GetMapping("/test/port")
    String getPort();
}
