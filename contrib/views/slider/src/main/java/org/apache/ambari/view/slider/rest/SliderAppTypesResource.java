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

package org.apache.ambari.view.slider.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.view.ViewResourceHandler;

import com.google.inject.Inject;

public class SliderAppTypesResource {

	@Inject
	ViewResourceHandler resourceHandler;

	@GET
	@Produces({ MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON })
	public Response getApps(@Context HttpHeaders headers, @Context UriInfo uri) {
		return resourceHandler.handleRequest(headers, uri, null);
	}

	@GET
	@Path("{appTypeId}")
	@Produces({ MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON })
	public Response getApp(@Context HttpHeaders headers, @Context UriInfo uri,
	    @PathParam("appTypeId") String appTypeId) {
		return resourceHandler.handleRequest(headers, uri, appTypeId);
	}

}
