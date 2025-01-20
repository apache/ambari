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
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.api.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import javax.xml.bind.JAXBException;

import org.apache.ambari.annotations.ApiIgnore;
import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;
import org.apache.ambari.server.utils.StageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

@Path("/persist/")
public class PersistKeyValueService {
  private static PersistKeyValueImpl persistKeyVal;
  private static final Logger LOG = LoggerFactory.getLogger(PersistKeyValueService.class);

  @Inject
  public static void init(PersistKeyValueImpl instance) {
    persistKeyVal = instance;
  }

  @SuppressWarnings("unchecked")
  @POST @ApiIgnore // until documented
  @Produces("text/plain")
  public Response update(String keyValues)
      throws WebApplicationException, InvalidStateTransitionException,
      JAXBException, IOException {
    LOG.debug("Received message from UI {}", keyValues);
    Map<String, String> keyValuesMap = StageUtils.fromJson(keyValues, Map.class);
    /* Call into the heartbeat handler */

    for (Map.Entry<String, String> keyValue: keyValuesMap.entrySet()) {
      persistKeyVal.put(keyValue.getKey(), keyValue.getValue());
    }
    return Response.status(Response.Status.ACCEPTED).build();
  }

  @SuppressWarnings("unchecked")
  @PUT @ApiIgnore // until documented
  @Produces("text/plain")
  public String store(String values) throws IOException, JAXBException {
    LOG.debug("Received message from UI {}", values);
    Collection<String> valueCollection = StageUtils.fromJson(values, Collection.class);
    Collection<String> keys = new ArrayList<>(valueCollection.size());
    for (String s : valueCollection) {
      keys.add(persistKeyVal.put(s));
    }
    String stringRet = StageUtils.jaxbToString(keys);
    LOG.debug("Returning {}", stringRet);
    return stringRet;
  }

  @GET @ApiIgnore // until documented
  @Produces("text/plain")
  @Path("{keyName}")
  public String getKey( @PathParam("keyName") String keyName) {
    LOG.debug("Looking for keyName {}", keyName);
    return persistKeyVal.getValue(keyName);
  }

  @GET @ApiIgnore // until documented
  @Produces("text/plain")
  public String getAllKeyValues() throws JAXBException, IOException {
    Map<String, String> ret = persistKeyVal.getAllKeyValues();
    String stringRet = StageUtils.jaxbToString(ret);
    LOG.debug("Returning {}", stringRet);
    return stringRet;
  }
}
