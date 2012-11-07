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
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;

import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;

import com.google.inject.Inject;

@Path("/persist/")
public class PersistKeyValueService {
  private static PersistKeyValueImpl persistKeyVal;
  private static Log LOG = LogFactory.getLog(PersistKeyValueService.class);

  @Inject
  public static void init(PersistKeyValueImpl instance) {
    persistKeyVal = instance;
  }

  @POST
  @Produces("text/plain")
  public Response update(String keyValues,
      @Context HttpServletRequest req)
      throws WebApplicationException, InvalidStateTransitionException,
      JsonGenerationException, JsonMappingException, JAXBException, IOException {
    LOG.info("Received message from UI " + keyValues);
    Map<String, String> keyValuesMap = StageUtils.fromJson(keyValues, Map.class);
    /* Call into the heartbeat handler */

    for (Map.Entry<String, String> keyValue: keyValuesMap.entrySet()) {
      persistKeyVal.put(keyValue.getKey(), keyValue.getValue());
    }
    return Response.status(Response.Status.ACCEPTED).build();
  }
  
  @GET
  @Produces("text/plain")
  @Path("{keyName}")
  public String getKey( @PathParam("keyName") String keyName) {
    LOG.info("Looking for keyName " + keyName);
    return persistKeyVal.getValue(keyName);
  }
  
  @GET
  @Produces("text/plain")
  public String getAllKeyValues() throws JsonGenerationException,
    JsonMappingException, JAXBException, IOException {
    Map<String, String> ret = persistKeyVal.getAllKeyValues();
    String stringRet = StageUtils.jaxbToString(ret);
    LOG.info("Returning " + stringRet);
    return stringRet;
  }
}
