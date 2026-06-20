package com.egg.esports.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.net.Proxy;

/**
 * HTTP客户端配置
 * <p>开发环境通过 application.yml 配置代理访问外网 API；
 * 生产环境部署在海外服务器则无需代理，留空即可。
 */
@Configuration
public class RestTemplateConfig {

    @Value("${egg.http.proxy-host:}")
    private String proxyHost;

    @Value("${egg.http.proxy-port:0}")
    private int proxyPort;

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(30000);

        if (!proxyHost.isEmpty() && proxyPort > 0) {
            factory.setProxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort)));
        }

        return new RestTemplate(factory);
    }
}