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

import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.jaxrs.listing.SwaggerSerializers;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Configuration
public class ApiDocConfig {

  @Bean
  public ApiListingResource apiListingResource() {
    return new ApiListingResource();
  }

  @Bean
  public SwaggerSerializers swaggerSerializers() {
    return new SwaggerSerializers();
  }

  @Bean
  public BeanConfig swaggerConfig() throws UnknownHostException {
    BeanConfig beanConfig = new BeanConfig();
    beanConfig.setSchemes(new String[]{"http", "https"});
    beanConfig.setHost(InetAddress.getLocalHost().getHostAddress() + ":61888"); // TODO: port from property
    beanConfig.setBasePath("/api/v1");
    beanConfig.setTitle("Log Search REST API");
    beanConfig.setDescription("Log aggregation, analysis, and visualization.");
    beanConfig.setLicense("Apache 2.0");
    beanConfig.setLicenseUrl("http://www.apache.org/licenses/LICENSE-2.0.html");
    beanConfig.setScan(true);
    beanConfig.setVersion("1.0.0");
    beanConfig.setResourcePackage("org.apache.ambari.logsearch.rest");
    return beanConfig;
  }
}
