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

package org.apache.ambari.view.hive.client;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.HashMap;

import static org.junit.Assert.*;

public class ConnectionTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testOpenConnection() throws Exception {
    HashMap<String, String> auth = new HashMap<String, String>();
    auth.put("auth", "NONE");

    thrown.expect(HiveClientException.class);
    thrown.expectMessage("Connection refused");
    new Connection("127.0.0.1", 42420, auth, "ambari-qa", null);
  }

  @Test
  public void testAskPasswordWithoutPassword() throws Exception {
    HashMap<String, String> auth = new HashMap<String, String>();
    auth.put("auth", "NONE");
    auth.put("password", "${ask_password}");

    thrown.expect(HiveAuthRequiredException.class);
    new Connection("127.0.0.1", 42420, auth, "ambari-qa", null);
  }

  @Test
  public void testAskPasswordWithPassword() throws Exception {
    HashMap<String, String> auth = new HashMap<String, String>();
    auth.put("auth", "NONE");
    auth.put("password", "${ask_password}");

    thrown.expect(HiveClientException.class);
    thrown.expectMessage("Connection refused");
    new Connection("127.0.0.1", 42420, auth, "ambari-qa", "password");
  }
}
