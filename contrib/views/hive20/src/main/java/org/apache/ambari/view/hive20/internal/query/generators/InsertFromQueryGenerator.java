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
import org.apache.ambari.view.hive20.client.ColumnDescription;
import org.apache.ambari.view.hive20.exceptions.ServiceException;
import org.apache.ambari.view.hive20.internal.dto.ColumnInfo;
import org.apache.ambari.view.hive20.resources.uploads.query.InsertFromQueryInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InsertFromQueryGenerator implements QueryGenerator{
  protected final static Logger LOG =
      LoggerFactory.getLogger(InsertFromQueryGenerator.class);

  private InsertFromQueryInput insertFromQueryInput;

  public InsertFromQueryGenerator(InsertFromQueryInput insertFromQueryInput) {
    this.insertFromQueryInput = insertFromQueryInput;
  }

  @Override
  public Optional<String> getQuery() throws ServiceException {
    StringBuilder insertQuery = new StringBuilder("INSERT INTO TABLE `").append(insertFromQueryInput.getToDatabase()).append("`.`")
        .append(insertFromQueryInput.getToTable()).append("`")
        .append(" SELECT ");

    boolean first = true;
    for(ColumnInfo column : insertFromQueryInput.getHeader()){
      String type = column.getType();
      boolean unhex = insertFromQueryInput.getUnhexInsert() && (
          ColumnDescription.DataTypes.STRING.toString().equals(type)
              || ColumnDescription.DataTypes.VARCHAR.toString().equals(type)
              || ColumnDescription.DataTypes.CHAR.toString().equals(type)
      );

      if(!first){
        insertQuery.append(", ");
      }

      if(unhex) {
        insertQuery.append("UNHEX(");
      }

      insertQuery.append(column.getName());

      if(unhex) {
        insertQuery.append(")");
      }

      first = false;
    }

    insertQuery.append(" FROM ").append("`").append(insertFromQueryInput.getFromDatabase()).append(".")
        .append(insertFromQueryInput.getFromTable()).append("` ").append(";");
    String query = insertQuery.toString();
    LOG.info("Insert From Query : {}", query);
    return Optional.of(query);
  }
}
