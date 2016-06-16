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

package org.apache.ambari.view.hive2.actor.message;

public class GetColumnMetadataJob extends HiveJob {
  private final String schemaPattern;
  private final String tablePattern;
  private final String columnPattern;

  public GetColumnMetadataJob(String username,
                              String schemaPattern, String tablePattern, String columnPattern) {
    super(Type.SYNC, username);
    this.schemaPattern = schemaPattern;
    this.tablePattern = tablePattern;
    this.columnPattern = columnPattern;
  }

  public GetColumnMetadataJob(String username,
                              String tablePattern, String columnPattern) {
    this(username, "*", tablePattern, columnPattern);
  }

  public GetColumnMetadataJob(String username,
                              String columnPattern) {
    this(username, "*", "*", columnPattern);
  }

  public GetColumnMetadataJob(String username) {
    this(username, "*", "*", "*");
  }

  public String getSchemaPattern() {
    return schemaPattern;
  }

  public String getTablePattern() {
    return tablePattern;
  }

  public String getColumnPattern() {
    return columnPattern;
  }
}
