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

package org.apache.ambari.server.orm.entities;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * LdapSyncSpecEntity tests.
 */
public class LdapSyncSpecEntityTest {
  @Test
  public void testGetPrincipalType() throws Exception {
    LdapSyncSpecEntity entity = new LdapSyncSpecEntity(LdapSyncSpecEntity.PrincipalType.Users,
        LdapSyncSpecEntity.SyncType.All, Collections.<String>emptyList());
    Assert.assertEquals(LdapSyncSpecEntity.PrincipalType.Users, entity.getPrincipalType());

    entity = new LdapSyncSpecEntity(LdapSyncSpecEntity.PrincipalType.Groups,
        LdapSyncSpecEntity.SyncType.All, Collections.<String>emptyList());
    Assert.assertEquals(LdapSyncSpecEntity.PrincipalType.Groups, entity.getPrincipalType());
  }

  @Test
  public void testGetSyncType() throws Exception {
    LdapSyncSpecEntity entity = new LdapSyncSpecEntity(LdapSyncSpecEntity.PrincipalType.Users,
        LdapSyncSpecEntity.SyncType.All, Collections.<String>emptyList());
    Assert.assertEquals(LdapSyncSpecEntity.SyncType.All, entity.getSyncType());

    entity = new LdapSyncSpecEntity(LdapSyncSpecEntity.PrincipalType.Users,
        LdapSyncSpecEntity.SyncType.Existing, Collections.<String>emptyList());
    Assert.assertEquals(LdapSyncSpecEntity.SyncType.Existing, entity.getSyncType());
  }

  @Test
  public void testGetPrincipalNames() throws Exception {
    List<String> names = new LinkedList<String>();
    names.add("joe");
    names.add("fred");

    LdapSyncSpecEntity entity = new LdapSyncSpecEntity(LdapSyncSpecEntity.PrincipalType.Users,
        LdapSyncSpecEntity.SyncType.Specific, names);
    Assert.assertEquals(names, entity.getPrincipalNames());
  }

  @Test
  public void testIllegalConstruction() throws Exception {
    try {
      new LdapSyncSpecEntity(LdapSyncSpecEntity.PrincipalType.Users,
          LdapSyncSpecEntity.SyncType.Specific, Collections.<String>emptyList());
      Assert.fail("expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // expected
    }

    List<String> names = new LinkedList<String>();
    names.add("joe");
    names.add("fred");

    try {
      new LdapSyncSpecEntity(LdapSyncSpecEntity.PrincipalType.Users,
          LdapSyncSpecEntity.SyncType.All, names);
      Assert.fail("expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // expected
    }

    try {
      new LdapSyncSpecEntity(LdapSyncSpecEntity.PrincipalType.Users,
          LdapSyncSpecEntity.SyncType.Existing, names);
      Assert.fail("expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }
}
