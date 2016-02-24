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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.filebrowser.utils.NotFoundFormattedException;
import org.apache.ambari.view.filebrowser.utils.ServiceFormattedException;
import org.apache.ambari.view.utils.hdfs.HdfsApi;

/**
 * Help service
 */
public class HelpService extends HdfsService {

  /**
   * Constructor
   * @param context View Context instance
   */
  public HelpService(ViewContext context) {
    super(context);
  }

  /**
   * Version
   * @return version
   */
  @GET
  @Path("/version")
  @Produces(MediaType.TEXT_PLAIN)
  public Response version() {
    return Response.ok("0.0.1-SNAPSHOT").build();
  }

  /**
   * Description
   * @return description
   */
  @GET
  @Path("/description")
  @Produces(MediaType.TEXT_PLAIN)
  public Response description() {
    return Response.ok("Application to work with HDFS").build();
  }

  /**
   * Filesystem configuration
   * @return filesystem configuration
   */
  @GET
  @Path("/filesystem")
  @Produces(MediaType.TEXT_PLAIN)
  public Response filesystem() {
    return Response.ok(
        context.getProperties().get("webhdfs.url")).build();
  }

  /**
   * Returns home directory
   * @return home directory
   */
  @GET
  @Path("/home")
  @Produces(MediaType.APPLICATION_JSON)
  public Response homeDir() {
    try {
      HdfsApi api = getApi(context);
      return Response
          .ok(getApi(context).fileStatusToJSON(api.getFileStatus(api.getHomeDir()
              .toString()))).build();
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  /**
   * Is trash enabled
   * @return is trash enabled
   */
  @GET
  @Path("/trash/enabled")
  @Produces(MediaType.APPLICATION_JSON)
  public Response trashEnabled() {
    try {
      HdfsApi api = getApi(context);
      return Response.ok(new FileOperationResult(api.trashEnabled())).build();
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  /**
   * Trash dir
   * @return trash dir
   */
  @GET
  @Path("/trashDir")
  @Produces(MediaType.APPLICATION_JSON)
  public Response trashdir() {
    try {
      HdfsApi api = getApi(context);
      return Response.ok(
          getApi(context).fileStatusToJSON(api.getFileStatus(api.getTrashDir()
              .toString()))).build();
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (FileNotFoundException ex) {
      throw new NotFoundFormattedException(ex.getMessage(), ex);
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

}
