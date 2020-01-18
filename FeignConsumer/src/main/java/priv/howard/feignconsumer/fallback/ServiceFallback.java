package priv.howard.feignconsumer.fallback;

import org.springframework.stereotype.Component;
import priv.howard.feignconsumer.service.ProviderService;

@Component
public class ServiceFallback implements ProviderService {
    /**
     * @Description ProviderService服务熔断或故障时的服务降级类，实现对应的Feign服务绑定接口
     */
    @Override
    public String sayHello() {
        return "Error";
    }

    @Override
    public String getPort() {
        return "Cannot get the port";
    }
}
