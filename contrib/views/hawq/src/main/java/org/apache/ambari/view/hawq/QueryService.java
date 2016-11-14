/*
  Licensed to the Apache Software Foundation (ASF) under one
  or more contributor license agreements.  See the NOTICE file
  distributed with this work for additional information
  regarding copyright ownership.  The ASF licenses this file
  to you under the Apache License, Version 2.0 (the
  "License"); you may not use this file except in compliance
  with the License.  You may obtain a copy of the License at
  <p>
  http://www.apache.org/licenses/LICENSE-2.0
  <p>
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */
package org.apache.ambari.view.hawq;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.ViewResourceHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * The Query service.
 */
public class QueryService {

    private final static Logger LOG =
            LoggerFactory.getLogger(QueryResourceProvider.class);


    /**
     * The resource request handler.
     */
    @Inject
    ViewResourceHandler resourceHandler;

    /**
     * The view context.
     */
    @Inject
    ViewContext context;

    /**
     * Handles: GET /queries Get all queries.
     *
     * @param headers http headers
     * @param ui      uri info
     * @return query collection resource representation
     */
    @GET
    @Produces({"text/plain", "application/json"})
    public Response getQueries(@Context HttpHeaders headers, @Context UriInfo ui) {

        Response response = resourceHandler.handleRequest(headers, ui, null);
        LOG.debug("Response Entity = {}", response.getEntity());
        return response;
    }

}
