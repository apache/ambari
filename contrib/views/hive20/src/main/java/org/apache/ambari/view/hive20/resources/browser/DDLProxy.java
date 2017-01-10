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

package org.apache.ambari.view.hive20.resources.browser;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Inbox;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.FluentIterable;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.hive20.ConnectionSystem;
import org.apache.ambari.view.hive20.actor.DatabaseManager;
import org.apache.ambari.view.hive20.client.ConnectionConfig;
import org.apache.ambari.view.hive20.client.DDLDelegator;
import org.apache.ambari.view.hive20.client.DDLDelegatorImpl;
import org.apache.ambari.view.hive20.client.Row;
import org.apache.ambari.view.hive20.exceptions.ServiceException;
import org.apache.ambari.view.hive20.internal.dto.DatabaseInfo;
import org.apache.ambari.view.hive20.internal.dto.DatabaseResponse;
import org.apache.ambari.view.hive20.internal.dto.TableInfo;
import org.apache.ambari.view.hive20.internal.dto.TableMeta;
import org.apache.ambari.view.hive20.internal.dto.TableResponse;
import org.apache.ambari.view.hive20.internal.parsers.TableMetaParserImpl;
import org.apache.ambari.view.hive20.internal.query.generators.AlterTableQueryGenerator;
import org.apache.ambari.view.hive20.internal.query.generators.CreateTableQueryGenerator;
import org.apache.ambari.view.hive20.internal.query.generators.DeleteDatabaseQueryGenerator;
import org.apache.ambari.view.hive20.internal.query.generators.DeleteTableQueryGenerator;
import org.apache.ambari.view.hive20.internal.query.generators.RenameTableQueryGenerator;
import org.apache.ambari.view.hive20.resources.jobs.JobServiceInternal;
import org.apache.ambari.view.hive20.resources.jobs.viewJobs.Job;
import org.apache.ambari.view.hive20.resources.jobs.viewJobs.JobController;
import org.apache.ambari.view.hive20.resources.jobs.viewJobs.JobImpl;
import org.apache.ambari.view.hive20.resources.jobs.viewJobs.JobResourceManager;
import org.apache.ambari.view.hive20.utils.ServiceFormattedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class DDLProxy {
  private static final Logger LOG = LoggerFactory.getLogger(DDLProxy.class);

  private final ViewContext context;
  private final TableMetaParserImpl tableMetaParser;

  @Inject
  public DDLProxy(ViewContext context, TableMetaParserImpl tableMetaParser) {
    this.context = context;
    this.tableMetaParser = tableMetaParser;
    LOG.info("Creating DDLProxy");
  }


  public Set<DatabaseResponse> getDatabases() {
    Set<DatabaseInfo> infos = getDatabaseInfos();
    return transformToDatabasesResponse(infos);
  }

  public DatabaseResponse getDatabase(final String databaseId) {
    Optional<DatabaseInfo> infoOptional = selectDatabase(databaseId);
    if (!infoOptional.isPresent()) {
      // Throw exception
    }

    return transformToDatabaseResponse(infoOptional.get());
  }

  public Set<TableResponse> getTables(final String databaseId) {
    Optional<DatabaseInfo> infoOptional = selectDatabase(databaseId);
    if (!infoOptional.isPresent()) {
      // Throw exception;
    }
    DatabaseInfo info = infoOptional.get();
    return transformToTablesResponse(info.getTables(), info.getName());
  }

  public TableResponse getTable(final String databaseName, final String tableName) {
    Optional<DatabaseInfo> databaseOptional = selectDatabase(databaseName);
    if (!databaseOptional.isPresent()) {
      // Throw exception;
    }
    Optional<TableInfo> tableOptional = selectTable(databaseOptional.get().getTables(), tableName);
    if (!tableOptional.isPresent()) {
      // Throw exception
    }
    return transformToTableResponse(tableOptional.get(), databaseName);
  }

  public TableMeta getTableProperties(ViewContext context, ConnectionConfig connectionConfig, String databaseName, String tableName) {
    DDLDelegator delegator = new DDLDelegatorImpl(context, ConnectionSystem.getInstance().getActorSystem(), ConnectionSystem.getInstance().getOperationController(context));
    List<Row> createTableStatementRows = delegator.getTableCreateStatement(connectionConfig, databaseName, tableName);
    List<Row> describeFormattedRows = delegator.getTableDescriptionFormatted(connectionConfig, databaseName, tableName);

    return tableMetaParser.parse(databaseName, tableName, createTableStatementRows, describeFormattedRows);
  }

  private Optional<DatabaseInfo> selectDatabase(final String databaseId) {
    Set<DatabaseInfo> infos = getDatabaseInfos();
    return FluentIterable.from(infos).filter(new Predicate<DatabaseInfo>() {
      @Override
      public boolean apply(@Nullable DatabaseInfo input) {
        return input.getName().equalsIgnoreCase(databaseId);
      }
    }).first();
  }

  private Set<DatabaseResponse> transformToDatabasesResponse(Set<DatabaseInfo> infos) {
    return FluentIterable.from(infos).transform(new Function<DatabaseInfo, DatabaseResponse>() {
      @Nullable
      @Override
      public DatabaseResponse apply(@Nullable DatabaseInfo input) {
        DatabaseResponse response = new DatabaseResponse();
        response.setId(input.getName());
        response.setName(input.getName());
        return response;
      }
    }).toSet();
  }

  private DatabaseResponse transformToDatabaseResponse(DatabaseInfo databaseInfo) {
    DatabaseResponse response = new DatabaseResponse();
    response.setName(databaseInfo.getName());
    response.setId(databaseInfo.getName());
    Set<TableResponse> tableResponses = transformToTablesResponse(databaseInfo.getTables(), databaseInfo.getName());
    response.addAllTables(tableResponses);
    return response;
  }

  private Set<TableResponse> transformToTablesResponse(final Set<TableInfo> tables, final String databaseName) {
    return FluentIterable.from(tables).transform(new Function<TableInfo, TableResponse>() {
      @Nullable
      @Override
      public TableResponse apply(@Nullable TableInfo input) {
        return transformToTableResponse(input, databaseName);
      }
    }).toSet();
  }

  private TableResponse transformToTableResponse(TableInfo tableInfo, String databaseName) {
    TableResponse response = new TableResponse();
    response.setId(databaseName + "/" + tableInfo.getName());
    response.setName(tableInfo.getName());
    response.setType(tableInfo.getType());
    response.setDatabaseId(databaseName);
    return response;
  }

  private Optional<TableInfo> selectTable(Set<TableInfo> tables, final String tableName) {
    return FluentIterable.from(tables).filter(new Predicate<TableInfo>() {
      @Override
      public boolean apply(@Nullable TableInfo input) {
        return input.getName().equalsIgnoreCase(tableName);
      }
    }).first();
  }

  private Set<DatabaseInfo> getDatabaseInfos() {
    ActorRef metaDataManager = ConnectionSystem.getInstance().getMetaDataManager(context);
    ActorSystem system = ConnectionSystem.getInstance().getActorSystem();

    Inbox inbox = Inbox.create(system);

    inbox.send(metaDataManager, new DatabaseManager.GetDatabases(context.getUsername()));
    Object receive;
    try {
      receive = inbox.receive(Duration.create(60 * 1000, TimeUnit.MILLISECONDS));
    } catch (Throwable ex) {
      String errorMessage = "Query timed out to fetch databases information for user: " + context.getUsername();
      LOG.error(errorMessage, ex);
      throw new ServiceFormattedException(errorMessage, ex);
    }
    Set<DatabaseInfo> infos = new HashSet<>();

    if (receive instanceof DatabaseManager.DatabasesResult) {
      infos = ((DatabaseManager.DatabasesResult) receive).getDatabases();
    }
    return infos;
  }

  public String generateCreateTableDDL(String databaseName, TableMeta tableMeta) throws ServiceException {
    if (Strings.isNullOrEmpty(tableMeta.getDatabase())) {
      tableMeta.setDatabase(databaseName);
    }
    Optional<String> createTableQuery = new CreateTableQueryGenerator(tableMeta).getQuery();
    if(createTableQuery.isPresent()) {
      LOG.info("generated create table query : {}", createTableQuery);
      return createTableQuery.get();
    }else {
      throw new ServiceException("could not generate create table query for database : " + databaseName + " table : " + tableMeta.getTable());
    }
  }

  public Job createTable(String databaseName, TableMeta tableMeta, JobResourceManager resourceManager) throws ServiceException {
    String createTableQuery = this.generateCreateTableDDL(databaseName, tableMeta);
    Map jobInfo = new HashMap<>();
    jobInfo.put("title", "Create table " + tableMeta.getDatabase() + "." + tableMeta.getTable());
    jobInfo.put("forcedContent", createTableQuery);
    jobInfo.put("dataBase", databaseName);

    try {
      Job job = new JobImpl(jobInfo);
      JobController createdJobController = new JobServiceInternal().createJob(job, resourceManager);
      Job returnableJob = createdJobController.getJobPOJO();
      LOG.info("returning job with id {} for create table {}", returnableJob.getId(), tableMeta.getTable());
      return returnableJob;
    } catch (Throwable e) {
      LOG.error("Exception occurred while creating the table for create Query : {}", createTableQuery, e);
      throw new ServiceException(e);
    }
  }

  public Job deleteTable(String databaseName, String tableName, JobResourceManager resourceManager) throws ServiceException {
    String deleteTableQuery = generateDeleteTableDDL(databaseName, tableName);
    Map jobInfo = new HashMap<>();
    jobInfo.put("title", "Delete table " + databaseName + "." + tableName);
    jobInfo.put("forcedContent", deleteTableQuery);
    jobInfo.put("dataBase", databaseName);

    try {
      Job job = new JobImpl(jobInfo);
      JobController createdJobController = new JobServiceInternal().createJob(job, resourceManager);
      Job returnableJob = createdJobController.getJobPOJO();
      LOG.info("returning job with id {} for the deletion of table : {}", returnableJob.getId(), tableName);
      return returnableJob;
    } catch (Throwable e) {
      LOG.error("Exception occurred while deleting the table for delete Query : {}", deleteTableQuery, e);
      throw new ServiceException(e);
    }
  }

  public String generateDeleteTableDDL(String databaseName, String tableName) throws ServiceException {
    Optional<String> deleteTableQuery = new DeleteTableQueryGenerator(databaseName, tableName).getQuery();
    if(deleteTableQuery.isPresent()) {
      LOG.info("deleting table {} with query {}", databaseName + "." + tableName, deleteTableQuery);
      return deleteTableQuery.get();
    }else{
      throw new ServiceException("Failed to generate query for delete table " + databaseName + "." + tableName);
    }
  }

  public Job alterTable(ViewContext context, ConnectionConfig hiveConnectionConfig, String databaseName, String oldTableName, TableMeta newTableMeta, JobResourceManager resourceManager) throws ServiceException {
    String alterQuery = generateAlterTableQuery(context, hiveConnectionConfig, databaseName, oldTableName, newTableMeta);
    Map jobInfo = new HashMap<>();
    jobInfo.put("title", "Alter table " + databaseName + "." + oldTableName);
    jobInfo.put("forcedContent", alterQuery);
    jobInfo.put("dataBase", databaseName);

    try {
      Job job = new JobImpl(jobInfo);
      JobController createdJobController = new JobServiceInternal().createJob(job, resourceManager);
      Job returnableJob = createdJobController.getJobPOJO();
      LOG.info("returning job with id {} for alter table {}", returnableJob.getId(), oldTableName);
      return returnableJob;
    } catch (Throwable e) {
      LOG.error("Exception occurred while creating the table for create Query : {}", alterQuery, e);
      throw new ServiceException(e);
    }
  }

  public String generateAlterTableQuery(ViewContext context, ConnectionConfig hiveConnectionConfig, String databaseName, String oldTableName, TableMeta newTableMeta) throws ServiceException {
    TableMeta oldTableMeta = this.getTableProperties(context, hiveConnectionConfig, databaseName, oldTableName);
    return generateAlterTableQuery(oldTableMeta, newTableMeta);
  }

  public String generateAlterTableQuery(TableMeta oldTableMeta, TableMeta newTableMeta) throws ServiceException {
    AlterTableQueryGenerator queryGenerator = new AlterTableQueryGenerator(oldTableMeta, newTableMeta);
    Optional<String> alterQuery = queryGenerator.getQuery();
    if(alterQuery.isPresent()){
      return alterQuery.get();
    }else{
      throw new ServiceException("Failed to generate alter table query for table " + oldTableMeta.getDatabase() + "." + oldTableMeta.getTable());
    }
  }

  public Job renameTable(String oldDatabaseName, String oldTableName, String newDatabaseName, String newTableName,
                         JobResourceManager resourceManager)
    throws ServiceException {
    RenameTableQueryGenerator queryGenerator = new RenameTableQueryGenerator(oldDatabaseName, oldTableName,
      newDatabaseName, newTableName);
    Optional<String> renameTable = queryGenerator.getQuery();
    if(renameTable.isPresent()) {
      String renameQuery = renameTable.get();
      LOG.info("Creating job for : {}", renameQuery);
      Map jobInfo = new HashMap<>();
      jobInfo.put("title", "Rename table " + oldDatabaseName + "." + oldTableName + " to " + newDatabaseName + "." + newTableName);
      jobInfo.put("forcedContent", renameQuery);
      jobInfo.put("dataBase", oldDatabaseName);

      try {
        Job job = new JobImpl(jobInfo);
        JobController createdJobController = new JobServiceInternal().createJob(job, resourceManager);
        Job returnableJob = createdJobController.getJobPOJO();
        LOG.info("returning job with id {} for rename table {}", returnableJob.getId(), oldTableName);
        return returnableJob;
      } catch (Throwable e) {
        LOG.error("Exception occurred while renaming the table for rename Query : {}", renameQuery, e);
        throw new ServiceException(e);
      }
    }else{
      throw new ServiceException("Failed to generate rename table query for table " + oldDatabaseName + "." +
        oldTableName);
    }
  }

  public Job deleteDatabase(String databaseName, JobResourceManager resourceManager) throws ServiceException {
    DeleteDatabaseQueryGenerator queryGenerator = new DeleteDatabaseQueryGenerator(databaseName);
    Optional<String> deleteDatabase = queryGenerator.getQuery();
    if(deleteDatabase.isPresent()) {
      String deleteQuery = deleteDatabase.get();
      LOG.info("Creating job for : {}", deleteQuery );
      Map jobInfo = new HashMap<>();
      jobInfo.put("title", "Delete database " + databaseName);
      jobInfo.put("forcedContent", deleteQuery);
      jobInfo.put("dataBase", databaseName);

      try {
        Job job = new JobImpl(jobInfo);
        JobController createdJobController = new JobServiceInternal().createJob(job, resourceManager);
        Job returnableJob = createdJobController.getJobPOJO();
        LOG.info("returning job with id {} for deleting database {}", returnableJob.getId(), databaseName);
        return returnableJob;
      } catch (Throwable e) {
        LOG.error("Exception occurred while renaming the table for rename Query : {}", deleteQuery, e);
        throw new ServiceException(e);
      }
    }else{
      throw new ServiceException("Failed to generate delete database query for database " + databaseName);
    }
  }
}