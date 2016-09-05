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

import org.apache.ambari.logsearch.query.converter.AnyGraphRequestConverter;
import org.apache.ambari.logsearch.query.converter.AuditBarGraphRequestConverter;
import org.apache.ambari.logsearch.query.converter.AuditLogRequestConverter;
import org.apache.ambari.logsearch.query.converter.BaseAuditLogRequestConverter;
import org.apache.ambari.logsearch.query.converter.BaseServiceLogRequestConverter;
import org.apache.ambari.logsearch.query.converter.FieldAuditLogRequestConverter;
import org.apache.ambari.logsearch.query.converter.FieldBarGraphRequestConverter;
import org.apache.ambari.logsearch.query.converter.ServiceAnyGraphRequestConverter;
import org.apache.ambari.logsearch.query.converter.ServiceExtremeDatesRequestConverter;
import org.apache.ambari.logsearch.query.converter.ServiceGraphRequestConverter;
import org.apache.ambari.logsearch.query.converter.ServiceLogExportRequestConverter;
import org.apache.ambari.logsearch.query.converter.ServiceLogFileRequestConverter;
import org.apache.ambari.logsearch.query.converter.ServiceLogRequestConverter;
import org.apache.ambari.logsearch.query.converter.ServiceLogTruncatedRequestConverter;
import org.apache.ambari.logsearch.query.converter.SimpleQueryRequestConverter;
import org.apache.ambari.logsearch.query.converter.UserExportRequestConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.convert.converter.Converter;

import java.util.HashSet;
import java.util.Set;

@Configuration
@ComponentScan("org.apache.ambari.logsearch")
@PropertySource(value = {"classpath:default.properties", "classpath:logsearch.properties"})
@ImportResource("META-INF/security-applicationContext.xml")
public class ApplicationConfig {

  @Bean
  public static PropertySourcesPlaceholderConfigurer propertyConfigurer() {
    return new PropertySourcesPlaceholderConfigurer();
  }

  @Bean(name="conversionService")
  public ConversionServiceFactoryBean conversionServiceFactoryBean() {
    ConversionServiceFactoryBean conversionServiceFactoryBean = new ConversionServiceFactoryBean();
    Set<Converter> converters = new HashSet<>();
    converters.add(new AnyGraphRequestConverter());
    converters.add(new AuditBarGraphRequestConverter());
    converters.add(new AuditLogRequestConverter());
    converters.add(new BaseAuditLogRequestConverter());
    converters.add(new BaseServiceLogRequestConverter());
    converters.add(new FieldAuditLogRequestConverter());
    converters.add(new FieldBarGraphRequestConverter());
    converters.add(new SimpleQueryRequestConverter());
    converters.add(new UserExportRequestConverter());
    converters.add(new ServiceAnyGraphRequestConverter());
    converters.add(new ServiceExtremeDatesRequestConverter());
    converters.add(new ServiceGraphRequestConverter());
    converters.add(new ServiceLogExportRequestConverter());
    converters.add(new ServiceLogFileRequestConverter());
    converters.add(new ServiceLogRequestConverter());
    converters.add(new ServiceLogTruncatedRequestConverter());
    conversionServiceFactoryBean.setConverters(converters);
    return conversionServiceFactoryBean;
  }

}
