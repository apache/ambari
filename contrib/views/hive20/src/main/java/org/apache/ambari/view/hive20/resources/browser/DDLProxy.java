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

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.FluentIterable;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.hive20.ConnectionFactory;
import org.apache.ambari.view.hive20.ConnectionSystem;
import org.apache.ambari.view.hive20.client.ConnectionConfig;
import org.apache.ambari.view.hive20.client.DDLDelegator;
import org.apache.ambari.view.hive20.client.DDLDelegatorImpl;
import org.apache.ambari.view.hive20.client.DatabaseMetadataWrapper;
import org.apache.ambari.view.hive20.client.HiveClientException;
import org.apache.ambari.view.hive20.client.Row;
import org.apache.ambari.view.hive20.exceptions.ServiceException;
import org.apache.ambari.view.hive20.internal.dto.ColumnStats;
import org.apache.ambari.view.hive20.internal.dto.DatabaseInfo;
import org.apache.ambari.view.hive20.internal.dto.DatabaseResponse;
import org.apache.ambari.view.hive20.internal.dto.TableInfo;
import org.apache.ambari.view.hive20.internal.dto.TableMeta;
import org.apache.ambari.view.hive20.internal.dto.TableResponse;
import org.apache.ambari.view.hive20.internal.parsers.TableMetaParserImpl;
import org.apache.ambari.view.hive20.internal.query.generators.AlterTableQueryGenerator;
import org.apache.ambari.view.hive20.internal.query.generators.AnalyzeTableQueryGenerator;
import org.apache.ambari.view.hive20.internal.query.generators.CreateDatabaseQueryGenerator;
import org.apache.ambari.view.hive20.internal.query.generators.CreateTableQueryGenerator;
import org.apache.ambari.view.hive20.internal.query.generators.DeleteDatabaseQueryGenerator;
import org.apache.ambari.view.hive20.internal.query.generators.DeleteTableQueryGenerator;
import org.apache.ambari.view.hive20.internal.query.generators.FetchColumnStatsQueryGenerator;
import org.apache.ambari.view.hive20.internal.query.generators.RenameTableQueryGenerator;
import org.apache.ambari.view.hive20.resources.jobs.JobServiceInternal;
import org.apache.ambari.view.hive20.resources.jobs.ResultsPaginationController;
import org.apache.ambari.view.hive20.resources.jobs.viewJobs.Job;
import org.apache.ambari.view.hive20.resources.jobs.viewJobs.JobController;
import org.apache.ambari.view.hive20.resources.jobs.viewJobs.JobImpl;
import org.apache.ambari.view.hive20.resources.jobs.viewJobs.JobResourceManager;
import org.apache.ambari.view.hive20.resources.settings.Setting;
import org.apache.ambari.view.hive20.resources.settings.SettingsResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class DDLProxy {
  private static final Logger LOG = LoggerFactory.getLogger(DDLProxy.class);

  private final ViewContext context;
  private final TableMetaParserImpl tableMetaParser;
  private SettingsResourceManager settingsResourceManager;

  @Inject
  public DDLProxy(ViewContext context, TableMetaParserImpl tableMetaParser, SettingsResourceManager settingsResourceManager) {
    this.context = context;
    this.tableMetaParser = tableMetaParser;
    this.settingsResourceManager = settingsResourceManager;
    LOG.info("Creating DDLProxy");
  }


  public Set<DatabaseResponse> getDatabases() {
    Set<DatabaseInfo> infos = getDatabaseInfos();
    return transformToDatabasesResponse(infos);
  }

  public DatabaseResponse getDatabase(final String databaseId) {
    DatabaseInfo dbInfo = new DatabaseInfo(databaseId);
    List<TableInfo> tables = getTableInfos(databaseId);
    dbInfo.setTables(new HashSet<>(tables));

    return transformToDatabaseResponse(dbInfo);
  }

  public Set<TableResponse> getTables(final String databaseId) {
    List<TableInfo> tables = getTableInfos(databaseId);

    return FluentIterable.from(tables).transform(new Function<TableInfo, TableResponse>() {
      @Nullable
      @Override
      public TableResponse apply(@Nullable TableInfo tableInfo) {
        TableResponse response = new TableResponse();
        response.setDatabaseId(databaseId);
        response.setId(databaseId + "/" + tableInfo.getName());
        response.setName(tableInfo.getName());
        return response;
      }
    }).toSet();
  }

  private List<TableInfo> getTableInfos(String databaseId) {
    ConnectionConfig hiveConnectionConfig = ConnectionFactory.create(context);
    DDLDelegator delegator = new DDLDelegatorImpl(context, ConnectionSystem.getInstance().getActorSystem(), ConnectionSystem.getInstance().getOperationController(context));
    return delegator.getTableList(hiveConnectionConfig, databaseId, "*");
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

  public Job getColumnStatsJob(final String databaseName, final String tableName, final String columnName,
                               JobResourceManager resourceManager) throws ServiceException {
    FetchColumnStatsQueryGenerator queryGenerator = new FetchColumnStatsQueryGenerator(databaseName, tableName,
        columnName);
    Optional<String> q = queryGenerator.getQuery();
    String jobTitle = "Fetch column stats for " + databaseName + "." + tableName + "." + columnName;
    if (q.isPresent()) {
      String query = q.get();
      Optional<String> settingsString = generateSettingsString();
      if (settingsString.isPresent()) {
        query = settingsString.get() + query;
      }
      return createJob(databaseName, query, jobTitle, resourceManager);
    } else {
      throw new ServiceException("Failed to generate job for {}" + jobTitle);
    }
  }

  public TableMeta getTableProperties(ViewContext context, ConnectionConfig connectionConfig, String databaseName, String tableName) {
    DDLDelegator delegator = new DDLDelegatorImpl(context, ConnectionSystem.getInstance().getActorSystem(), ConnectionSystem.getInstance().getOperationController(context));
    List<Row> createTableStatementRows = delegator.getTableCreateStatement(connectionConfig, databaseName, tableName);
    List<Row> describeFormattedRows = delegator.getTableDescriptionFormatted(connectionConfig, databaseName, tableName);
    DatabaseMetadataWrapper databaseMetadata = new DatabaseMetadataWrapper(1, 0);
    try {
      databaseMetadata = delegator.getDatabaseMetadata(connectionConfig);
    } catch (ServiceException e) {
      LOG.error("Exception while fetching hive version", e);
    }

    return tableMetaParser.parse(databaseName, tableName, createTableStatementRows, describeFormattedRows, databaseMetadata);
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
    ConnectionConfig hiveConnectionConfig = ConnectionFactory.create(context);
    DDLDelegator delegator = new DDLDelegatorImpl(context, ConnectionSystem.getInstance().getActorSystem(), ConnectionSystem.getInstance().getOperationController(context));
    List<DatabaseInfo> databases = delegator.getDbList(hiveConnectionConfig, "*");
    return new HashSet<>(databases);
  }

  public String generateCreateTableDDL(String databaseName, TableMeta tableMeta) throws ServiceException {
    if (Strings.isNullOrEmpty(tableMeta.getDatabase())) {
      tableMeta.setDatabase(databaseName);
    }
    Optional<String> createTableQuery = new CreateTableQueryGenerator(tableMeta).getQuery();
    if (createTableQuery.isPresent()) {
      LOG.info("generated create table query : {}", createTableQuery);
      return createTableQuery.get();
    } else {
      throw new ServiceException("could not generate create table query for database : " + databaseName + " table : " + tableMeta.getTable());
    }
  }

  public Job createTable(String databaseName, TableMeta tableMeta, JobResourceManager resourceManager) throws ServiceException {
    String createTableQuery = this.generateCreateTableDDL(databaseName, tableMeta);
    Optional<String> settingsString = generateSettingsString();
    if (settingsString.isPresent()) {
      createTableQuery = settingsString.get() + createTableQuery;
    }
    String jobTitle = "Create table " + tableMeta.getDatabase() + "." + tableMeta.getTable();

    return createJob(databaseName, createTableQuery, jobTitle, resourceManager);
  }

  private Optional<String> generateSettingsString() {
    List<Setting> settings = settingsResourceManager.getSettings();
    if (null != settings && !settings.isEmpty()) {
      return Optional.of(Joiner.on(";\n").join(FluentIterable.from(settings).transform(new Function<Setting, String>() {
        @Override
        public String apply(Setting setting) {
          return "set " + setting.getKey() + "=" + setting.getValue();
        }
      }).toList()) + ";\n"/*need this ;\n at the end of last line also.*/);
    } else {
      return Optional.absent();
    }
  }

  public Job deleteTable(String databaseName, String tableName, JobResourceManager resourceManager) throws ServiceException {
    String deleteTableQuery = generateDeleteTableDDL(databaseName, tableName);
    String jobTitle = "Delete table " + databaseName + "." + tableName;
    Optional<String> settingsString = generateSettingsString();
    if (settingsString.isPresent()) {
      deleteTableQuery = settingsString.get() + deleteTableQuery;
    }

    return createJob(databaseName, deleteTableQuery, jobTitle, resourceManager);
  }

  public String generateDeleteTableDDL(String databaseName, String tableName) throws ServiceException {
    Optional<String> deleteTableQuery = new DeleteTableQueryGenerator(databaseName, tableName).getQuery();
    if (deleteTableQuery.isPresent()) {
      LOG.info("deleting table {} with query {}", databaseName + "." + tableName, deleteTableQuery);
      return deleteTableQuery.get();
    } else {
      throw new ServiceException("Failed to generate query for delete table " + databaseName + "." + tableName);
    }
  }

  public Job alterTable(ViewContext context, ConnectionConfig hiveConnectionConfig, String databaseName, String oldTableName, TableMeta newTableMeta, JobResourceManager resourceManager) throws ServiceException {
    String alterQuery = generateAlterTableQuery(context, hiveConnectionConfig, databaseName, oldTableName, newTableMeta);
    String jobTitle = "Alter table " + databaseName + "." + oldTableName;
    Optional<String> settingsString = generateSettingsString();
    if (settingsString.isPresent()) {
      alterQuery = settingsString.get() + alterQuery;
    }

    return createJob(databaseName, alterQuery, jobTitle, resourceManager);
  }

  public String generateAlterTableQuery(ViewContext context, ConnectionConfig hiveConnectionConfig, String databaseName, String oldTableName, TableMeta newTableMeta) throws ServiceException {
    TableMeta oldTableMeta = this.getTableProperties(context, hiveConnectionConfig, databaseName, oldTableName);
    return generateAlterTableQuery(oldTableMeta, newTableMeta);
  }

  public String generateAlterTableQuery(TableMeta oldTableMeta, TableMeta newTableMeta) throws ServiceException {
    AlterTableQueryGenerator queryGenerator = new AlterTableQueryGenerator(oldTableMeta, newTableMeta);
    Optional<String> alterQuery = queryGenerator.getQuery();
    if (alterQuery.isPresent()) {
      return alterQuery.get();
    } else {
      throw new ServiceException("Failed to generate alter table query for table " + oldTableMeta.getDatabase() + "." + oldTableMeta.getTable() + ". No difference was found.");
    }
  }

  public Job renameTable(String oldDatabaseName, String oldTableName, String newDatabaseName, String newTableName,
                         JobResourceManager resourceManager)
      throws ServiceException {
    RenameTableQueryGenerator queryGenerator = new RenameTableQueryGenerator(oldDatabaseName, oldTableName,
        newDatabaseName, newTableName);
    Optional<String> renameTable = queryGenerator.getQuery();
    if (renameTable.isPresent()) {
      String renameQuery = renameTable.get();
      String jobTitle = "Rename table " + oldDatabaseName + "." + oldTableName + " to " + newDatabaseName + "." +
          newTableName;
      Optional<String> settingsString = generateSettingsString();
      if (settingsString.isPresent()) {
        renameQuery = settingsString.get() + renameQuery;
      }
      return createJob(oldDatabaseName, renameQuery, jobTitle, resourceManager);
    } else {
      throw new ServiceException("Failed to generate rename table query for table " + oldDatabaseName + "." +
          oldTableName);
    }
  }

  public Job deleteDatabase(String databaseName, JobResourceManager resourceManager) throws ServiceException {
    DeleteDatabaseQueryGenerator queryGenerator = new DeleteDatabaseQueryGenerator(databaseName);
    Optional<String> deleteDatabase = queryGenerator.getQuery();
    if (deleteDatabase.isPresent()) {
      String deleteQuery = deleteDatabase.get();
      Optional<String> settingsString = generateSettingsString();
      if (settingsString.isPresent()) {
        deleteQuery = settingsString.get() + deleteQuery;
      }

      return createJob(databaseName, deleteQuery, "Delete database " + databaseName, resourceManager);
    } else {
      throw new ServiceException("Failed to generate delete database query for database " + databaseName);
    }
  }

  public Job createDatabase(String databaseName, JobResourceManager resourceManager) throws ServiceException {
    CreateDatabaseQueryGenerator queryGenerator = new CreateDatabaseQueryGenerator(databaseName);
    Optional<String> createDatabase = queryGenerator.getQuery();
    if (createDatabase.isPresent()) {
      String createQuery = createDatabase.get();
      Optional<String> settingsString = generateSettingsString();
      if (settingsString.isPresent()) {
        createQuery = settingsString.get() + createQuery;
      }

      return createJob("default", createQuery, "CREATE DATABASE " + databaseName, resourceManager);
    } else {
      throw new ServiceException("Failed to generate create database query for database " + databaseName);
    }
  }

  public Job createJob(String databaseName, String query, String jobTitle, JobResourceManager resourceManager)
      throws ServiceException {
    LOG.info("Creating job for : {}", query);
    Map jobInfo = new HashMap<>();
    jobInfo.put("title", jobTitle);
    jobInfo.put("forcedContent", query);
    jobInfo.put("dataBase", databaseName);
    jobInfo.put("referrer", JobImpl.REFERRER.INTERNAL.name());

    try {
      Job job = new JobImpl(jobInfo);
      JobController createdJobController = new JobServiceInternal().createJob(job, resourceManager);
      Job returnableJob = createdJobController.getJobPOJO();
      LOG.info("returning job with id {} for {}", returnableJob.getId(), jobTitle);
      return returnableJob;
    } catch (Throwable e) {
      LOG.error("Exception occurred while {} : {}", jobTitle, query, e);
      throw new ServiceException(e);
    }
  }

  public Job analyzeTable(String databaseName, String tableName, Boolean shouldAnalyzeColumns, JobResourceManager resourceManager, ConnectionConfig hiveConnectionConfig) throws ServiceException {
    TableMeta tableMeta = this.getTableProperties(context, hiveConnectionConfig, databaseName, tableName);

    AnalyzeTableQueryGenerator queryGenerator = new AnalyzeTableQueryGenerator(tableMeta, shouldAnalyzeColumns);
    Optional<String> analyzeTable = queryGenerator.getQuery();
    String jobTitle = "Analyze table " + databaseName + "." + tableName;
    if (analyzeTable.isPresent()) {
      String query = analyzeTable.get();
      Optional<String> settingsString = generateSettingsString();
      if (settingsString.isPresent()) {
        query = settingsString.get() + query;
      }
      return createJob(databaseName, query, jobTitle, resourceManager);
    } else {
      throw new ServiceException("Failed to generate job for {}" + jobTitle);
    }
  }

  public ColumnStats fetchColumnStats(String columnName, String jobId, ViewContext context) throws ServiceException {
    try {
      ResultsPaginationController.ResultsResponse results = ResultsPaginationController.getResult(jobId, null, null, null, null, context);
      if (results.getHasResults()) {
        List<String[]> rows = results.getRows();
        Map<Integer, String> headerMap = new HashMap<>();
        boolean header = true;
        ColumnStats columnStats = new ColumnStats();
        for (String[] row : rows) {
          if (header) {
            for (int i = 0; i < row.length; i++) {
              if (!Strings.isNullOrEmpty(row[i])) {
                headerMap.put(i, row[i].trim());
              }
            }
            header = false;
          } else if (row.length > 0) {
            if (columnName.equals(row[0])) { // the first column of the row contains column name
              createColumnStats(row, headerMap, columnStats);
            } else if (row.length > 1 && row[0].equalsIgnoreCase("COLUMN_STATS_ACCURATE")) {
              columnStats.setColumnStatsAccurate(row[1]);
            }
          }
        }

        return columnStats;
      } else {
        throw new ServiceException("Cannot find any result for this jobId: " + jobId);
      }
    } catch (HiveClientException e) {
      LOG.error("Exception occurred while fetching results for column statistics with jobId: {}", jobId, e);
      throw new ServiceException(e);
    }
  }

  /**
   * order of values in array
   * row [# col_name, data_type, min, max, num_nulls, distinct_count, avg_col_len, max_col_len,num_trues,num_falses,comment]
   * indexes : 0           1        2    3     4             5               6             7           8         9    10
   *
   * @param row
   * @param headerMap
   * @param columnStats
   * @return
   */
  private ColumnStats createColumnStats(String[] row, Map<Integer, String> headerMap, ColumnStats columnStats) throws ServiceException {
    if (null == row) {
      throw new ServiceException("row cannot be null.");
    }
    for (int i = 0; i < row.length; i++) {
      switch (headerMap.get(i)) {
        case ColumnStats.COLUMN_NAME:
          columnStats.setColumnName(row[i]);
          break;
        case ColumnStats.DATA_TYPE:
          columnStats.setDataType(row[i]);
          break;
        case ColumnStats.MIN:
          columnStats.setMin(row[i]);
          break;
        case ColumnStats.MAX:
          columnStats.setMax(row[i]);
          break;
        case ColumnStats.NUM_NULLS:
          columnStats.setNumNulls(row[i]);
          break;
        case ColumnStats.DISTINCT_COUNT:
          columnStats.setDistinctCount(row[i]);
          break;
        case ColumnStats.AVG_COL_LEN:
          columnStats.setAvgColLen(row[i]);
          break;
        case ColumnStats.MAX_COL_LEN:
          columnStats.setMaxColLen(row[i]);
          break;
        case ColumnStats.NUM_TRUES:
          columnStats.setNumTrues(row[i]);
          break;
        case ColumnStats.NUM_FALSES:
          columnStats.setNumFalse(row[i]);
          break;
        case ColumnStats.COMMENT:
          columnStats.setComment(row[i]);
      }
    }

    return columnStats;
  }
}