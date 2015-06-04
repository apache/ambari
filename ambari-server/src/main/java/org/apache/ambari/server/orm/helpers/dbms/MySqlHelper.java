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
import org.eclipse.persistence.platform.database.DatabasePlatform;

public class MySqlHelper extends GenericDbmsHelper {
  public MySqlHelper(DatabasePlatform databasePlatform) {
    super(databasePlatform);
  }

  @Override
  public boolean supportsColumnTypeChange() {
    return true;
  }

  @Override
  public String quoteObjectName(String name) {
    return "`" + name + "`";
  }

  @Override
  public StringBuilder writeColumnRenameString(StringBuilder builder, String oldName, DBAccessor.DBColumnInfo newColumnInfo) {

    builder.append(" CHANGE ").append(oldName).append(" ").append(newColumnInfo.getName()).append(" ");
    writeColumnType(builder, newColumnInfo);

    return builder;
  }

  @Override
  public StringBuilder writeColumnModifyString(StringBuilder builder, DBAccessor.DBColumnInfo columnInfo) {
    builder.append(" MODIFY ").append(columnInfo.getName()).append(" ");
    writeColumnType(builder, columnInfo);
    return builder;
  }

  @Override
  public StringBuilder writeSetNullableString(StringBuilder builder,
      String tableName, DBAccessor.DBColumnInfo columnInfo, boolean nullable) {
    builder.append(" MODIFY ").append(columnInfo.getName()).append(" ");
    writeColumnType(builder, columnInfo);
    String nullStatement = nullable ? " NULL" : " NOT NULL";
    builder.append(nullStatement);
    return builder;
  }

  @Override
  public String writeGetTableConstraints(String databaseName, String tableName) {
    // https://dev.mysql.com/doc/refman/5.7/en/table-constraints-table.html
    StringBuilder statement = new StringBuilder()
                                .append("SELECT ")
                                .append("constraints.CONSTRAINT_NAME as CONSTRAINT_NAME,")
                                    .append("constraints.CONSTRAINT_TYPE as CONSTRAINT_TYPE ")
                                .append("FROM information_schema.TABLE_CONSTRAINTS as constraints ")
                                .append("WHERE ")
                                    .append("constraints.TABLE_SCHEMA = \"").append(databaseName).append("\" ")
                                    .append("AND constraints.TABLE_NAME = \"").append(tableName).append("\"");
    return statement.toString();
  }
}
