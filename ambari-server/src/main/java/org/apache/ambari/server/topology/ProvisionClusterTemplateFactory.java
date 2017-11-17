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
    return objectMapper.isEnabled(SerializationFeature.INDENT_OUTPUT);;
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
