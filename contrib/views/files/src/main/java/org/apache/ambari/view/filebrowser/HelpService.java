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
import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.view.ViewContext;

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
        context.getProperties().get("dataworker.defaultFs")).build();
  }

  /**
   * Returns home directory
   * @return home directory
   * @throws Exception
   */
  @GET
  @Path("/home")
  @Produces(MediaType.APPLICATION_JSON)
  public Response homeDir()
      throws
      Exception {
    HdfsApi api = getApi(context);
    return Response
        .ok(HdfsApi.fileStatusToJSON(api.getFileStatus(api.getHomeDir()
            .toString()))).build();
  }

  /**
   * Is trash enabled
   * @return is trash enabled
   * @throws Exception
   */
  @GET
  @Path("/trash/enabled")
  @Produces(MediaType.APPLICATION_JSON)
  public Response trashEnabled()
      throws Exception {
    HdfsApi api = getApi(context);
    return Response.ok(new BoolResult(api.trashEnabled())).build();
  }

  /**
   * Trash dir
   * @return trash dir
   * @throws Exception
   */
  @GET
  @Path("/trashDir")
  @Produces(MediaType.APPLICATION_JSON)
  public Response trashdir()
      throws Exception {
    HdfsApi api = getApi(context);
    try {
      return Response.ok(
          HdfsApi.fileStatusToJSON(api.getFileStatus(api.getTrashDir()
              .toString()))).build();
    } catch (FileNotFoundException ex) {
      return Response.ok(new BoolResult(false)).status(Status.NOT_FOUND)
          .build();
    }
  }

}
