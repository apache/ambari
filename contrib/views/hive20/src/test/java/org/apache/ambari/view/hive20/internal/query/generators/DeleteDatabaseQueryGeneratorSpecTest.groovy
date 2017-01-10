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
import spock.lang.Specification

class DeleteDatabaseQueryGeneratorSpecTest extends Specification {
  def "correct drop database"() {
    setup:
    DeleteDatabaseQueryGenerator generator = new DeleteDatabaseQueryGenerator("databaseName");

    when:
    Optional<String> databaseDeleteQuery = generator.getQuery()

    then:
    databaseDeleteQuery.isPresent()

    when:
    String query = databaseDeleteQuery.get();

    then:
    query == "drop database `databaseName`"
  }

  def "sending null as database name throws exception"() {
    setup:
    DeleteDatabaseQueryGenerator generator = new DeleteDatabaseQueryGenerator(null);

    when:
    Optional<String> databaseDeleteQuery = generator.getQuery()

    then:
    def exception = thrown(ServiceException)
    exception.message == "Database name cannot be null or empty."
  }

  def "sending enpty string as database name throws exception"() {
    setup:
    DeleteDatabaseQueryGenerator generator = new DeleteDatabaseQueryGenerator(null);

    when:
    Optional<String> databaseDeleteQuery = generator.getQuery()

    then:
    def exception = thrown(ServiceException)
    exception.message == "Database name cannot be null or empty."
  }
}
