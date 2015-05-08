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

package org.apache.ambari.view.hive.resources.udfs;

import org.apache.ambari.view.ViewResourceHandler;
import org.apache.ambari.view.hive.BaseService;
import org.apache.ambari.view.hive.persistence.utils.ItemNotFound;
import org.apache.ambari.view.hive.persistence.utils.OnlyOwnersFilteringStrategy;
import org.apache.ambari.view.hive.resources.resources.FileResourceResourceManager;
import org.apache.ambari.view.hive.utils.NotFoundFormattedException;
import org.apache.ambari.view.hive.utils.ServiceFormattedException;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;

/**
 * Servlet for UDFs
 * API:
 * GET /:id
 *      read udf
 * POST /
 *      create new udf
 * GET /
 *      get all udf of current user
 */
public class UDFService extends BaseService {
  @Inject
  ViewResourceHandler handler;

  protected UDFResourceManager resourceManager = null;
  protected FileResourceResourceManager fileResourceResourceManager = null;
  protected final static Logger LOG =
      LoggerFactory.getLogger(UDFService.class);

  protected synchronized UDFResourceManager getResourceManager() {
    if (resourceManager == null) {
      resourceManager = new UDFResourceManager(getSharedObjectsFactory(), context);
    }
    return resourceManager;
  }

  protected synchronized FileResourceResourceManager getFileResourceResourceManager() {
    if (fileResourceResourceManager == null) {
      fileResourceResourceManager = new FileResourceResourceManager(getSharedObjectsFactory(), context);
    }
    return fileResourceResourceManager;
  }

  /**
   * Get single item
   */
  @GET
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getOne(@PathParam("id") String id) {
    try {
      UDF udf = getResourceManager().read(id);
      JSONObject object = new JSONObject();
      object.put("udf", udf);
      return Response.ok(object).build();
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (ItemNotFound itemNotFound) {
      throw new NotFoundFormattedException(itemNotFound.getMessage(), itemNotFound);
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  /**
   * Delete single item
   */
  @DELETE
  @Path("{id}")
  public Response delete(@PathParam("id") String id) {
    try {
      getResourceManager().delete(id);
      return Response.status(204).build();
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (ItemNotFound itemNotFound) {
      throw new NotFoundFormattedException(itemNotFound.getMessage(), itemNotFound);
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  /**
   * Get all UDFs
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getList() {
    try {
      LOG.debug("Getting all udf");
      List items = getResourceManager().readAll(
          new OnlyOwnersFilteringStrategy(this.context.getUsername()));  //TODO: move strategy to PersonalCRUDRM

      JSONObject object = new JSONObject();
      object.put("udfs", items);
      return Response.ok(object).build();
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  /**
   * Update item
   */
  @PUT
  @Path("{id}")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response update(UDFRequest request,
                         @PathParam("id") String id) {
    try {
      if (request.udf.getFileResource() != null)
        getFileResourceResourceManager().read(request.udf.getFileResource());
      getResourceManager().update(request.udf, id);
      return Response.status(204).build();
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (ItemNotFound itemNotFound) {
      throw new NotFoundFormattedException(itemNotFound.getMessage(), itemNotFound);
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  /**
   * Create udf
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  public Response create(UDFRequest request, @Context HttpServletResponse response,
                         @Context UriInfo ui) {
    try {
      if (request.udf.getFileResource() != null)
        getFileResourceResourceManager().read(request.udf.getFileResource());
      getResourceManager().create(request.udf);

      UDF item = null;

      item = getResourceManager().read(request.udf.getId());

      response.setHeader("Location",
          String.format("%s/%s", ui.getAbsolutePath().toString(), request.udf.getId()));

      JSONObject object = new JSONObject();
      object.put("udf", item);
      return Response.ok(object).status(201).build();
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (ItemNotFound itemNotFound) {
      throw new NotFoundFormattedException(itemNotFound.getMessage(), itemNotFound);
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  /**
   * Wrapper object for json mapping
   */
  public static class UDFRequest {
    public UDF udf;
  }
}
