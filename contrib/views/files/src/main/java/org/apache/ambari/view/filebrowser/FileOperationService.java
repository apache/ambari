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

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.ambari.view.ViewContext;

public class FileOperationService extends HdfsService {

    public FileOperationService(ViewContext context) {
        super(context);
    }

    @XmlRootElement
    public static class MkdirRequest {
        @XmlElement(nillable = false, required = true)
        public String path;
    }


    @XmlRootElement
    public static class SrcDstFileRequest {
        @XmlElement(nillable = false, required = true)
        public String src;
        @XmlElement(nillable = false, required = true)
        public String dst;
    }

    @XmlRootElement
    public static class RemoveRequest {
        @XmlElement(nillable = false, required = true)
        public String path;
        public boolean recursive;
    }

    @GET
    @Path("/listdir")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listdir(@QueryParam("path") String path,
        @Context HttpHeaders headers, @Context UriInfo ui) throws Exception {
        try {
            return Response.ok(
                HdfsApi.fileStatusToJSON(getApi(context).listdir(path))).build();
        } catch (FileNotFoundException ex) {
            return Response.ok(Response.Status.NOT_FOUND.getStatusCode())
                .entity(ex.getMessage()).build();
        } catch (Throwable ex) {
            throw new Exception(ex.getMessage());
        }
    }

    @POST
    @Path("/rename")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response rename(final SrcDstFileRequest request,
        @Context HttpHeaders headers, @Context UriInfo ui) throws IOException,
        Exception {
        HdfsApi api = getApi(context);
        ResponseBuilder result;
        if (api.rename(request.src, request.dst)) {
            result = Response.ok(HdfsApi.fileStatusToJSON(api
                .getFileStatus(request.dst)));
        } else {
            result = Response.ok(new BoolResult(false)).status(422);
        }
        return result.build();
    }

    @POST
    @Path("/copy")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response copy(final SrcDstFileRequest request,
                         @Context HttpHeaders headers, @Context UriInfo ui) throws IOException,
            Exception {
        HdfsApi api = getApi(context);
        ResponseBuilder result;
        if (api.copy(request.src, request.dst)) {
            result = Response.ok(HdfsApi.fileStatusToJSON(api
                    .getFileStatus(request.dst)));
        } else {
            result = Response.ok(new BoolResult(false)).status(422);
        }
        return result.build();
    }

    @PUT
    @Path("/mkdir")
    @Produces(MediaType.APPLICATION_JSON)
    public Response mkdir(final MkdirRequest request,
        @Context HttpHeaders headers, @Context UriInfo ui) throws IOException,
        Exception {
        HdfsApi api = getApi(context);
        ResponseBuilder result;
        if (api.mkdir(request.path)) {
            result = Response.ok(HdfsApi.fileStatusToJSON(api.getFileStatus(request.path)));
        } else {
            result = Response.ok(new BoolResult(false)).status(422);
        }
        return result.build();
    }

    @DELETE
    @Path("/trash/emptyTrash")
    @Produces(MediaType.APPLICATION_JSON)
    public Response emptyTrash(@Context HttpHeaders headers,
        @Context UriInfo ui) throws IOException, Exception {
        HdfsApi api = getApi(context);
        api.emptyTrash();
        return Response.ok(new BoolResult(true)).build();
    }

    @DELETE
    @Path("/moveToTrash")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response moveToTrash(RemoveRequest request, @Context HttpHeaders headers,
        @Context UriInfo ui) throws IOException, Exception {
        HdfsApi api = getApi(context);
        ResponseBuilder result;
        if (api.moveToTrash(request.path)){
            result = Response.ok(new BoolResult(true)).status(204);
        } else {
            result = Response.ok(new BoolResult(false)).status(422);
        }
        return result.build();
    }

    @DELETE
    @Path("/remove")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response remove(RemoveRequest request, @Context HttpHeaders headers,
        @Context UriInfo ui) throws IOException, Exception {
        HdfsApi api = getApi(context);
        ResponseBuilder result;
        if (api.delete(request.path, request.recursive)){
            result = Response.ok(new BoolResult(true)).status(204);
        } else {
            result = Response.ok(new BoolResult(false)).status(422);
        }
        return result.build();
    }

}
