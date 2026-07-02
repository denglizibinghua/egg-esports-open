package com.egg.esports.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.zip.GZIPInputStream;

/**
 * HTTP客户端配置
 * <p>开发环境通过 application.yml 配置代理访问外网 API；
 * 生产环境部署在海外服务器则无需代理，留空即可。
 * <p>注册 gzip 支持拦截器：Liquipedia API 强制要求 Accept-Encoding: gzip
 * （无该头返回 406），而 SimpleClientHttpRequestFactory（JDK HttpURLConnection）
 * 默认既不发送 Accept-Encoding: gzip 也不自动解压，故需显式处理。
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
