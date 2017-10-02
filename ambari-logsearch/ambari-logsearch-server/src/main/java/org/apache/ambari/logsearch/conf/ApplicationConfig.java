/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ambari.logsearch.conf;

import freemarker.template.TemplateException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;

import java.io.IOException;

import static org.apache.ambari.logsearch.common.LogSearchConstants.LOGSEARCH_PROPERTIES_FILE;

@Configuration
@PropertySource(value = {
  "classpath:default.properties",
  "classpath:info.properties",
  "classpath:"+ LOGSEARCH_PROPERTIES_FILE
  }
)
public class ApplicationConfig {

  @Bean
  public static PropertySourcesPlaceholderConfigurer propertyConfigurer() {
    return new PropertySourcesPlaceholderConfigurer();
  }

  @Bean(name="conversionService")
  public ConversionServiceFactoryBean conversionServiceFactoryBean() {
    ConversionServiceFactoryBean conversionServiceFactoryBean = new ConversionServiceFactoryBean();
    conversionServiceFactoryBean.afterPropertiesSet();
    return conversionServiceFactoryBean;
  }

  @Bean
  public freemarker.template.Configuration freemarkerConfiguration() throws IOException, TemplateException {
    FreeMarkerConfigurationFactoryBean factoryBean = new FreeMarkerConfigurationFactoryBean();
    factoryBean.setPreferFileSystemAccess(false);
    factoryBean.setTemplateLoaderPath("classpath:/templates");
    factoryBean.afterPropertiesSet();
    return factoryBean.getObject();
  }

}
