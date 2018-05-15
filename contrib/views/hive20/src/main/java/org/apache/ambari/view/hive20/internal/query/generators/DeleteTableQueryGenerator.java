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
import org.apache.parquet.Strings;

public class DeleteTableQueryGenerator implements QueryGenerator{
  private final String databaseName;
  private final String tableName;
  private Boolean purge = Boolean.FALSE;

  public DeleteTableQueryGenerator(String databaseName, String tableName) {
    this(databaseName, tableName, Boolean.FALSE);
  }

  public DeleteTableQueryGenerator(String databaseName, String tableName, Boolean purge) {
    this.databaseName = databaseName;
    this.tableName = tableName;
    if( null != purge ) this.purge = purge;
  }

  public String getDatabaseName() {
    return databaseName;
  }

  public String getTableName() {
    return tableName;
  }

  public Boolean getPurge() {
    return purge;
  }

  public void setPurge(Boolean purge) {
    this.purge = purge;
  }

  /**
   * @return
   * @throws ServiceException
   */
  @Override
  public Optional<String> getQuery() throws ServiceException {
    if(Strings.isNullOrEmpty(this.getDatabaseName()) || Strings.isNullOrEmpty(this.getTableName()))
      throw new ServiceException("databaseName or tableName was null.");

    return Optional.of("DROP TABLE `" + databaseName + "`.`" + tableName + "`" + (this.getPurge() ? " PURGE " : ""));
  }
}
