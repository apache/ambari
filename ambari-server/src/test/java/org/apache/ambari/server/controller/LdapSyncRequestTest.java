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

package org.apache.ambari.server.controller;

import org.apache.ambari.server.orm.entities.LdapSyncSpecEntity;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * LdapSyncRequest tests.
 */
public class LdapSyncRequestTest {
  @Test
  public void testAddPrincipalNames() throws Exception {
    Set<String> names = new HashSet<String>();
    names.add("name1");

    LdapSyncRequest request = new LdapSyncRequest(LdapSyncSpecEntity.SyncType.SPECIFIC,names);

    names = new HashSet<String>();
    names.add("name2");
    names.add("name3");

    request.addPrincipalNames(names);

    Set<String> principalNames = request.getPrincipalNames();
    Assert.assertEquals(3, principalNames.size());
    Assert.assertTrue(principalNames.contains("name1"));
    Assert.assertTrue(principalNames.contains("name2"));
    Assert.assertTrue(principalNames.contains("name3"));
  }

  @Test
  public void testGetPrincipalNames() throws Exception {
    Set<String> names = new HashSet<String>();
    names.add("name1");
    names.add("name2");
    names.add("name3");

    LdapSyncRequest request = new LdapSyncRequest(LdapSyncSpecEntity.SyncType.SPECIFIC,names);

    Set<String> principalNames = request.getPrincipalNames();
    Assert.assertEquals(3, principalNames.size());
    Assert.assertTrue(principalNames.contains("name1"));
    Assert.assertTrue(principalNames.contains("name2"));
    Assert.assertTrue(principalNames.contains("name3"));
  }

  @Test
  public void testGetType() throws Exception {
    Set<String> names = new HashSet<String>();

    LdapSyncRequest request = new LdapSyncRequest(LdapSyncSpecEntity.SyncType.SPECIFIC, names);

    Assert.assertEquals(LdapSyncSpecEntity.SyncType.SPECIFIC, request.getType());

    request = new LdapSyncRequest(LdapSyncSpecEntity.SyncType.ALL);

    Assert.assertEquals(LdapSyncSpecEntity.SyncType.ALL, request.getType());

    request = new LdapSyncRequest(LdapSyncSpecEntity.SyncType.EXISTING);

    Assert.assertEquals(LdapSyncSpecEntity.SyncType.EXISTING, request.getType());
  }
}
