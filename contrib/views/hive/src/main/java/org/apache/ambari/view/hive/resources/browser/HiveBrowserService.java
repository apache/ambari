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

package org.apache.ambari.view.hive.resources.browser;

import com.google.inject.Inject;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.ViewResourceHandler;
import org.apache.ambari.view.hive.client.ColumnDescription;
import org.apache.ambari.view.hive.client.Cursor;
import org.apache.ambari.view.hive.client.IConnectionFactory;
import org.apache.ambari.view.hive.resources.jobs.ResultsPaginationController;
import org.apache.ambari.view.hive.utils.BadRequestFormattedException;
import org.apache.ambari.view.hive.utils.ServiceFormattedException;
import org.apache.ambari.view.hive.utils.SharedObjectsFactory;
import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.apache.hive.service.cli.thrift.TSessionHandle;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Database access resource
 */
public class HiveBrowserService {
  @Inject
  ViewResourceHandler handler;
  @Inject
  protected ViewContext context;

  protected final static Logger LOG =
      LoggerFactory.getLogger(HiveBrowserService.class);

  private static final long EXPIRING_TIME = 10*60*1000;  // 10 minutes
  private static Map<String, Cursor> resultsCache;
  private IConnectionFactory connectionFactory;

  public static Map<String, Cursor> getResultsCache() {
    if (resultsCache == null) {
      PassiveExpiringMap<String, Cursor> resultsCacheExpiringMap =
          new PassiveExpiringMap<String, Cursor>(EXPIRING_TIME);
      resultsCache = Collections.synchronizedMap(resultsCacheExpiringMap);
    }
    return resultsCache;
  }

  private IConnectionFactory getConnectionFactory() {
    if (connectionFactory == null)
      connectionFactory = new SharedObjectsFactory(context);
    return new SharedObjectsFactory(context);
  }

  /**
   * Returns list of databases
   */
  @GET
  @Path("database")
  @Produces(MediaType.APPLICATION_JSON)
  public Response databases(@QueryParam("like")String like,
                            @QueryParam("first") String fromBeginning,
                            @QueryParam("count") Integer count,
                            @QueryParam("columns") final String requestedColumns) {
    if (like == null)
      like = "*";
    else
      like = "*" + like + "*";
    String curl = null;
    try {
      JSONObject response = new JSONObject();
      TSessionHandle session = getConnectionFactory().getHiveConnection().getOrCreateSessionByTag("DDL");
      List<String> tables = getConnectionFactory().getHiveConnection().ddl().getDBList(session, like);
      response.put("databases", tables);
      return Response.ok(response).build();
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (IllegalArgumentException ex) {
      throw new BadRequestFormattedException(ex.getMessage(), ex);
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex, curl);
    }
  }

  /**
   * Returns list of databases
   */
  @GET
  @Path("database.page")
  @Produces(MediaType.APPLICATION_JSON)
  public Response databasesPaginated(@QueryParam("like")String like,
                            @QueryParam("first") String fromBeginning,
                            @QueryParam("count") Integer count,
                            @QueryParam("searchId") String searchId,
                            @QueryParam("columns") final String requestedColumns) {
    if (like == null)
      like = "*";
    else
      like = "*" + like + "*";
    String curl = null;
    try {
      final String finalLike = like;
      return ResultsPaginationController.getInstance(context)
          .request("databases", searchId, false, fromBeginning, count,
              new Callable<Cursor>() {
                @Override
                public Cursor call() throws Exception {
                  TSessionHandle session = getConnectionFactory().getHiveConnection().getOrCreateSessionByTag("DDL");
                  return getConnectionFactory().getHiveConnection().ddl().getDBListCursor(session, finalLike);
                }
              }).build();
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (IllegalArgumentException ex) {
      throw new BadRequestFormattedException(ex.getMessage(), ex);
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex, curl);
    }
  }

  /**
   * Returns list of databases
   */
  @GET
  @Path("database/{db}/table")
  @Produces(MediaType.APPLICATION_JSON)
  public Response tablesInDatabase(@PathParam("db") String db,
                                   @QueryParam("like")String like,
                                   @QueryParam("first") String fromBeginning,
                                   @QueryParam("count") Integer count,
                                   @QueryParam("columns") final String requestedColumns) {
    if (like == null)
      like = "*";
    else
      like = "*" + like + "*";
    String curl = null;
    try {
      JSONObject response = new JSONObject();
      TSessionHandle session = getConnectionFactory().getHiveConnection().getOrCreateSessionByTag("DDL");
      List<String> tables = getConnectionFactory().getHiveConnection().ddl().getTableList(session, db, like);
      response.put("tables", tables);
      response.put("database", db);
      return Response.ok(response).build();
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (IllegalArgumentException ex) {
      throw new BadRequestFormattedException(ex.getMessage(), ex);
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex, curl);
    }
  }

  /**
   * Returns list of databases
   */
  @GET
  @Path("database/{db}/table.page")
  @Produces(MediaType.APPLICATION_JSON)
  public Response tablesInDatabasePaginated(@PathParam("db") final String db,
                                   @QueryParam("like")String like,
                                   @QueryParam("first") String fromBeginning,
                                   @QueryParam("count") Integer count,
                                   @QueryParam("searchId") String searchId,
                                   @QueryParam("columns") final String requestedColumns) {
    if (like == null)
      like = "*";
    else
      like = "*" + like + "*";
    String curl = null;
    try {
      final String finalLike = like;
      return ResultsPaginationController.getInstance(context)
          .request(db + ":tables", searchId, false, fromBeginning, count,
              new Callable<Cursor>() {
                @Override
                public Cursor call() throws Exception {
                  TSessionHandle session = getConnectionFactory().getHiveConnection().getOrCreateSessionByTag("DDL");
                  Cursor cursor = getConnectionFactory().getHiveConnection().ddl().getTableListCursor(session, db, finalLike);
                  cursor.selectColumns(requestedColumns);
                  return cursor;
                }
              }).build();
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (IllegalArgumentException ex) {
      throw new BadRequestFormattedException(ex.getMessage(), ex);
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex, curl);
    }
  }

  /**
   * Returns list of databases
   */
  @GET
  @Path("database/{db}/table/{table}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response describeTable(@PathParam("db") String db,
                                @PathParam("table") String table,
                                @QueryParam("like") String like,
                                @QueryParam("columns") String requestedColumns,
                                @QueryParam("extended") String extended) {
    boolean extendedTableDescription = (extended != null && extended.equals("true"));
    String curl = null;
    try {
      JSONObject response = new JSONObject();
      TSessionHandle session = getConnectionFactory().getHiveConnection().getOrCreateSessionByTag("DDL");
      List<ColumnDescription> columnDescriptions = getConnectionFactory().getHiveConnection().ddl()
          .getTableDescription(session, db, table, like, extendedTableDescription);
      response.put("columns", columnDescriptions);
      response.put("database", db);
      response.put("table", table);
      return Response.ok(response).build();
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (IllegalArgumentException ex) {
      throw new BadRequestFormattedException(ex.getMessage(), ex);
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex, curl);
    }
  }

  /**
   * Returns list of databases
   */
  @GET
  @Path("database/{db}/table/{table}.page")
  @Produces(MediaType.APPLICATION_JSON)
  public Response describeTablePaginated(@PathParam("db") final String db,
                                         @PathParam("table") final String table,
                                         @QueryParam("like") final String like,
                                         @QueryParam("first") String fromBeginning,
                                         @QueryParam("searchId") String searchId,
                                         @QueryParam("count") Integer count,
                                         @QueryParam("columns") final String requestedColumns) {
    String curl = null;
    try {
      return ResultsPaginationController.getInstance(context)
          .request(db + ":tables:" + table + ":columns", searchId, false, fromBeginning, count,
              new Callable<Cursor>() {
                @Override
                public Cursor call() throws Exception {
                  TSessionHandle session = getConnectionFactory().getHiveConnection().getOrCreateSessionByTag("DDL");
                  Cursor cursor = getConnectionFactory().getHiveConnection().ddl().
                      getTableDescriptionCursor(session, db, table, like);
                  cursor.selectColumns(requestedColumns);
                  return cursor;
                }
              }).build();
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (IllegalArgumentException ex) {
      throw new BadRequestFormattedException(ex.getMessage(), ex);
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex, curl);
    }
  }
}
