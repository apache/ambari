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
import java.io.OutputStream;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.annotation.XmlElement;

import com.google.gson.Gson;
import org.apache.ambari.view.filebrowser.utils.NotFoundFormattedException;
import org.apache.ambari.view.filebrowser.utils.ServiceFormattedException;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.ambari.view.ViewContext;
import org.json.simple.JSONObject;
//import org.glassfish.jersey.server.ChunkedOutput;

/**
 * Service for download and aggregate files
 */
public class DownloadService extends HdfsService {

  public DownloadService(ViewContext context) {
    super(context);
  }

  /**
   * Download entire file
   * @param path path to file
   * @param download download as octet strem or as file mime type
   * @param headers http headers
   * @param ui uri info
   * @return response with file
   */
  @GET
  @Path("/browse")
  @Produces(MediaType.TEXT_PLAIN)
  public Response browse(@QueryParam("path") String path, @QueryParam("download") boolean download,
                         @Context HttpHeaders headers, @Context UriInfo ui) {
    try {
      HdfsApi api = getApi(context);
      FileStatus status = api.getFileStatus(path);
      FSDataInputStream fs = api.open(path);
      ResponseBuilder result = Response.ok(fs);
      if (download) {
        result.header("Content-Disposition",
            "inline; filename=\"" + status.getPath().getName() + "\"").type(MediaType.APPLICATION_OCTET_STREAM);
      } else {
        FileNameMap fileNameMap = URLConnection.getFileNameMap();
        String mimeType = fileNameMap.getContentTypeFor(status.getPath().getName());
        result.header("Content-Disposition",
            "filename=\"" + status.getPath().getName() + "\"").type(mimeType);
      }
      return result.build();
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (FileNotFoundException ex) {
      throw new NotFoundFormattedException(ex.getMessage(), ex);
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  private void zipFile(ZipOutputStream zip, String path) throws InterruptedException, IOException {
    try {
      zip.putNextEntry(new ZipEntry(path.substring(1)));
      FSDataInputStream in = getApi(context).open(path);
      byte[] chunk = new byte[1024];
      while (in.read(chunk) != -1) {
        zip.write(chunk);
      }
    } catch (IOException ex) {
      String msg = "Error zipping file " + path.substring(1) + ": "
          + ex.getMessage();
      logger.error(msg);
      zip.write(ex.getMessage().getBytes());
      throw new ServiceFormattedException(ex.getMessage(), ex);
    } finally {
      zip.closeEntry();
    }
  }

  private void zipDirectory(ZipOutputStream zip, String path) {
    try {
      zip.putNextEntry(new ZipEntry(path.substring(1) + "/"));
      zip.closeEntry();
    } catch (IOException ex) {
      String msg = "Error zipping directory " + path.substring(1) + "/" + ": "
          + ex.getMessage();
      logger.error(msg);
      throw new ServiceFormattedException(msg, ex);
    }
  }

  /**
   * Download ZIP of passed file list
   * @param request download request
   * @return response with zip
   */
  @POST
  @Path("/zip")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response downloadGZip(final DownloadRequest request) {
    try {
      StreamingOutput result = new StreamingOutput() {
        public void write(OutputStream output) throws IOException,
            ServiceFormattedException {
          ZipOutputStream zip = new ZipOutputStream(output);
          try {
            HdfsApi api = getApi(context);
            Queue<String> files = new LinkedList<String>();
            for (String file : request.entries) {
              files.add(file);
            }
            while (!files.isEmpty()) {
              String path = files.poll();
              FileStatus status = api.getFileStatus(path);
              if (status.isDirectory()) {
                FileStatus[] subdir = api.listdir(path);
                for (FileStatus file : subdir) {
                  files.add(org.apache.hadoop.fs.Path
                      .getPathWithoutSchemeAndAuthority(file.getPath())
                      .toString());
                }
                zipDirectory(zip, path);
              } else {
                zipFile(zip, path);
              }
            }
          } catch (Exception ex) {
            logger.error("Error occurred: " + ex.getMessage());
            throw new ServiceFormattedException(ex.getMessage(), ex);
          } finally {
            zip.close();
          }
        }
      };
      return Response.ok(result)
          .header("Content-Disposition", "inline; filename=\"hdfs.zip\"").build();
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  /**
   * Concatenate files
   * @param request download request
   * @return response with all files concatenated
   */
  @POST
  @Path("/concat")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response concat(final DownloadRequest request) {
    try {
      StreamingOutput result = new StreamingOutput() {
        public void write(OutputStream output) throws IOException,
            ServiceFormattedException {
          FSDataInputStream in = null;
          for (String path : request.entries) {
            try {
              in = getApi(context).open(path);
              byte[] chunk = new byte[1024];
              while (in.read(chunk) != -1) {
                output.write(chunk);
              }
            } catch (Exception ex) {
              throw new ServiceFormattedException(ex.getMessage(), ex);
            } finally {
              if (in != null)
                in.close();
            }
          }
        }
      };
      ResponseBuilder response = Response.ok(result);
      if (request.download) {
        response.header("Content-Disposition", "inline; filename=\"concatResult.txt\"").type(MediaType.APPLICATION_OCTET_STREAM);
      } else {
        response.header("Content-Disposition", "filename=\"concatResult.txt\"").type(MediaType.TEXT_PLAIN);
      }
      return response.build();
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  // ===============================
  // Download files by unique link

  /**
   * Download zip by unique link
   * @param requestId id of request
   * @return response with zip
   */
  @GET
  @Path("/zip")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response zipByRequestId(@QueryParam("requestId") String requestId) {
    try {
      String json = context.getInstanceData(requestId);
      DownloadRequest request = gson.fromJson(json, DownloadRequest.class);
      context.removeInstanceData(requestId);
      return downloadGZip(request);
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  /**
   * Generate link for zip
   * @param request download request
   * @return response wth request id
   * @see #zipByRequestId(String)
   */
  @POST
  @Path("/zip/generate-link")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response zipGenerateLink(final DownloadRequest request) {
    try {
      String requestId = generateUniqueIdentifer(request);
      JSONObject json = new JSONObject();
      json.put("requestId", requestId);
      return Response.ok(json).build();
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  /**
   * Concatenate files by unique link
   * @param requestId id of request
   * @return response with concatenated files
   */
  @GET
  @Path("/concat")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response concatByRequestId(@QueryParam("requestId") String requestId) {
    try {
      String json = context.getInstanceData(requestId);
      DownloadRequest request = gson.fromJson(json, DownloadRequest.class);
      context.removeInstanceData(requestId);
      return concat(request);
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  /**
   * Generate link for concat
   * @param request download request
   * @return response wth request id
   * @see #concatByRequestId(String)
   */
  @POST
  @Path("/concat/generate-link")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response concatGenerateLink(final DownloadRequest request) {
    try {
      String requestId = generateUniqueIdentifer(request);
      JSONObject json = new JSONObject();
      json.put("requestId", requestId);
      return Response.ok(json).build();
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  private Gson gson = new Gson();

  private String generateUniqueIdentifer(DownloadRequest request) {
    String uuid = UUID.randomUUID().toString().replaceAll("-", "");
    String json = gson.toJson(request);
    context.putInstanceData(uuid, json);
    return uuid;
  }

    /*
     * Temporary use Stream Output
     *
     * @POST
     *
     * @Path("/concat")
     *
     * @Consumes(MediaType.APPLICATION_JSON)
     *
     * @Produces(MediaType.APPLICATION_OCTET_STREAM) public ChunkedOutput<byte[]>
     * concat(final DownloadRequest request) { final ChunkedOutput<byte[]> output
     * = new ChunkedOutput<byte[]>(byte[].class);
     *
     * new Thread() { public void run() { try { FSDataInputStream in = null; for
     * (String path : request.entries) { try { in = getApi(context).open(path);
     * byte[] chunk = new byte[1024]; while (in.read(chunk) != -1) {
     * output.write(chunk); } } finally { if (in != null) in.close(); }
     *
     * } } catch (Exception ex) { logger.error("Error occured: " +
     * ex.getMessage()); } finally { try { output.close(); } catch (IOException e)
     * { e.printStackTrace(); } } } }.start();
     *
     * return output; }
     */

  /**
   * Wrapper for json mapping of download request
   */
  public static class DownloadRequest {
    @XmlElement(nillable = false, required = true)
    public String[] entries;
    @XmlElement(required = false)
    public boolean download;
  }
}
