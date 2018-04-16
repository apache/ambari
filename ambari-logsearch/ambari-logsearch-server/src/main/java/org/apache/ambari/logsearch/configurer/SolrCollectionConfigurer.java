/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ambari.logsearch.configurer;

import org.apache.ambari.logsearch.conf.SolrPropsConfig;
import org.apache.ambari.logsearch.conf.global.SolrCollectionState;
import org.apache.ambari.logsearch.dao.SolrDaoBase;
import org.apache.ambari.logsearch.handler.ACLHandler;
import org.apache.ambari.logsearch.handler.CreateCollectionHandler;
import org.apache.ambari.logsearch.handler.ListCollectionHandler;
import org.apache.ambari.logsearch.handler.ReloadCollectionHandler;
import org.apache.ambari.logsearch.handler.UploadConfigurationHandler;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.Krb5HttpClientBuilder;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.solr.core.SolrTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class SolrCollectionConfigurer implements Configurer {

  private Logger LOG = LoggerFactory.getLogger(SolrCollectionConfigurer.class);

  private static final int SETUP_RETRY_SECOND = 10;
  private static final int SESSION_TIMEOUT = 15000;
  private static final int CONNECTION_TIMEOUT = 30000;
  private static final String JAVA_SECURITY_AUTH_LOGIN_CONFIG = "java.security.auth.login.config";
  private static final String SOLR_HTTPCLIENT_BUILDER_FACTORY = "solr.httpclient.builder.factory";

  private final SolrDaoBase solrDaoBase;
  private final boolean hasEnumConfig; // enumConfig.xml for solr collection

  public SolrCollectionConfigurer(SolrDaoBase solrDaoBase, boolean hasEnumConfig) {
    this.solrDaoBase = solrDaoBase;
    this.hasEnumConfig = hasEnumConfig;
  }

  @Override
  public void start() {
    setupSecurity();
    final SolrPropsConfig solrPropsConfig = solrDaoBase.getSolrPropsConfig();
    final SolrCollectionState state = solrDaoBase.getSolrCollectionState();
    final String separator = FileSystems.getDefault().getSeparator();
    final String localConfigSetLocation = String.format("%s%s%s%sconf", solrPropsConfig.getConfigSetFolder(), separator,
      solrPropsConfig.getConfigName(), separator);
    final File configSetFolder = new File(localConfigSetLocation);
    if (!configSetFolder.exists()) { // show exception only once during startup
      throw new RuntimeException(String.format("Cannot load config set location: %s", localConfigSetLocation));
    }
    Thread setupThread = new Thread("setup_collection_" + solrPropsConfig.getCollection()) {
      @Override
      public void run() {
        LOG.info("Started monitoring thread to check availability of Solr server. collection=" + solrPropsConfig.getCollection());
        while (!stopSetupCondition(state)) {
          int retryCount = 0;
          try {
            retryCount++;
            Thread.sleep(SETUP_RETRY_SECOND * 1000);
            openZkConnectionAndUpdateStatus(state, solrPropsConfig);
            if (solrDaoBase.getSolrTemplate() == null) {
              solrDaoBase.setSolrTemplate(createSolrTemplate(solrPropsConfig));
            }
            CloudSolrClient cloudSolrClient = (CloudSolrClient) solrDaoBase.getSolrTemplate().getSolrClient();
            boolean reloadCollectionNeeded = uploadConfigurationsIfNeeded(cloudSolrClient, configSetFolder, state, solrPropsConfig);
            checkSolrStatus(cloudSolrClient);
            createCollectionsIfNeeded(cloudSolrClient, state, solrPropsConfig, reloadCollectionNeeded);
          } catch (Exception e) {
            retryCount++;
            LOG.error("Error setting collection. collection=" + solrPropsConfig.getCollection() + ", retryCount=" + retryCount, e);
          }
        }
      }
    };
    setupThread.setDaemon(true);
    setupThread.start();
  }

  private boolean uploadConfigurationsIfNeeded(CloudSolrClient cloudSolrClient, File configSetFolder, SolrCollectionState state, SolrPropsConfig solrPropsConfig) throws Exception {
    boolean reloadCollectionNeeded = new UploadConfigurationHandler(configSetFolder, hasEnumConfig).handle(cloudSolrClient, solrPropsConfig);
    if (!state.isConfigurationUploaded()) {
      state.setConfigurationUploaded(true);
    }
    return reloadCollectionNeeded;
  }

  public boolean stopSetupCondition(SolrCollectionState state) {
    return state.isSolrCollectionReady();
  }

  public SolrTemplate createSolrTemplate(SolrPropsConfig solrPropsConfig) {
    return new SolrTemplate(createClient(
      solrPropsConfig.getSolrUrl(),
      solrPropsConfig.getZkConnectString(),
      solrPropsConfig.getCollection()));
  }

  private CloudSolrClient createClient(String solrUrl, String zookeeperConnectString, String defaultCollection) {
    if (StringUtils.isNotEmpty(zookeeperConnectString)) {
      CloudSolrClient cloudSolrClient = new CloudSolrClient.Builder().withZkHost(zookeeperConnectString).build();
      cloudSolrClient.setDefaultCollection(defaultCollection);
      return cloudSolrClient;
    } else if (StringUtils.isNotEmpty(solrUrl)) {
      throw new UnsupportedOperationException("Currently only cloud mode is supported. Set zookeeper connect string.");
    }
    throw new IllegalStateException(
      "Solr url or zookeeper connection string is missing. collection: " + defaultCollection);
  }

  private void setupSecurity() {
    boolean securityEnabled = solrDaoBase.getSolrKerberosConfig().isEnabled();
    if (securityEnabled) {
      String javaSecurityConfig = System.getProperty(JAVA_SECURITY_AUTH_LOGIN_CONFIG);
      String solrHttpBuilderFactory = System.getProperty(SOLR_HTTPCLIENT_BUILDER_FACTORY);
      LOG.info("setupSecurity() called for kerberos configuration, jaas file: {}, solr http client factory: {}",
        javaSecurityConfig, solrHttpBuilderFactory);
    }
  }

  private void openZkConnectionAndUpdateStatus(final SolrCollectionState state, final SolrPropsConfig solrPropsConfig) throws Exception {
    ZooKeeper zkClient = null;
    try {
      LOG.info("Checking that Znode ('{}') is ready or not... ", solrPropsConfig.getZkConnectString());
      zkClient = openZookeeperConnection(solrPropsConfig);
      if (!state.isZnodeReady()) {
        LOG.info("State change: Zookeeper ZNode is available for {}", solrPropsConfig.getZkConnectString());
        state.setZnodeReady(true);
      }
    } catch (Exception e) {
      LOG.error("Error occurred during the creation of zk client (connection string: {})", solrPropsConfig.getZkConnectString());
      throw e;
    } finally {
      try {
        if (zkClient != null) {
          zkClient.close();
        }
      } catch (Exception e) {
        LOG.error("Could not close zk connection properly.", e);
      }
    }
  }

  private ZooKeeper openZookeeperConnection(final SolrPropsConfig solrPropsConfig) throws InterruptedException, IOException {
    final CountDownLatch connSignal = new CountDownLatch(1);
    ZooKeeper zooKeeper = new ZooKeeper(solrPropsConfig.getZkConnectString(), SESSION_TIMEOUT, new Watcher() {
      public void process(WatchedEvent event) {
        if (event.getState() == Event.KeeperState.SyncConnected) {
          connSignal.countDown();
        }
      }
    });
    connSignal.await(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS);
    return zooKeeper;
  }

  private boolean checkSolrStatus(CloudSolrClient cloudSolrClient) {
    int waitDurationMS = 3 * 60 * 1000;
    boolean status = false;
    try {
      long beginTimeMS = System.currentTimeMillis();
      long waitIntervalMS = 2000;
      int pingCount = 0;
      while (true) {
        pingCount++;
        try {
          List<String> collectionList = new ListCollectionHandler().handle(cloudSolrClient, null);
          if (collectionList != null) {
            LOG.info("checkSolrStatus(): Solr getCollections() is success. collectionList=" + collectionList);
            status = true;
            break;
          }
        } catch (Exception ex) {
          LOG.error("Error while doing Solr check", ex);
        }
        if (System.currentTimeMillis() - beginTimeMS > waitDurationMS) {
          LOG.error("Solr is not reachable even after " + (System.currentTimeMillis() - beginTimeMS) + " ms. " +
            "If you are using alias, then you might have to restart LogSearch after Solr is up and running.");
          break;
        } else {
          LOG.warn("Solr is not not reachable yet. getCollections() attempt count=" + pingCount + ". " +
            "Will sleep for " + waitIntervalMS + " ms and try again.");
        }
        Thread.sleep(waitIntervalMS);

      }
    } catch (Throwable t) {
      LOG.error("Seems Solr is not up.");
    }
    return status;
  }

  private void createCollectionsIfNeeded(CloudSolrClient solrClient, SolrCollectionState state, SolrPropsConfig solrPropsConfig,
      boolean reloadCollectionNeeded) {
    try {
      List<String> allCollectionList = new ListCollectionHandler().handle(solrClient, null);
      solrDaoBase.waitForLogSearchConfig();
      CreateCollectionHandler handler = new CreateCollectionHandler(solrDaoBase.getLogSearchConfig(), allCollectionList);
      boolean collectionCreated = handler.handle(solrClient, solrPropsConfig);
      boolean collectionReloaded = true;
      if (reloadCollectionNeeded) {
        collectionReloaded = new ReloadCollectionHandler().handle(solrClient, solrPropsConfig);
      }
      boolean aclsUpdated = new ACLHandler().handle(solrClient, solrPropsConfig);
      if (!state.isSolrCollectionReady() && collectionCreated && collectionReloaded && aclsUpdated) {
        state.setSolrCollectionReady(true);
      }
    } catch (Exception ex) {
      LOG.error("Error during creating/updating collection. collectionName=" + solrPropsConfig.getCollection(), ex);
    }
  }
}
