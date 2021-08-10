package com.acmen.flowable.config;


import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlowableConfig implements EngineConfigurationConfigurer<SpringProcessEngineConfiguration> {


    @Override
    public void configure(SpringProcessEngineConfiguration springProcessEngineConfiguration) {
        springProcessEngineConfiguration.setLabelFontName("宋体");
        springProcessEngineConfiguration.setActivityFontName("宋体");
        springProcessEngineConfiguration.setAnnotationFontName("宋体");
    }
}
