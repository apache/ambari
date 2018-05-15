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

package org.apache.ambari.view.hive2.resources.savedQueries;

import org.apache.ambari.view.ViewResourceHandler;
import org.apache.ambari.view.hive2.BaseService;
import org.apache.ambari.view.hive2.persistence.utils.ItemNotFound;
import org.apache.ambari.view.hive2.persistence.utils.OnlyOwnersFilteringStrategy;
import org.apache.ambari.view.hive2.utils.NotFoundFormattedException;
import org.apache.ambari.view.hive2.utils.ServiceFormattedException;
import org.apache.ambari.view.utils.hdfs.HdfsApi;
import org.apache.ambari.view.utils.hdfs.HdfsUtil;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;

/**
 * Servlet for queries
 * API:
 * GET /:id
 *      read SavedQuery
 * POST /
 *      create new SavedQuery
 *      Required: title, queryFile
 * GET /
 *      get all SavedQueries of current user
 */
public class SavedQueryService extends BaseService {
  @Inject
  ViewResourceHandler handler;

  protected SavedQueryResourceManager resourceManager = null;
  protected final static Logger LOG =
      LoggerFactory.getLogger(SavedQueryService.class);

  protected synchronized SavedQueryResourceManager getResourceManager() {
    return getSharedObjectsFactory().getSavedQueryResourceManager();
  }

  protected void setResourceManager(SavedQueryResourceManager resourceManager) {
    this.resourceManager = resourceManager;
  }

  /**
   * Get single item
   */
  @GET
  @Path("{queryId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getOne(@PathParam("queryId") String queryId,
		         @QueryParam("op") String operation) {
    try {
      final SavedQuery savedQuery = getResourceManager().read(queryId);    
      if(operation.equals("download")) {
    	StreamingOutput stream = new StreamingOutput() {
    	@Override
    	public void write(OutputStream os) throws IOException, WebApplicationException {
    	  Writer writer = new BufferedWriter(new OutputStreamWriter(os));
    	  try { 
            BufferedReader br=new BufferedReader(new InputStreamReader(getSharedObjectsFactory().getHdfsApi().open(savedQuery.getQueryFile())));
	    String line;
    	    line=br.readLine();
    	    while (line != null){
    	      writer.write(line+"\n");  
    	      line = br.readLine();
            }
            writer.flush();
    	  } catch (InterruptedException e) {
	    e.printStackTrace();
	  } finally {
	    writer.close();
    	  }
    	}
    	};
    	return Response.ok(stream).
               type(MediaType.TEXT_PLAIN).
    	       build();
      }
      else {
    	 JSONObject object = new JSONObject();
         object.put("savedQuery", savedQuery);
         return Response.ok(object).build();
      }
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
  @Path("{queryId}")
  public Response delete(@PathParam("queryId") String queryId) {
    try {
      getResourceManager().delete(queryId);
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
   * Get all SavedQueries
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getList() {
    try {
      LOG.debug("Getting all SavedQuery");
      List allSavedQueries = getResourceManager().readAll(
          new OnlyOwnersFilteringStrategy(this.context.getUsername()));  //TODO: move strategy to PersonalCRUDRM

      JSONObject object = new JSONObject();
      object.put("savedQueries", allSavedQueries);
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
  @Path("{queryId}")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response update(SavedQueryRequest request,
                         @PathParam("queryId") String queryId) {
    try {
      getResourceManager().update(request.savedQuery, queryId);
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
   * Create savedQuery
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  public Response create(SavedQueryRequest request, @Context HttpServletResponse response,
                         @Context UriInfo ui) {
    try {
      getResourceManager().create(request.savedQuery);

      SavedQuery item = null;

      item = getResourceManager().read(request.savedQuery.getId());

      response.setHeader("Location",
          String.format("%s/%s", ui.getAbsolutePath().toString(), request.savedQuery.getId()));

      JSONObject object = new JSONObject();
      object.put("savedQuery", item);
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
   * Get default settings for query
   */
  @GET
  @Path("defaultSettings")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getDefaultSettings() {
    try {
      String defaultsFile = context.getProperties().get("scripts.settings.defaults-file");
      HdfsApi hdfsApi = getSharedObjectsFactory().getHdfsApi();

      String defaults = "{\"settings\": {}}";
      if (hdfsApi.exists(defaultsFile)) {
        defaults = HdfsUtil.readFile(hdfsApi, defaultsFile);
      }
      return Response.ok(JSONValue.parse(defaults)).build();
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  /**
   * Set default settings for query (overwrites if present)
   */
  @POST
  @Path("defaultSettings")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response setDefaultSettings(JSONObject settings) {
    try {
      String defaultsFile = context.getProperties().get("scripts.settings.defaults-file");
      HdfsApi hdfsApi = getSharedObjectsFactory().getHdfsApi();

      HdfsUtil.putStringToFile(hdfsApi, defaultsFile,
          settings.toString());
      String defaults = HdfsUtil.readFile(hdfsApi, defaultsFile);
      return Response.ok(JSONValue.parse(defaults)).build();
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  /**
   * Wrapper object for json mapping
   */
  public static class SavedQueryRequest {
    public SavedQuery savedQuery;
  }
}
