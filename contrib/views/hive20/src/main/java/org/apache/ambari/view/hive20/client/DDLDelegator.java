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

package org.apache.ambari.view.hive20.client;

import java.util.List;

import org.apache.ambari.view.hive20.exceptions.ServiceException;
import org.apache.ambari.view.hive20.internal.dto.DatabaseInfo;
import org.apache.ambari.view.hive20.internal.dto.TableInfo;

public interface DDLDelegator {

  List<DatabaseInfo> getDbList(ConnectionConfig config, String like);

  List<TableInfo> getTableList(ConnectionConfig config, String database, String like);

  List<Row> getTableDescriptionFormatted(ConnectionConfig config, String database, String table);

  List<Row> getTableCreateStatement(ConnectionConfig config, String database, String table);

  List<ColumnDescription> getTableDescription(ConnectionConfig config, String database, String table, String like, boolean extended);

  Cursor<Row, ColumnDescription> getDbListCursor(ConnectionConfig config, String like);

  Cursor<Row, ColumnDescription> getTableListCursor(ConnectionConfig config, String database, String like);

  Cursor<Row, ColumnDescription> getTableDescriptionCursor(ConnectionConfig config, String database, String table, String like, boolean extended);

  DatabaseMetadataWrapper getDatabaseMetadata(ConnectionConfig config) throws ServiceException;
}
