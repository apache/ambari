/**
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
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.api.services;

import java.io.IOException;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;

import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackInfo;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.ser.FilterProvider;
import org.codehaus.jackson.map.ser.impl.SimpleBeanPropertyFilter;
import org.codehaus.jackson.map.ser.impl.SimpleFilterProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

@Path("/stacks/")
public class AmbariMetaService {
  private static AmbariMetaInfo ambariMetainfo;
  private static Logger LOG = LoggerFactory.getLogger(AmbariMetaService.class);

  @Inject
  public static void init(AmbariMetaInfo instance) {
    ambariMetainfo = instance;
  }

  /**
   * Filter properties from the service info and others
   * @param object
   * @return
   * @throws IOException
   * @throws JsonMappingException
   * @throws JsonGenerationException
   */
  public String filterProperties(Object object, boolean ignoreConfigs) throws
  JsonGenerationException, JsonMappingException, IOException {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
    mapper.configure(SerializationConfig.Feature.USE_ANNOTATIONS, true);
    if (ignoreConfigs) {
    FilterProvider filters = new SimpleFilterProvider().addFilter(
          "propertiesfilter",
          SimpleBeanPropertyFilter.serializeAllExcept("properties"));
      mapper.setFilters(filters);
    } else {
      FilterProvider filters = new SimpleFilterProvider().addFilter(
          "propertiesfilter", SimpleBeanPropertyFilter.serializeAllExcept());
      mapper.setFilters(filters);
    }
    String json = mapper.writeValueAsString(object);
    return json;
  }

  @GET
  @Produces("text/plain")
  public Response getStacks() throws JsonGenerationException,
  JsonMappingException, JAXBException, IOException {
    List<StackInfo> stackInfos = ambariMetainfo.getSupportedStacks();
    String output = filterProperties(stackInfos, true);
    return Response.status(Response.Status.OK).entity(output).build();
  }

  @GET
  @Path("{stackName}/version/{versionNumber}")
  @Produces("text/plain")
  public Response getStack(@PathParam("stackName") String stackName,
      @PathParam("versionNumber") String versionNumber) throws
      JsonGenerationException, JsonMappingException, JAXBException, IOException  {
    StackInfo stackInfo = ambariMetainfo.getStackInfo(stackName, versionNumber);
    String output = filterProperties(stackInfo, true);
    return Response.status(Response.Status.OK).entity(output).build();
  }

  @GET
  @Path("{stackName}/version/{versionNumber}/services/{serviceName}")
  @Produces("text/plain")
  public Response getServiceInfo(@PathParam("stackName") String stackName,
      @PathParam("versionNumber") String versionNumber,
      @PathParam("serviceName") String serviceName) throws
      JsonGenerationException, JsonMappingException, JAXBException, IOException  {
    ServiceInfo serviceInfo = ambariMetainfo.getServiceInfo(stackName,
        versionNumber, serviceName);
    String output = filterProperties(serviceInfo, false);
    return Response.status(Response.Status.OK).entity(output).build();
  }

}
