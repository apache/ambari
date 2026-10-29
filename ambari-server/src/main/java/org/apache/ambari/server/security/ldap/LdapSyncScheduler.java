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

import java.util.concurrent.TimeUnit;

import org.apache.ambari.server.AmbariService;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.LdapSyncRequest;
import org.apache.ambari.server.orm.entities.LdapSyncSpecEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Runs LDAP user/group synchronization periodically, in-process, with no
 * external trigger (cron/systemd timer/CI job) and no externally supplied
 * admin credentials required.
 * <p/>
 * Disabled by default. Enable via ambari.properties:
 * <pre>
 *   ldap.sync.auto.enabled=true
 *   ldap.sync.auto.interval.minutes=60
 *   ldap.sync.auto.type=existing        # existing | all
 *   ldap.sync.auto.initial.delay.minutes=5
 *   ldap.sync.auto.post.process.existing.users=false
 * </pre>
 * This intentionally reuses the exact same code path
 * ({@link AmbariManagementController#synchronizeLdapUsersAndGroups}) that the
 * {@code ambari-server sync-ldap} CLI and the {@code /api/v1/ldap_sync_events}
 * REST endpoint already use, so sync behavior stays identical and there is
 * only one implementation of the actual sync logic to maintain.
 * <p/>
 * Registered via {@link AmbariService}, so it is instantiated, member-injected
 * and started/stopped automatically along with the rest of ambari-server's
 * background services (see {@code ControllerModule#bindByAnnotation}).
 */
@AmbariService
public class LdapSyncScheduler extends AbstractScheduledService {

  private static final Logger LOG = LoggerFactory.getLogger(LdapSyncScheduler.class);

  @Inject
  private Configuration configuration;

  @Inject
  private Provider<AmbariManagementController> controllerProvider;

  @Override
  protected void startUp() {
    if (!configuration.isLdapAutoSyncEnabled()) {
      LOG.info("Automatic LDAP sync is disabled ({}=false). " +
          "Enable it in ambari.properties to turn on periodic sync.",
          Configuration.LDAP_SYNC_AUTO_ENABLED.getKey());
      return;
    }
    LOG.info("Automatic LDAP sync enabled. type='{}', interval={} min, initial delay={} min",
        getSyncType(), configuration.getLdapAutoSyncIntervalMinutes(),
        configuration.getLdapAutoSyncInitialDelayMinutes());
  }

  @Override
  protected void runOneIteration() {
    if (!configuration.isLdapAutoSyncEnabled()) {
      // Feature can be toggled off without a restart; simply skip ticks
      // while disabled rather than stopping the service outright.
      return;
    }

    try {
      String syncType = getSyncType();
      LOG.info("Starting automatic LDAP sync (type={})", syncType);

      LdapSyncSpecEntity.SyncType specType = "all".equals(syncType)
          ? LdapSyncSpecEntity.SyncType.ALL
          : LdapSyncSpecEntity.SyncType.EXISTING;

      boolean postProcessExistingUsers = configuration.isLdapAutoSyncPostProcessExistingUsers();

      LdapSyncRequest userRequest = new LdapSyncRequest(specType, postProcessExistingUsers);
      LdapSyncRequest groupRequest = new LdapSyncRequest(specType, postProcessExistingUsers);

      LdapBatchDto batchInfo = controllerProvider.get().synchronizeLdapUsersAndGroups(userRequest, groupRequest);

      LOG.info("Automatic LDAP sync completed. users created={}, users become-ldap={}, " +
              "groups created={}, groups become-ldap={}",
          batchInfo.getUsersToBeCreated().size(),
          batchInfo.getUsersToBecomeLdap().size(),
          batchInfo.getGroupsToBeCreated().size(),
          batchInfo.getGroupsToBecomeLdap().size());

    } catch (Exception e) {
      // Deliberately broad: a failed sync (e.g. LDAP server temporarily
      // unreachable) must never crash or destabilize the scheduler —
      // just log and retry on the next tick.
      LOG.error("Automatic LDAP sync failed; will retry on next scheduled run.", e);
    }
  }

  /**
   * @return the configured sync type, normalized to "existing" or "all";
   *         falls back to "existing" for an unrecognized value
   */
  private String getSyncType() {
    String type = configuration.getLdapAutoSyncType();
    if (!"existing".equalsIgnoreCase(type) && !"all".equalsIgnoreCase(type)) {
      LOG.warn("Invalid value '{}' for {} — falling back to 'existing'",
          type, Configuration.LDAP_SYNC_AUTO_TYPE.getKey());
      return "existing";
    }
    return type.toLowerCase();
  }

  @Override
  protected Scheduler scheduler() {
    int initialDelay = configuration.getLdapAutoSyncInitialDelayMinutes();
    int interval = configuration.getLdapAutoSyncIntervalMinutes();
    return Scheduler.newFixedDelaySchedule(initialDelay, interval, TimeUnit.MINUTES);
  }
}
