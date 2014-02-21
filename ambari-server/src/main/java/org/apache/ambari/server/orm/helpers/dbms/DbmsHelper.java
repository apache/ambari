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

import org.apache.ambari.server.orm.DBAccessor;

import java.util.List;

public interface DbmsHelper {

  /**
   * Check if column type can be modified directly
   * @return
   */
  boolean supportsColumnTypeChange();

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

  String getCreateIndexStatement(String indexName, String tableName,
                                 String... columnNames);

  String getAddForeignKeyStatement(String tableName, String constraintName,
                                   List<String> keyColumns,
                                   String referenceTableName,
                                   List<String> referenceColumns);

  String getAddColumnStatement(String tableName, DBAccessor.DBColumnInfo columnInfo);

  String getRenameColumnStatement(String tableName, String oldColumnName,
                                  String newColumnName);

  String getDropTableStatement(String tableName);

  String getDropConstraintStatement(String tableName, String constraintName);

  String getDropSequenceStatement(String sequenceName);
}
