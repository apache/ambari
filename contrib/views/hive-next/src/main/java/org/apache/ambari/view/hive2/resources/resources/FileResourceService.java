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

package org.apache.ambari.view.hive2.resources.resources;

import org.apache.ambari.view.ViewResourceHandler;
import org.apache.ambari.view.hive2.BaseService;
import org.apache.ambari.view.hive2.persistence.utils.ItemNotFound;
import org.apache.ambari.view.hive2.persistence.utils.OnlyOwnersFilteringStrategy;
import org.apache.ambari.view.hive2.utils.NotFoundFormattedException;
import org.apache.ambari.view.hive2.utils.ServiceFormattedException;
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
 * Servlet for Resources
 * API:
 * GET /:id
 *      read resource
 * POST /
 *      create new resource
 * GET /
 *      get all resource of current user
 */
public class FileResourceService extends BaseService {
  @Inject
  ViewResourceHandler handler;

  protected FileResourceResourceManager resourceManager = null;
  protected final static Logger LOG =
      LoggerFactory.getLogger(FileResourceService.class);

  protected synchronized FileResourceResourceManager getResourceManager() {
    if (resourceManager == null) {
      resourceManager = new FileResourceResourceManager(getSharedObjectsFactory(), context);
    }
    return resourceManager;
  }

  /**
   * Get single item
   */
  @GET
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getOne(@PathParam("id") String id) {
    try {
      FileResourceItem fileResourceItem = getResourceManager().read(id);
      JSONObject object = new JSONObject();
      object.put("fileResource", fileResourceItem);
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
   * Get all resources
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getList() {
    try {
      LOG.debug("Getting all resources");
      List items = getResourceManager().readAll(
          new OnlyOwnersFilteringStrategy(this.context.getUsername()));  //TODO: move strategy to PersonalCRUDRM

      JSONObject object = new JSONObject();
      object.put("fileResources", items);
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
  public Response update(ResourceRequest request,
                         @PathParam("id") String id) {
    try {
      getResourceManager().update(request.fileResource, id);
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
   * Create resource
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  public Response create(ResourceRequest request, @Context HttpServletResponse response,
                         @Context UriInfo ui) {
    try {
      getResourceManager().create(request.fileResource);

      FileResourceItem item = null;

      item = getResourceManager().read(request.fileResource.getId());

      response.setHeader("Location",
          String.format("%s/%s", ui.getAbsolutePath().toString(), request.fileResource.getId()));

      JSONObject object = new JSONObject();
      object.put("fileResource", item);
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
  public static class ResourceRequest {
    public FileResourceItem fileResource;
  }
}
