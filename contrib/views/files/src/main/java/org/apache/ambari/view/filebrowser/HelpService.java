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

public class HelpService extends HdfsService {

    public HelpService(ViewContext context) {
        super(context);
    }

    @GET
    @Path("/version")
    @Produces(MediaType.TEXT_PLAIN)
    public Response version(@Context HttpHeaders headers, @Context UriInfo ui) {
        return Response.ok("0.0.1-SNAPSHOT").build();
    }

    @GET
    @Path("/description")
    @Produces(MediaType.TEXT_PLAIN)
    public Response description(@Context HttpHeaders headers, @Context UriInfo ui) {
        return Response.ok("Application to work with HDFS").build();
    }

    @GET
    @Path("/filesystem")
    @Produces(MediaType.TEXT_PLAIN)
    public Response filesystem(@Context HttpHeaders headers, @Context UriInfo ui) {
        return Response.ok(
            context.getProperties().get("dataworker.defaultFs").toString()).build();
    }

    @GET
    @Path("/home")
    @Produces(MediaType.APPLICATION_JSON)
    public Response homeDir(@Context HttpHeaders headers, @Context UriInfo ui)
        throws FileNotFoundException, IOException, InterruptedException,
        Exception {
        HdfsApi api = getApi(context);
        return Response
            .ok(HdfsApi.fileStatusToJSON(api.getFileStatus(api.getHomeDir()
                .toString()))).build();
    }

    @GET
    @Path("/trash/enabled")
    @Produces(MediaType.APPLICATION_JSON)
    public Response trashEnabled(@Context HttpHeaders headers, @Context UriInfo ui)
        throws Exception {
        HdfsApi api = getApi(context);
        return Response.ok(new BoolResult(api.trashEnabled())).build();
    }

    @GET
    @Path("/trashDir")
    @Produces(MediaType.APPLICATION_JSON)
    public Response trashdir(@Context HttpHeaders headers, @Context UriInfo ui)
        throws IOException, Exception {
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
