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
