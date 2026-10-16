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
package org.apache.ambari.server.security.ldap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.LdapSyncRequest;
import org.apache.ambari.server.orm.entities.LdapSyncSpecEntity;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Provider;

/**
 * Unit tests for {@link LdapSyncScheduler}.
 * <p/>
 * These exercise {@code runOneIteration()} directly rather than waiting on
 * the real {@code AbstractScheduledService} timing, since we only care about
 * the decision logic (enabled/disabled, which sync type gets requested,
 * exceptions get swallowed) not the actual scheduling delay.
 */
public class LdapSyncSchedulerTest {

  private Configuration configuration;
  private AmbariManagementController controller;
  private LdapSyncScheduler scheduler;

  @Before
  public void setUp() throws Exception {
    configuration = mock(Configuration.class);
    controller = mock(AmbariManagementController.class);

    scheduler = new LdapSyncScheduler();
    setField("configuration", configuration);
    setField("controllerProvider", (Provider<AmbariManagementController>) () -> controller);
  }

  private void setField(String name, Object value) throws Exception {
    Field field = LdapSyncScheduler.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(scheduler, value);
  }

  @Test
  public void doesNotSyncWhenDisabled() throws Exception {
    when(configuration.isLdapAutoSyncEnabled()).thenReturn(false);

    scheduler.runOneIteration();

    verify(controller, never()).synchronizeLdapUsersAndGroups(any(), any());
  }

  @Test
  public void syncsWhenEnabled() throws Exception {
    when(configuration.isLdapAutoSyncEnabled()).thenReturn(true);
    when(configuration.getLdapAutoSyncType()).thenReturn("existing");
    when(controller.synchronizeLdapUsersAndGroups(any(), any())).thenReturn(new LdapBatchDto());

    scheduler.runOneIteration();

    verify(controller, times(1)).synchronizeLdapUsersAndGroups(
        argThatSyncType(LdapSyncSpecEntity.SyncType.EXISTING),
        argThatSyncType(LdapSyncSpecEntity.SyncType.EXISTING));
  }

  @Test
  public void syncsAllWhenConfiguredForAll() throws Exception {
    when(configuration.isLdapAutoSyncEnabled()).thenReturn(true);
    when(configuration.getLdapAutoSyncType()).thenReturn("all");
    when(controller.synchronizeLdapUsersAndGroups(any(), any())).thenReturn(new LdapBatchDto());

    scheduler.runOneIteration();

    verify(controller, times(1)).synchronizeLdapUsersAndGroups(
        argThatSyncType(LdapSyncSpecEntity.SyncType.ALL),
        argThatSyncType(LdapSyncSpecEntity.SyncType.ALL));
  }

  @Test
  public void doesNotThrowWhenSyncFails() throws Exception {
    when(configuration.isLdapAutoSyncEnabled()).thenReturn(true);
    when(configuration.getLdapAutoSyncType()).thenReturn("existing");
    when(controller.synchronizeLdapUsersAndGroups(any(), any()))
        .thenThrow(new RuntimeException("LDAP server unreachable"));

    // Must not propagate: a thrown exception here would stop
    // AbstractScheduledService's scheduling loop entirely.
    scheduler.runOneIteration();

    verify(controller, times(1)).synchronizeLdapUsersAndGroups(any(), any());
  }

  @Test
  public void invalidSyncTypeFallsBackToExisting() throws Exception {
    when(configuration.isLdapAutoSyncEnabled()).thenReturn(true);
    when(configuration.getLdapAutoSyncType()).thenReturn("bogus-value");
    when(controller.synchronizeLdapUsersAndGroups(any(), any())).thenReturn(new LdapBatchDto());

    scheduler.runOneIteration();

    verify(controller, times(1)).synchronizeLdapUsersAndGroups(
        argThatSyncType(LdapSyncSpecEntity.SyncType.EXISTING),
        argThatSyncType(LdapSyncSpecEntity.SyncType.EXISTING));
  }

  @Test
  public void passesPostProcessExistingUsersWhenConfigured() throws Exception {
    when(configuration.isLdapAutoSyncEnabled()).thenReturn(true);
    when(configuration.getLdapAutoSyncType()).thenReturn("existing");
    when(configuration.isLdapAutoSyncPostProcessExistingUsers()).thenReturn(true);
    when(controller.synchronizeLdapUsersAndGroups(any(), any())).thenReturn(new LdapBatchDto());

    scheduler.runOneIteration();

    verify(controller, times(1)).synchronizeLdapUsersAndGroups(
        argThatPostProcess(true), argThatPostProcess(true));
  }

  @Test
  public void doesNotPostProcessExistingUsersByDefault() throws Exception {
    when(configuration.isLdapAutoSyncEnabled()).thenReturn(true);
    when(configuration.getLdapAutoSyncType()).thenReturn("existing");
    when(controller.synchronizeLdapUsersAndGroups(any(), any())).thenReturn(new LdapBatchDto());

    scheduler.runOneIteration();

    verify(controller, times(1)).synchronizeLdapUsersAndGroups(
        argThatPostProcess(false), argThatPostProcess(false));
  }

  private static LdapSyncRequest argThatSyncType(LdapSyncSpecEntity.SyncType type) {
    return org.mockito.ArgumentMatchers.argThat(request -> request != null && request.getType() == type);
  }

  private static LdapSyncRequest argThatPostProcess(boolean postProcessExistingUsers) {
    return org.mockito.ArgumentMatchers.argThat(
        request -> request != null && request.getPostProcessExistingUsers() == postProcessExistingUsers);
  }
}
