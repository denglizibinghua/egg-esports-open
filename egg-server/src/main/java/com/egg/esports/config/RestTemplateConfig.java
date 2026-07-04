package com.egg.esports.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

/**
 * HTTP客户端配置
 * <p>基于 Apache HttpClient5 连接池，替换默认的 JDK HttpURLConnection。
 * <p>开发环境通过 application.yml 配置代理访问外网 API；
 * 生产环境部署在海外服务器则无需代理，留空即可。
 * <p>注册 gzip 支持拦截器：Liquipedia API 强制要求 Accept-Encoding: gzip
 * （无该头返回 406），HttpClient5 默认不自动处理 gzip，故需显式处理。
 */
@Configuration
public class RestTemplateConfig {

    @Value("${egg.http.proxy-host:}")
    private String proxyHost;

    @Value("${egg.http.proxy-port:0}")
    private int proxyPort;

    @Bean
    public RestTemplate restTemplate() {
        // 连接池：maxTotal 控制总连接数，defaultMaxPerRoute 控制单 host 连接数
        // 20 总连接 / 10 单 host 足以支撑定时同步场景（每分钟一次，串行调用）
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(20);
        cm.setDefaultMaxPerRoute(10);

        // RequestConfig：连接超时 10s，响应超时 30s，可选代理
        RequestConfig.Builder configBuilder = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(10))
                .setResponseTimeout(Timeout.ofSeconds(30));
        if (!proxyHost.isEmpty() && proxyPort > 0) {
            // HttpClient5 代理通过 HttpHost 设置（替代 JDK java.net.Proxy）
            configBuilder.setProxy(new HttpHost(proxyHost, proxyPort));
        }
        RequestConfig config = configBuilder.build();

        CloseableHttpClient client = HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultRequestConfig(config)
                .build();

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(client);
        RestTemplate restTemplate = new RestTemplate(factory);
        // 注册 gzip 支持拦截器（对所有数据源安全：仅当响应 Content-Encoding: gzip 时才解压）
        restTemplate.getInterceptors().add(new GzipSupportInterceptor());
        return restTemplate;
    }

    /**
     * gzip 支持拦截器
     * <p>请求阶段：声明 Accept-Encoding: gzip（Liquipedia 强制要求，缺失返回 406）
     * <p>响应阶段：若 Content-Encoding 含 gzip，用 GZIPInputStream 包装 body 流解压
     * <p>对不返回 gzip 的 API（Riot/OpenDota/PandaScore/GRID）无副作用——
     * 仅在响应实际带 Content-Encoding: gzip 时才介入。
     */
    static class GzipSupportInterceptor implements ClientHttpRequestInterceptor {
        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                            ClientHttpRequestExecution execution) throws IOException {
            // 仅当未显式设置时追加 gzip，避免覆盖业务自定义的 Accept-Encoding
            if (!request.getHeaders().containsKey(HttpHeaders.ACCEPT_ENCODING)) {
                request.getHeaders().add(HttpHeaders.ACCEPT_ENCODING, "gzip");
            }
            ClientHttpResponse response = execution.execute(request, body);
            String encoding = response.getHeaders().getFirst(HttpHeaders.CONTENT_ENCODING);
            if (encoding != null && encoding.toLowerCase().contains("gzip")) {
                return new GzipDecompressingResponse(response);
            }
            return response;
        }
    }

    /**
     * 包装 gzip 响应：getBody() 返回解压流；移除 Content-Encoding/Content-Length 头，
     * 避免下游消息转换器按错误长度截断或重复解压。
     */
    static class GzipDecompressingResponse implements ClientHttpResponse {
        private final ClientHttpResponse delegate;
        private final InputStream decompressedBody;

        GzipDecompressingResponse(ClientHttpResponse delegate) throws IOException {
            this.delegate = delegate;
            // 包装原始输入流为 GZIP 解压流
            this.decompressedBody = new GZIPInputStream(delegate.getBody());
        }

        @Override
        public InputStream getBody() {
            return decompressedBody;
        }

        @Override
        public HttpHeaders getHeaders() {
            HttpHeaders headers = new HttpHeaders();
            headers.putAll(delegate.getHeaders());
            // 已解压：移除编码与长度头，防止下游重复处理
            headers.remove(HttpHeaders.CONTENT_ENCODING);
            headers.remove(HttpHeaders.CONTENT_LENGTH);
            return headers;
        }

        @Override
        public HttpStatusCode getStatusCode() throws IOException {
            return delegate.getStatusCode();
        }

        @Override
        public int getRawStatusCode() throws IOException {
            return delegate.getStatusCode().value();
        }

        @Override
        public String getStatusText() throws IOException {
            return delegate.getStatusText();
        }

        @Override
        public void close() {
            delegate.close();
        }
    }
}
