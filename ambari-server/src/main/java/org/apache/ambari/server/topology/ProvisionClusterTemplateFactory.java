/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distribut
 * ed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.topology;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class ProvisionClusterTemplateFactory {

  private ObjectMapper objectMapper;

  public ProvisionClusterTemplateFactory() {
    createObjectMapper();
  }

  public boolean isPrettyPrintJson() {
    return objectMapper.isEnabled(SerializationFeature.INDENT_OUTPUT);
  }

  public void setPrettyPrintJson(boolean prettyPrintJson) {
    if (prettyPrintJson) {
      objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }
    else {
      objectMapper.disable(SerializationFeature.INDENT_OUTPUT);
    }
  }

  public ObjectMapper getObjectMapper() {
    return objectMapper;
  }

  private void createObjectMapper() {
    objectMapper = new ObjectMapper();
//    SimpleModule module = new SimpleModule("CustomModel", Version.unknownVersion());
//    SimpleAbstractTypeResolver resolver = new SimpleAbstractTypeResolver();
//    resolver.addMapping(HostGroupV2.class, HostGroupV2Impl.class);
//    module.setAbstractTypes(resolver);
//    objectMapper.registerModule(module);
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
  }

  public ProvisionClusterTemplate convertFromJson(String clusterTemplateJson) throws IOException {
    return objectMapper.readValue(clusterTemplateJson, ProvisionClusterTemplate.class);
  }

}
