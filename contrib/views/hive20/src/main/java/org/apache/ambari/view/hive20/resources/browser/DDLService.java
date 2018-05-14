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

package org.apache.ambari.view.hive20.resources.browser;

import org.apache.ambari.view.hive20.BaseService;
import org.apache.ambari.view.hive20.client.ConnectionConfig;
import org.apache.ambari.view.hive20.exceptions.ServiceException;
import org.apache.ambari.view.hive20.internal.dto.ColumnStats;
import org.apache.ambari.view.hive20.internal.dto.DatabaseResponse;
import org.apache.ambari.view.hive20.internal.dto.TableMeta;
import org.apache.ambari.view.hive20.internal.dto.TableResponse;
import org.apache.ambari.view.hive20.resources.jobs.viewJobs.Job;
import org.apache.ambari.view.hive20.resources.jobs.viewJobs.JobResourceManager;
import org.apache.ambari.view.hive20.utils.ServiceFormattedException;
import org.apache.ambari.view.hive20.utils.SharedObjectsFactory;
import org.apache.parquet.Strings;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Set;

/**
 * Resource to get the DDL information for the database
 */
public class DDLService extends BaseService {

  private static final String CREATE_TABLE = "create-table";
  private static final String ALTER_TABLE = "alter-table";
  private final DDLProxy proxy;
  private JobResourceManager resourceManager;

  protected final static Logger LOG =
    LoggerFactory.getLogger(DDLService.class);

  protected synchronized JobResourceManager getResourceManager() {
    if (resourceManager == null) {
      SharedObjectsFactory connectionsFactory = getSharedObjectsFactory();
      resourceManager = new JobResourceManager(connectionsFactory, context);
    }
    return resourceManager;
  }

  @Inject
  public DDLService(DDLProxy proxy) {
    this.proxy = proxy;
  }


  @GET
  @Path("databases")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getDatabases(@QueryParam("like") String like) {
    Set<DatabaseResponse> infos = proxy.getDatabases();
    JSONObject response = new JSONObject();
    response.put("databases", infos);
    return Response.ok(response).build();
  }

  @GET
  @Path("databases/{database_id}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getDatabase(@PathParam("database_id") String databaseId) {
    DatabaseResponse database = proxy.getDatabase(databaseId);
    JSONObject response = new JSONObject();
    response.put("database", database);
    return Response.ok(response).build();
  }

  @DELETE
  @Path("databases/{database_id}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response deleteDatabase(@PathParam("database_id") String databaseId) {
    Job job = null;
    try {
      job = proxy.deleteDatabase(databaseId, getResourceManager());
      JSONObject response = new JSONObject();
      response.put("job", job);
      return Response.status(Response.Status.ACCEPTED).entity(response).build();
    } catch (ServiceException e) {
      LOG.error("Exception occurred while delete database {}", databaseId, e);
      throw new ServiceFormattedException(e);
    }
  }

  @POST
  @Path("databases")
  @Produces(MediaType.APPLICATION_JSON)
  public Response createDatabase(CreateDatabaseRequestWrapper wrapper) {
    String databaseId = wrapper.database.name;
    Job job = null;
    try {
      job = proxy.createDatabase(databaseId, getResourceManager());
      JSONObject response = new JSONObject();
      response.put("job", job);
      return Response.status(Response.Status.ACCEPTED).entity(response).build();
    } catch (ServiceException e) {
      LOG.error("Exception occurred while delete database {}", databaseId, e);
      throw new ServiceFormattedException(e);
    }
  }

  @GET
  @Path("databases/{database_id}/tables")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getTables(@PathParam("database_id") String databaseName) {
    Set<TableResponse> tables = proxy.getTables(databaseName);
    JSONObject response = new JSONObject();
    response.put("tables", tables);
    return Response.ok(response).build();
  }

  @POST
  @Path("databases/{database_id}/tables")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public Response createTable(@PathParam("database_id") String databaseName, TableMetaRequest request) {
    try {
      Job job = proxy.createTable(databaseName, request.tableInfo, getResourceManager());
      JSONObject response = new JSONObject();
      response.put("job", job);
      return Response.status(Response.Status.ACCEPTED).entity(response).build();
    } catch (ServiceException e) {
      LOG.error("Exception occurred while creatint table for db {} with details : {}", databaseName, request.tableInfo, e);
      throw new ServiceFormattedException(e);
    }
  }

  @PUT
  @Path("databases/{database_id}/tables/{table_id}/rename")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public Response renameTable(@PathParam("database_id") String oldDatabaseName, @PathParam("table_id") String oldTableName,
                              TableRenameRequest request) {
    try {
      Job job = proxy.renameTable(oldDatabaseName, oldTableName, request.newDatabase, request.newTable, getResourceManager());
      JSONObject response = new JSONObject();
      response.put("job", job);
      return Response.status(Response.Status.ACCEPTED).entity(response).build();
    } catch (ServiceException e) {
      LOG.error("Exception occurred while renaming table for oldDatabaseName {}, oldTableName: {}, newDatabaseName : {}," +
        " newTableName : {}", oldDatabaseName, oldTableName, request.newDatabase, request.newTable, e);
      throw new ServiceFormattedException(e);
    }
  }

  @PUT
  @Path("databases/{database_id}/tables/{table_id}/analyze")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public Response analyzeTable(@PathParam("database_id") String databaseName, @PathParam("table_id") String tableName,
                              @QueryParam("analyze_columns") String analyzeColumns) {
    Boolean shouldAnalyzeColumns = Boolean.FALSE;
    if(!Strings.isNullOrEmpty(analyzeColumns)){
      shouldAnalyzeColumns = Boolean.valueOf(analyzeColumns.trim());
    }
    try {
      ConnectionConfig hiveConnectionConfig = getHiveConnectionConfig();
      Job job = proxy.analyzeTable(databaseName, tableName, shouldAnalyzeColumns, getResourceManager(), hiveConnectionConfig);
      JSONObject response = new JSONObject();
      response.put("job", job);
      return Response.status(Response.Status.ACCEPTED).entity(response).build();
    } catch (ServiceException e) {
      LOG.error("Exception occurred while analyzing table for database {}, table: {}, analyzeColumns: {}" ,
        databaseName, tableName, analyzeColumns, e);
      throw new ServiceFormattedException(e);
    }
  }

  @POST
  @Path("databases/{database_id}/tables/ddl")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public Response generateDDL(TableMetaRequest request, @QueryParam("query_type") String queryType) {
    try {
      String query = null;
      if (queryType.equals(CREATE_TABLE)) {
        query = proxy.generateCreateTableDDL(request.tableInfo.getDatabase(), request.tableInfo);
      }else if(queryType.equals(ALTER_TABLE)){
        query = proxy.generateAlterTableQuery(context, getHiveConnectionConfig(), request.tableInfo.getDatabase(),
                request.tableInfo.getTable(), request.tableInfo);
      }else{
        throw new ServiceException("query_type = '" + queryType + "' is not supported");
      }
      JSONObject response = new JSONObject();
      response.put("ddl", new DDL(query));
      return Response.status(Response.Status.ACCEPTED).entity(response).build();
    } catch (ServiceException e) {
      LOG.error("Exception occurred while generating {} ddl for : {}", queryType, request.tableInfo, e);
      throw new ServiceFormattedException(e);
    }
  }

  @GET
  @Path("databases/{database_id}/tables/{table_id}")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public Response getTable(@PathParam("database_id") String databaseName, @PathParam("table_id") String tableName) {
    TableResponse table = proxy.getTable(databaseName, tableName);
    JSONObject response = new JSONObject();
    response.put("table", table);
    return Response.ok(response).build();
  }

  /**
   *
   * @param databaseName
   * @param oldTableName : this is required in case if the name of table itself is changed in tableMeta
   * @param tableMetaRequest
   * @return
   */
  @PUT
  @Path("databases/{database_id}/tables/{table_id}")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public Response alterTable(@PathParam("database_id") String databaseName, @PathParam("table_id") String oldTableName, TableMetaRequest tableMetaRequest) {
    try {
      ConnectionConfig hiveConnectionConfig = getHiveConnectionConfig();
      Job job = proxy.alterTable(context, hiveConnectionConfig, databaseName, oldTableName, tableMetaRequest.tableInfo, getResourceManager());
      JSONObject response = new JSONObject();
      response.put("job", job);
      return Response.status(Response.Status.ACCEPTED).entity(response).build();
    } catch (ServiceException e) {
      LOG.error("Exception occurred while creatint table for db {} with details : {}", databaseName, tableMetaRequest.tableInfo, e);
      throw new ServiceFormattedException(e);
    }
  }

  @DELETE
  @Path("databases/{database_id}/tables/{table_id}")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public Response deleteTable(@PathParam("database_id") String databaseName, @PathParam("table_id") String tableName) {
    try {
      Job job = proxy.deleteTable(databaseName, tableName, getResourceManager());
      JSONObject response = new JSONObject();
      response.put("job", job);
      return Response.status(Response.Status.ACCEPTED).entity(response).build();
    } catch (ServiceException e) {
      LOG.error("Exception occurred while deleting table for db {}, tableName : {}", databaseName, tableName, e);
      throw new ServiceFormattedException(e);
    }
  }

  @GET
  @Path("databases/{database_id}/tables/{table_id}/info")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getTableInfo(@PathParam("database_id") String databaseName, @PathParam("table_id") String tableName) {
    ConnectionConfig hiveConnectionConfig = getHiveConnectionConfig();
    TableMeta meta = proxy.getTableProperties(context, hiveConnectionConfig, databaseName, tableName);
    JSONObject response = new JSONObject();
    response.put("tableInfo", meta);
    return Response.ok(response).build();
  }

  @GET
  @Path("databases/{database_id}/tables/{table_id}/column/{column_id}/stats")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public Response getColumnStats(@PathParam("database_id") String databaseName, @PathParam("table_id") String tableName,
                            @PathParam("column_id") String columnName) {
    try {
      Job job = proxy.getColumnStatsJob(databaseName, tableName, columnName, getResourceManager());
      JSONObject response = new JSONObject();
      response.put("job", job);
      return Response.status(Response.Status.ACCEPTED).entity(response).build();
    } catch (ServiceException e) {
      LOG.error("Exception occurred while fetching column stats", databaseName, tableName, e);
      throw new ServiceFormattedException(e);
    }
  }

  @GET
  @Path("databases/{database_id}/tables/{table_id}/column/{column_id}/fetch_stats")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public Response fetchColumnStats(@PathParam("database_id") String databaseName, @PathParam("table_id") String
    tablename, @PathParam("column_id") String columnName, @QueryParam("job_id") String jobId) {
    try {
      ColumnStats columnStats = proxy.fetchColumnStats(columnName, jobId, context);
      columnStats.setTableName(tablename);
      columnStats.setDatabaseName(databaseName);
      JSONObject response = new JSONObject();
      response.put("columnStats", columnStats);
      return Response.status(Response.Status.ACCEPTED).entity(response).build();
    } catch (ServiceException e) {
      LOG.error("Exception occurred while fetching column stats for column: {} and jobId: {}", columnName, jobId,  e);
      throw new ServiceFormattedException(e);
    }
  }

  public static class DDL {
    String query;

    public DDL(String query) {
      this.query = query;
    }
  }

  /**
   * Wrapper class for table meta request
   */
  public static class TableMetaRequest {
    public TableMeta tableInfo;
  }

  /**
   * Wrapper class for create database request
   */
  public static class CreateDatabaseRequestWrapper {
    public CreateDatabaseRequest database;
  }

  /**
   * Request class for create database
   */
  public static class CreateDatabaseRequest {
    public String name;
  }

  /**
   * Wrapper class for table rename request
   */
  public static class TableRenameRequest {
    /* New database name */
    public String newDatabase;

    /* New table name */
    public String newTable;
  }
}
