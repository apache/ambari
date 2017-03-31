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

import com.google.gson.Gson;
import org.apache.ambari.view.hive20.internal.dto.TableMeta;
import spock.lang.Specification;
import com.google.common.base.Optional;

class AnalyzeTableQueryGeneratorSpecTest extends Specification {
  def "analyze with partition and for columns"() {
    setup:
    String tableMetaJson = "{" +
            "\"database\": \"d1\"," +
            "\"table\": \"t2\"," +
            "\"columns\": [{" +
            "\"name\": \"col_name1\"," +
            "\"type\": \"string\"," +
            "\"comment\": \"col_name1 comment\"" +
            "}, {" +
            "\"name\": \"col_name2\"," +
            "\"type\": \"decimal(10,2)\"," +
            "\"comment\": \"col_name2 comment\"" +
            "}]," +
            "\"partitionInfo\": {" +
            "\"columns\": [{" +
            "\"name\": \"col_name4\"," +
            "\"type\": \"char(1)\"," +
            "\"comment\": \"col_name4 comment\"" +
            "}, {" +
            "\"name\": \"col_name3\"," +
            "\"type\": \"string\"," +
            "\"comment\": \"col_name3 comment\"" +
            "}]" +
            "}" +
            "}";

    TableMeta tableMeta = new Gson().fromJson(tableMetaJson, TableMeta.class);
    AnalyzeTableQueryGenerator generator = new AnalyzeTableQueryGenerator(tableMeta, true);

    when:
    Optional<String> databaseDeleteQuery = generator.getQuery()

    then:
    databaseDeleteQuery.isPresent()

    when:
    String query = databaseDeleteQuery.get();

    then:
    query == "ANALYZE TABLE `d1`.`t2` PARTITION (`col_name4`,`col_name3`) COMPUTE STATISTICS ;\n" +
            "ANALYZE TABLE `d1`.`t2` PARTITION (`col_name4`,`col_name3`) COMPUTE STATISTICS  FOR COLUMNS ;"
  }
  def "analyze with partition"() {
    setup:
    String tableMetaJson = "{" +
            "\"database\": \"d1\"," +
            "\"table\": \"t2\"," +
            "\"columns\": [{" +
            "\"name\": \"col_name1\"," +
            "\"type\": \"string\"," +
            "\"comment\": \"col_name1 comment\"" +
            "}, {" +
            "\"name\": \"col_name2\"," +
            "\"type\": \"decimal(10,2)\"," +
            "\"comment\": \"col_name2 comment\"" +
            "}]," +
            "\"partitionInfo\": {" +
            "\"columns\": [{" +
            "\"name\": \"col_name4\"," +
            "\"type\": \"char(1)\"," +
            "\"comment\": \"col_name4 comment\"" +
            "}, {" +
            "\"name\": \"col_name3\"," +
            "\"type\": \"string\"," +
            "\"comment\": \"col_name3 comment\"" +
            "}]" +
            "}" +
            "}";

    TableMeta tableMeta = new Gson().fromJson(tableMetaJson, TableMeta.class);
    AnalyzeTableQueryGenerator generator = new AnalyzeTableQueryGenerator(tableMeta, false);

    when:
    Optional<String> databaseDeleteQuery = generator.getQuery()

    then:
    databaseDeleteQuery.isPresent()

    when:
    String query = databaseDeleteQuery.get();

    then:
    query == "ANALYZE TABLE `d1`.`t2` PARTITION (`col_name4`,`col_name3`) COMPUTE STATISTICS ;"
  }

  def "analyze without partition"() {
    setup:
    String tableMetaJson = "{" +
            "\"database\": \"d1\"," +
            "\"table\": \"t2\"," +
            "\"columns\": [{" +
            "\"name\": \"col_name1\"," +
            "\"type\": \"string\"," +
            "\"comment\": \"col_name1 comment\"" +
            "}, {" +
            "\"name\": \"col_name2\"," +
            "\"type\": \"decimal(10,2)\"," +
            "\"comment\": \"col_name2 comment\"" +
            "}," +
            "{" +
            "\"name\": \"col_name4\"," +
            "\"type\": \"char(1)\"," +
            "\"comment\": \"col_name4 comment\"" +
            "}, {" +
            "\"name\": \"col_name3\"," +
            "\"type\": \"string\"," +
            "\"comment\": \"col_name3 comment\"" +
            "}" +
            "]" +
            "}";

    TableMeta tableMeta = new Gson().fromJson(tableMetaJson, TableMeta.class);
    AnalyzeTableQueryGenerator generator = new AnalyzeTableQueryGenerator(tableMeta, true);

    when:
    Optional<String> databaseDeleteQuery = generator.getQuery()

    then:
    databaseDeleteQuery.isPresent()

    when:
    String query = databaseDeleteQuery.get();

    then:
    query == "ANALYZE TABLE `d1`.`t2` COMPUTE STATISTICS ;\n" +
            "ANALYZE TABLE `d1`.`t2` COMPUTE STATISTICS  FOR COLUMNS ;"
  }

  def "analyze for table only"() {
    setup:
    String tableMetaJson = "{" +
            "\"database\": \"d1\"," +
            "\"table\": \"t2\"," +
            "\"columns\": [{" +
            "\"name\": \"col_name1\"," +
            "\"type\": \"string\"," +
            "\"comment\": \"col_name1 comment\"" +
            "}, {" +
            "\"name\": \"col_name2\"," +
            "\"type\": \"decimal(10,2)\"," +
            "\"comment\": \"col_name2 comment\"" +
            "}," +
            "{" +
            "\"name\": \"col_name4\"," +
            "\"type\": \"char(1)\"," +
            "\"comment\": \"col_name4 comment\"" +
            "}, {" +
            "\"name\": \"col_name3\"," +
            "\"type\": \"string\"," +
            "\"comment\": \"col_name3 comment\"" +
            "}" +
            "]" +
            "}";

    TableMeta tableMeta = new Gson().fromJson(tableMetaJson, TableMeta.class);
    AnalyzeTableQueryGenerator generator = new AnalyzeTableQueryGenerator(tableMeta, false);

    when:
    Optional<String> databaseDeleteQuery = generator.getQuery()

    then:
    databaseDeleteQuery.isPresent()

    when:
    String query = databaseDeleteQuery.get();

    then:
    query == "ANALYZE TABLE `d1`.`t2` COMPUTE STATISTICS ;"
  }
}
