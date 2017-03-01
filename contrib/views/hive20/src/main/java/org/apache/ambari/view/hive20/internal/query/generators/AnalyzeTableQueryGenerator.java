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

package org.apache.ambari.view.hive20.internal.query.generators;

import com.google.common.base.Optional;
import org.apache.ambari.view.hive20.exceptions.ServiceException;

public class AnalyzeTableQueryGenerator implements QueryGenerator {
  private final String databaseName;
  private final String tableName;
  private final Boolean shouldAnalyzeColumns;

  public AnalyzeTableQueryGenerator(String databaseName, String tableName, Boolean shouldAnalyzeColumns) {
    this.databaseName = databaseName;
    this.tableName = tableName;
    this.shouldAnalyzeColumns = shouldAnalyzeColumns;
  }

  @Override
  public Optional<String> getQuery() throws ServiceException {
    return Optional.of("ANALYZE TABLE " + "`" + databaseName + "`.`" + tableName + "`" + " COMPUTE STATISTICS " +
      (shouldAnalyzeColumns? " FOR COLUMNS ": "") + ";");
  }
}
