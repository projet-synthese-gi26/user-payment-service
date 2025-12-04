package com.yowyob.template.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class WebClientConfig {

//    @Bean
//    public StockApiClient stockApiClient(WebClient.Builder builder,
//                                         @Value("${application.external.stock-service-url}") String url) {
//
//        WebClient webClient = builder.baseUrl(url).build();
//        WebClientAdapter adapter = WebClientAdapter.create(webClient);
//        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
//
//        return factory.createClient(StockApiClient.class);
//    }
}