/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.oozie.ambari.view;

import static org.apache.oozie.ambari.view.Constants.MESSAGE_KEY;
import static org.apache.oozie.ambari.view.Constants.STATUS_KEY;
import static org.apache.oozie.ambari.view.Constants.STATUS_OK;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.view.ViewContext;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.hadoop.security.AccessControlException;
import org.apache.oozie.ambari.view.assets.AssetResource;
import org.apache.oozie.ambari.view.workflowmanager.WorkflowManagerService;
import org.apache.oozie.ambari.view.workflowmanager.WorkflowsManagerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;

/**
 * This is a class used to bridge the communication between the and the Oozie
 * API executing inside ambari.
 */
@Singleton
public class OozieProxyImpersonator {
  private final static Logger LOGGER = LoggerFactory
    .getLogger(OozieProxyImpersonator.class);

  private ViewContext viewContext;
  private final Utils utils = new Utils();


  private final HDFSFileUtils hdfsFileUtils;
  private final WorkflowFilesService workflowFilesService;
  private WorkflowManagerService workflowManagerService;
  private static final boolean PROJ_MANAGER_ENABLED = true;
  private final OozieDelegate oozieDelegate;
  private final OozieUtils oozieUtils = new OozieUtils();
  private final AssetResource assetResource;
  private  final AmbariIOUtil ambariIOUtil;
  private static enum ErrorCodes {
    OOZIE_SUBMIT_ERROR("error.oozie.submit", "Oozie Submit error"), OOZIE_IO_ERROR(
      "error.oozie.io", "Oozie I/O error"), FILE_ACCESS_ACL_ERROR(
      "error.file.access.control",
      "Access Error to file due to access control"), FILE_ACCESS_UNKNOWN_ERROR(
      "error.file.access", "Error accessing file"), WORKFLOW_PATH_EXISTS(
      "error.workflow.path.exists", "Worfklow path exists");
    private String errorCode;
    private String description;

    ErrorCodes(String errorCode, String description) {
      this.errorCode = errorCode;
      this.description = description;
    }

    public String getErrorCode() {
      return errorCode;
    }

    public String getDescription() {
      return description;
    }
  }

  @Inject
  public OozieProxyImpersonator(ViewContext viewContext) {
    this.viewContext = viewContext;
    hdfsFileUtils = new HDFSFileUtils(viewContext);
    workflowFilesService = new WorkflowFilesService(hdfsFileUtils);
    this.oozieDelegate = new OozieDelegate(viewContext);
    assetResource = new AssetResource(viewContext);
    if (PROJ_MANAGER_ENABLED) {
      workflowManagerService = new WorkflowManagerService(viewContext);
    }
    ambariIOUtil=new AmbariIOUtil(viewContext);

    LOGGER.info(String.format(
      "OozieProxyImpersonator initialized for instance: %s",
      viewContext.getInstanceName()));

  }

  @GET
  @Path("hdfsCheck")
  public Response hdfsCheck(){
    hdfsFileUtils.hdfsCheck();
    return Response.ok().build();
  }

  @GET
  @Path("homeDirCheck")
  public Response homeDirCheck(){
    hdfsFileUtils.homeDirCheck();
    return Response.ok().build();
  }

  @Path("/fileServices")
  public FileServices fileServices() {
    return new FileServices(viewContext);
  }

  @Path("/wfprojects")
  public WorkflowsManagerResource workflowsManagerResource() {
    return new WorkflowsManagerResource(viewContext);
  }

  @Path("/assets")
  public AssetResource assetResource() {
    return this.assetResource;
  }

  @GET
  @Path("/getCurrentUserName")
  public Response getCurrentUserName() {
    return Response.ok(viewContext.getUsername()).build();
  }

  @POST
  @Path("/submitJob")
  @Consumes({MediaType.TEXT_PLAIN + "," + MediaType.TEXT_XML})
  public Response submitJob(String postBody, @Context HttpHeaders headers,
                            @Context UriInfo ui, @QueryParam("app.path") String appPath,
                            @QueryParam("projectId") String projectId,
                            @DefaultValue("false") @QueryParam("overwrite") Boolean overwrite,
                            @QueryParam("description") String description,
                            @QueryParam("jobType") String jobType) {
    LOGGER.info("submit workflow job called");
    return submitJobInternal(postBody, headers, ui, appPath, overwrite,
      JobType.valueOf(jobType), projectId, description);
  }

  @POST
  @Path("/saveWorkflow")
  @Consumes({MediaType.TEXT_PLAIN + "," + MediaType.TEXT_XML})
  public Response saveWorkflow(String postBody, @Context HttpHeaders headers,
                               @Context UriInfo ui, @QueryParam("app.path") String appPath, @QueryParam("description") String description,
                               @QueryParam("projectId") String projectId, @QueryParam("jobType") String jobType,
                               @DefaultValue("false") @QueryParam("overwrite") Boolean overwrite) {
    LOGGER.info("save workflow  called");
    if (StringUtils.isEmpty(appPath)) {
      throw new RuntimeException("app path can't be empty.");
    }

    JobType deducedJobType = oozieUtils.deduceJobType(postBody);
    appPath = workflowFilesService.getWorkflowFileName(appPath.trim(),deducedJobType);

    if (!overwrite) {
      boolean fileExists = hdfsFileUtils.fileExists(appPath);
      if (fileExists) {
        return getFileExistsResponse();
      }
    }

    postBody = utils.formatXml(postBody);
    try {
      String filePath = workflowFilesService.createFile(appPath,
        postBody, overwrite);
      LOGGER.info(String.format(
        "submit workflow job done. filePath=[%s]", filePath));
      if (PROJ_MANAGER_ENABLED) {
        String workflowName = oozieUtils.deduceWorkflowNameFromXml(postBody);
        workflowManagerService.saveWorkflow(projectId, appPath,
          deducedJobType, description,
          viewContext.getUsername(), workflowName);
      }

      return Response.ok().build();
    } catch (Exception ex) {
      LOGGER.error(ex.getMessage(), ex);
      return getRespCodeForException(ex);

    }
  }

  @POST
  @Path("/saveWorkflowDraft")
  @Consumes({MediaType.TEXT_PLAIN + "," + MediaType.TEXT_XML})
  public Response saveDraft(String postBody, @Context HttpHeaders headers,
                            @Context UriInfo ui, @QueryParam("app.path") String appPath,
                            @QueryParam("projectId") String projectId, @QueryParam("description") String description,
                            @DefaultValue("false") @QueryParam("overwrite") Boolean overwrite, @QueryParam("jobType") String jobTypeStr)
    throws IOException {
    LOGGER.info("save workflow  called");
    if (StringUtils.isEmpty(appPath)) {
      throw new RuntimeException("app path can't be empty.");
    }
    JobType jobType = StringUtils.isEmpty(jobTypeStr) ? JobType.WORKFLOW : JobType.valueOf(jobTypeStr);
    appPath = workflowFilesService.getWorkflowDraftFileName(appPath.trim(),jobType);
    workflowFilesService.createFile(appPath, postBody, overwrite);
    if (PROJ_MANAGER_ENABLED) {
      String name = oozieUtils.deduceWorkflowNameFromJson(postBody);
      workflowManagerService.saveWorkflow(projectId, appPath,
        jobType, description,
        viewContext.getUsername(), name);
    }
    return Response.ok().build();
  }

  @POST
  @Path("/publishAsset")
  @Consumes({MediaType.TEXT_PLAIN + "," + MediaType.TEXT_XML})
  public Response publishAsset(String postBody, @Context HttpHeaders headers,
                               @Context UriInfo ui, @QueryParam("uploadPath") String uploadPath,
                               @DefaultValue("false") @QueryParam("overwrite") Boolean overwrite) {
    LOGGER.info("publish asset called");
    if (StringUtils.isEmpty(uploadPath)) {
      throw new RuntimeException("upload path can't be empty.");
    }
    uploadPath = uploadPath.trim();
    Map<String, String> validateAsset = assetResource.validateAsset(headers, postBody,
      ui.getQueryParameters());
    if (!STATUS_OK.equals(validateAsset.get(STATUS_KEY))) {
      return Response.status(Status.BAD_REQUEST).entity(
        validateAsset.get(MESSAGE_KEY)).build();
    }
    return saveAsset(postBody, uploadPath, overwrite);
  }

  private Response saveAsset(String postBody, String uploadPath,
                             Boolean overwrite) {
    uploadPath = workflowFilesService.getAssetFileName(uploadPath);
    if (!overwrite) {
      boolean fileExists = hdfsFileUtils.fileExists(uploadPath);
      if (fileExists) {
        return getFileExistsResponse();
      }
    }
    postBody = utils.formatXml(postBody);
    try {
      String filePath = workflowFilesService.createAssetFile(uploadPath,
        postBody, overwrite);
      LOGGER.info(String.format("publish asset job done. filePath=[%s]",
        filePath));
      return Response.ok().build();
    } catch (Exception ex) {
      LOGGER.error(ex.getMessage(), ex);
      return getRespCodeForException(ex);
    }
  }
  @GET
  @Path("/readAsset")
  public Response readAsset(
          @QueryParam("assetPath") String assetPath) {
    if (StringUtils.isEmpty(assetPath)) {
      throw new RuntimeException("assetPath can't be empty.");
    }
    try {
      final InputStream is = workflowFilesService
              .readAssset(assetPath);
      StreamingOutput streamer = new StreamingOutput() {
        @Override
        public void write(OutputStream os) throws IOException,
                WebApplicationException {
          IOUtils.copy(is, os);
          is.close();
          os.close();
        }
      };
      return Response.ok(streamer).status(200).build();
    } catch (IOException e) {
      return getRespCodeForException(e);
    }
  }


  @GET
  @Path("/readWorkflowDraft")
  public Response readDraft(@QueryParam("workflowXmlPath") String workflowPath) {
    if (StringUtils.isEmpty(workflowPath)) {
      throw new RuntimeException("workflowXmlPath can't be empty.");
    }
    try {
      final InputStream is = workflowFilesService.readDraft(workflowPath);
      StreamingOutput streamer = new StreamingOutput() {
        @Override
        public void write(OutputStream os) throws IOException,
          WebApplicationException {
          IOUtils.copy(is, os);
          is.close();
          os.close();
        }
      };
      return Response.ok(streamer).status(200).build();
    } catch (IOException e) {
      return getRespCodeForException(e);
    }
  }

  @POST
  @Path("/discardWorkflowDraft")
  public Response discardDraft(
    @QueryParam("workflowXmlPath") String workflowPath)
    throws IOException {
    workflowFilesService.discardDraft(workflowPath);
    return Response.ok().build();
  }

  private Response submitJobInternal(String postBody, HttpHeaders headers,
                                     UriInfo ui, String appPath, Boolean overwrite, JobType jobType,
                                     String projectId, String description) {
    if (StringUtils.isEmpty(appPath)) {
      throw new RuntimeException("app path can't be empty.");
    }
    appPath = workflowFilesService.getWorkflowFileName(appPath.trim(), jobType);
    if (!overwrite) {
      boolean fileExists = hdfsFileUtils.fileExists(appPath);
      if (fileExists) {
        return getFileExistsResponse();
      }
    }
    postBody = utils.formatXml(postBody);
    try {
      String filePath = workflowFilesService.createFile(appPath, postBody,
        overwrite);
      LOGGER.info(String.format(
        "submit workflow job done. filePath=[%s]", filePath));
    } catch (Exception ex) {
      LOGGER.error(ex.getMessage(), ex);
      return getRespCodeForException(ex);

    }
    if (PROJ_MANAGER_ENABLED) {
      String name = oozieUtils.deduceWorkflowNameFromXml(postBody);
      workflowManagerService.saveWorkflow(projectId, appPath, jobType,
        "todo description", viewContext.getUsername(), name);
    }

    String response = oozieDelegate.submitWorkflowJobToOozie(headers,
      appPath, ui.getQueryParameters(), jobType);
    if (response != null && response.trim().startsWith("{")) {
      // dealing with oozie giving error but with 200 response.
      return Response.status(Response.Status.OK).entity(response).build();
    } else {
      HashMap<String, String> resp = new HashMap<String, String>();
      resp.put("status", ErrorCodes.OOZIE_SUBMIT_ERROR.getErrorCode());
      resp.put("message", response);
      return Response.status(Response.Status.BAD_REQUEST).entity(resp)
        .build();
    }

  }

  private Response getRespCodeForException(Exception ex) {
    if (ex instanceof AccessControlException) {
      HashMap<String, String> errorDetails = getErrorDetails(
        ErrorCodes.FILE_ACCESS_ACL_ERROR.getErrorCode(),
        ErrorCodes.FILE_ACCESS_ACL_ERROR.getDescription(), ex);
      return Response.status(Response.Status.BAD_REQUEST)
        .entity(errorDetails).build();
    } else if (ex instanceof IOException) {
      HashMap<String, String> errorDetails = getErrorDetails(
        ErrorCodes.FILE_ACCESS_UNKNOWN_ERROR.getErrorCode(),
        ErrorCodes.FILE_ACCESS_UNKNOWN_ERROR.getDescription(), ex);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
        .entity(errorDetails).build();
    } else {
      HashMap<String, String> errorDetails = getErrorDetails(
        ErrorCodes.FILE_ACCESS_UNKNOWN_ERROR.getErrorCode(),
        ErrorCodes.FILE_ACCESS_UNKNOWN_ERROR.getDescription(), ex);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
        .entity(errorDetails).build();
    }

  }

  private Response getFileExistsResponse() {
    HashMap<String, String> resp = new HashMap<String, String>();
    resp.put("status", ErrorCodes.WORKFLOW_PATH_EXISTS.getErrorCode());
    resp.put("message", ErrorCodes.WORKFLOW_PATH_EXISTS.getDescription());
    return Response.status(Response.Status.BAD_REQUEST).entity(resp)
      .build();
  }

  @GET
  @Path("/readWorkflowDetail")
  public Response getWorkflowDetail(
    @QueryParam("workflowXmlPath") String workflowPath) {
    WorkflowFileInfo workflowDetails = workflowFilesService
      .getWorkflowDetails(workflowPath, null);
    return Response.ok(workflowDetails).build();
  }

  @GET
  @Path("/readWorkflow")
  public Response readWorkflow(
    @QueryParam("workflowPath") String workflowPath,@QueryParam("jobType") String jobTypeStr) {
    WorkflowFileInfo workflowDetails = workflowFilesService
      .getWorkflowDetails(workflowPath,JobType.valueOf(jobTypeStr));
    String filePath;
    String responseType;

    if (workflowPath.endsWith(Constants.WF_DRAFT_EXTENSION) || workflowDetails.getIsDraftCurrent()){
      filePath=workflowFilesService.getWorkflowDraftFileName(workflowPath,JobType.valueOf(jobTypeStr));
      responseType="draft";
    }else{
      filePath=workflowFilesService.getWorkflowFileName(workflowPath,JobType.valueOf(jobTypeStr));
      responseType="xml";
    }
    try {
      final InputStream is = workflowFilesService
        .readWorkflowXml(filePath);
      StreamingOutput streamer = new StreamingOutput() {
        @Override
        public void write(OutputStream os) throws IOException,
          WebApplicationException {
          IOUtils.copy(is, os);
          is.close();
          os.close();
        }
      };
      return Response.ok(streamer).header("response-type",responseType).status(200).build();
    } catch (IOException e) {
      return getRespCodeForException(e);
    }
  }

  @GET
  @Path("/readWorkflowXml")
  public Response readWorkflowXml(
    @QueryParam("workflowXmlPath") String workflowPath,@QueryParam("jobType") String jobTypeStr) {
    if (StringUtils.isEmpty(workflowPath)) {
      throw new RuntimeException("workflowXmlPath can't be empty.");
    }

    try {
      final InputStream is = workflowFilesService
        .readWorkflowXml(workflowPath);
      StreamingOutput streamer = new StreamingOutput() {
        @Override
        public void write(OutputStream os) throws IOException,
          WebApplicationException {
          IOUtils.copy(is, os);
          is.close();
          os.close();
        }
      };
      return Response.ok(streamer).status(200).build();
    } catch (IOException e) {
      return getRespCodeForException(e);
    }
  }

  private HashMap<String, String> getErrorDetails(String status,
                                                  String message, Exception ex) {
    HashMap<String, String> resp = new HashMap<String, String>();
    resp.put("status", status);
    if (message != null) {
      resp.put("message", message);
    }
    if (ex != null) {
      resp.put("stackTrace", ExceptionUtils.getFullStackTrace(ex));
    }
    return resp;
  }

  @GET
  @Path("/getDag")
  @Produces("image/png")
  public Response getDag(@Context HttpHeaders headers,
                         @Context UriInfo ui, @QueryParam("jobid") String jobid) {
    Map<String, String> newHeaders = utils.getHeaders(headers);
    final InputStream is = oozieDelegate.readFromOozie(headers,
      oozieDelegate.getDagUrl(jobid), HttpMethod.GET, null,
      newHeaders);
    StreamingOutput streamer = new StreamingOutput() {
      @Override
      public void write(OutputStream os) throws IOException,
        WebApplicationException {
        IOUtils.copy(is, os);
        is.close();
        os.close();
      }

    };
    return Response.ok(streamer).status(200).build();
  }

  @GET
  @Path("/{path: .*}")
  public Response handleGet(@Context HttpHeaders headers, @Context UriInfo ui) {
    try {
      return oozieDelegate.consumeService(headers, ui.getAbsolutePath()
        .getPath(), ui.getQueryParameters(), HttpMethod.GET, null);
    } catch (Exception ex) {
      LOGGER.error("Error in GET proxy", ex);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
        .entity(getErrorDetailsForException("Oozie", ex)).build();
    }
  }

  @POST
  @Path("/{path: .*}")
  public Response handlePost(String xml, @Context HttpHeaders headers,
                             @Context UriInfo ui) {
    try {

      return oozieDelegate.consumeService(headers, ui.getAbsolutePath()
        .getPath(), ui.getQueryParameters(), HttpMethod.POST, xml);
    } catch (Exception ex) {
      LOGGER.error("Error in POST proxy", ex);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
        .entity(getErrorDetailsForException("Oozie", ex)).build();
    }
  }

  @DELETE
  @Path("/{path: .*}")
  public Response handleDelete(@Context HttpHeaders headers,
                               @Context UriInfo ui) {
    try {
      return oozieDelegate.consumeService(headers, ui.getAbsolutePath()
        .getPath(), ui.getQueryParameters(), HttpMethod.POST, null);
    } catch (Exception ex) {
      LOGGER.error("Error in DELETE proxy", ex);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
        .entity(getErrorDetailsForException("Oozie", ex)).build();
    }
  }

  @PUT
  @Path("/{path: .*}")
  public Response handlePut(String body, @Context HttpHeaders headers,
                            @Context UriInfo ui) {
    try {
      return oozieDelegate.consumeService(headers, ui.getAbsolutePath()
        .getPath(), ui.getQueryParameters(), HttpMethod.PUT, body);
    } catch (Exception ex) {
      LOGGER.error("Error in PUT proxy", ex);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
        .entity(getErrorDetailsForException("Oozie", ex)).build();
    }
  }

  private Map<String, String> getErrorDetailsForException(String component,
                                                          Exception ex) {
    String errorCode = component + "exception";
    String errorMessage = component + " Exception";
    if (ex instanceof RuntimeException) {
      Throwable cause = ex.getCause();
      if (cause instanceof IOException) {
        errorCode = component + "io.exception";
        errorMessage = component + "IO Exception";
      }
    }
    return getErrorDetails(errorCode, errorMessage, ex);
  }
}
