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
package org.apache.ambari.infra.conf;

import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.jaxrs.listing.SwaggerSerializers;
import io.swagger.models.Info;
import io.swagger.models.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InfraManagerApiDocConfig {

  private static final String DESCRIPTION = "Manager component for Ambari Infra";
  private static final String VERSION = "1.0.0";
  private static final String TITLE = "Infra Manager REST API";
  private static final String LICENSE = "Apache 2.0";
  private static final String LICENSE_URL = "http://www.apache.org/licenses/LICENSE-2.0.html";
  private static final String RESOURCE_PACKAGE = "org.apache.ambari.infra.rest";
  private static final String BASE_PATH = "/api/v1";

  @Bean
  public ApiListingResource apiListingResource() {
    return new ApiListingResource();
  }

  @Bean
  public SwaggerSerializers swaggerSerializers() {
    return new SwaggerSerializers();
  }

  @Bean
  public BeanConfig swaggerConfig() {
    BeanConfig beanConfig = new BeanConfig();
    beanConfig.setSchemes(new String[]{"http", "https"});
    beanConfig.setBasePath(BASE_PATH);
    beanConfig.setTitle(TITLE);
    beanConfig.setDescription(DESCRIPTION);
    beanConfig.setLicense(LICENSE);
    beanConfig.setLicenseUrl(LICENSE_URL);
    beanConfig.setScan(true);
    beanConfig.setVersion(VERSION);
    beanConfig.setResourcePackage(RESOURCE_PACKAGE);

    License license = new License();
    license.setName(LICENSE);
    license.setUrl(LICENSE_URL);

    Info info = new Info();
    info.setDescription(DESCRIPTION);
    info.setTitle(TITLE);
    info.setVersion(VERSION);
    info.setLicense(license);
    beanConfig.setInfo(info);
    return beanConfig;
  }
}
