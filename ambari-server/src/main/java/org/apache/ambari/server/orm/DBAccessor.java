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

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.eclipse.persistence.sessions.DatabaseSession;

/**
 * Interface for schema manipulation
 * Note: IF NOT EXISTS is default for all supported DDL statements
 */
public interface DBAccessor {

  /**
   * @return new database connection
   */
  Connection getNewConnection();

  /**
   * Wraps object name with dbms-specific quotes
   * @param name object name without quotes
   * @return quoted name
   */
  String quoteObjectName(String name);

  /**
   * Create new table
   * @param tableName
   * @param columnInfo
   * @param primaryKeyColumns
   * @throws SQLException
   */
  public void createTable(String tableName, List<DBColumnInfo> columnInfo,
                          String... primaryKeyColumns) throws SQLException;

  /**
   * Create new index
   * @param indexName
   * @param tableName
   * @param columnNames
   * @throws SQLException
   */
  public void createIndex(String indexName, String tableName,
                          String... columnNames) throws SQLException;


  /**
   * Add foreign key for a relation
   * @param tableName
   * @param constraintName
   * @param keyColumn
   * @param referenceColumn
   * @throws SQLException
   */
  public void addFKConstraint(String tableName,
                              String constraintName,
                              String keyColumn,
                              String referenceTableName,
                              String referenceColumn,
                              boolean ignoreFailure) throws SQLException;

  /**
   * Add foreign key for a relation
   * @param tableName
   * @param constraintName
   * @param keyColumn
   * @param referenceColumn
   * @throws SQLException
   */
  public void addFKConstraint(String tableName,
                              String constraintName,
                              String[] keyColumns,
                              String referenceTableName,
                              String[] referenceColumns,
                              boolean ignoreFailure) throws SQLException;

  /**
   * Add column to existing table
   * @param tableName
   * @param columnInfo
   * @throws SQLException
   */
  public void addColumn(String tableName,
                        DBColumnInfo columnInfo) throws SQLException;

  /**
   * Rename existing column
   * @param tableName
   * @param oldColumnName
   * @param columnInfo
   * @throws SQLException
   */
  void renameColumn(String tableName, String oldColumnName,
                    DBColumnInfo columnInfo) throws SQLException;

  /**
   * Alter column from existing table, only supports varchar extension <br/>
   * Use following sequence for more complex stuff: <br/>
   * <li/>{@link #addColumn(String, org.apache.ambari.server.orm.DBAccessor.DBColumnInfo)}
   * <li/>{@link #updateTable(String, String, Object, String)}
   * <li/>{@link #dropColumn(String, String)}
   * <li/>{@link #renameColumn(String, String, org.apache.ambari.server.orm.DBAccessor.DBColumnInfo)}
   * @param tableName
   * @param columnInfo
   * @throws SQLException
   */
  public void alterColumn(String tableName,
                          DBColumnInfo columnInfo) throws SQLException;

  /**
   * Insert row into table
   *
   * @param tableName
   * @param columnNames
   * @param values
   * @param ignoreFailure
   * @return
   * @throws SQLException
   */
  boolean insertRow(String tableName, String[] columnNames, String[] values, boolean ignoreFailure) throws SQLException;

  /**
   * Simple update operation on table
   * @param tableName
   * @param columnName
   * @param value
   * @param whereClause
   * @return
   * @throws SQLException
   */
  public int updateTable(String tableName, String columnName, Object value,
                         String whereClause) throws SQLException;

  /**
   * Simple update operation on table
   *
   * @param tableName
   * @param columnNameSrc
   * @param columnNameTgt
   * @return
   * @throws SQLException
   */
  public void updateTable(String tableName, DBColumnInfo columnNameSrc,
                         DBColumnInfo columnNameTgt) throws SQLException;

  /**
   * Helper method to run third party scripts like Quartz DDL
   * @param filePath
   * @throws SQLException
   */
  public void executeScript(String filePath) throws SQLException, IOException;


  /**
   * Execute ad-hoc query on DB.
   * @param query
   * @throws SQLException
   */
  public void executeQuery(String query) throws SQLException;

  /**
   * Execute select query
   * @param query
   * @return
   * @throws SQLException
   */
  ResultSet executeSelect(String query) throws SQLException;

  /**
   * Execute select query
   * @param query
   * @param resultSetType
   * @param resultSetConcur
   * @return
   * @throws SQLException
   */
  ResultSet executeSelect(String query, int resultSetType, int resultSetConcur) throws SQLException;

  /**
   * Execute query on DB
   * @param query
   * @param ignoreFailure
   * @throws SQLException
   */
  void executeQuery(String query, boolean ignoreFailure) throws SQLException;

  /**
   * Drop table from schema
   * @param tableName
   * @throws SQLException
   */
  public void dropTable(String tableName) throws SQLException;

  /**
   * Delete all table data
   * @param tableName
   * @throws SQLException
   */
  public void truncateTable(String tableName) throws SQLException;

  /**
   * Drop a column from table
   * @param tableName
   * @param columnName
   * @throws SQLException
   */
  public void dropColumn(String tableName, String columnName) throws SQLException;

  /**
   * Drop sequence
   * @param sequenceName
   * @throws SQLException
   */
  public void dropSequence(String sequenceName) throws SQLException;

  /**
   * Drop a constraint from table
   * @param tableName
   * @param constraintName
   * @throws SQLException
   */
  public void dropConstraint(String tableName, String constraintName) throws SQLException;

  /**
   * Drop a constraint from table
   * @param tableName
   * @param constraintName
   * @throws SQLException
   */
  void dropConstraint(String tableName, String constraintName, boolean ignoreFailure) throws SQLException;

  /**
   * Verify if table exists by looking at metadata.
   * @param tableName
   * @return
   * @throws SQLException
   */
  public boolean tableExists(String tableName) throws SQLException;

  /**
   * Verify if table has any data
   * @param tableName
   * @return
   * @throws SQLException
   */
  public boolean tableHasData(String tableName) throws SQLException;

  /**
   * Verify if table already has a column defined.
   * @param tableName
   * @param columnName
   * @return
   * @throws SQLException
   */
  public boolean tableHasColumn(String tableName, String columnName) throws SQLException;

  /**
   * Verify if table has a FK constraint.
   * @param tableName
   * @param fkName
   * @return true if FK with such name exists
   * @throws SQLException
   */
  public boolean tableHasForeignKey(String tableName, String fkName) throws SQLException;

  /**
   * Verify if table already has a FK constraint.
   * @param tableName
   * @param refTableName
   * @param columnName
   * @param refColumnName
   * @return true if described relation exists
   * @throws SQLException
   */
  public boolean tableHasForeignKey(String tableName, String refTableName,
             String columnName, String refColumnName) throws SQLException;

  /**
   * Verify if table already has a FK constraint.
   * @param tableName
   * @param referenceTableName
   * @param keyColumns
   * @param referenceColumns
   * @return true if described relation exists
   * @throws SQLException
   */
  public boolean tableHasForeignKey(String tableName, String referenceTableName, String[] keyColumns,
                             String[] referenceColumns) throws SQLException;

  /**
   * Get a new DB session
   * @return
   */
  public DatabaseSession getNewDatabaseSession();

  /**
   * Gets the column's SQL type
   * 
   * @param tableName
   *          the name of the table (not {@code null}).
   * @param columnName
   *          the name of the column to retrieve type for (not {@code null}).
   * @return the integer representation of the column type from {@link Types}.
   * @throws SQLException
   * @see {@link Types}
   */
  public int getColumnType(String tableName, String columnName)
      throws SQLException;

  /**
   * Sets the specified column to either allow or prohibit {@code NULL}.
   * 
   * @param tableName
   *          the name of the table (not {@code null}).
   * @param columnInfo
   *          the column object to get name and type of column (not {@code null}).
   * @param nullable
   *          {@code true} to indicate that the column allows {@code NULL}
   *          values, {@code false} otherwise.
   * @throws SQLException
   */
  public void setNullable(String tableName, DBAccessor.DBColumnInfo columnInfo, boolean nullable)
      throws SQLException;

  public static enum DbType {
    ORACLE,
    MYSQL,
    POSTGRES,
    DERBY,
    UNKNOWN
  }

  /**
   * Get type of database platform
   * @return @DbType
   */
  public DbType getDbType();

  /**
   * Capture column type
   */
  public class DBColumnInfo {
    private String name;
    private Class type;
//    private DBColumnType type;
    private Integer length;
    private Object defaultValue;
    private boolean isNullable;

    public DBColumnInfo(String name, Class type, Integer length) {
      this(name, type, length, null, true);
    }

    public DBColumnInfo(String name, Class type, Integer length,
                        Object defaultValue, boolean nullable) {
      this.name = name;
      this.type = type;
      this.length = length;
      this.defaultValue = defaultValue;
      isNullable = nullable;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public Class getType() {
      return type;
    }

    public void setType(Class type) {
      this.type = type;
    }

    public Integer getLength() {
      return length;
    }

    public void setLength(Integer length) {
      this.length = length;
    }

    public Object getDefaultValue() {
      return defaultValue;
    }

    public void setDefaultValue(Object defaultValue) {
      this.defaultValue = defaultValue;
    }

    public boolean isNullable() {
      return isNullable;
    }

    public void setNullable(boolean nullable) {
      isNullable = nullable;
    }

    public enum DBColumnType {
      VARCHAR,
      CHAR,
      INT,
      LONG,
      BOOL,
      TIME,
      BLOB
    }
  }

}
