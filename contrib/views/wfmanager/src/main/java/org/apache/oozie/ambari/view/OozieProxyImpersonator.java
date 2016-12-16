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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.view.ViewContext;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.hadoop.security.AccessControlException;
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

	private static final String OOZIEPARAM_PREFIX = "oozieparam.";
	private static final int OOZIEPARAM_PREFIX_LENGTH = OOZIEPARAM_PREFIX
			.length();
	private static final String EQUAL_SYMBOL = "=";
	private static final String OOZIE_WF_RERUN_FAILNODES_CONF_KEY = "oozie.wf.rerun.failnodes";
	private static final String OOZIE_USE_SYSTEM_LIBPATH_CONF_KEY = "oozie.use.system.libpath";

	private ViewContext viewContext;

	private static final String USER_NAME_HEADER = "user.name";
	private static final String USER_OOZIE_SUPER = "oozie";
	private static final String DO_AS_HEADER = "doAs";

	private static final String SERVICE_URI_PROP = "oozie.service.uri";
	private static final String DEFAULT_SERVICE_URI = "http://sandbox.hortonworks.com:11000/oozie";
	private Utils utils = new Utils();
	private AmbariIOUtil ambariIOUtil;
	private OozieUtils oozieUtils = new OozieUtils();
	private HDFSFileUtils hdfsFileUtils;
	private WorkflowFilesService workflowFilesService;
	//private WorkflowManagerService workflowManagerService;

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
		ambariIOUtil=new AmbariIOUtil(viewContext);
		//workflowManagerService = new WorkflowManagerService(viewContext);
		LOGGER.info(String.format(
				"OozieProxyImpersonator initialized for instance: %s",
				viewContext.getInstanceName()));

	}

	@Path("/fileServices")
	public FileServices fileServices() {
		return new FileServices(viewContext);
	}

   @Path("/wfprojects")
    public WorkflowsManagerResource workflowsManagerResource() {
        return new WorkflowsManagerResource(viewContext);
    }
	
	@GET
	@Path("/getCurrentUserName")
	public Response getCurrentUserName() {
		return Response.ok(viewContext.getUsername()).build();
	}

	@POST
	@Path("/submitJob")
	@Consumes({ MediaType.TEXT_PLAIN + "," + MediaType.TEXT_XML })
	public Response submitJob(String postBody, @Context HttpHeaders headers,
			@Context UriInfo ui, @QueryParam("app.path") String appPath,
			@DefaultValue("false") @QueryParam("overwrite") Boolean overwrite,
			@QueryParam("jobType") String jobType) {
		LOGGER.info("submit workflow job called");
		return submitJobInternal(postBody, headers, ui, appPath, overwrite,
				JobType.valueOf(jobType));
	}

	@POST
	@Path("/submitWorkflow")
	@Consumes({ MediaType.TEXT_PLAIN + "," + MediaType.TEXT_XML })
	public Response submitWorkflow(String postBody,
			@Context HttpHeaders headers, @Context UriInfo ui,
			@QueryParam("app.path") String appPath,
			@DefaultValue("false") @QueryParam("overwrite") Boolean overwrite) {
		LOGGER.info("submit workflow job called");
		return submitJobInternal(postBody, headers, ui, appPath, overwrite,
				JobType.WORKFLOW);
	}

	@POST
	@Path("/saveWorkflow")
	@Consumes({ MediaType.TEXT_PLAIN + "," + MediaType.TEXT_XML })
	public Response saveWorkflow(String postBody, @Context HttpHeaders headers,
			@Context UriInfo ui, @QueryParam("app.path") String appPath,
			@DefaultValue("false") @QueryParam("overwrite") Boolean overwrite) {
		LOGGER.info("save workflow  called");
		if (StringUtils.isEmpty(appPath)) {
			throw new RuntimeException("app path can't be empty.");
		}
		appPath = appPath.trim();
		if (!overwrite) {
			boolean fileExists = hdfsFileUtils.fileExists(appPath);
			if (fileExists) {
				return getFileExistsResponse();
			}
		}
		postBody = utils.formatXml(postBody);
		try {
			String filePath = workflowFilesService.createWorkflowFile(appPath,
					postBody, overwrite);
			LOGGER.info(String.format(
					"submit workflow job done. filePath=[%s]", filePath));
		/*	workflowManagerService.saveWorkflow(appPath, JobType.WORKFLOW,
                    "todo description", viewContext.getUsername());*/
			return Response.ok().build();
		} catch (Exception ex) {
			LOGGER.error(ex.getMessage(), ex);
			return getRespCodeForException(ex);

		}
	}
	
	@POST
	@Path("/publishAsset")
	@Consumes({ MediaType.TEXT_PLAIN + "," + MediaType.TEXT_XML })
	public Response publishAsset(String postBody, @Context HttpHeaders headers,
			@Context UriInfo ui, @QueryParam("uploadPath") String uploadPath,
			@DefaultValue("false") @QueryParam("overwrite") Boolean overwrite) {
		LOGGER.info("publish asset called");
		if (StringUtils.isEmpty(uploadPath)) {
			throw new RuntimeException("upload path can't be empty.");
		}
		uploadPath = uploadPath.trim();
		Response dryRunResponse = validateAsset(headers, postBody, ui.getQueryParameters());
		if (dryRunResponse.getStatus() == 200) {
			return saveAsset(postBody, uploadPath, overwrite);
		}
		return dryRunResponse;
	}

	private Response validateAsset(HttpHeaders headers, String postBody, MultivaluedMap<String, String> queryParams) {
		String workflowXml = oozieUtils.generateWorkflowXml(postBody);	
		try {
			String tempWfPath = "/user/"+viewContext.getUsername()+"/tmpooziewfs/tempwf.xml";
			hdfsFileUtils.writeToFile(tempWfPath, workflowXml, true);
			queryParams.put("oozieparam.action",getAsList("dryrun"));
			queryParams.put("oozieconfig.rerunOnFailure",getAsList("false"));
			queryParams.put("oozieconfig.useSystemLibPath",getAsList("true"));
			queryParams.put("resourceManager",getAsList("useDefault"));
			String dryRunResp = submitWorkflowJobToOozie(headers,tempWfPath,queryParams,JobType.WORKFLOW);
			LOGGER.info(String.format("resp from validating asset=[%s]",dryRunResp));
			if (dryRunResp != null && dryRunResp.trim().startsWith("{")) {
				return Response.status(Response.Status.OK).entity(dryRunResp).build();
			} else {
				HashMap<String, String> resp = new HashMap<String, String>();
				resp.put("status", ErrorCodes.OOZIE_SUBMIT_ERROR.getErrorCode());
				resp.put("message", dryRunResp);
				//resp.put("stackTrace", dryRunResp);				
				return Response.status(Response.Status.BAD_REQUEST).entity(resp).build();
			}			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private List<String> getAsList(String string) {
		ArrayList<String> li=new ArrayList<>(1);
		li.add(string);
		return li;
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
			LOGGER.info(String.format(
					"publish asset job done. filePath=[%s]", filePath));
			return Response.ok().build();
		} catch (Exception ex) {
			LOGGER.error(ex.getMessage(), ex);
			return getRespCodeForException(ex);
		}
	}

	@POST
	@Path("/saveWorkflowDraft")
	@Consumes({ MediaType.TEXT_PLAIN + "," + MediaType.TEXT_XML })
	public Response saveDraft(String postBody, @Context HttpHeaders headers,
			@Context UriInfo ui, @QueryParam("app.path") String appPath,
			@DefaultValue("false") @QueryParam("overwrite") Boolean overwrite)
			throws IOException {
		LOGGER.info("save workflow  called");
		if (StringUtils.isEmpty(appPath)) {
			throw new RuntimeException("app path can't be empty.");
		}
		appPath = appPath.trim();
		workflowFilesService.saveDraft(appPath, postBody, overwrite);
	/*	 workflowManagerService.saveWorkflow(appPath, JobType.WORKFLOW,
	                "todo description", viewContext.getUsername());*/
		return Response.ok().build();
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
	public Response discardDraft(@QueryParam("workflowXmlPath") String workflowPath) throws IOException{
		 workflowFilesService.discardDraft(workflowPath);
		return Response.ok().build();
	}

	private Response submitJobInternal(String postBody, HttpHeaders headers,
			UriInfo ui, String appPath, Boolean overwrite, JobType jobType) {
		if (StringUtils.isEmpty(appPath)) {
			throw new RuntimeException("app path can't be empty.");
		}
		appPath = appPath.trim();
		if (!overwrite) {
			boolean fileExists = hdfsFileUtils.fileExists(appPath);
			if (fileExists) {
				return getFileExistsResponse();
			}
		}
		postBody = utils.formatXml(postBody);
		try {
			String filePath = hdfsFileUtils.writeToFile(appPath, postBody,
					overwrite);
			LOGGER.info(String.format(
					"submit workflow job done. filePath=[%s]", filePath));
		} catch (Exception ex) {
			LOGGER.error(ex.getMessage(), ex);
			return getRespCodeForException(ex);

		}
		/* workflowManagerService.saveWorkflow(appPath, jobType,
	                "todo description", viewContext.getUsername());*/
		String response = submitWorkflowJobToOozie(headers, appPath,
				ui.getQueryParameters(), jobType);
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
	public Response isDraftAvailable(@QueryParam("workflowXmlPath") String workflowPath){
		WorkflowFileInfo workflowDetails = workflowFilesService.getWorkflowDetails(workflowPath);
		return Response.ok(workflowDetails).build();
	}
	
	@GET
	@Path("/readWorkflowXml")
	public Response readWorkflowXxml(
			@QueryParam("workflowXmlPath") String workflowPath) {
		if (StringUtils.isEmpty(workflowPath)) {
			throw new RuntimeException("workflowXmlPath can't be empty.");
		}
		try {
			final InputStream is = workflowFilesService.readWorkflowXml(workflowPath);
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
	public Response submitWorkflow(@Context HttpHeaders headers,
			@Context UriInfo ui, @QueryParam("jobid") String jobid) {
		String imgUrl = getServiceUri() + "/v2/job/" + jobid + "?show=graph";
		Map<String, String> newHeaders = utils.getHeaders(headers);
		final InputStream is = readFromOozie(headers, imgUrl, HttpMethod.GET,
				null, newHeaders);
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
			String serviceURI = buildURI(ui);
			return consumeService(headers, serviceURI, HttpMethod.GET, null);
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
			String serviceURI = buildURI(ui);
			return consumeService(headers, serviceURI, HttpMethod.POST, xml);
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
			String serviceURI = buildURI(ui);
			return consumeService(headers, serviceURI, HttpMethod.POST, null);
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
			String serviceURI = buildURI(ui);
			return consumeService(headers, serviceURI, HttpMethod.PUT, body);
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

	private String submitWorkflowJobToOozie(HttpHeaders headers,
			String filePath, MultivaluedMap<String, String> queryParams,
			JobType jobType) {
		String nameNode = "hdfs://"
				+ viewContext.getCluster().getConfigurationValue("hdfs-site",
						"dfs.namenode.rpc-address");

		if (!queryParams.containsKey("config.nameNode")) {
			ArrayList<String> nameNodes = new ArrayList<String>();
			LOGGER.info("Namenode===" + nameNode);
			nameNodes.add(nameNode);
			queryParams.put("config.nameNode", nameNodes);
		}

		Map<String, String> workflowConigs = getWorkflowConfigs(filePath,
				queryParams, jobType, nameNode);
		String configXMl = oozieUtils.generateConfigXml(workflowConigs);
		LOGGER.info("Config xml==" + configXMl);
		HashMap<String, String> customHeaders = new HashMap<String, String>();
		customHeaders.put("Content-Type", "application/xml;charset=UTF-8");
		Response serviceResponse = consumeService(headers, getServiceUri()
				+ "/v2/jobs?" + getJobSumbitOozieParams(queryParams),
				HttpMethod.POST, configXMl, customHeaders);

		LOGGER.info("Resp from oozie status entity=="
				+ serviceResponse.getEntity());
		if (serviceResponse.getEntity() instanceof String) {
			return (String) serviceResponse.getEntity();
		} else {
			return "success";
		}

	}

	private Map<String, String> getWorkflowConfigs(String filePath,
			MultivaluedMap<String, String> queryParams, JobType jobType,
			String nameNode) {
		HashMap<String, String> workflowConigs = new HashMap<String, String>();
		if (queryParams.containsKey("resourceManager")
				&& "useDefault".equals(queryParams.getFirst("resourceManager"))) {
			String jobTrackerNode = viewContext.getCluster()
					.getConfigurationValue("yarn-site",
							"yarn.resourcemanager.address");
			LOGGER.info("jobTrackerNode===" + jobTrackerNode);
			workflowConigs.put("resourceManager", jobTrackerNode);
			workflowConigs.put("jobTracker", jobTrackerNode);
		}
		if (queryParams != null) {
			for (Map.Entry<String, List<String>> entry : queryParams.entrySet()) {
				if (entry.getKey().startsWith("config.")) {
					if (entry.getValue() != null && entry.getValue().size() > 0) {
						workflowConigs.put(entry.getKey().substring(7), entry
								.getValue().get(0));
					}
				}
			}
		}

		if (queryParams.containsKey("oozieconfig.useSystemLibPath")) {
			String useSystemLibPath = queryParams
					.getFirst("oozieconfig.useSystemLibPath");
			workflowConigs.put(OOZIE_USE_SYSTEM_LIBPATH_CONF_KEY,
					useSystemLibPath);
		} else {
			workflowConigs.put(OOZIE_USE_SYSTEM_LIBPATH_CONF_KEY, "true");
		}
		if (queryParams.containsKey("oozieconfig.rerunOnFailure")) {
			String rerunFailnodes = queryParams
					.getFirst("oozieconfig.rerunOnFailure");
			workflowConigs.put(OOZIE_WF_RERUN_FAILNODES_CONF_KEY,
					rerunFailnodes);
		} else {
			workflowConigs.put(OOZIE_WF_RERUN_FAILNODES_CONF_KEY, "true");
		}
		workflowConigs.put("user.name", viewContext.getUsername());
		workflowConigs.put(oozieUtils.getJobPathPropertyKey(jobType), nameNode
				+ filePath);
		return workflowConigs;
	}

	private String getJobSumbitOozieParams(
			MultivaluedMap<String, String> queryParams) {
		StringBuilder query = new StringBuilder();
		if (queryParams != null) {
			for (Map.Entry<String, List<String>> entry : queryParams.entrySet()) {
				if (entry.getKey().startsWith(OOZIEPARAM_PREFIX)) {
					if (entry.getValue() != null && entry.getValue().size() > 0) {
						for (String val : entry.getValue()) {
							query.append(
									entry.getKey().substring(
											OOZIEPARAM_PREFIX_LENGTH))
									.append(EQUAL_SYMBOL).append(val)
									.append("&");
						}
					}
				}
			}
		}
		return query.toString();
	}

	private String buildURI(UriInfo ui) {
		String uiURI = ui.getAbsolutePath().getPath();
		int index = uiURI.indexOf("proxy/") + 5;
		uiURI = uiURI.substring(index);
		String serviceURI = getServiceUri();
		serviceURI += uiURI;
		MultivaluedMap<String, String> params = addOrReplaceUserName(ui
				.getQueryParameters());
		return serviceURI + utils.convertParamsToUrl(params);
	}

	private MultivaluedMap<String, String> addOrReplaceUserName(
			MultivaluedMap<String, String> parameters) {
		for (Map.Entry<String, List<String>> entry : parameters.entrySet()) {
			if ("user.name".equals(entry.getKey())) {
				ArrayList<String> vals = new ArrayList<String>(1);
				vals.add(viewContext.getUsername());
				entry.setValue(vals);
			}
		}
		return parameters;
	}

	private String getServiceUri() {
		String serviceURI = viewContext.getProperties().get(SERVICE_URI_PROP) != null ? viewContext
				.getProperties().get(SERVICE_URI_PROP) : DEFAULT_SERVICE_URI;
		return serviceURI;
	}

	private Response consumeService(HttpHeaders headers, String urlToRead,
			String method, String body) throws Exception {
		return consumeService(headers, urlToRead, method, body, null);
	}

	private Response consumeService(HttpHeaders headers, String urlToRead,
			String method, String body, Map<String, String> customHeaders) {
		Response response = null;
		InputStream stream = readFromOozie(headers, urlToRead, method, body,
				customHeaders);
		String stringResponse = null;
		try {
			stringResponse = IOUtils.toString(stream);
		} catch (IOException e) {
			LOGGER.error("Error while converting stream to string", e);
			throw new RuntimeException(e);
		}
		if (stringResponse.contains(Response.Status.BAD_REQUEST.name())) {
			response = Response.status(Response.Status.BAD_REQUEST)
					.entity(stringResponse).type(MediaType.TEXT_PLAIN).build();
		} else {
			response = Response.status(Response.Status.OK)
					.entity(stringResponse)
					.type(utils.deduceType(stringResponse)).build();
		}
		return response;
	}

	private InputStream readFromOozie(HttpHeaders headers, String urlToRead,
			String method, String body, Map<String, String> customHeaders) {

		Map<String, String> newHeaders = utils.getHeaders(headers);
		newHeaders.put(USER_NAME_HEADER, USER_OOZIE_SUPER);

		newHeaders.put(DO_AS_HEADER, viewContext.getUsername());
		newHeaders.put("Accept", MediaType.APPLICATION_JSON);
		if (customHeaders != null) {
			newHeaders.putAll(customHeaders);
		}
		LOGGER.info(String.format("Proxy request for url: [%s] %s", method,
				urlToRead));

		return ambariIOUtil.readFromUrl(urlToRead, method, body, newHeaders);
	}
}