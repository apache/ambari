/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.view.hive.resources.uploads;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;
import org.apache.ambari.view.hive.BaseService;
import org.apache.ambari.view.hive.client.ColumnDescription;
import org.apache.ambari.view.hive.client.HiveClientException;
import org.apache.ambari.view.hive.persistence.utils.ItemNotFound;
import org.apache.ambari.view.hive.resources.jobs.NoOperationStatusSetException;
import org.apache.ambari.view.hive.resources.jobs.viewJobs.Job;
import org.apache.ambari.view.hive.resources.jobs.viewJobs.JobController;
import org.apache.ambari.view.hive.resources.jobs.viewJobs.JobImpl;
import org.apache.ambari.view.hive.resources.jobs.viewJobs.JobResourceManager;
import org.apache.ambari.view.hive.utils.ServiceFormattedException;
import org.apache.ambari.view.hive.utils.SharedObjectsFactory;
import org.apache.ambari.view.utils.ambari.AmbariApi;
import org.apache.commons.io.input.ReaderInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servlet for queries
 * API:
 * POST /preview
 * POST /upload
 * POST /createTable
 * GET /createTable/status
 */
public class UploadService extends BaseService {

  private AmbariApi ambariApi;

  protected JobResourceManager resourceManager;

  final private String HIVE_META_STORE_LOCATION_KEY = "hive.metastore.warehouse.dir";
  final private String HIVE_SITE = "hive-site";
  final private String HIVE_DEFAULT_DB = "default";

  @POST
  @Path("/preview")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public Response uploadForPreview(
          @FormDataParam("file") InputStream uploadedInputStream,
          @FormDataParam("file") FormDataContentDisposition fileDetail) {

    ParseOptions parseOptions = new ParseOptions();
    parseOptions.setOption(ParseOptions.OPTIONS_FILE_TYPE, ParseOptions.FILE_TYPE_CSV);
    parseOptions.setOption(ParseOptions.OPTIONS_HEADER, ParseOptions.HEADER_FIRST_RECORD);

    try {
      DataParser dataParser = new DataParser(new InputStreamReader(uploadedInputStream), parseOptions);

      dataParser.parsePreview();

      Map<String, Object> retData = new HashMap<String, Object>();
      retData.put("header", dataParser.getHeader());
      retData.put("rows", dataParser.getPreviewRows());
      retData.put("isFirstRowHeader", true);

      JSONObject jsonObject = new JSONObject(retData);
      return Response.ok(jsonObject).build();
    } catch (IOException e) {
      throw new ServiceFormattedException(e.getMessage(), e);
    }
  }

  public static class TableInput {
    public Boolean isFirstRowHeader;
    public String header;
    public String tableName;
    public String databaseName;

    public TableInput() {
    }

    public Boolean getIsFirstRowHeader() {
      return isFirstRowHeader;
    }

    public void setIsFirstRowHeader(Boolean isFirstRowHeader) {
      this.isFirstRowHeader = isFirstRowHeader;
    }

    public String getHeader() {
      return header;
    }

    public void setHeader(String header) {
      this.header = header;
    }

    public String getTableName() {
      return tableName;
    }

    public void setTableName(String tableName) {
      this.tableName = tableName;
    }

    public String getDatabaseName() {
      return databaseName;
    }

    public void setDatabaseName(String databaseName) {
      this.databaseName = databaseName;
    }
  }

  @Path("/createTable")
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response createTable(TableInput tableInput) throws IllegalAccessException, InvocationTargetException, ItemNotFound, NoSuchMethodException {
    String header = tableInput.getHeader();
    String databaseName = tableInput.getDatabaseName();
    String tableName = tableInput.getTableName();
    Boolean isFirstRowHeader = (Boolean) tableInput.getIsFirstRowHeader();

    Object headerObj = JSONValue.parse(header);
    JSONArray array = (JSONArray) headerObj;
    List<ColumnDescription> cdList = new ArrayList<ColumnDescription>(array.size());
    for (Object o : array) {
      JSONObject jo = (JSONObject) o;
      String name = (String) jo.get("name");
      String type = (String) jo.get("type");
      Long p = (Long) jo.get("position");
      Integer position = p != null ? p.intValue() : 0;

      ColumnDescriptionImpl cdi = new ColumnDescriptionImpl(name, type, position);
      cdList.add(cdi);
    }

    Map jobInfo = new HashMap<String, String>();//PropertyUtils.describe(request.job);
    jobInfo.put("title", "Internal Table Creation");
    jobInfo.put("forcedContent", generateCreateQuery(databaseName, tableName, cdList));
    jobInfo.put("dataBase", databaseName);

    LOG.info("jobInfo : " + jobInfo);
    Job job = new JobImpl(jobInfo);
    LOG.info("job : " + job);
    getResourceManager().create(job);

    JobController createdJobController = getResourceManager().readController(job.getId());
    createdJobController.submit();
    getResourceManager().saveIfModified(createdJobController);

    String filePath = (databaseName == null || databaseName.equals(HIVE_DEFAULT_DB)) ? "" : databaseName + ".db/";
    filePath += tableName + "/" + tableName + ".csv";

    JSONObject jobObject = new JSONObject();
    jobObject.put("jobId", job.getId());
    jobObject.put("filePath", filePath);

    LOG.info("Create table query submitted : file should be uploaded at location : {}", filePath);
    return Response.ok(jobObject).status(201).build();
  }

  @Path("/createTable/status")
  @GET
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response isTableCreated(@QueryParam("jobId") int jobId) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, ItemNotFound, HiveClientException, NoOperationStatusSetException {
    JobController jobController = getResourceManager().readController(jobId + "");
    LOG.info("jobController.getStatus().status : {} for job : {}", jobController.getStatus().status, jobController.getJob().getId());
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("status", jobController.getStatus().status);
    return Response.ok(jsonObject).build();
  }

  @Path("/upload")
  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public Response uploadFile(
          @FormDataParam("file") InputStream uploadedInputStream,
          @FormDataParam("file") FormDataContentDisposition fileDetail,
          @FormDataParam("isFirstRowHeader") Boolean isFirstRowHeader,
          @FormDataParam("filePath") String filePath

  ) throws IOException, InterruptedException {
    LOG.info("inside uploadFile : isFirstRowHeader : {} , filePath : {}", isFirstRowHeader, filePath);
/*  This is not working as expected.
    ParseOptions parseOptions = new ParseOptions();
    parseOptions.setOption(ParseOptions.OPTIONS_FILE_TYPE, ParseOptions.FILE_TYPE_CSV);
    parseOptions.setOption(ParseOptions.HEADERS,cdList);

    if(isFirstRowHeader)
      parseOptions.setOption(ParseOptions.OPTIONS_HEADER,ParseOptions.HEADER_FIRST_RECORD);
    else
      parseOptions.setOption(ParseOptions.OPTIONS_HEADER,ParseOptions.HEADER_PROVIDED_BY_USER);

    DataParser dataParser = new DataParser(new InputStreamReader(uploadedInputStream),parseOptions);

    // remove first row if it is header and send the rest to HDFS
    if(isFirstRowHeader){
      if( dataParser.iterator().hasNext() ){
        dataParser.iterator().next();
      }
    }

    Reader csvReader = dataParser.getCSVReader();
*/

    // TODO : workaround alert as above method is not working properly
    // remove first row if it is header and send the rest to HDFS
    Reader r = new InputStreamReader(uploadedInputStream);
    if (isFirstRowHeader) {
      BufferedReader br = new BufferedReader(r, 1); //
      br.readLine(); // TODO : remove the header line. Wrong if first record is beyond first endline
    }

    String basePath = getHiveMetaStoreLocation();
    if (null == basePath)
      basePath = "/apps/hive/warehouse";

    if (!basePath.endsWith("/"))
      basePath = basePath + "/";

    String fullPath = basePath + filePath;

    uploadTable(new ReaderInputStream(r), fullPath);

    LOG.info("create the table successfully at : {}", fullPath);
    return Response.ok().build();
  }

  private String getHiveMetaStoreLocation() {
    return this.getAmbariApi().getCluster().getConfigurationValue(HIVE_SITE, HIVE_META_STORE_LOCATION_KEY);
  }

  private void uploadTable(InputStream is, String path) throws IOException, InterruptedException {
    if (!path.endsWith("/")) {
      path = path + "/";
    }

    uploadFile(path, is);
  }

  private void uploadFile(final String filePath, InputStream uploadedInputStream)
          throws IOException, InterruptedException {
    byte[] chunk = new byte[1024];
    FSDataOutputStream out = getSharedObjectsFactory().getHdfsApi().create(filePath, false);
    while (uploadedInputStream.read(chunk) != -1) {
      out.write(chunk);
    }
    out.close();
  }


  protected synchronized JobResourceManager getResourceManager() {
    if (resourceManager == null) {
      SharedObjectsFactory connectionsFactory = getSharedObjectsFactory();
      resourceManager = new JobResourceManager(connectionsFactory, context);
    }
    return resourceManager;
  }

  protected synchronized AmbariApi getAmbariApi() {
    if (null == ambariApi) {
      ambariApi = new AmbariApi(this.context);
    }
    return ambariApi;
  }

  private String generateCreateQuery(String databaseName, String tableName, List<ColumnDescription> cdList) {
    return new QueryGenerator().generateCreateQuery(new TableInfo(databaseName, tableName, cdList));
  }
}
