/*
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
package org.apache.ambari.server.orm;

import com.google.inject.Inject;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.helpers.ScriptRunner;
import org.apache.ambari.server.orm.helpers.dbms.*;
import org.eclipse.persistence.internal.helper.DBPlatformHelper;
import org.eclipse.persistence.internal.sessions.DatabaseSessionImpl;
import org.eclipse.persistence.logging.AbstractSessionLog;
import org.eclipse.persistence.logging.SessionLogEntry;
import org.eclipse.persistence.platform.database.*;
import org.eclipse.persistence.sessions.DatabaseLogin;
import org.eclipse.persistence.sessions.DatabaseSession;
import org.eclipse.persistence.sessions.Login;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DBAccessorImpl implements DBAccessor {
  private static final Logger LOG = LoggerFactory.getLogger(DBAccessorImpl.class);
  private final DatabasePlatform databasePlatform;
  private final Connection connection;
  private final DbmsHelper dbmsHelper;
  private Configuration configuration;
  private DatabaseMetaData databaseMetaData;

  @Inject
  public DBAccessorImpl(Configuration configuration) {
    this.configuration = configuration;

    try {
      Class.forName(configuration.getDatabaseDriver());

      connection = DriverManager.getConnection(configuration.getDatabaseUrl(),
        configuration.getDatabaseUser(),
        configuration.getDatabasePassword());

      connection.setAutoCommit(true); //enable autocommit

      //TODO create own mapping and platform classes for supported databases
      String vendorName = connection.getMetaData().getDatabaseProductName() +
        connection.getMetaData().getDatabaseMajorVersion();
      String dbPlatform = DBPlatformHelper.getDBPlatform(vendorName, new AbstractSessionLog() {
        @Override
        public void log(SessionLogEntry sessionLogEntry) {
          LOG.debug(sessionLogEntry.getMessage());
        }
      });
      this.databasePlatform = (DatabasePlatform) Class.forName(dbPlatform).newInstance();
      this.dbmsHelper = loadHelper(databasePlatform);
    } catch (Exception e) {
      String message = "Error while creating database accessor ";
      LOG.error(message, e);
      throw new RuntimeException(e);
    }
  }

  protected DbmsHelper loadHelper(DatabasePlatform databasePlatform) {
    if (databasePlatform instanceof OraclePlatform) {
      return new OracleHelper(databasePlatform);
    }else if (databasePlatform instanceof MySQLPlatform) {
      return new MySqlHelper(databasePlatform);
    }else if (databasePlatform instanceof PostgreSQLPlatform) {
      return new PostgresHelper(databasePlatform);
    }else if (databasePlatform instanceof DerbyPlatform) {
      return new DerbyHelper(databasePlatform);
    } else {
      return new GenericDbmsHelper(databasePlatform);
    }
  }

  protected Connection getConnection() {
    return connection;
  }

  @Override
  public void createTable(String tableName, List<DBColumnInfo> columnInfo,
                          String... primaryKeyColumns) throws SQLException {
    if (!tableExists(tableName)) {
      String query = dbmsHelper.getCreateTableStatement(tableName, columnInfo, Arrays.asList(primaryKeyColumns));

      executeQuery(query);
    }
  }

  protected DatabaseMetaData getDatabaseMetaData() throws SQLException {
    if (databaseMetaData == null) {
      databaseMetaData = connection.getMetaData();
    }

    return databaseMetaData;
  }

  private String convertObjectName(String objectName) throws SQLException {
    DatabaseMetaData metaData = getDatabaseMetaData();
    if (metaData.storesLowerCaseIdentifiers()) {
      return objectName.toLowerCase();
    }else if (metaData.storesUpperCaseIdentifiers()) {
      return objectName.toUpperCase();
    }

    return objectName;
  }

  @Override
  public boolean tableExists(String tableName) throws SQLException {
    boolean result = false;
    DatabaseMetaData metaData = getDatabaseMetaData();
    String schemaFilter = null;
    if (getDbType().equals(Configuration.ORACLE_DB_NAME)) {
      // Optimization to not query everything
      schemaFilter = configuration.getDatabaseUser();
    }

    ResultSet res = metaData.getTables(null, schemaFilter,
      convertObjectName(tableName), new String[] { "TABLE" });

    if (res != null) {
      try {
        if (res.next()) {
          return res.getString("TABLE_NAME") != null && res.getString
            ("TABLE_NAME").equalsIgnoreCase(tableName);
        }
      } finally {
        res.close();
      }
    }

    return result;
  }

  protected String getDbType() {
    String dbUrl = configuration.getDatabaseUrl();
    String dbType;

    if (dbUrl.contains(Configuration.POSTGRES_DB_NAME)) {
      dbType = Configuration.POSTGRES_DB_NAME;
    } else if (dbUrl.contains(Configuration.ORACLE_DB_NAME)) {
      dbType = Configuration.ORACLE_DB_NAME;
    } else if (dbUrl.contains(Configuration.MYSQL_DB_NAME)) {
      dbType = Configuration.MYSQL_DB_NAME;
    } else if (dbUrl.contains(Configuration.DERBY_DB_NAME)) {
      dbType = Configuration.DERBY_DB_NAME;
    } else {
      throw new RuntimeException("Unable to determine database type.");
    }

    return dbType;
  }

  @Override
  public boolean tableHasData(String tableName) throws SQLException {
    String query = "SELECT count(*) from " + tableName;
    Statement statement = getConnection().createStatement();
    ResultSet rs = statement.executeQuery(query);
    boolean retVal = false;
    if (rs != null) {
      if (rs.next()) {
        return rs.getInt(1) > 0;
      }
    }
    return retVal;
  }

  @Override
  public boolean tableHasColumn(String tableName, String columnName) throws SQLException {
    boolean result = false;
    DatabaseMetaData metaData = getDatabaseMetaData();
    String schemaFilter = null;
    if (getDbType().equals(Configuration.ORACLE_DB_NAME)) {
      // Optimization to not query everything
      schemaFilter = configuration.getDatabaseUser();
    }

    ResultSet rs = metaData.getColumns(null, schemaFilter,
        convertObjectName(tableName), convertObjectName(columnName));

    if (rs != null) {
      try {
        if (rs.next()) {
          return rs.getString("COLUMN_NAME") != null && rs.getString
            ("COLUMN_NAME").equalsIgnoreCase(columnName);
        }
      } finally {
        rs.close();
      }
    }

    return result;
  }

  @Override
  public boolean tableHasForeignKey(String tableName, String refTableName,
              String columnName, String refColumnName) throws SQLException {
    boolean result = false;
    DatabaseMetaData metaData = getDatabaseMetaData();
    String schemaFilter = null;
    if (getDbType().equals(Configuration.ORACLE_DB_NAME)) {
      // Optimization to not query everything
      schemaFilter = configuration.getDatabaseUser();
    }

    ResultSet rs = metaData.getCrossReference(null, schemaFilter, convertObjectName(tableName),
      null, schemaFilter, convertObjectName(refTableName));

    if (rs != null) {
      try {
        while (rs.next()) {
          String refColumn = rs.getString("FKCOLUMN_NAME");
          if (refColumn != null && refColumn.equalsIgnoreCase(refColumnName)) {
            result = true;
          }
        }
      } finally {
        rs.close();
      }
    }

    return result;
  }

  @Override
  public void createIndex(String indexName, String tableName,
                          String... columnNames) throws SQLException {
    String query = dbmsHelper.getCreateIndexStatement(indexName, tableName, columnNames);

    executeQuery(query);
  }

  @Override
  public void addFKConstraint(String tableName, String constraintName,
                              String keyColumn, String referenceTableName,
                              String referenceColumn, boolean ignoreFailure) throws SQLException {

    if (!tableHasForeignKey(tableName, referenceTableName, keyColumn, referenceColumn)) {

      String query = dbmsHelper.getAddForeignKeyStatement(tableName, constraintName,
        Collections.singletonList(keyColumn),
        referenceTableName,
        Collections.singletonList(referenceColumn)
      );

      try {
        executeQuery(query);
      } catch (SQLException e) {
        LOG.warn("Add FK constraint failed" +
          ", constraintName = " + constraintName +
          ", tableName = " + tableName, e);
        if (!ignoreFailure) {
          throw e;
        }
      }
    }
  }

  @Override
  public void addFKConstraint(String tableName, String constraintName,
                              String[] keyColumns, String referenceTableName,
                              String[] referenceColumns, boolean ignoreFailure) throws SQLException {
    String query = dbmsHelper.getAddForeignKeyStatement(tableName, constraintName,
        Arrays.asList(keyColumns),
        referenceTableName,
        Arrays.asList(referenceColumns)
    );

    try {
      executeQuery(query);
    } catch (SQLException e) {
      LOG.warn("Add FK constraint failed" +
              ", constraintName = " + constraintName +
              ", tableName = " + tableName, e);
      if (!ignoreFailure) {
        throw e;
      }
    }
  }

  @Override
  public void renameColumn(String tableName, String oldColumnName,
                           DBColumnInfo columnInfo) throws SQLException {
    //it is mandatory to specify type in column change clause for mysql
    String renameColumnStatement = dbmsHelper.getRenameColumnStatement(tableName, oldColumnName, columnInfo);
    executeQuery(renameColumnStatement);

  }

  @Override
  public void addColumn(String tableName, DBColumnInfo columnInfo) throws SQLException {
    if (!tableHasColumn(tableName, columnInfo.getName())) {
      //TODO workaround for default values, possibly we will have full support later
      if (columnInfo.getDefaultValue() != null) {
        columnInfo.setNullable(true);
      }
      String query = dbmsHelper.getAddColumnStatement(tableName, columnInfo);
      executeQuery(query);

      if (columnInfo.getDefaultValue() != null) {
        updateTable(tableName, columnInfo.getName(), columnInfo.getDefaultValue(), "");
      }
    }
  }

  @Override
  public void alterColumn(String tableName, DBColumnInfo columnInfo) throws SQLException {
    //varchar extension only (derby limitation, but not too much for others),
    //use addColumn-update-drop-rename for more
    String statement = dbmsHelper.getAlterColumnStatement(tableName, columnInfo);
    executeQuery(statement);
  }

  @Override
  public boolean insertRow(String tableName, String[] columnNames, String[] values, boolean ignoreFailure) throws SQLException {
    StringBuilder builder = new StringBuilder();
    builder.append("INSERT INTO ").append(tableName).append("(");
    if (columnNames.length != values.length) {
      throw new IllegalArgumentException("number of columns should be equal to number of values");
    }

    for (int i = 0; i < columnNames.length; i++) {
      builder.append(columnNames[i]);
      if(i!=columnNames.length-1){
        builder.append(",");
      }
    }

    builder.append(") VALUES(");

    for (int i = 0; i < values.length; i++) {
      builder.append(values[i]);
      if(i!=values.length-1){
        builder.append(",");
      }
    }

    builder.append(")");

    Statement statement = getConnection().createStatement();
    int rowsUpdated = 0;
    String query = builder.toString();
    try {
      rowsUpdated = statement.executeUpdate(query);
    } catch (SQLException e) {
      LOG.warn("Unable to execute query: " + query, e);
      if (!ignoreFailure) {
        throw e;
      }
    }

    return rowsUpdated != 0;
  }


  @Override
  public int updateTable(String tableName, String columnName, Object value,
                         String whereClause) throws SQLException {

    StringBuilder query = new StringBuilder
      (String.format("UPDATE %s SET %s = ", tableName, columnName));

    // Only String and number supported.
    // Taken from: org.eclipse.persistence.internal.databaseaccess.appendParameterInternal
    Object dbValue = databasePlatform.convertToDatabaseType(value);
    String valueString = value.toString();
    if (dbValue instanceof String) {
      valueString = "'" + value.toString() + "'";
    }

    query.append(valueString);
    query.append(" ");
    query.append(whereClause);

    Statement statement = getConnection().createStatement();

    return statement.executeUpdate(query.toString());
  }

  @Override
  public void executeQuery(String query) throws SQLException {
    executeQuery(query, false);
  }

  @Override
  public ResultSet executeSelect(String query) throws SQLException {
    Statement statement = getConnection().createStatement();
    return statement.executeQuery(query);
  }

  @Override
  public void executeQuery(String query, boolean ignoreFailure) throws SQLException {
    LOG.info("Executing query: {}", query);
    Statement statement = getConnection().createStatement();
    try {
      statement.execute(query);
    } catch (SQLException e) {
      LOG.warn("Error executing query: "+query, e);
      if (!ignoreFailure) {
        throw e;
      }
    }
  }

  @Override
  public void dropTable(String tableName) throws SQLException {
    String query = dbmsHelper.getDropTableStatement(tableName);
    executeQuery(query);
  }

  @Override
  public void truncateTable(String tableName) throws SQLException {
    String query = "DELETE FROM " + tableName;
    executeQuery(query);
  }

  @Override
  public void dropColumn(String tableName, String columnName) throws SQLException {
    throw new UnsupportedOperationException("Drop column not supported");
  }

  @Override
  public void dropSequence(String sequenceName) throws SQLException {
    executeQuery(dbmsHelper.getDropSequenceStatement(sequenceName));
  }

  @Override
  public void dropConstraint(String tableName, String constraintName) throws SQLException {
    String query = dbmsHelper.getDropConstraintStatement(tableName, constraintName);

    executeQuery(query);
  }

  @Override
  /**
   * Execute script with autocommit and error tolerance, like psql and sqlplus do by default
   */
  public void executeScript(String filePath) throws SQLException, IOException {
    BufferedReader br = new BufferedReader(new FileReader(filePath));
    ScriptRunner scriptRunner = new ScriptRunner(getConnection(), false, false);
    scriptRunner.runScript(br);
  }

  @Override
  public DatabaseSession getNewDatabaseSession() {
    DatabaseLogin login = new DatabaseLogin();
    login.setUserName(configuration.getDatabaseUser());
    login.setPassword(configuration.getDatabasePassword());
    login.setDatasourcePlatform(databasePlatform);
    login.setDatabaseURL(configuration.getDatabaseUrl());
    login.setDriverClassName(configuration.getDatabaseDriver());


    return new DatabaseSessionImpl(login);
  }
}
