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
package org.apache.ambari.server.security.encryption;

import java.util.Collection;
import java.util.Map;

import org.apache.ambari.server.audit.AuditLoggerModule;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ControllerModule;
import org.apache.ambari.server.ldap.LdapModule;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.utils.EventBusSynchronizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

public class SensitiveDataEncryption {
  private static final Logger LOG = LoggerFactory.getLogger
      (SensitiveDataEncryption.class);

  private PersistService persistService;
  private DBAccessor dbAccessor;
  private Injector injector;


  @Inject
  public SensitiveDataEncryption(DBAccessor dbAccessor,
                                 Injector injector,
                                 PersistService persistService) {
    this.dbAccessor = dbAccessor;
    this.injector = injector;
    this.persistService = persistService;
  }

  /**
   * Extension of main controller module
   */
  public static class EncryptionHelperControllerModule extends ControllerModule {

    public EncryptionHelperControllerModule() throws Exception {
    }

    @Override
    protected void configure() {
      super.configure();
      EventBusSynchronizer.synchronizeAmbariEventPublisher(binder());
    }
  }

  /**
   * Extension of audit logger module
   */
  public static class EncryptionHelperAuditModule extends AuditLoggerModule {

    public EncryptionHelperAuditModule() throws Exception {
    }

    @Override
    protected void configure() {
      super.configure();
    }

  }

  public void startPersistenceService() {
    persistService.start();
  }

  public void stopPersistenceService() {
    persistService.stop();
  }

  public static void main(String[] args) {
    if (args.length < 1 || !"encryption".equals(args[0]) || !"decryption".equals(args[0] )){
      LOG.error("Expect encryption/decryption action parameter");
      return;
    }
    boolean encrypt = "encryption".equals(args[0]);
    SensitiveDataEncryption sensitiveDataEncryption = null;
    try {
      Injector injector = Guice.createInjector(new EncryptionHelperControllerModule(), new EncryptionHelperAuditModule(), new LdapModule());
      sensitiveDataEncryption = injector.getInstance(SensitiveDataEncryption.class);
      sensitiveDataEncryption.startPersistenceService();
      AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
      Encryptor<Config> configEncryptor = injector.getInstance(ConfigPropertiesEncryptor.class);
      Clusters clusters = ambariManagementController.getClusters();
      if (clusters != null) {
        Map<String, Cluster> clusterMap = clusters.getClusters();
        if (clusterMap != null && !clusterMap.isEmpty()) {
          for (final Cluster cluster : clusterMap.values()) {
            Collection<Config> configs = cluster.getAllConfigs();
            for (Config config : configs) {
              if (encrypt) {
                config.encryptSensitiveDataAndSave(configEncryptor);
              } else {
                config.decryptSensitiveDataAndSave(configEncryptor);
              }
            }
          }
        }
      }
    } catch (Throwable e) {
      LOG.error("Exception occurred during config encryption/decryption:", e);
    } finally {
      if (sensitiveDataEncryption != null) {
        sensitiveDataEncryption.stopPersistenceService();
      }
    }
  }
}
