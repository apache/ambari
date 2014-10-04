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

package org.apache.ambari.view.filebrowser;

import java.io.FileNotFoundException;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.filebrowser.utils.NotFoundFormattedException;
import org.apache.ambari.view.filebrowser.utils.ServiceFormattedException;

/**
 * File operations service
 */
public class FileOperationService extends HdfsService {

  /**
   * Constructor
   * @param context View Context instance
   */
  public FileOperationService(ViewContext context) {
    super(context);
  }

  /**
   * List dir
   * @param path path
   * @return response with dir content
   */
  @GET
  @Path("/listdir")
  @Produces(MediaType.APPLICATION_JSON)
  public Response listdir(@QueryParam("path") String path) {
    try {
      return Response.ok(
          HdfsApi.fileStatusToJSON(getApi(context).listdir(path))).build();
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (FileNotFoundException ex) {
      throw new NotFoundFormattedException(ex.getMessage(), ex);
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  /**
   * Rename
   * @param request rename request
   * @return response with success
   */
  @POST
  @Path("/rename")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response rename(final SrcDstFileRequest request) {
    try {
      HdfsApi api = getApi(context);
      ResponseBuilder result;
      if (api.rename(request.src, request.dst)) {
        result = Response.ok(HdfsApi.fileStatusToJSON(api
            .getFileStatus(request.dst)));
      } else {
        result = Response.ok(new BoolResult(false)).status(422);
      }
      return result.build();
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  /**
   * Chmod
   * @param request chmod request
   * @return response with success
   */
  @POST
  @Path("/chmod")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response chmod(final ChmodRequest request) {
    try {
      HdfsApi api = getApi(context);
      ResponseBuilder result;
      if (api.chmod(request.path, request.mode)) {
        result = Response.ok(HdfsApi.fileStatusToJSON(api
            .getFileStatus(request.path)));
      } else {
        result = Response.ok(new BoolResult(false)).status(422);
      }
      return result.build();
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  /**
   * Copy file
   * @param request source and destination request
   * @return response with success
   */
  @POST
  @Path("/copy")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response copy(final SrcDstFileRequest request,
                       @Context HttpHeaders headers, @Context UriInfo ui) {
    try {
      HdfsApi api = getApi(context);
      ResponseBuilder result;
      if (api.copy(request.src, request.dst)) {
        result = Response.ok(HdfsApi.fileStatusToJSON(api
            .getFileStatus(request.dst)));
      } else {
        result = Response.ok(new BoolResult(false)).status(422);
      }
      return result.build();
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  /**
   * Make directory
   * @param request make directory request
   * @return response with success
   */
  @PUT
  @Path("/mkdir")
  @Produces(MediaType.APPLICATION_JSON)
  public Response mkdir(final MkdirRequest request) {
    try{
      HdfsApi api = getApi(context);
      ResponseBuilder result;
      if (api.mkdir(request.path)) {
        result = Response.ok(HdfsApi.fileStatusToJSON(api.getFileStatus(request.path)));
      } else {
        result = Response.ok(new BoolResult(false)).status(422);
      }
      return result.build();
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  /**
   * Empty trash
   * @return response with success
   */
  @DELETE
  @Path("/trash/emptyTrash")
  @Produces(MediaType.APPLICATION_JSON)
  public Response emptyTrash() {
    try {
      HdfsApi api = getApi(context);
      api.emptyTrash();
      return Response.ok(new BoolResult(true)).build();
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  /**
   * Move to trash
   * @param request remove request
   * @return response with success
   */
  @DELETE
  @Path("/moveToTrash")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response moveToTrash(RemoveRequest request) {
    try {
      HdfsApi api = getApi(context);
      ResponseBuilder result;
      if (api.moveToTrash(request.path)){
        result = Response.ok(new BoolResult(true)).status(204);
      } else {
        result = Response.ok(new BoolResult(false)).status(422);
      }
      return result.build();
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  /**
   * Remove
   * @param request remove request
   * @return response with success
   */
  @DELETE
  @Path("/remove")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response remove(RemoveRequest request, @Context HttpHeaders headers,
                         @Context UriInfo ui) {
    try {
      HdfsApi api = getApi(context);
      ResponseBuilder result;
      if (api.delete(request.path, request.recursive)) {
        result = Response.ok(new BoolResult(true)).status(204);
      } else {
        result = Response.ok(new BoolResult(false)).status(422);
      }
      return result.build();
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  /**
   * Wrapper for json mapping of mkdir request
   */
  @XmlRootElement
  public static class MkdirRequest {
    @XmlElement(nillable = false, required = true)
    public String path;
  }

  /**
   * Wrapper for json mapping of chmod request
   */
  @XmlRootElement
  public static class ChmodRequest {
    @XmlElement(nillable = false, required = true)
    public String path;
    @XmlElement(nillable = false, required = true)
    public String mode;
  }

  /**
   * Wrapper for json mapping of request with
   * source and destination
   */
  @XmlRootElement
  public static class SrcDstFileRequest {
    @XmlElement(nillable = false, required = true)
    public String src;
    @XmlElement(nillable = false, required = true)
    public String dst;
  }

  /**
   * Wrapper for json mapping of remove request
   */
  @XmlRootElement
  public static class RemoveRequest {
    @XmlElement(nillable = false, required = true)
    public String path;
    public boolean recursive;
  }
}
