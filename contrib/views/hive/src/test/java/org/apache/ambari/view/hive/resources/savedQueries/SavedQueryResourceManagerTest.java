/**
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

package org.apache.ambari.view.hive.resources.savedQueries;

import org.junit.Assert;
import org.junit.Test;

public class SavedQueryResourceManagerTest {

  @Test
  public void testMakeShortQuery() throws Exception {
    String query = "select * from table;";
    String shortQuery = SavedQueryResourceManager.makeShortQuery(query);
    Assert.assertEquals(query, shortQuery);
  }

  @Test
  public void testMakeShortQuery42Trim() throws Exception {
    String str50 = "12345678901234567890123456789012345678901234567890";
    String str42 = "123456789012345678901234567890123456789012";
    String shortQuery = SavedQueryResourceManager.makeShortQuery(str50);
    Assert.assertEquals(str42, shortQuery);
  }

  @Test
  public void testMakeShortQueryRemoveSet() throws Exception {
    String str50 = "set hive.execution.engine=tez;\nselect * from table;";
    String shortQuery = SavedQueryResourceManager.makeShortQuery(str50);
    Assert.assertEquals("select * from table;", shortQuery);

    str50 = "set hive.execution.engine = tez;  \n select * from table;";
    shortQuery = SavedQueryResourceManager.makeShortQuery(str50);
    Assert.assertEquals("select * from table;", shortQuery);

    str50 = "SET  property=value;\nselect * from table;";
    shortQuery = SavedQueryResourceManager.makeShortQuery(str50);
    Assert.assertEquals("select * from table;", shortQuery);
  }
}