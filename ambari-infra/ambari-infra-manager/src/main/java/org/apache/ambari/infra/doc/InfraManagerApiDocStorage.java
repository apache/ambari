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
package org.apache.ambari.infra.doc;

import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.models.Swagger;
import io.swagger.util.Yaml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Named
public class InfraManagerApiDocStorage {

  private static final Logger LOG = LoggerFactory.getLogger(InfraManagerApiDocStorage.class);

  private final Map<String, Object> swaggerMap = new ConcurrentHashMap<>();

  @Inject
  private BeanConfig beanConfig;

  @PostConstruct
  private void postConstruct() {
    Thread loadApiDocThread = new Thread("load_swagger_api_doc") {
      @Override
      public void run() {
        LOG.info("Start thread to scan REST API doc from endpoints.");
        Swagger swagger = beanConfig.getSwagger();
        beanConfig.configure(swagger);
        beanConfig.scanAndRead();
        setSwagger(swagger);
        try {
          if (swagger != null) {
            String yaml = Yaml.mapper().writeValueAsString(swagger);
            StringBuilder b = new StringBuilder();
            String[] parts = yaml.split("\n");
            for (String part : parts) {
              b.append(part);
              b.append("\n");
            }
            setSwaggerYaml(b.toString());
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
        LOG.info("Scanning REST API endpoints and generating docs has been successful.");
      }
    };
    loadApiDocThread.setDaemon(true);
    loadApiDocThread.start();
  }

  public Swagger getSwagger() {
    return (Swagger) swaggerMap.get("swaggerObject");
  }

  public void setSwagger(final Swagger swagger) {
    swaggerMap.put("swaggerObject", swagger);
  }

  public void setSwaggerYaml(final String swaggerYaml) {
    swaggerMap.put("swaggerYaml", swaggerYaml);
  }

  public String getSwaggerYaml() {
    return (String) swaggerMap.get("swaggerYaml");
  }

}
