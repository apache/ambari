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
package org.apache.ambari.datastore;

import org.apache.ambari.common.rest.entities.Stack;
import org.apache.ambari.datastore.DataStore;
import org.apache.ambari.datastore.StaticDataStore;
import org.testng.annotations.Test;
import static org.testng.AssertJUnit.assertEquals;

public class TestStaticDataStore {

  @Test
  public void testGetStack() throws Exception {
    DataStore ds = new StaticDataStore();
    Stack stack = ds.retrieveStack("hadoop-security", -1);
    assertEquals("can fetch revision -1", "0", stack.getRevision());
  }
}
