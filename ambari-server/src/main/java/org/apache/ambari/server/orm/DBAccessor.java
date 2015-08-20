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
import java.sql.SQLException;
import java.util.List;

import org.apache.ambari.server.configuration.Configuration.DatabaseType;
import org.eclipse.persistence.internal.databaseaccess.FieldTypeDefinition;
import org.eclipse.persistence.sessions.DatabaseSession;

/**
 * Interface for schema manipulation
 * Note: IF NOT EXISTS is default for all supported DDL statements
 */
public interface DBAccessor {

  /**
   * @return database connection
   */
  Connection getConnection();

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
  void createTable(String tableName, List<DBColumnInfo> columnInfo,
                          String... primaryKeyColumns) throws SQLException;

  /**
   * Create new index
   * @param indexName
   * @param tableName
   * @param columnNames
   * @throws SQLException
   */
  void createIndex(String indexName, String tableName,
                          String... columnNames) throws SQLException;

  /**
   * Add foreign key for a relation
   * @param tableName
   * @param constraintName
   * @param keyColumn
   * @param referenceColumn
   * @throws SQLException
   */
  void addFKConstraint(String tableName,
                              String constraintName,
                              String keyColumn,
                              String referenceTableName,
                              String referenceColumn,
                              boolean ignoreFailure) throws SQLException;

  /**
   *
   * @param tableName
   * @param constraintName
   * @param keyColumn
   * @param referenceTableName
   * @param referenceColumn
   * @param shouldCascadeOnDelete
   * @param ignoreFailure
   * @throws SQLException
   */
  void addFKConstraint(String tableName,
                              String constraintName,
                              String keyColumn,
                              String referenceTableName,
                              String referenceColumn,
                              boolean shouldCascadeOnDelete,
                              boolean ignoreFailure) throws SQLException;

  /**
   * Add foreign key for a relation
   * @param tableName
   * @param constraintName
   * @param keyColumns
   * @param referenceTableName
   * @param referenceColumns
   * @param shouldCascadeOnDelete
   * @param ignoreFailure
   * @throws SQLException
   */
  void addFKConstraint(String tableName,
                              String constraintName,
                              String[] keyColumns,
                              String referenceTableName,
                              String[] referenceColumns,
                              boolean shouldCascadeOnDelete,
                              boolean ignoreFailure) throws SQLException;

  /**
   * Add foreign key for a relation
   * @param tableName
   * @param constraintName
   * @param keyColumns
   * @param referenceTableName
   * @param referenceColumns
   * @param ignoreFailure
   * @throws SQLException
   */
  void addFKConstraint(String tableName,
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
  void addColumn(String tableName,
                        DBColumnInfo columnInfo) throws SQLException;

  /**
   * Add unique table constraint
   * @param constraintName name of the constraint
   * @param tableName name of the table
   * @param columnNames list of columns
   * @throws SQLException
   */
  void addUniqueConstraint(String tableName, String constraintName, String... columnNames)
    throws SQLException;

  /**
   *
   * @param tableName name of the table
   * @param constraintName name of the constraint
   * @param columnName name of the column
   * @param ignoreErrors true to ignore database errors
   * @throws SQLException
   */
  void addPKConstraint(String tableName, String constraintName,boolean ignoreErrors, String... columnName) throws SQLException;

  /**
   *
   * @param tableName name of the table
   * @param constraintName name of the constraint
   * @param columnName name of the column
   * @throws SQLException
   */
  void addPKConstraint(String tableName, String constraintName, String... columnName) throws SQLException;

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
  void alterColumn(String tableName,
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
  int updateTable(String tableName, String columnName, Object value,
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
  void updateTable(String tableName, DBColumnInfo columnNameSrc,
                         DBColumnInfo columnNameTgt) throws SQLException;

  /**
   * Helper method to run third party scripts like Quartz DDL
   * @param filePath
   * @throws SQLException
   */
  void executeScript(String filePath) throws SQLException, IOException;

  /**
   *
   * @param query update query
   * @return same like {@code java.sql.Statement}
   * @throws SQLException
   */
  int executeUpdate(String query) throws SQLException;

  /**
   *
   * @param query update query
   * @param ignoreErrors true to ignore errors
   * @return same like {@code java.sql.Statement}
   * @throws SQLException
   */
  int executeUpdate(String query, boolean ignoreErrors) throws SQLException;

  /**
   * Conditional ad-hoc query on DB
   * @param query
   * @param tableName
   * @param hasColumnName
   * @throws SQLException
   */
  void executeQuery(String query, String tableName, String hasColumnName) throws SQLException;

  /**
   * Execute ad-hoc query on DB.
   * @param query
   * @throws SQLException
   */
  void executeQuery(String query) throws SQLException;

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
  void dropTable(String tableName) throws SQLException;

  /**
   * Delete all table data
   * @param tableName
   * @throws SQLException
   */
  void truncateTable(String tableName) throws SQLException;

  /**
   * Drop a column from table
   * @param tableName
   * @param columnName
   * @throws SQLException
   */
  void dropColumn(String tableName, String columnName) throws SQLException;

  /**
   * Drop sequence
   * @param sequenceName
   * @throws SQLException
   */
  void dropSequence(String sequenceName) throws SQLException;

  /**
   * Drop a FK constraint from table
   * @param tableName name of the table
   * @param constraintName name of the constraint
   * @throws SQLException
   */
  void dropFKConstraint(String tableName, String constraintName) throws SQLException;

  /**
   * Drop a PK constraint from table
   * @param tableName
   * @param constraintName name of the constraint
   * @param ignoreFailure
   * @param cascade cascade delete
   * @throws SQLException
   */
  void dropPKConstraint(String tableName, String constraintName, boolean ignoreFailure, boolean cascade) throws SQLException;

  /**
   * Drop a PK constraint from table
   * @param tableName name of the table
   * @param constraintName name of the constraint
   * @param cascade cascade delete
   * @throws SQLException
   */
  void dropPKConstraint(String tableName, String constraintName, boolean cascade) throws SQLException;

  /**
   * Drop a PK constraint from table
   * @param tableName name of the table
   * @param constraintName name of the constraint
   * @param columnName name of the column from the pk constraint
   * @param cascade cascade delete
   * @throws SQLException
   */
  void dropPKConstraint(String tableName, String constraintName, String columnName, boolean cascade) throws SQLException;

  /**
   * Drop a FK constraint from table
   * @param tableName name of the table
   * @param constraintName name of the constraint
   * @throws SQLException
   */
  void dropFKConstraint(String tableName, String constraintName, boolean ignoreFailure) throws SQLException;

  /**
   * Drop a unique constraint from table
   * @param tableName name of the table
   * @param constraintName name of the constraint
   * @param ignoreFailure
   * @throws SQLException
   */
  void dropUniqueConstraint(String tableName, String constraintName, boolean ignoreFailure) throws SQLException;

  /**
   * Drop a unique constraint from table
   * @param tableName name of the table
   * @param constraintName name of the constraint
   * @throws SQLException
   */
  void dropUniqueConstraint(String tableName, String constraintName) throws SQLException;

  /**
   * Verify if table exists by looking at metadata.
   * @param tableName name of the table
   * @return
   * @throws SQLException
   */
  boolean tableExists(String tableName) throws SQLException;

  /**
   * Verify if table has any data
   * @param tableName
   * @return
   * @throws SQLException
   */
  boolean tableHasData(String tableName) throws SQLException;

  /**
   * Verify if table already has a column defined.
   * @param tableName
   * @param columnName
   * @return
   * @throws SQLException
   */
  boolean tableHasColumn(String tableName, String columnName) throws SQLException;

  /**
   * Verify if table already has a column defined.
   * @param tableName name of the table
   * @param columnName name of the column to check
   * @return false if one from passed column names not exists
   * @throws SQLException
   */
  boolean tableHasColumn(String tableName, String... columnName) throws SQLException;

  /**
   * Verify if table has a FK constraint.
   * @param tableName
   * @param fkName
   * @return true if FK with such name exists
   * @throws SQLException
   */
  boolean tableHasForeignKey(String tableName, String fkName) throws SQLException;

  /**
   * Verify if table already has a FK constraint.
   * @param tableName
   * @param refTableName
   * @param columnName
   * @param refColumnName
   * @return true if described relation exists
   * @throws SQLException
   */
  boolean tableHasForeignKey(String tableName, String refTableName,
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
  boolean tableHasForeignKey(String tableName, String referenceTableName, String[] keyColumns,
                             String[] referenceColumns) throws SQLException;

  /**
   * Get a new DB session
   * @return
   */
  DatabaseSession getNewDatabaseSession();


  /**
   * Table has primary key
   * @param tableName name of the table
   * @param columnName name of the constraint, could be {@code null}
   * @return true if constraint exists
   * @throws SQLException
   */
  boolean tableHasPrimaryKey(String tableName, String columnName) throws SQLException;

  /**
   * Gets list of index names from database metadata
   * @param tableName
   *            the name of the table (not {@code null}).
   * @param unique
   *            list only unique indexes (not {@code null}).
   * @return the string list of index names
   * @throws SQLException
   */
  List<String> getIndexesList(String tableName, boolean unique) throws SQLException;

  /**
   * Check if index is already in scheme
   * @param tableName
   *            the name of the table (not {@code null}).
   * @param unique
   *            list only unique indexes (not {@code null}).
   * @param indexName
   *            name of the index to check
   * @return true if index present in the schema
   */
  boolean tableHasIndex(String tableName, boolean unique, String indexName) throws SQLException;

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
  int getColumnType(String tableName, String columnName)
      throws SQLException;

  /**
   * Get type class of the column
   * @param tableName name of the table
   * @param columnName name of the column
   * @return type class of the column
   * @throws SQLException
   * @throws ClassNotFoundException
   */
  Class getColumnClass(String tableName, String columnName) throws SQLException, ClassNotFoundException;

  /**
   * Check if column could be nullable
   * @param tableName name of the table
   * @param columnName name of the column
   * @return true if column could be nullable
   * @throws SQLException
   */
  boolean isColumnNullable(String tableName, String columnName) throws SQLException;

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
  void setColumnNullable(String tableName, DBAccessor.DBColumnInfo columnInfo, boolean nullable)
      throws SQLException;

  void setColumnNullable(String tableName, String columnName, boolean nullable)
    throws SQLException;

  /**
   * Alter column wrapper, which handle DB specific type conversion
   * @param tableName name of the table
   * @param columnName name of the column
   * @param fromType previous type
   * @param toType new desired type
   * @throws SQLException
   */
  void changeColumnType(String tableName, String columnName, Class fromType, Class toType) throws SQLException;

  /**
   * Queries the database to determine the name of the primary key constraint on
   * the specified table. Currently, this is only implemented for
   * {@link DatabaseType#ORACLE} and {@link DatabaseType#SQL_SERVER}.
   *
   * @param tableName
   *          the name of the table to lookup the PK constraint.
   * @return the name of the PK, or {@code null} if none.
   * @throws SQLException
   */
  String getPrimaryKeyConstraintName(String tableName) throws SQLException;

  enum DbType {
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
  DbType getDbType();

  /**
   * Capture column type
   */
  class DBColumnInfo {
    private String name;
    private Class type;
    private Integer length;
    private Object defaultValue;
    private boolean isNullable;

    private FieldTypeDefinition dbType = null;

    public DBColumnInfo(String name, Class type) {
      this(name, type, null, null, true);
    }

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

    public DBColumnInfo(String name, FieldTypeDefinition dbType, Integer length, Object defaultValue, boolean isNullable) {
      this.name = name;
      this.length = length;
      this.isNullable = isNullable;
      this.defaultValue = defaultValue;
      this.dbType = dbType;
    }

    public DBColumnInfo(String name, FieldTypeDefinition dbType, Integer length) {
      this(name, dbType, length, null, true);
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

    public FieldTypeDefinition getDbType() {
      return dbType;
    }

    public void setDbType(FieldTypeDefinition dbType) {
      this.dbType = dbType;
    }
  }

}
