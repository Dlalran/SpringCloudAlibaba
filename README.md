[toc]

# Spring Cloud Alibaba

**官网：[Spring Cloud Alibaba](https://github.com/alibaba/spring-cloud-alibaba/blob/master/README-zh.md)**

---

## 加入依赖控制

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-alibaba-dependencies</artifactId>
            <version>2.1.0.RELEASE</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

---

## Nacos

​		**官网： [Nacos](https://nacos.io/zh-cn/)**

​		**一个集合了服务发现与管理、服务健康监测、动态配置和动态DNS服务等功能的平台。**

#### 配置并使用Nacos Server

1. 从[Nacos GitHub](https://github.com/alibaba/nacos/releases)中下载Nacos Server压缩包，解压并打开目录`nacos/bin/startup.cmd`启动Nacos服务中心
2. 通过`http://localhost:8848/nacos`访问服务中心，默认用户名密码均为nacos



#### 注册服务到Nacos

1. 添加Nacos服务发现依赖

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
```

2. 在配置文件中配置注册中心

```yml
server:
  port: 8081

spring:
  application:
    name: NacosProvider
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
```

3. 主配置类中加入注解`@EnableDiscoveryClient`启动服务发现功能，<u>**注意所有要注册到Nacos的服务都要添加该注解**</u>

4. 运行服务并在Nacos服务中心服务列表查看服务是否被成功注册



#### 服务监控

​	可以通过服务中心直接观察服务运行情况，也可以通过Spring Boot Actuator进行服务运行情况的监控，*Nacos Server中的服务元数据与通过Acturtor获得的相同*

1. 添加Spring Boot Actuator依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

2. 配置文件中启动所有的Spring Boot Actuator检测项(即暴露所有的端点Endpoints)

```yml
management:
  endpoints:
    web:
      exposure:
        include: "*"
```

3. Nacos向其提供了一个Endpoint，通过地址`http://服务地址/actuator/nacos-discovery`进行访问，可以获得包含该服务元数据的一个JSON



#### 原生调用服务

1. 主配置类中注入`RestTemplate`

```java
@Bean
public RestTemplate restTemplate() {
    return new RestTemplate();
}
```

2. Controller中使用`RestTemplate`对远程服务进行调用以及`LoadBalancerClient`实现负载均衡

   ​		`RestTemplate`用于以REST方式调用HTTP服务，`LoadBalancerClient`用于代替Ribbon进行负载均衡访问，以及通过服务注册名访问服务。

   ​		*Ribbon内部实际上是通过`RibbonLoadBalancerClient`对`LoadBalancerClient`进行实现*

   - 向Controller中分别注入`RestTemplate`和`LoadBalancerClient`

   ```java
   @Autowired
   private RestTemplate restTemplate;
   
   @Autowired
   private LoadBalancerClient loadBalancerClient;
   ```

   - 通过`LoadBalancerClient`获取服务实例，获取服务主机URL并拼接URL，再通过`RestTemplate`向服务发送请求

   ```java
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
   ```



#### Feign调用服务

​		使用OpenFeign服务进行接口式调用，其中还集成了Ribbon以实现负载均衡。*详细使用见<u>Spring Cloud Netfilx</u>笔记*

##### 	接口式调用

1. 加入OpenFeign依赖

   **注意版本要对应于使用Spring Cloud Alibaba的版本，此处为2.1.0，还要补充缺失的Netflix Archaius和Guava**

```xml
<!--        Feign(OpenFeign)-->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
            <version>2.1.0.RELEASE</version>
        </dependency>
<!--        Netflix Archaius-->
        <dependency>
            <groupId>com.netflix.archaius</groupId>
            <artifactId>archaius-core</artifactId>
            <version>0.7.6</version>
            <exclusions>
                <exclusion>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                </exclusion>
            </exclusions>
        </dependency>
<!--        Guava-->
        <dependency>
            <groupId>com.google.guava</groupId>
            <artifactId>guava</artifactId>
            <version>18.0</version>
        </dependency>
```

2. 主配置类中加入注解`@EnableFeignClients`启动Feign注解扫描
3. 编写服务接口，添加`@FeignClient`并在其中指定绑定的服务名

```java
package priv.howard.feignconsumer.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient("NacosProvider")
public interface ProviderService {
    /**
     * @Description Feign服务接口，实现在服务NacosProvider中
     */
    @GetMapping("/test")
    public String sayHello();
}
```

4. Controller中注入接口，并通过该接口进行服务调用

```java
@Autowired
private ProviderService providerService;

@GetMapping
public String getHello() {
    return providerService.sayHello();
}
```

##### 	负载均衡

​		Feign集成了Ribbon负载均衡，不需要额外配置，且默认采用的负载均衡策略是轮询，但也可以使用其他的负载均衡策略。

1. 在主配置类中注入指定负载均衡策略类Bean *详细策略类列表参考Spring Cloud Netflix - Ribbon 中的说明*

```java
// 注入指定负载均衡策略类，默认为轮询(RoundRobinRule)
@Bean
public IRule balanceRule() {
    return new RandomRule();
}
```

2. 运行多个服务提供者服务实例以模拟集群

   ​		修改每个实例的端口号，在IDEA的运行 - 编辑配置中点击服务提供者模块的"允许并行运行"选项，运行不同端口多个服务实例并通过使用Feign的服务消费者进行访问，查看端口号的变化来确定使用负载均衡的策略是否生效。



#### 服务配置

​		通过Nacos Server对服务进行配置来代替本地配置。*代替Spring Cloud Config*

##### 添加配置

- 在Nacos服务中心 - 配置管理 - 配置列表，点击右上角的加号添加配置，填写配置id(**注意要添加后缀，如yaml类型则可添加为.yml**)、组和配置内容(还可以在更多选项中指定配置归属应用)，保存后即将配置发布在服务配置中心。

##### 使用配置

1. 服务端添加Nacos Config依赖

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
</dependency>
```

2. 在<u>**bootstrap.yml**</u>中进行配置

   ​		**注意配置文件名的解析规则，详细见注释**

```yml
#此处配置的不是应用名，而是Nacos配置中心中除去后缀部分的配置ID，实际应用名在配置中指定
spring:
  application:
    name: NacosProvider-Config

  cloud:
    nacos:
      config:
#        配置文件后缀，不配置则默认为properties，
#        注意：实际检索的配置文件名为 上面指定的应用名.这里配置的后缀名
        file-extension: yml
#        Nacos Server地址
        server-addr: localhost:8848
```

##### 配置热部署

​		在Nacos Server并修改并发布配置后，配置内容会热部署到使用该配置的应用上，不需要重启服务(除端口号等特殊配置)。

​		**注意在服务中如Controller等组件使用`@Value`注入配置变量值时，需要在该类上添加`@RefreshScope`注解使得获取的配置值动态刷新**，原理是当远程配置修改事件触发时，会清空添加该注解的Bean的缓存并重新实例化Bean，同时添加该注解的Bean都会实现懒加载。

##### 多环境配置

​		Nacos Server的多环境配置基于SpringBoot的多Prodiles配置功能，通过在服务中指定使用的环境名来切换使用不同的远程应用配置文件。

1. 在Nacos配置中心中添加配置，命名为 配置id-环境名.yml

2. 在服务的`bootstrap.yml`配置文件中指定使用的环境 **注意此时检索的配置文件名，见注释**

```yml
#此例实际检索的配置文件名为 应用名-环境名.后缀名
spring:
#  指定使用的环境名
  profiles:
    active: dev
#  此处配置的不是应用名，而是Nacos配置中心中的配置ID，实际应用名在配置中指定
  application:
    name: nacosprovider-config

  cloud:
    nacos:
      config:
#        配置文件后缀，不配置则默认为properties，
        file-extension: yml
#        Nacos Server地址
        server-addr: localhost:8848
```

​		*`bootstrap.yml`本身也可以配置多环境，可以命名为`bootstrap-环境名.yml`，并在启动时在运行参数中指定使用的环境名，详细见SpringBoot的Demo项目。*

---

## Sentinel 

​		**官网：[Sentinel](https://github.com/alibaba/Sentinel/wiki/介绍)**

​		Sentinel 以流量为切入点，从流量控制、熔断降级、系统负载保护等多个维度保护服务的稳定性。*用于代替Netflix Hystrix*

#### 服务降级

1. 加入Sentinel依赖

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
</dependency>
```

2. 配置文件中开启Sentinel支持

```yml
feign:
  sentinel:
    enabled: true
```

3. 编写服务的降级类(即服务故障或熔断等不可用情况时的代替类)

```java
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
```

4. 在Feign服务接口的`@FeignClient`注解中指定使用的服务降级类

```java
@FeignClient(value = "NacosProvider", fallback = ServiceFallback.class)
```

5. 关闭服务提供者应用，查看是否能够得到服务降级类中返回的结果。



#### Sentinel控制台

1. 从[官网-Releases](https://github.com/alibaba/Sentinel/releases)上下载Sentinel Dashboard jar包

2. 通过以下命令启动Sentinel控制台

   ​		其中默认使用的端口是8080，详细启动参数说明见[官网Wiki-启动配置项](https://github.com/alibaba/Sentinel/wiki/启动配置项)

```shell
java -Dserver.port=8080 -Dcsp.sentinel.dashboard.server=localhost:8080 -Dproject.name=sentinel-dashboard -jar sentinel-dashboard-1.7.1.jar
```

3. 在被监控服务配置文件中加入配置

```yml
    sentinel:
      transport:
#        指定该服务提供监控信息的端口
        port: 8719
#        指定控制台地址
        dashboard: localhost:8080
```

4. 登录控制台

   ​		通过启动命令中配置的地址访问控制台，默认用户名密码均为sentinel，点击对应的应用可以查看其监控数据。

   ​		**注意要在消费者服务启动并被访问或触发熔断后才会被Sentinel控制台监控，并且一段时间内的请求才会被记录并显示**

   ​		*如果对如Spring Cloud Gateway等的网关服务进行监控，在网关应用服务时添加环境变量`-Dcsp.sentinel.app.type=1`，可以进行路由映射管理、API分组管理等操作。*

---

## Spring Cloud Gateway

​		官网：[Spring Cloud Gateway](https://spring.io/projects/spring-cloud-gateway)

​		一个基于异步非阻塞的Web框架Spring WebFlux和应用程序框架Netty的微服务网关，采用响应式编程风格。

​		*代替Netflix Zuul*

#### 路由映射

1. 引入Gateway和Servlet依赖，**注意不要引入Spring Boot Web依赖，因为底层使用Netty，会与Tomcat冲突**

```xml
<!--        Spring Cloud Gateway-->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-gateway</artifactId>
            <version>${spring-cloud-alibaba.version}</version>
        </dependency>
<!--        Servlet-->
        <dependency>
            <groupId>javax.servlet</groupId>
            <artifactId>javax.servlet-api</artifactId>
        </dependency>
    </dependencies>
```

2. 配置文件中进行网关配置

   ​		基本配置解释见代码注释，其中路由规则的谓词(predicates)项详细说明见[路由规则介绍](https://www.jianshu.com/p/d2c3b6851e1d?utm_source=desktop&utm_medium=timeline)

```yml
spring:
  application:
    name: SpringCloudGateway
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
    gateway:
#      开启基于服务发现的根据服务名转发的路由策略
      discovery:
        locator:
          enabled: true
#      配置路由规则
      routes:
#          路由映射名
        - id: feign_consumer
#          路由映射地址，采用LoadBalancerClient方式转发时，则配置为lb://服务注册名
          uri: lb://FeignConsumer
#          路由谓词，即拦截并转发的判断条件，例如请求路径、方式、时间等
          predicates:
            - Method=GET,POST
```

3. 启动网关并通过`http://网关地址/服务名/接口URL`访问服务，测试网关转发是否成功

​        *通过Sentinel可以对网关的路由映射链路进行管理，网关启动时添加环境参数`-Dcsp.sentinel.app.type=1`，并在Sentinel控制台 网关服务名-请求链路 中进行查看*



#### 过滤器

##### 服务过滤器

​		Spring CLoud Gateway自带的过滤器：[官方文档](https://cloud.spring.io/spring-cloud-gateway/2.1.x/single/spring-cloud-gateway.html#_gatewayfilter_factories)，[第三方文档](https://www.jianshu.com/p/17bbc8e10545)

- 在对应的服务路由规则(routes)内添加过滤器

​		此处使用的是AddRequestHeader添加请求头过滤器，请求头名为X-Request-Foo，值为Bar。

```xml
filters:
        - AddRequestHeader=X-Request-Foo, Bar
```

##### 全局过滤器		

​		全局过滤器使得网关可以在对请求进行分发前，对请求进行权限认证、IP访问限制等操作，并决定予以放行或拒绝。

1. 编写自定义全局过滤器

​		如下是一个基于参数的权限判定过滤器，需要实现GlobalFilter、Ordered接口，不需要其他配置即直接加入过滤器链并被使用，详细见代码注释。

```java
package priv.howard.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class AuthFilter implements GlobalFilter, Ordered {
    /**
     * @Description 基于WebFlux的鉴权过滤器，检查请求中是否有不为空的名为token的参数并决定是否放行
     * 需要实现GlobalFilter、Ordered接口并实现其中的方法
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
//        通过ServerWebExchange可以获取请求和响应的上下文，
//        获得的请求是ServerHttpRequest(相对于SpringMVC/Servlet的HttpServletRequest)，
//        并获取第一个名为token的参数值，进行权限判断
        String token = exchange.getRequest().getQueryParams().getFirst("token");

//        如果口令为空则拒绝请求并传回错误提示
        if (token == null || token.isEmpty()) {
//            获取响应类ServerHttpResponse
            ServerHttpResponse response = exchange.getResponse();

//            使用Guava新建HashMap并放入响应的数据(如错误提示)
            Map<String, Object> responseData = Maps.newHashMap();
            responseData.put("code", 401);
            responseData.put("message", "请求未授权");
            responseData.put("cause", "参数token为空");

            try {
//                将Map中的数据转换成字节流
                ObjectMapper objectMapper = new ObjectMapper();
                byte[] data = objectMapper.writeValueAsBytes(responseData);

//                将错误信息返回给页面
                DataBuffer buffer = response.bufferFactory().wrap(data);
//                设置响应状态码为401-Unauthorized(未授权)
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
//                设置响应格式为JSON
                response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
                return response.writeWith(Mono.just(buffer));

            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }

//        放行给下一个拦截器
        return chain.filter(exchange);
    }

//    指定过滤器的优先级(顺序)
    @Override
    public int getOrder() {
        return 0;
    }
}
```

2. 直接通过网关访问服务，查看是否获得返回的错误提示JSON，然后在URL中添加参数`?token=foo`再访问，测试是否成功访问

---

## SkyWalking

​		官网：[Apache SkyWalking](http://skywalking.apache.org/zh/)

​		分布式系统的应用程序性能监视工具，专为微服务、云原生架构和基于容器（Docker、K8s、Mesos）架构而设计。



---

## Dubbo

​		官网：[Apache Dubbo](http://dubbo.apache.org/zh-cn/index.html), [Dubbo GitHub](https://github.com/apache/dubbo)

​		**一种思想是内部服务间远程调用可以使用Dubbo进行RPC实现，外部的访问再通过REST(RestTemplate、Feign)进行实现。**

​		*此时的Dubbo仅作为RPC的一种实现，一般不使用其如负载均衡、服务降级等附加功能，因为这些功能将通过Spring Cloud Alibaba的组件进行实现。*

#### 统一依赖管理

​		创建统一的依赖管理模块，其中包含Spring Boot、Spring Cloud、Spring Cloud Alibaba、Dubbo的以及他们之间的依赖。

```xml
    <dependencyManagement>
        <dependencies>
<!--            Spring Boot BOM-->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
<!--            Spring Cloud BOM-->
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
<!--            Spring Cloud Alibaba BOM-->
            <dependency>
                <groupId>com.alibaba.cloud</groupId>
                <artifactId>spring-cloud-alibaba-dependencies</artifactId>
                <version>${spring-cloud-alibaba.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
<!--            Dubbo SpringBoot Starter-->
            <dependency>
                <groupId>org.apache.dubbo</groupId>
                <artifactId>dubbo-spring-boot-starter</artifactId>
                <version>${dubbo.version}</version>
            </dependency>
<!--            Dubbo-->
            <dependency>
                <groupId>org.apache.dubbo</groupId>
                <artifactId>dubbo</artifactId>
                <version>${dubbo.version}</version>
                <exclusions>
                    <exclusion>
                        <groupId>org.springframework</groupId>
                        <artifactId>spring</artifactId>
                    </exclusion>
                    <exclusion>
                        <groupId>javax.servlet</groupId>
                        <artifactId>servlet-api</artifactId>
                    </exclusion>
                    <exclusion>
                        <groupId>log4j</groupId>
                        <artifactId>log4j</artifactId>
                    </exclusion>
                </exclusions>
            </dependency>
<!--            Dubbo Actuator-->
            <dependency>
                <groupId>org.apache.dubbo</groupId>
                <artifactId>dubbo-spring-boot-actuator</artifactId>
                <version>${dubbo.version}</version>
            </dependency>
<!--            Alibaba Spring Context Support-->
            <dependency>
                <groupId>com.alibaba.spring</groupId>
                <artifactId>spring-context-support</artifactId>
                <version>${alibaba-spring-context-support.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
```



#### 服务接口

​		不同于Feign中的声明式服务接口仅提供给所在的服务使用，使用Dubbo时，接口提供给消费者以及服务提供者本身使用，因此建议将其独立化作为一个模块。

- 编写接口，示例如下

```java
package priv.howard.dubboprovider.api;

public interface ProviderService {
    /**
     * @Description 服务的API接口，基于Dubbo使用
     */
    String sayHello(String msg);
}
```



#### 服务提供者

1. 加入依赖

   ​		**如果是模块化开发，注意还要加入Spring Boot以及服务接口的依赖**

```xml
<!--        Dubbo-->
        <dependency>
            <groupId>org.apache.dubbo</groupId>
            <artifactId>dubbo</artifactId>
        </dependency>
        <dependency>
            <groupId>org.apache.dubbo</groupId>
            <artifactId>dubbo-spring-boot-starter</artifactId>
        </dependency>
<!--        Dubbo Nacos-->
        <dependency>
            <groupId>org.apache.dubbo</groupId>
            <artifactId>dubbo-registry-nacos</artifactId>
        </dependency>
<!--        Nacos Client-->
        <dependency>
            <groupId>com.alibaba.nacos</groupId>
            <artifactId>nacos-client</artifactId>
        </dependency>
<!--        Alibaba Spring Context Support-->
        <dependency>
            <groupId>com.alibaba.spring</groupId>
            <artifactId>spring-context-support</artifactId>
        </dependency>
<!--        服务接口-->
        <dependency>
            <groupId>priv.howard</groupId>
            <artifactId>DubboAPI</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
```

2. 配置文件中进行Dubbo相关配置

```yml
dubbo:
#  扫描Dubbo注解的包，已在主配置类注解中指定
  #  scan:
  #    base-packages: priv.howard.dubboprovider.service
#  使用的RPC协议以及端口，端口号可以设置为-1以自动分配，一般默认为20880
  protocol:
    name: dubbo
    port: 20880
#    注册中心的地址，使用Nacos代替Zookeeper
  registry:
    address: nacos://localhost:8848
```

3. 编写服务实现

​		实现服务接口，并添加**Dubbo提供的注解**`@Service`，使其可以被远程调用，

​		*其中interfaceName参数指定实现的接口名，version指定服务版本，均可省略，详细查看Dubbo的Demo项目*

```java
package priv.howard.dubboprovider.service;

import org.apache.dubbo.config.annotation.Service;
import priv.howard.dubboprovider.api.ProviderService;

@Service(interfaceName = "priv.howard.dubboprovider.api.ProviderService", version = "1.0.0")
public class ProviderServiceImpl implements ProviderService {
    @Override
    public String sayHello(String msg) {
        return "Hello, " + msg + "!";
    }
}
```

4. 在主配置类中添加注解指定扫描注解的包名(也可以在配置文件中指定，见2中的代码注释)

```java
@EnableDubbo(scanBasePackages = "priv.howard.dubboprovider.service")
```



#### 服务消费者

​		服务消费者使用Dubbo通过接口对服务提供者提供的服务进行远程调用，并注册在Nacos通过REST对外提供服务。

1. 添加依赖与服务提供者相同，**注意在添加Spring Boot和服务接口依赖之外还要额外添加Spring Boot Web**，需要监控服务状况则再添加Spring Boot Actuator和Dubbo Spring Boot Actuator

2. 配置文件也与服务提供者相同，以下额外给出服务监控相关的配置

```yml
dubbo:
  protocol:
    name: dubbo
    port: 20880
  registry:
    address: nacos://localhost:8848

#暴露Dubbo监控端点
endpoints:
  dubbo:
    enabled: true

management:
#  开启Dubbo服务健康监控
  health:
    dubbo:
      status:
        defaults: memory
        extras: load,threadpool
#  暴露所有监控端点
  endpoints:
    web:
      exposure:
        include: "*"
```

3. 访问服务消费者测试是否能够间接调用服务提供者的服务，还可以通过`http://服务提供者地址/acturtor`查看所有可以查看的Dubbo监控端点，如``http://服务提供者地址/acturtor/dubbo/configs``等



#### 高速序列化

​		Dubbo RPC核心的是一种高性能、高吞吐量的远程调用方式，消费者与提供者间采用单一多路复用的TCP长连接进行数据传输。Dubbo默认的序列化协议(不同于通信协议)是基于Netty + Hessain的序列化方式，支持的序列化协议包括针对Java语言的Kryo、FST等，跨语言的ProtoStuff、ProtoBuf、Thrift、Avro等、针对JSON的Fastjson等。

​		**因此需要注意，涉及Dubbo远程调用的实体类都要实现序列化接口(如`public class User implements Serializable`)**

​		这里使用其中性能最优之一的Kryo进行序列化，Dubbo对其进行了整合并提供了jar包，并且对于常用的Java类(如ArrayList、HashMap、Object[]等)进行了序列化注册，因此使用Kryo序列化时仅需要对自定义的实体类(DTO、DO、PO)实现序列化接口即可(即`implements Serializable`)。

1. 统一依赖控制中添加Dubbo Kryo依赖，并在服务提供者和消费者应用中添加该依赖

```xml
<dependency>
    <groupId>org.apache.dubbo</groupId>
    <artifactId>dubbo-serialization-kryo</artifactId>
    <version>${dubbo.version}</version>
</dependency>
```

2. 服务提供者和消费者中加入配置`dubbo.protocol.serialization=kryo`，使得两者分别通过Kryo进行序列化和反序列化



#### 负载均衡

​		Dubbo自带的负载均衡包括四种策略，默认的是Random，即基于权重的随机负载均衡策略；还有RoundRobin，即基于权重的轮询负载均衡策略；以及LeastActive，即最少活跃调用数优先的负载均衡策略；最后是ConsistentHash，即一致性哈希负载均衡策略，对于相同参数的请求总是分发到相同的提供者。

​		由于Nacos默认的是轮询策略，因此可以将Dubbo的策略也指定为轮询以保持整体策略一致。

- 对指定服务指定负载均衡策略，在服务实现的注解中指定，如`@Service(loadbalance = "roundrobin")`

- 对整个服务提供者应用指定负载均衡策略，在配置文件中加入`dubbo.provider.loadbalance=roundrobin`

  *其他的策略名为上述策略名的小写形式。*



#### Dubbo Admin

​		由于不再使用Zookeeper作为服务注册中心，因此需要在运行jar包来启动Dubbo控制台时指定运行参数。

- 运行dubbo-admin-server的jar包命令如下

```shell
java -jar -Dadmin.registry.address=nacos://127.0.0.1:8848 -Dadmin.config-center=nacos://127.0.0.1:8848 -Dadmin.metadata-report.address=nacos://127.0.0.1:8848 dubbo-admin-server-0.1.jar
```



​		*Dubbo还有许多其他功能，如服务降级(Mock)、集群容错、服务分组等，由于这里Dubbo仅作为RPC的实现，因此不予赘述，详细查看Dubbo的Spring Boot Demo项目内容。*
