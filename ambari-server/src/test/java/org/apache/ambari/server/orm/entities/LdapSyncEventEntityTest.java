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
import java.util.List;

/**
 * LdapSyncEventEntity tests.
 */
public class LdapSyncEventEntityTest {
  @Test
  public void testGetId() throws Exception {
    LdapSyncEventEntity event = new LdapSyncEventEntity(1L);
    Assert.assertEquals(1L, event.getId());
  }

  @Test
  public void testSetGetStatus() throws Exception {
    LdapSyncEventEntity event = new LdapSyncEventEntity(1L);
    Assert.assertEquals(LdapSyncEventEntity.Status.Pending, event.getStatus());

    event.setStatus(LdapSyncEventEntity.Status.Running);
    Assert.assertEquals(LdapSyncEventEntity.Status.Running, event.getStatus());

    event.setStatus(LdapSyncEventEntity.Status.Complete);
    Assert.assertEquals(LdapSyncEventEntity.Status.Complete, event.getStatus());

    event.setStatus(LdapSyncEventEntity.Status.Error);
    Assert.assertEquals(LdapSyncEventEntity.Status.Error, event.getStatus());

    event.setStatus(LdapSyncEventEntity.Status.Pending);
    Assert.assertEquals(LdapSyncEventEntity.Status.Pending, event.getStatus());
  }

  @Test
  public void testSetGetStatusDetail() throws Exception {
    LdapSyncEventEntity event = new LdapSyncEventEntity(1L);
    event.setStatusDetail("some detail");
    Assert.assertEquals("some detail", event.getStatusDetail());
  }

  @Test
  public void testSetGetSpecs() throws Exception {
    LdapSyncEventEntity event = new LdapSyncEventEntity(1L);
    LdapSyncSpecEntity spec = new LdapSyncSpecEntity(LdapSyncSpecEntity.PrincipalType.Groups,
        LdapSyncSpecEntity.SyncType.All, Collections.<String>emptyList());

    event.setSpecs(Collections.singletonList(spec));

    List<LdapSyncSpecEntity> specs = event.getSpecs();
    Assert.assertEquals(1, specs.size());

    Assert.assertEquals(spec, specs.get(0));
  }

  @Test
  public void testSetGetStartTime() throws Exception {
    LdapSyncEventEntity event = new LdapSyncEventEntity(1L);
    event.setStartTime(10001000L);
    Assert.assertEquals(10001000L, event.getStartTime());
  }

  @Test
  public void testSetGetEndTime() throws Exception {
    LdapSyncEventEntity event = new LdapSyncEventEntity(1L);
    event.setEndTime(90009000L);
    Assert.assertEquals(90009000L, event.getEndTime());
  }

  @Test
  public void testSetGetUsersFetched() throws Exception {
    LdapSyncEventEntity event = new LdapSyncEventEntity(1L);
    event.setUsersFetched(99);
    Assert.assertEquals(Integer.valueOf(99), event.getUsersFetched());
  }

  @Test
  public void testSetGetUsersCreated() throws Exception {
    LdapSyncEventEntity event = new LdapSyncEventEntity(1L);
    event.setUsersCreated(98);
    Assert.assertEquals(Integer.valueOf(98), event.getUsersCreated());
  }

  @Test
  public void testSetGetUsersUpdated() throws Exception {
    LdapSyncEventEntity event = new LdapSyncEventEntity(1L);
    event.setUsersUpdated(97);
    Assert.assertEquals(Integer.valueOf(97), event.getUsersUpdated());
  }

  @Test
  public void testSetGetUsersRemoved() throws Exception {
    LdapSyncEventEntity event = new LdapSyncEventEntity(1L);
    event.setUsersRemoved(96);
    Assert.assertEquals(Integer.valueOf(96), event.getUsersRemoved());
  }

  @Test
  public void testSetGetGroupsFetched() throws Exception {
    LdapSyncEventEntity event = new LdapSyncEventEntity(1L);
    event.setGroupsFetched(95);
    Assert.assertEquals(Integer.valueOf(95), event.getGroupsFetched());
  }

  @Test
  public void testSetGetGroupsCreated() throws Exception {
    LdapSyncEventEntity event = new LdapSyncEventEntity(1L);
    event.setGroupsCreated(94);
    Assert.assertEquals(Integer.valueOf(94), event.getGroupsCreated());
  }

  @Test
  public void testSetGetGroupsUpdated() throws Exception {
    LdapSyncEventEntity event = new LdapSyncEventEntity(1L);
    event.setGroupsUpdated(93);
    Assert.assertEquals(Integer.valueOf(93), event.getGroupsUpdated());
  }

  @Test
  public void testSetGetGroupsRemoved() throws Exception {
    LdapSyncEventEntity event = new LdapSyncEventEntity(1L);
    event.setGroupsRemoved(92);
    Assert.assertEquals(Integer.valueOf(92), event.getGroupsRemoved());
  }

  @Test
  public void testSetGetMembershipsFetched() throws Exception {
    LdapSyncEventEntity event = new LdapSyncEventEntity(1L);
    event.setMembershipsFetched(91);
    Assert.assertEquals(Integer.valueOf(91), event.getMembershipsFetched());
  }

  @Test
  public void testSetGetMembershipsCreated() throws Exception {
    LdapSyncEventEntity event = new LdapSyncEventEntity(1L);
    event.setMembershipsCreated(90);
    Assert.assertEquals(Integer.valueOf(90), event.getMembershipsCreated());
  }

  @Test
  public void testSetGetMembershipsUpdated() throws Exception {
    LdapSyncEventEntity event = new LdapSyncEventEntity(1L);
    event.setMembershipsUpdated(99);
    Assert.assertEquals(Integer.valueOf(99), event.getMembershipsUpdated());
  }
}
