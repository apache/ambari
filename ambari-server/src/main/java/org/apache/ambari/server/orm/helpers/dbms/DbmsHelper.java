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

package org.apache.ambari.server.orm.helpers.dbms;

import java.util.List;

import org.apache.ambari.server.orm.DBAccessor;

public interface DbmsHelper {

  /**
   * Check if column type can be modified directly
   * @return
   */
  boolean supportsColumnTypeChange();

  String quoteObjectName(String name);

  /**
   * Generate rename column statement
   * @param tableName
   * @param oldName
   * @param columnInfo definition of new column
   * @return
   */
  String getRenameColumnStatement(String tableName, String oldName, DBAccessor.DBColumnInfo columnInfo);

  /**
   * Generate alter column statement
   * @param tableName
   * @param columnInfo
   * @return
   */
  String getAlterColumnStatement(String tableName, DBAccessor.DBColumnInfo columnInfo);

  String getCreateTableStatement(String tableName,
                                 List<DBAccessor.DBColumnInfo> columns,
                                 List<String> primaryKeyColumns);

  String getTableConstraintsStatement(String databaseName, String tablename);

  String getCreateIndexStatement(String indexName, String tableName,
                                 String... columnNames);

  String getCreateIndexStatement(String indexName, String tableName, boolean isUnique,
                                 String... columnNames);

  /**
   * Gets DROP INDEX statement
   *
   * @param indexName
   * @param tableName
   * @param columnNames
   * @return
   */
  String getDropIndexStatement(String indexName, String tableName);

  /**
   * Generate alter table statement to add unique constraint
   * @param tableName name of the table
   * @param constraintName name of the constraint
   * @param columnNames name of the column
   * @return alter table statement
   */
  String getAddUniqueConstraintStatement(String tableName, String constraintName, String... columnNames);

  /**
   * Generate alter table statement to add primary key index
   * @param tableName name of the table
   * @param constraintName name of the primary key
   * @param columnName name of the column
   * @return alter table statement
   */
  String getAddPrimaryKeyConstraintStatement(String tableName, String constraintName, String... columnName);


  String getAddForeignKeyStatement(String tableName, String constraintName,
                                   List<String> keyColumns,
                                   String referenceTableName,
                                   List<String> referenceColumns,
                                   boolean shouldCascadeOnDelete);

  String getAddColumnStatement(String tableName, DBAccessor.DBColumnInfo columnInfo);

  String getDropTableColumnStatement(String tableName, String columnName);

  String getRenameColumnStatement(String tableName, String oldColumnName,
                                  String newColumnName);

  String getDropTableStatement(String tableName);

  String getDropFKConstraintStatement(String tableName, String constraintName);

  String getDropUniqueConstraintStatement(String tableName, String constraintName);

  String getDropSequenceStatement(String sequenceName);

  String getDropPrimaryKeyStatement(String tableName, String constraintName, boolean cascade);

  /**
   * Gets the {@code SET NULL} or {@code SET NOT NULL} statement.
   *
   * @param tableName
   *          the table (not {@code null}).
   * @param columnInfo
   *          the column object to get name and type of column (not {@code null}).
   * @param nullable
   *          {@code true} to indicate that the column allows {@code NULL}
   *          values, {@code false} otherwise.
   * @return the statement (never {@code null}).
   */
  String getSetNullableStatement(String tableName, DBAccessor.DBColumnInfo columnInfo, boolean nullable);

  /**
   * Gets whether the database platform supports adding contraints after the
   * {@code NULL} constraint. Some database, such as Oracle, don't allow this.
   * Unfortunately, EclipsLink hard codes the order of constraints.
   *
   * @return {@code true} if contraints can be added after the {@code NULL}
   *         constraint, {@code false} otherwise.
   */
  boolean isConstraintSupportedAfterNullability();
}
