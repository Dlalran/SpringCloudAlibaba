package priv.howard.nacosconsumer.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/test")
public class ConsumerController {
    /**
     * @Description 消费者Controller,通过RestTemplate调用远程服务
     */

    @Autowired
    private RestTemplate restTemplate;

    /**
     * 通过LoadBalancerClient代替Ribbon进行负载均衡访问，以及通过服务注册名访问服务
     * Ribbon内部实际上是通过RibbonLoadBalancerClient对LoadBalancerClient进行实现
     */
    @Autowired
    private LoadBalancerClient loadBalancerClient;

    @GetMapping
    public String getHello() {
        /**
         * 通过LoadBalancerClient的choose方法来获取指定服务注册名的服务实例的抽象ServiceInstance
         * 可以通过ServiceInstance获取如服务主机名、端口名、URL等元数据
         */
        ServiceInstance instance = loadBalancerClient.choose("NacosProvider");
//        1通过String.format使用服务提供者的主机名和端口号拼接URL
//        String url = String.format("http://%s:%s/test", instance.getHost(),instance.getPort());
//        2直接获取服务URL并拼接URL
        String url = instance.getUri().toString() + "/test";
        return restTemplate.getForObject(url, String.class);
    }
}
