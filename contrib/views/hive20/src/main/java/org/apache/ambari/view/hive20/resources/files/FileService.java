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

package org.apache.ambari.view.hive20.resources.files;

import com.google.common.base.Optional;
import com.jayway.jsonpath.JsonPath;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.ViewResourceHandler;
import org.apache.ambari.view.commons.hdfs.UserService;
import org.apache.ambari.view.commons.hdfs.ViewPropertyHelper;
import org.apache.ambari.view.hive20.BaseService;
import org.apache.ambari.view.hive20.utils.*;
import org.apache.ambari.view.utils.hdfs.HdfsApi;
import org.apache.ambari.view.utils.hdfs.HdfsUtil;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileAlreadyExistsException;
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * File access resource
 * API:
 * GET /:path
 *      read entire file
 * POST /
 *      create new file
 *      Required: filePath
 *      file should not already exists
 * PUT /:path
 *      update file content
 */
public class FileService extends BaseService {
  public static final String FAKE_FILE = "fakefile://";
  public static final String JSON_PATH_FILE = "jsonpath:";
  public static final String VIEW_CONF_KEYVALUES = "view.conf.keyvalues";

  @Inject
  ViewResourceHandler handler;

  protected final static Logger LOG =
      LoggerFactory.getLogger(FileService.class);

  /**
   * Get single item
   */
  @GET
  @Path("{filePath:.*}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getFilePage(@PathParam("filePath") String filePath, @QueryParam("page") Long page) throws IOException, InterruptedException {

    LOG.debug("Reading file " + filePath);
    try {
      FileResource file = new FileResource();

      if (page == null)
        page = 0L;

      if (filePath.startsWith(FAKE_FILE)) {
        if (page > 1)
          throw new IllegalArgumentException("There's only one page in fake files");

        String encodedContent = filePath.substring(FAKE_FILE.length());
        String content = new String(Base64.decodeBase64(encodedContent));

        fillFakeFileObject(filePath, file, content);
      } else if (filePath.startsWith(JSON_PATH_FILE)) {
        if (page > 1)
          throw new IllegalArgumentException("There's only one page in fake files");

        String content = getJsonPathContentByUrl(filePath);
        fillFakeFileObject(filePath, file, content);
      } else  {

        filePath = sanitizeFilePath(filePath);
        FilePaginator paginator = new FilePaginator(filePath, getSharedObjectsFactory().getHdfsApi());

        fillRealFileObject(filePath, page, file, paginator);
      }

      JSONObject object = new JSONObject();
      object.put("file", file);
      return Response.ok(object).status(200).build();
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (FileNotFoundException ex) {
      throw new NotFoundFormattedException(ex.getMessage(), ex);
    } catch (IllegalArgumentException ex) {
      throw new BadRequestFormattedException(ex.getMessage(), ex);
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  protected String getJsonPathContentByUrl(String filePath) throws IOException {
    URL url = new URL(filePath.substring(JSON_PATH_FILE.length()));

    InputStream responseInputStream = context.getURLStreamProvider().readFrom(url.toString(), "GET",
        (String)null, new HashMap<String, String>());
    String response = IOUtils.toString(responseInputStream);

    for (String ref : url.getRef().split("!")) {
      response = JsonPath.read(response, ref);
    }
    return response;
  }

  public void fillRealFileObject(String filePath, Long page, FileResource file, FilePaginator paginator) throws IOException, InterruptedException {
    file.setFilePath(filePath);
    file.setFileContent(paginator.readPage(page));
    file.setHasNext(paginator.pageCount() > page + 1);
    file.setPage(page);
    file.setPageCount(paginator.pageCount());
  }

  public void fillFakeFileObject(String filePath, FileResource file, String content) {
    file.setFilePath(filePath);
    file.setFileContent(content);
    file.setHasNext(false);
    file.setPage(0);
    file.setPageCount(1);
  }

  /**
   * Delete single item
   */
  @DELETE
  @Path("{filePath:.*}")
  public Response deleteFile(@PathParam("filePath") String filePath) throws IOException, InterruptedException {
    try {
      filePath = sanitizeFilePath(filePath);
      LOG.debug("Deleting file " + filePath);
      if (getSharedObjectsFactory().getHdfsApi().delete(filePath, false)) {
        return Response.status(204).build();
      }
      throw new NotFoundFormattedException("FileSystem.delete returned false", null);
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
  @Path("{filePath:.*}")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response updateFile(FileResourceRequest request,
                             @PathParam("filePath") String filePath) throws IOException, InterruptedException {
    try {
      filePath = sanitizeFilePath(filePath);
      LOG.debug("Rewriting file " + filePath);
      FSDataOutputStream output = getSharedObjectsFactory().getHdfsApi().create(filePath, true);
      output.writeBytes(request.file.getFileContent());
      output.close();
      return Response.status(204).build();
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  /**
   * Create script
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  public Response createFile(FileResourceRequest request,
                             @Context HttpServletResponse response, @Context UriInfo ui)
      throws IOException, InterruptedException {
    try {
      LOG.debug("Creating file " + request.file.getFilePath());
      try {
        FSDataOutputStream output = getSharedObjectsFactory().getHdfsApi().create(request.file.getFilePath(), false);
        if (request.file.getFileContent() != null) {
          output.writeBytes(request.file.getFileContent());
        }
        output.close();
      } catch (FileAlreadyExistsException ex) {
        throw new ServiceFormattedException("F020 File already exists", ex, 400);
      }
      response.setHeader("Location",
          String.format("%s/%s", ui.getAbsolutePath().toString(), request.file.getFilePath()));
      return Response.status(204).build();
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  /**
   * Checks connection to HDFS
   * @param context View Context
   */
  public static void hdfsSmokeTest(ViewContext context) {
    try {
      Optional<Map<String, String>> props = ViewPropertyHelper.getViewConfigs(context, VIEW_CONF_KEYVALUES);
      HdfsApi api;
      if(props.isPresent()){
        api = HdfsUtil.connectToHDFSApi(context, props.get());
      }else{
        api = HdfsUtil.connectToHDFSApi(context);
      }

      api.getStatus();
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  /**
   * Checks connection to User HomeDirectory
   * @param context View Context
   */
  public static void userhomeSmokeTest(ViewContext context) {
    try {
      UserService userservice = new UserService(context, getViewConfigs(context));
      userservice.homeDir();
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  /**
   * Wrapper object for json mapping
   */
  public static class FileResourceRequest {
    public FileResource file;
  }

  private String sanitizeFilePath(String filePath){
    if (!filePath.startsWith("/") && !filePath.startsWith(".")) {
      filePath = "/" + filePath;  // some servers strip double slashes in URL
    }
    return filePath;
  }

  private static Map<String,String> getViewConfigs(ViewContext context) {
    Optional<Map<String, String>> props = ViewPropertyHelper.getViewConfigs(context, VIEW_CONF_KEYVALUES);
    return props.isPresent()? props.get() : new HashMap<String, String>();
  }
}
