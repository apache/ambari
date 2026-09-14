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
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.JAXBException;

import org.apache.ambari.annotations.ApiIgnore;
import org.apache.ambari.server.security.authorization.AuthorizationException;
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
      JAXBException, IOException, AuthorizationException {
    LOG.debug("Received persisted key-value update");
    Map<String, String> keyValuesMap = StageUtils.fromJson(keyValues, Map.class);
    persistKeyVal.putLegacyValues(keyValuesMap);
    return Response.status(Response.Status.ACCEPTED).build();
  }

  @SuppressWarnings("unchecked")
  @PUT @ApiIgnore // until documented
  @Produces("text/plain")
  public String store(String values) throws IOException, JAXBException, AuthorizationException {
    LOG.debug("Received generated-key persisted value update");
    Collection<String> valueCollection = StageUtils.fromJson(values, Collection.class);
    Collection<String> keys = persistKeyVal.putLegacyGeneratedValues(valueCollection);
    String stringRet = StageUtils.jaxbToString(keys);
    return stringRet;
  }

  @GET
  @ApiIgnore
  @Path("scopes/{scopeType}/{scopeId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getScopedState(@PathParam("scopeType") String scopeType,
      @PathParam("scopeId") String scopeId,
      @QueryParam("summary") @DefaultValue("false") boolean summary) throws AuthorizationException {
    ScopedWorkflowState state = persistKeyVal.getScopedState(scopeType, scopeId, summary);
    return Response.ok(state.toResponse(summary), MediaType.APPLICATION_JSON_TYPE).build();
  }

  @GET
  @ApiIgnore
  @Path("scopes/drafts")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getCreationDraftDirectory() throws AuthorizationException {
    return Response.ok(persistKeyVal.getCreationDraftDirectory(), MediaType.APPLICATION_JSON_TYPE).build();
  }

  @GET
  @ApiIgnore
  @Path("scopes/drafts/{scopeId}/cluster")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getCreationDraftCluster(@PathParam("scopeId") String scopeId)
      throws AuthorizationException {
    return Response.ok(persistKeyVal.getCreationDraftCluster(scopeId),
        MediaType.APPLICATION_JSON_TYPE).build();
  }

  @PUT
  @ApiIgnore
  @Path("scopes/{scopeType}/{scopeId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response putScopedState(@PathParam("scopeType") String scopeType,
      @PathParam("scopeId") String scopeId, String requestBody) throws AuthorizationException {
    if (requestBody == null || requestBody.length() > PersistKeyValueImpl.MAX_SCOPED_REQUEST_BYTES
        || requestBody.getBytes(StandardCharsets.UTF_8).length > PersistKeyValueImpl.MAX_SCOPED_REQUEST_BYTES) {
      throw new WebApplicationException(Response.status(Response.Status.REQUEST_ENTITY_TOO_LARGE)
          .type(MediaType.APPLICATION_JSON_TYPE)
          .entity(Map.of("code", "WORKFLOW_STATE_TOO_LARGE",
              "message", "The workflow request exceeds the maximum persisted size"))
          .build());
    }
    ScopedWorkflowUpdate update;
    try {
      update = StageUtils.fromJson(requestBody, ScopedWorkflowUpdate.class);
    } catch (IOException e) {
      throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
          .type(MediaType.APPLICATION_JSON_TYPE)
          .entity(Map.of("code", "INVALID_WORKFLOW_STATE", "message", "Request body is not valid JSON"))
          .build());
    }
    ScopedWorkflowState state = persistKeyVal.putScopedState(scopeType, scopeId, update);
    return Response.ok(state.toResponse(false), MediaType.APPLICATION_JSON_TYPE).build();
  }

  @GET @ApiIgnore // until documented
  @Produces("text/plain")
  @Path("{keyName}")
  public String getKey( @PathParam("keyName") String keyName) throws AuthorizationException {
    LOG.debug("Looking for keyName {}", keyName);
    return persistKeyVal.getLegacyValue(keyName);
  }

  @GET @ApiIgnore // until documented
  @Produces("text/plain")
  public String getAllKeyValues() throws JAXBException, IOException {
    Map<String, String> ret = persistKeyVal.getAllLegacyKeyValues();
    String stringRet = StageUtils.jaxbToString(ret);
    return stringRet;
  }
}
