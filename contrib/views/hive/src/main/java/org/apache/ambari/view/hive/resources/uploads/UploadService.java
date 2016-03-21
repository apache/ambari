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
import org.apache.ambari.view.hive.persistence.utils.ItemNotFound;
import org.apache.ambari.view.hive.resources.jobs.viewJobs.Job;
import org.apache.ambari.view.hive.resources.jobs.viewJobs.JobController;
import org.apache.ambari.view.hive.resources.jobs.viewJobs.JobImpl;
import org.apache.ambari.view.hive.resources.jobs.viewJobs.JobResourceManager;
import org.apache.ambari.view.hive.resources.uploads.parsers.DataParser;
import org.apache.ambari.view.hive.resources.uploads.parsers.ParseOptions;
import org.apache.ambari.view.hive.resources.uploads.parsers.PreviewData;
import org.apache.ambari.view.hive.resources.uploads.query.*;
import org.apache.ambari.view.hive.utils.ServiceFormattedException;
import org.apache.ambari.view.hive.utils.SharedObjectsFactory;
import org.apache.ambari.view.utils.ambari.AmbariApi;
import org.apache.commons.io.input.ReaderInputStream;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.json.simple.JSONObject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * UI driven end points for creation of new hive table and inserting data into it.
 * It uploads a file, parses it partially based on its type, generates preview,
 * creates temporary hive table for storage as CSV and actual hive table,
 * uploads the file again, parses it, create CSV stream and upload to hdfs in temporary table,
 * insert rows from temporary table to actual table, delete temporary table.
 * <p/>
 * API:
 * POST /preview : takes stream, parses it and returns preview rows, headers and column type suggestions
 * POST /createTable : runs hive query to create table in hive
 * POST /upload : takes stream, parses it and converts it into CSV and uploads it to the temporary table
 * POST /insertIntoTable : runs hive query to insert data from temporary table to actual hive table
 * POST /deleteTable : deletes the temporary table
 */
public class UploadService extends BaseService {

  private AmbariApi ambariApi;
  protected JobResourceManager resourceManager;

  final private static String HIVE_METASTORE_LOCATION_KEY = "hive.metastore.warehouse.dir";
  final private static String HIVE_SITE = "hive-site";
  final private static String HIVE_METASTORE_LOCATION_KEY_VIEW_PROPERTY = HIVE_METASTORE_LOCATION_KEY;
  private static final String HIVE_DEFAULT_METASTORE_LOCATION = "/apps/hive/warehouse" ;
  final private static String HIVE_DEFAULT_DB = "default";

  @POST
  @Path("/previewFromHdfs")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response uploadForPreviewFromHDFS(UploadFromHdfsInput input) {

    InputStream uploadedInputStream = null;
    try {
      uploadedInputStream = getHDFSFileStream(input.getHdfsPath());
      PreviewData pd = generatePreview(input.getIsFirstRowHeader(), input.getInputFileType(), uploadedInputStream);
      String tableName = getBasenameFromPath(input.getHdfsPath());
      return createPreviewResponse(pd, input.getIsFirstRowHeader(),tableName);
    } catch (Exception e) {
      LOG.error("Exception occurred while generating preview for hdfs file : " + input.getHdfsPath(), e);
      throw new ServiceFormattedException(e.getMessage(), e);
    } finally {
      if (null != uploadedInputStream) {
        try {
          uploadedInputStream.close();
        } catch (IOException e) {
          LOG.error("Exception occured while closing the HDFS file stream for path " + input.getHdfsPath(), e);
        }
      }
    }
  }

  @POST
  @Path("/preview")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public Response uploadForPreview(
          @FormDataParam("file") InputStream uploadedInputStream,
          @FormDataParam("file") FormDataContentDisposition fileDetail,
          @FormDataParam("isFirstRowHeader") Boolean isFirstRowHeader,
          @FormDataParam("inputFileType") String inputFileType
  ) {
    try {
      PreviewData pd = generatePreview(isFirstRowHeader, inputFileType, uploadedInputStream);
      return createPreviewResponse(pd, isFirstRowHeader,getBasename(fileDetail.getFileName()));
    } catch (Exception e) {
      LOG.error("Exception occurred while generating preview for local file", e);
      throw new ServiceFormattedException(e.getMessage(), e);
    }
  }


  @Path("/createTable")
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response createTable(TableInput tableInput) {
    try {
      List<ColumnDescriptionImpl> header = tableInput.getHeader();
      String databaseName = tableInput.getDatabaseName();
      String tableName = tableInput.getTableName();
      Boolean isFirstRowHeader = tableInput.getIsFirstRowHeader();
      String fileTypeStr = tableInput.getFileType();
      HiveFileType hiveFileType = HiveFileType.valueOf(fileTypeStr);


      TableInfo ti = new TableInfo(databaseName, tableName, header, hiveFileType);
      String tableCreationQuery = generateCreateQuery(ti);

      LOG.info("tableCreationQuery : {}", tableCreationQuery);

      Job actualTableJob = createJob(tableCreationQuery, databaseName);
      String actualTableJobId = actualTableJob.getId();

      JSONObject jobObject = new JSONObject();
      jobObject.put("jobId", actualTableJobId);

      LOG.info("table creation jobId {}", actualTableJobId);
      return Response.ok(jobObject).status(201).build();
    } catch (Exception e) {
      LOG.error("Exception occurred while creating table with input : " + tableInput, e);
      throw new ServiceFormattedException(e.getMessage(), e);
    }
  }

  @Path("/uploadFromHDFS")
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response uploadFileFromHdfs(UploadFromHdfsInput input ) {
    if (ParseOptions.InputFileType.CSV.toString().equals(input.getInputFileType()) && input.getIsFirstRowHeader().equals(Boolean.FALSE)) {
      // upload using the LOAD query
      LoadQueryInput loadQueryInput = new LoadQueryInput(input.getHdfsPath(), input.getDatabaseName(), input.getTableName());
      String loadQuery = new QueryGenerator().generateLoadQuery(loadQueryInput);

      try {
        Job job = createJob(loadQuery,  input.getDatabaseName());

        JSONObject jo = new JSONObject();
        jo.put("jobId", job.getId());

        return Response.ok(jo).build();
      } catch (Exception e) {
        LOG.error("Exception occurred while creating job for Load From HDFS query : " + loadQuery, e);
        throw new ServiceFormattedException(e.getMessage(), e);
      }

    } else {
      // create stream and upload
      InputStream hdfsStream = null;
      try {
        hdfsStream = getHDFSFileStream(input.getHdfsPath());
        String path = uploadFileFromStream(hdfsStream, input.getIsFirstRowHeader(),input.getInputFileType(),input.getTableName(), input.getDatabaseName());

        JSONObject jo = new JSONObject();
        jo.put("uploadedPath", path);

        return Response.ok(jo).build();
      } catch (Exception e) {
        LOG.error("Exception occurred while uploading the file from HDFS with path : " + input.getHdfsPath(), e);
        throw new ServiceFormattedException(e.getMessage(), e);
      } finally {
        if (null != hdfsStream)
          try {
            hdfsStream.close();
          } catch (IOException e) {
            LOG.error("Exception occured while closing the HDFS stream for path : " + input.getHdfsPath(), e);
          }
      }
    }
  }

  @Path("/upload")
  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  public Response uploadFile(
          @FormDataParam("file") InputStream uploadedInputStream,
          @FormDataParam("file") FormDataContentDisposition fileDetail,
          @FormDataParam("isFirstRowHeader") Boolean isFirstRowHeader,
          @FormDataParam("inputFileType") String inputFileType,   // the format of the file uploaded. CSV/JSON etc.
          @FormDataParam("tableName") String tableName,
          @FormDataParam("databaseName") String databaseName
  ) {
    try {

      String path = uploadFileFromStream(uploadedInputStream,isFirstRowHeader,inputFileType,tableName,databaseName);

      JSONObject jo = new JSONObject();
      jo.put("uploadedPath", path);
      return Response.ok(jo).build();
    } catch (Exception e) {
      throw new ServiceFormattedException(e.getMessage(), e);
    }
  }

  @Path("/insertIntoTable")
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response insertFromTempTable(InsertFromQueryInput input) {
    try {
      String insertQuery = generateInsertFromQuery(input);
      LOG.info("insertQuery : {}", insertQuery);

      Job job = createJob(insertQuery, "default");

      JSONObject jo = new JSONObject();
      jo.put("jobId", job.getId());

      return Response.ok(jo).build();
    } catch (Exception e) {
      throw new ServiceFormattedException(e.getMessage(), e);
    }
  }

  @Path("/deleteTable")
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response deleteTable(DeleteQueryInput input) {
    try {
      String deleteQuery = generateDeleteQuery(input);
      LOG.info("deleteQuery : {}", deleteQuery);

      Job job = createJob(deleteQuery, "default");

      JSONObject jo = new JSONObject();
      jo.put("jobId", job.getId());

      return Response.ok(jo).build();
    } catch (Exception e) {
      throw new ServiceFormattedException(e.getMessage(), e);
    }
  }

  private String uploadIntoTable(Reader reader, String databaseName, String tempTableName) {
    try {
      String basePath = getHiveMetaStoreLocation();

      if (!basePath.endsWith("/")) {
        basePath = basePath + "/";
      }

      if (databaseName != null && !databaseName.equals(HIVE_DEFAULT_DB)) {
        basePath = basePath + databaseName + ".db/";
      }

      String fullPath = basePath + tempTableName + "/" + tempTableName + ".csv";

      LOG.info("Uploading file into : {}", fullPath);

      uploadFile(fullPath, new ReaderInputStream(reader));

      return fullPath;
    } catch (Exception e) {
      throw new ServiceFormattedException(e.getMessage(), e);
    }
  }

  private synchronized JobResourceManager getResourceManager() {
    if (resourceManager == null) {
      SharedObjectsFactory connectionsFactory = getSharedObjectsFactory();
      resourceManager = new JobResourceManager(connectionsFactory, context);
    }
    return resourceManager;
  }

  private synchronized AmbariApi getAmbariApi() {
    if (null == ambariApi) {
      ambariApi = new AmbariApi(this.context);
    }
    return ambariApi;
  }

  private String generateCreateQuery(TableInfo ti) {
    return new QueryGenerator().generateCreateQuery(ti);
  }

  private String generateInsertFromQuery(InsertFromQueryInput input) {
    return new QueryGenerator().generateInsertFromQuery(input);
  }

  private String generateDeleteQuery(DeleteQueryInput deleteQueryInput) {
    return new QueryGenerator().generateDropTableQuery(deleteQueryInput);
  }

  private Job createJob(String query, String databaseName) throws InvocationTargetException, IllegalAccessException, ItemNotFound {
    Map jobInfo = new HashMap<String, String>();
    jobInfo.put("title", "Internal Table Creation");
    jobInfo.put("forcedContent", query);
    jobInfo.put("dataBase", databaseName);

    LOG.info("jobInfo : " + jobInfo);
    Job job = new JobImpl(jobInfo);
    LOG.info("job : " + job);
    getResourceManager().create(job);

    JobController createdJobController = getResourceManager().readController(job.getId());
    createdJobController.submit();
    getResourceManager().saveIfModified(createdJobController);

    return job;
  }

  private String getHiveMetaStoreLocation() {
    String dir = this.getAmbariApi().getProperty(HIVE_SITE,HIVE_METASTORE_LOCATION_KEY,HIVE_METASTORE_LOCATION_KEY_VIEW_PROPERTY);
    if(dir != null && !dir.trim().isEmpty()){
      return dir;
    }else{
      LOG.debug("Neither found associated cluster nor found the view property {}. Returning default location : {}", HIVE_METASTORE_LOCATION_KEY_VIEW_PROPERTY, HIVE_DEFAULT_METASTORE_LOCATION);
      return HIVE_DEFAULT_METASTORE_LOCATION;
    }
  }

  private void uploadFile(final String filePath, InputStream uploadedInputStream)
          throws IOException, InterruptedException {
    byte[] chunk = new byte[1024];
    FSDataOutputStream out = getSharedObjectsFactory().getHdfsApi().create(filePath, false);
    int n = -1;
    while ((n = uploadedInputStream.read(chunk)) != -1) {
      out.write(chunk, 0, n);
    }
    out.close();
  }

  private PreviewData generatePreview(Boolean isFirstRowHeader, String inputFileType, InputStream uploadedInputStream) throws IOException {
    ParseOptions parseOptions = new ParseOptions();
    parseOptions.setOption(ParseOptions.OPTIONS_FILE_TYPE, inputFileType);
    if (inputFileType.equals(ParseOptions.InputFileType.CSV.toString()) && !isFirstRowHeader)
      parseOptions.setOption(ParseOptions.OPTIONS_HEADER, ParseOptions.HEADER.PROVIDED_BY_USER.toString());
    else
      parseOptions.setOption(ParseOptions.OPTIONS_HEADER, ParseOptions.HEADER.FIRST_RECORD.toString());

    LOG.info("isFirstRowHeader : {}, inputFileType : {}", isFirstRowHeader, inputFileType);

    DataParser dataParser = new DataParser(new InputStreamReader(uploadedInputStream), parseOptions);

    return dataParser.parsePreview();
  }

  private Response createPreviewResponse(PreviewData pd, Boolean isFirstRowHeader, String tableName) {
    Map<String, Object> retData = new HashMap<String, Object>();
    retData.put("header", pd.getHeader());
    retData.put("rows", pd.getPreviewRows());
    retData.put("isFirstRowHeader", isFirstRowHeader);
    retData.put("tableName", tableName);

    JSONObject jsonObject = new JSONObject(retData);
    return Response.ok(jsonObject).build();
  }

  private InputStream getHDFSFileStream(String path) throws IOException, InterruptedException {
    FSDataInputStream fsStream = getSharedObjectsFactory().getHdfsApi().open(path);
    return fsStream;
  }

  private String uploadFileFromStream(
          InputStream uploadedInputStream,
          Boolean isFirstRowHeader,
          String inputFileType,   // the format of the file uploaded. CSV/JSON etc.
          String tableName,
          String databaseName

  ) throws IOException {
    LOG.info(" uploading file into databaseName {}, tableName {}", databaseName, tableName);
    ParseOptions parseOptions = new ParseOptions();
    parseOptions.setOption(ParseOptions.OPTIONS_FILE_TYPE, inputFileType);

    DataParser dataParser = new DataParser(new InputStreamReader(uploadedInputStream), parseOptions);

    if (inputFileType.equals(ParseOptions.InputFileType.CSV.toString()) && isFirstRowHeader)
      dataParser.extractHeader(); // removes the header line if any from the stream

    Reader csvReader = dataParser.getTableDataReader();
    String path = uploadIntoTable(csvReader, databaseName, tableName);
    return path;
  }

  private String getBasenameFromPath(String path) {
    String fileName = new File(path).getName();
    return getBasename(fileName);
  }

  private String getBasename(String fileName){
    int index = fileName.indexOf(".");
    if(index != -1){
      return fileName.substring(0,index);
    }

    return fileName;
  }
}
