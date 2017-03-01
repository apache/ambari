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

import org.apache.ambari.view.hive20.exceptions.ServiceException
import spock.lang.Specification

public class RenameTableQueryGeneratorSpecTest extends Specification{

  def "rename simple query"(){
    setup:
    def  renameTable = new RenameTableQueryGenerator("d1", "tab1", "d2", "tab2")

    when:
    def query = renameTable.getQuery()

    then:
      query.isPresent()

    when:
    def hiveQuery = query.get();

    then:
    with(hiveQuery){
      hiveQuery == "ALTER TABLE `d1`.`tab1` RENAME TO `d2`.`tab2`"
    }
  }

  def "rename without old database query"(){
    setup:
    def  renameTable = new RenameTableQueryGenerator(null, "tab1", "d2", "tab2")

    when:
    def query = renameTable.getQuery()

    then:
      query.isPresent()

    when:
    def hiveQuery = query.get();

    then:
    with(hiveQuery){
      hiveQuery == "ALTER TABLE `tab1` RENAME TO `d2`.`tab2`"
    }
  }

  def "rename without old and new database query"(){
    setup:
    def  renameTable = new RenameTableQueryGenerator(null, "tab1", "", "tab2")

    when:
    def query = renameTable.getQuery()

    then:
      query.isPresent()

    when:
    def hiveQuery = query.get();

    then:
    with(hiveQuery){
      hiveQuery == "ALTER TABLE `tab1` RENAME TO `tab2`"
    }
  }

  def "rename with empty old tablename throws exception"(){
    setup:
    def  renameTable = new RenameTableQueryGenerator("d1", "", "d2", "tab2")

    when:
    def query = renameTable.getQuery()

    then:
      thrown(ServiceException)
  }

  def "rename with null new tablename throws exception"(){
    setup:
    def  renameTable = new RenameTableQueryGenerator("d1", "tab1", "d2", null)

    when:
    def query = renameTable.getQuery()

    then:
      thrown(ServiceException)
  }
}