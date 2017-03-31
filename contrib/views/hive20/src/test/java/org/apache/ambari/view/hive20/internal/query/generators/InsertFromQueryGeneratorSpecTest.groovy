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
package org.apache.ambari.view.hive20.internal.query.generators

import com.google.common.base.Optional
import org.apache.ambari.view.hive20.exceptions.ServiceException
import org.apache.ambari.view.hive20.internal.dto.ColumnInfo
import org.apache.ambari.view.hive20.resources.uploads.query.InsertFromQueryInput
import spock.lang.Specification

class InsertFromQueryGeneratorSpecTest extends Specification {
  def "insert from without unhexing"() {
    setup:
    List<ColumnInfo> colInfos = Arrays.asList(new ColumnInfo("col1", "STRING"), new ColumnInfo("col2", "INT"), new ColumnInfo("col3", "VARCHAR", 255),
            new ColumnInfo("col4", "CHAR", 25))
    InsertFromQueryInput insertFromQueryInput = new InsertFromQueryInput("d1", "t1", "d2", "t2", colInfos, false)
    InsertFromQueryGenerator generator = new InsertFromQueryGenerator(insertFromQueryInput);

    when:
    Optional<String> query = generator.getQuery()

    then:
    query.isPresent()

    when:
    String queryStr = query.get();

    then:
    queryStr == "INSERT INTO TABLE `d2`.`t2` SELECT `col1`, `col2`, `col3`, `col4` FROM `d1.t1` ;"
  }

  def "insert from with unhexing"() {
    setup:
    List<ColumnInfo> colInfos = Arrays.asList(new ColumnInfo("col1", "STRING"), new ColumnInfo("col2", "INT"), new ColumnInfo("col3", "VARCHAR", 255),
            new ColumnInfo("col4", "CHAR", 25))
    InsertFromQueryInput insertFromQueryInput = new InsertFromQueryInput("d1", "t1", "d2", "t2", colInfos, true)
    InsertFromQueryGenerator generator = new InsertFromQueryGenerator(insertFromQueryInput);

    when:
    Optional<String> query = generator.getQuery()

    then:
    query.isPresent()

    when:
    String queryStr = query.get();

    then:
    queryStr == "INSERT INTO TABLE `d2`.`t2` SELECT UNHEX(`col1`), `col2`, UNHEX(`col3`), UNHEX(`col4`) FROM `d1.t1` ;"
  }
}
