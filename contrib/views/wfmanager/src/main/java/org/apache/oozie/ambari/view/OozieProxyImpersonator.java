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
package org.apache.oozie.ambari.view;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.ambari.view.URLStreamProvider;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.utils.ambari.AmbariApi;
import org.apache.ambari.view.utils.hdfs.HdfsApi;
import org.apache.ambari.view.utils.hdfs.HdfsUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * This is a class used to bridge the communication between the and the Oozie
 * API executing inside ambari.
 */
public class OozieProxyImpersonator {

	private ViewContext viewContext;
	private AmbariApi ambariApi;
	private HdfsApi _hdfsApi = null;


	private static final String USER_NAME_HEADER = "user.name";
	private static final String USER_OOZIE_SUPER = "oozie"; // TODO get this
															// from Jaas Config?
	private static final String DO_AS_HEADER = "doAs";

	private static final String SERVICE_URI_PROP = "oozie.service.uri";
	private static final String DEFAULT_SERVICE_URI = "http://sandbox.hortonworks.com:11000/oozie";

	protected final static Logger LOGGER = LoggerFactory
			.getLogger(OozieProxyImpersonator.class);

	@Inject
	public OozieProxyImpersonator(ViewContext viewContext) {
		this.viewContext = viewContext;
		this.ambariApi = new AmbariApi(viewContext);
		LOGGER.info(String.format(
				"OozieProxyImpersonator initialized for instance: %s",
				viewContext.getInstanceName()));
	}

	@Path("/fileServices")
	public FileServices fileServices(){
		 return new FileServices(viewContext);
	}

	@POST
	@Path("/submitWorkflow")
	@Consumes({ MediaType.TEXT_PLAIN + "," + MediaType.TEXT_XML })
	public Response submitWorkflow(String postBody,
			@Context HttpHeaders headers, @Context UriInfo ui,
			@QueryParam("app.path") String appPath,@DefaultValue("false") @QueryParam("overwrite") Boolean overwrite) throws IOException {
		LOGGER.info("submit workflow job called");
		try {
			if (StringUtils.isEmpty(appPath)){
				throw new RuntimeException("app path can't be empty.");
			}
			appPath=appPath.trim();
			if (!overwrite){
				boolean fileExists = getHdfsgetApi().exists(appPath);
				LOGGER.info("FILE exists for ["+appPath+"] returned ["+fileExists+"]");
				if (fileExists){
					HashMap<String,String> resp=new HashMap<String,String>();
					resp.put("status","workflow.folder.exists");
					resp.put("message","Workflow Folder exists");
					return Response.status(Response.Status.BAD_REQUEST).entity(resp).build();
					//return Response.status(Response.Status.BAD_REQUEST).entity("Folder exists").build();
				}
			}
			String workflowFile=null;
			if (appPath.endsWith(".xml")){
				workflowFile=appPath;
			}else{
				workflowFile = appPath+(appPath.endsWith("/")?"":"/")+"workflow.xml";
			}

//			if (!appPath.trim().startsWith("/")) {
//				workflowFile = deduceAppPath(appPath);
//			}
			postBody = formatXml(postBody);
			String filePath = createWorkflowFile(postBody, workflowFile,overwrite);
			LOGGER.info(String.format(
					"submit workflow job done. filePath=[%s]", filePath));
			String response = submitWorkflowJobToOozie(headers, appPath,
					ui.getQueryParameters());
			if (response!=null && response.trim().startsWith("{")){//dealing with oozie giving error but with 200 response.
				return Response.status(Response.Status.OK).entity(response).build();
			}else{
				HashMap<String,String> resp=new HashMap<String,String>();
				resp.put("status","workflow.oozie.error");
				resp.put("message",response);
				return Response.status(Response.Status.BAD_REQUEST).entity(resp).build();
			}
		} catch (InterruptedException e) {
			throw new IOException(e);
		} catch (Exception e) {
			throw new IOException(e);
		}
	}
	@GET
	@Path("/readWorkflowXml")
	public Response readWorkflowXxml(@QueryParam("workflowXmlPath") String workflowPath)throws IOException{
		if (StringUtils.isEmpty(workflowPath)) {
			throw new RuntimeException("workflowXmlPath can't be empty.");
		}
		try {
			final FSDataInputStream is = getHdfsgetApi().open(workflowPath);
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
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	@GET
	@Path("/getDag")
	@Produces("image/png")
	public Response submitWorkflow(@Context HttpHeaders headers,
			@Context UriInfo ui, @QueryParam("jobid") String jobid)
			throws IOException {
		String imgUrl = getServiceUri() + "/v2/job/" + jobid + "?show=graph";
		Map<String, String> newHeaders = getHeaders(headers);
		final InputStream is = readFromOozie(headers, imgUrl, HttpMethod.GET, null,
				newHeaders);
		StreamingOutput streamer = new StreamingOutput() {

			@Override
			public void write(OutputStream os) throws IOException,
					WebApplicationException {
				IOUtils.copy(is,os);
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
			LOGGER.error(ex.getMessage(), ex);
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(ex.toString()).build();
		}
	}

	@POST
	@Path("/{path: .*}")
	public Response handlePost(String xml, @Context HttpHeaders headers,
			@Context UriInfo ui) throws IOException {
		try {
			String serviceURI = buildURI(ui);
			return consumeService(headers, serviceURI, HttpMethod.POST, xml);
		} catch (Exception ex) {
			LOGGER.error(ex.getMessage(), ex);
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(ex.toString()).build();
		}
	}

	@DELETE
	@Path("/{path: .*}")
	public Response handleDelete(@Context HttpHeaders headers,
			@Context UriInfo ui) throws IOException {
		try {
			String serviceURI = buildURI(ui);
			return consumeService(headers, serviceURI, HttpMethod.POST, null);
		} catch (Exception ex) {
			LOGGER.error(ex.getMessage(), ex);
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(ex.toString()).build();
		}
	}

	@PUT
	@Path("/{path: .*}")
	public Response handlePut(String body,@Context HttpHeaders headers, @Context UriInfo ui)
			throws IOException {

		try {
			String serviceURI = buildURI(ui);
			return consumeService(headers, serviceURI, HttpMethod.PUT, body);
		} catch (Exception ex) {
			LOGGER.error(ex.getMessage(), ex);
			return Response.status(Response.Status.BAD_REQUEST)
					.entity(ex.toString()).build();
		}
	}

	private String submitWorkflowJobToOozie(HttpHeaders headers,
			String filePath, MultivaluedMap<String, String> queryParams)
			throws Exception {
		String nameNode = "hdfs://"+ viewContext.getCluster().getConfigurationValue(
				"hdfs-site", "dfs.namenode.rpc-address");

		if (!queryParams.containsKey("config.nameNode")) {
			ArrayList<String> nameNodes = new ArrayList<String>();
			LOGGER.info("Namenode===" + nameNode);
			nameNodes.add(nameNode);
			queryParams.put("config.nameNode", nameNodes);
		}

		HashMap<String, String> workflowConigs = new HashMap<String, String>();
		if (queryParams.containsKey("resourceManager") && "useDefault".equals( queryParams.getFirst("resourceManager"))){
			String jobTrackerNode = viewContext.getCluster().getConfigurationValue(
					"yarn-site", "yarn.resourcemanager.address");
			LOGGER.info("jobTrackerNode===" + jobTrackerNode);
			workflowConigs.put("resourceManager", jobTrackerNode);
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
			String useSystemLibPath=queryParams.getFirst("oozieconfig.useSystemLibPath");
			workflowConigs.put("oozie.use.system.libpath", useSystemLibPath);
		}else{
			workflowConigs.put("oozie.use.system.libpath", "true");
		}
		if (queryParams.containsKey("oozieconfig.rerunOnFailure")) {
			String rerunFailnodes=queryParams.getFirst("oozieconfig.rerunOnFailure");
			workflowConigs.put("oozie.wf.rerun.failnodes", rerunFailnodes);
		}else{
			workflowConigs.put("oozie.wf.rerun.failnodes", "true");
		}

		workflowConigs.put("user.name", viewContext.getUsername());
		workflowConigs.put("oozie.wf.application.path", nameNode+filePath);
		String configXMl = generateConigXml(workflowConigs);
		LOGGER.info("Config xml=="+configXMl);
		HashMap<String, String> customHeaders = new HashMap<String, String>();
		customHeaders.put("Content-Type", "application/xml;charset=UTF-8");
		Response serviceResponse = consumeService(headers, getServiceUri()
				+ "/v2/jobs", HttpMethod.POST, configXMl, customHeaders);
		// LOGGER.info("REsp from oozie status code=="+serviceResponse.getStatusInfo().getStatusCode());
		LOGGER.info("REsp from oozie status entity=="
				+ serviceResponse.getEntity());
		if (serviceResponse.getEntity() instanceof String) {
			return (String) serviceResponse.getEntity();
		} else {
			return "success";
		}

	}

	private String createWorkflowFile(String postBody, String workflowFile,boolean overwrite)
			throws Exception, IOException, InterruptedException {
		FSDataOutputStream fsOut = getHdfsgetApi().create(workflowFile, overwrite);
		fsOut.write(postBody.getBytes());
		fsOut.close();
		return workflowFile;
	}


	private String buildURI(UriInfo ui) {

		String uiURI = ui.getAbsolutePath().getPath();
		int index = uiURI.indexOf("proxy/") + 5;
		uiURI = uiURI.substring(index);
		String serviceURI = getServiceUri();
		serviceURI += uiURI;

		MultivaluedMap<String, String> parameters = ui.getQueryParameters();
		StringBuilder urlBuilder = new StringBuilder(serviceURI);
		boolean firstEntry = true;
		for (Map.Entry<String, List<String>> entry : parameters.entrySet()) {
			if ("user.name".equals(entry.getKey())) {
				ArrayList<String> vals = new ArrayList<String>();
				vals.add(viewContext.getUsername());
				entry.setValue(vals);
			}
			if (firstEntry) {
				urlBuilder.append("?");
			} else {
				urlBuilder.append("&");
			}
			boolean firstVal = true;
			for (String val : entry.getValue()) {
				urlBuilder.append(firstVal ? "" : "&").append(entry.getKey())
						.append("=").append(val);
				firstVal = false;
			}
			firstEntry = false;
		}
		return urlBuilder.toString();
	}

	private String getServiceUri() {
		String serviceURI = viewContext.getProperties().get(SERVICE_URI_PROP) != null ? viewContext
				.getProperties().get(SERVICE_URI_PROP) : DEFAULT_SERVICE_URI;
		return serviceURI;
	}

	public Response consumeService(HttpHeaders headers, String urlToRead,
			String method, String body, Map<String, String> customHeaders)
			throws Exception {
		Response response = null;
		InputStream stream = readFromOozie(headers, urlToRead, method, body,
				customHeaders);
		String stringResponse = IOUtils.toString(stream);
		if (stringResponse.contains(Response.Status.BAD_REQUEST.name())) {
			response = Response.status(Response.Status.BAD_REQUEST)
					.entity(stringResponse).type(MediaType.TEXT_PLAIN).build();
		} else {
			response = Response.status(Response.Status.OK)
					.entity(stringResponse).type(deduceType(stringResponse))
					.build();
		}
		return response;
	}

	private InputStream readFromOozie(HttpHeaders headers, String urlToRead,
			String method, String body, Map<String, String> customHeaders)
			throws IOException {
		URLStreamProvider streamProvider = viewContext.getURLStreamProvider();
		Map<String, String> newHeaders = getHeaders(headers);
		newHeaders.put(USER_NAME_HEADER, USER_OOZIE_SUPER);

		newHeaders.put(DO_AS_HEADER, viewContext.getUsername());
		newHeaders.put("Accept", MediaType.APPLICATION_JSON);
		if (customHeaders != null) {
			newHeaders.putAll(customHeaders);
		}
		LOGGER.info(String.format("Proxy request for url: [%s] %s", method,
				urlToRead));
		boolean securityEnabled = isSecurityEnabled();
		LOGGER.debug(String.format("IS security enabled:[%b]",securityEnabled));
		InputStream stream =null;
		if (securityEnabled){
			stream = streamProvider.readAsCurrent(urlToRead, method, body,
					newHeaders);
		}else{
			stream = streamProvider.readFrom(urlToRead, method, body,
					newHeaders);
		}
		return stream;
	}

	public Response consumeService(HttpHeaders headers, String urlToRead,
			String method, String body) throws Exception {
		return consumeService(headers, urlToRead, method, body, null);
	}

	public Map<String, String> getHeaders(HttpHeaders headers) {
		MultivaluedMap<String, String> requestHeaders = headers
				.getRequestHeaders();
		Set<Entry<String, List<String>>> headerEntrySet = requestHeaders
				.entrySet();
		HashMap<String, String> headersMap = new HashMap<String, String>();
		for (Entry<String, List<String>> headerEntry : headerEntrySet) {
			String key = headerEntry.getKey();
			List<String> values = headerEntry.getValue();
			headersMap.put(key, strJoin(values, ","));
		}
		return headersMap;
	}

	public String strJoin(List<String> strings, String separator) {
		StringBuilder stringBuilder = new StringBuilder();
		for (int i = 0, il = strings.size(); i < il; i++) {
			if (i > 0) {
				stringBuilder.append(separator);
			}
			stringBuilder.append(strings.get(i));
		}
		return stringBuilder.toString();
	}

	private MediaType deduceType(String stringResponse) {
		if (stringResponse.startsWith("{")) {
			return MediaType.APPLICATION_JSON_TYPE;
		} else if (stringResponse.startsWith("<")) {
			return MediaType.TEXT_XML_TYPE;
		} else {
			return MediaType.APPLICATION_JSON_TYPE;
		}
	}

	public HdfsApi getHdfsgetApi() {
		if (_hdfsApi == null) {
			try {
				_hdfsApi = HdfsUtil.connectToHDFSApi(viewContext);
			} catch (Exception ex) {
				throw new RuntimeException(
						"HdfsApi connection failed. Check \"webhdfs.url\" property",
						ex);
			}
		}
		return _hdfsApi;
	}

	private String generateConigXml(Map<String, String> map) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db;
		try {
			db = dbf.newDocumentBuilder();
			Document doc = db.newDocument();
			Element configElement = doc.createElement("configuration");
			doc.appendChild(configElement);
			for (Map.Entry<String, String> entry : map.entrySet()) {
				Element propElement = doc.createElement("property");
				configElement.appendChild(propElement);
				Element nameElem = doc.createElement("name");
				nameElem.setTextContent(entry.getKey());
				Element valueElem = doc.createElement("value");
				valueElem.setTextContent(entry.getValue());
				propElement.appendChild(nameElem);
				propElement.appendChild(valueElem);
		}
			DOMSource domSource = new DOMSource(doc);
			StringWriter writer = new StringWriter();
			StreamResult result = new StreamResult(writer);
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(
					"{http://xml.apache.org/xslt}indent-amount", "4");
			transformer.transform(domSource, result);
			return writer.toString();
		} catch (ParserConfigurationException | TransformerException e) {
			throw new RuntimeException(e);
		}

	}

	private String formatXml(String xml) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db;
		try {
			db = dbf.newDocumentBuilder();
			StreamResult result = new StreamResult(new StringWriter());
			Document document = db
					.parse(new InputSource(new StringReader(xml)));
			Transformer transformer = TransformerFactory.newInstance()
					.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(
					"{http://xml.apache.org/xslt}indent-amount", "4");
			DOMSource source = new DOMSource(document);
			transformer.transform(source, result);
			return result.getWriter().toString();
		} catch (ParserConfigurationException | SAXException | IOException
				| TransformerFactoryConfigurationError | TransformerException e) {
			throw new RuntimeException(e);
		}
	}

	private boolean isSecurityEnabled() {
		boolean securityEnabled = Boolean.valueOf(getHadoopConfigs().get(
				"security_enabled"));
		return securityEnabled;
	}

	private Map<String, String> getHadoopConfigs() {
		return viewContext.getInstanceData();
	}


}
