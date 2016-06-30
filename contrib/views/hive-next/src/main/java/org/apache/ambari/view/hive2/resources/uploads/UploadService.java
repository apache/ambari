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

package org.apache.ambari.view.hive2.resources.uploads;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;
import org.apache.ambari.view.hive2.BaseService;
import org.apache.ambari.view.hive2.resources.jobs.viewJobs.Job;
import org.apache.ambari.view.hive2.resources.jobs.viewJobs.JobController;
import org.apache.ambari.view.hive2.resources.jobs.viewJobs.JobImpl;
import org.apache.ambari.view.hive2.resources.jobs.viewJobs.JobResourceManager;
import org.apache.ambari.view.hive2.resources.uploads.parsers.DataParser;
import org.apache.ambari.view.hive2.resources.uploads.parsers.ParseOptions;
import org.apache.ambari.view.hive2.resources.uploads.parsers.PreviewData;
import org.apache.ambari.view.hive2.resources.uploads.query.DeleteQueryInput;
import org.apache.ambari.view.hive2.resources.uploads.query.InsertFromQueryInput;
import org.apache.ambari.view.hive2.resources.uploads.query.LoadQueryInput;
import org.apache.ambari.view.hive2.resources.uploads.query.QueryGenerator;
import org.apache.ambari.view.hive2.resources.uploads.query.TableInfo;
import org.apache.ambari.view.hive2.utils.ServiceFormattedException;
import org.apache.ambari.view.hive2.utils.SharedObjectsFactory;
import org.apache.ambari.view.utils.ambari.AmbariApi;
import org.apache.commons.io.input.ReaderInputStream;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

  private final static Logger LOG =
    LoggerFactory.getLogger(UploadService.class);

  private AmbariApi ambariApi;
  protected JobResourceManager resourceManager;

  final private static String HIVE_METASTORE_LOCATION_KEY = "hive.metastore.warehouse.dir";
  final private static String HIVE_SITE = "hive-site";
  final private static String HIVE_METASTORE_LOCATION_KEY_VIEW_PROPERTY = HIVE_METASTORE_LOCATION_KEY;
  private static final String HIVE_DEFAULT_METASTORE_LOCATION = "/apps/hive/warehouse";
  final private static String HIVE_DEFAULT_DB = "default";

  public void validateForUploadFile(UploadFromHdfsInput input){
    if( null == input.getInputFileType()){
      throw new IllegalArgumentException("inputFileType parameter cannot be null.");
    }
    if( null == input.getHdfsPath()){
      throw new IllegalArgumentException("hdfsPath parameter cannot be null.");
    }
    if( null == input.getTableName()){
      throw new IllegalArgumentException("tableName parameter cannot be null.");
    }
    if( null == input.getDatabaseName()){
      throw new IllegalArgumentException("databaseName parameter cannot be null.");
    }

    if( input.getIsFirstRowHeader() == null ){
      input.setIsFirstRowHeader(false);
    }
  }

  public void validateForPreview(UploadFromHdfsInput input){
    if( input.getIsFirstRowHeader() == null ){
      input.setIsFirstRowHeader(false);
    }

    if( null == input.getInputFileType()){
      throw new IllegalArgumentException("inputFileType parameter cannot be null.");
    }
    if( null == input.getHdfsPath()){
      throw new IllegalArgumentException("hdfsPath parameter cannot be null.");
    }
  }

  @POST
  @Path("/previewFromHdfs")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response uploadForPreviewFromHDFS(UploadFromHdfsInput input) {
    InputStream uploadedInputStream = null;
    try {
      uploadedInputStream = getHDFSFileStream(input.getHdfsPath());
      this.validateForPreview(input);
      PreviewData pd = generatePreview(input.getIsFirstRowHeader(), input.getInputFileType(), uploadedInputStream);
      String tableName = getBasenameFromPath(input.getHdfsPath());
      return createPreviewResponse(pd, input.getIsFirstRowHeader(), tableName);
    } catch (WebApplicationException e) {
      LOG.error(getErrorMessage(e), e);
      throw e;
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
      throw new ServiceFormattedException(e);
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
      if( null == inputFileType)
        throw new IllegalArgumentException("inputFileType parameter cannot be null.");

      if( null == isFirstRowHeader )
        isFirstRowHeader = false;

      PreviewData pd = generatePreview(isFirstRowHeader, inputFileType, uploadedInputStream);
      return createPreviewResponse(pd, isFirstRowHeader, getBasename(fileDetail.getFileName()));
    } catch (WebApplicationException e) {
      LOG.error(getErrorMessage(e), e);
      throw e;
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
      throw new ServiceFormattedException(e);
    }
  }


  @Path("/createTable")
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Job createTable(TableInput tableInput) {
    try {
      tableInput.validate();
      List<ColumnDescriptionImpl> header = tableInput.getHeader();
      String databaseName = tableInput.getDatabaseName();
      String tableName = tableInput.getTableName();
      Boolean isFirstRowHeader = tableInput.getIsFirstRowHeader();
      String fileTypeStr = tableInput.getFileType();
      HiveFileType hiveFileType = HiveFileType.valueOf(fileTypeStr);

      TableInfo ti = new TableInfo(databaseName, tableName, header, hiveFileType);
      String tableCreationQuery = generateCreateQuery(ti);

      LOG.info("tableCreationQuery : {}", tableCreationQuery);

      Job job = createJob(tableCreationQuery, databaseName);
      LOG.info("job created for table creation {}", job);
      return job;
    } catch (WebApplicationException e) {
      LOG.error(getErrorMessage(e), e);
      throw e;
    } catch (Throwable e) {
      LOG.error(e.getMessage(), e);
      throw new ServiceFormattedException(e);
    }
  }

  @Path("/uploadFromHDFS")
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response uploadFileFromHdfs(UploadFromHdfsInput input) {
    if (ParseOptions.InputFileType.CSV.toString().equals(input.getInputFileType()) && input.getIsFirstRowHeader().equals(Boolean.FALSE)) {
      try {
        // upload using the LOAD query
        LoadQueryInput loadQueryInput = new LoadQueryInput(input.getHdfsPath(), input.getDatabaseName(), input.getTableName());
        String loadQuery = new QueryGenerator().generateLoadQuery(loadQueryInput);
        Job job = createJob(loadQuery, input.getDatabaseName());

        JSONObject jo = new JSONObject();
        jo.put("jobId", job.getId());
        return Response.ok(jo).build();
      } catch (WebApplicationException e) {
        LOG.error(getErrorMessage(e), e);
        throw e;
      } catch (Throwable e) {
        LOG.error(e.getMessage(), e);
        throw new ServiceFormattedException(e);
      }
    } else {
      // create stream and upload
      InputStream hdfsStream = null;
      try {
        hdfsStream = getHDFSFileStream(input.getHdfsPath());
        String path = uploadFileFromStream(hdfsStream, input.getIsFirstRowHeader(), input.getInputFileType(), input.getTableName(), input.getDatabaseName());

        JSONObject jo = new JSONObject();
        jo.put("uploadedPath", path);

        return Response.ok(jo).build();
      } catch (WebApplicationException e) {
        LOG.error(getErrorMessage(e), e);
        throw e;
      } catch (Exception e) {
        LOG.error(e.getMessage(), e);
        throw new ServiceFormattedException(e);
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
      String path = uploadFileFromStream(uploadedInputStream, isFirstRowHeader, inputFileType, tableName, databaseName);

      JSONObject jo = new JSONObject();
      jo.put("uploadedPath", path);
      return Response.ok(jo).build();
    } catch (WebApplicationException e) {
      LOG.error(getErrorMessage(e), e);
      throw e;
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
      throw new ServiceFormattedException(e);
    }
  }

  @Path("/insertIntoTable")
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Job insertFromTempTable(InsertFromQueryInput input) {
    try {
      String insertQuery = generateInsertFromQuery(input);
      LOG.info("insertQuery : {}", insertQuery);

      Job job = createJob(insertQuery, "default");
      LOG.info("Job created for insert from temp table : {}", job);
      return job;
    } catch (WebApplicationException e) {
      LOG.error(getErrorMessage(e), e);
      throw e;
    } catch (Throwable e) {
      LOG.error(e.getMessage(), e);
      throw new ServiceFormattedException(e);
    }
  }

  @Path("/deleteTable")
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Job deleteTable(DeleteQueryInput input) {
    try {
      String deleteQuery = generateDeleteQuery(input);
      LOG.info("deleteQuery : {}", deleteQuery);

      Job job = createJob(deleteQuery, "default");
      LOG.info("Job created for delete temp table : {} ", job);
      return job;
    } catch (WebApplicationException e) {
      LOG.error(getErrorMessage(e), e);
      throw e;
    } catch (Throwable e) {
      LOG.error(e.getMessage(), e);
      throw new ServiceFormattedException(e);
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
    } catch (WebApplicationException e) {
      LOG.error(getErrorMessage(e), e);
      throw e;
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
      throw new ServiceFormattedException(e);
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

  private Job createJob(String query, String databaseName) throws Throwable {
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
    String dir = context.getProperties().get(HIVE_METASTORE_LOCATION_KEY_VIEW_PROPERTY);
    if (dir != null && !dir.trim().isEmpty()) {
      return dir;
    } else {
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

  private static String getErrorMessage(WebApplicationException e) {
    if (null != e.getResponse() && null != e.getResponse().getEntity())
      return e.getResponse().getEntity().toString();
    else return e.getMessage();
  }

  private PreviewData generatePreview(Boolean isFirstRowHeader, String inputFileType, InputStream uploadedInputStream) throws Exception {
    ParseOptions parseOptions = new ParseOptions();
    parseOptions.setOption(ParseOptions.OPTIONS_FILE_TYPE, inputFileType);
    if (inputFileType.equals(ParseOptions.InputFileType.CSV.toString())){
      if(isFirstRowHeader)
        parseOptions.setOption(ParseOptions.OPTIONS_HEADER, ParseOptions.HEADER.FIRST_RECORD.toString());
      else
        parseOptions.setOption(ParseOptions.OPTIONS_HEADER, ParseOptions.HEADER.NONE.toString());
    }
    else
      parseOptions.setOption(ParseOptions.OPTIONS_HEADER, ParseOptions.HEADER.EMBEDDED.toString());

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

  ) throws Exception {
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

  private String getBasename(String fileName) {
    int index = fileName.indexOf(".");
    if (index != -1) {
      return fileName.substring(0, index);
    }

    return fileName;
  }
}
