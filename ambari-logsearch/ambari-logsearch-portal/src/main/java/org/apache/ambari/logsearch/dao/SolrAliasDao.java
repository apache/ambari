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
package org.apache.ambari.logsearch.dao;

import org.apache.ambari.logsearch.conf.SolrAuditLogPropsConfig;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.response.CollectionAdminResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

@Component
public class SolrAliasDao {

  private static final Logger LOG = LoggerFactory.getLogger(SolrAliasDao.class);

  private static final int ALIAS_SETUP_RETRY_SECOND = 30*60;

  @Inject
  private SolrCollectionDao solrCollectionDao;

  protected void setupAlias(final SolrSchemaFieldDao solrSchemaFieldDao, final CloudSolrClient solrClient,
                            final SolrAuditLogPropsConfig solrPropsConfig, final SolrDaoBase solrDaoBase) throws Exception {
    final Collection<String> collectionListIn = Arrays.asList(solrPropsConfig.getCollection(), solrPropsConfig.getRangerCollection().trim());

    if (solrPropsConfig.getAliasNameIn() == null || collectionListIn.size() == 0 || solrClient == null) {
      LOG.info("Will not create alias " + solrPropsConfig.getAliasNameIn() + " for " +
        collectionListIn.toString() + ", solrCloudClient=" + solrClient);
      return;
    }

    LOG.info("setupAlias " + solrPropsConfig.getAliasNameIn() + " for " + collectionListIn.toString());
    // Start a background thread to do setup
    Thread setupThread = new Thread("setup_alias_" + solrPropsConfig.getAliasNameIn()) {
      @Override
      public void run() {
        LOG.info("Started monitoring thread to check availability of Solr server. alias=" + solrPropsConfig.getAliasNameIn() +
          ", collections=" + collectionListIn.toString());
        int retryCount = 0;
        while (true) {
          try {
            int count = createAlias(solrClient, solrPropsConfig.getAliasNameIn(), collectionListIn);
            if (count > 0) {
              solrClient.setDefaultCollection(solrPropsConfig.getAliasNameIn());
              if (count == collectionListIn.size()) {
                LOG.info("Setup for alias " + solrPropsConfig.getAliasNameIn() + " is successful. Exiting setup retry thread. " +
                  "Collections=" + collectionListIn);
                solrSchemaFieldDao.populateSchemaFields(solrClient, solrPropsConfig, solrDaoBase);
                break;
              }
            } else {
              LOG.warn("Not able to create alias=" + solrPropsConfig.getAliasNameIn() + ", retryCount=" + retryCount);
            }
          } catch (Exception e) {
            LOG.error("Error setting up alias=" + solrPropsConfig.getAliasNameIn(), e);
          }
          try {
            Thread.sleep(ALIAS_SETUP_RETRY_SECOND * 1000);
          } catch (InterruptedException sleepInterrupted) {
            LOG.info("Sleep interrupted while setting up alias " + solrPropsConfig.getAliasNameIn());
            break;
          }
          retryCount++;
        }
      }
    };
    setupThread.setDaemon(true);
    setupThread.start();
  }

  private int createAlias(final CloudSolrClient solrClient, String aliasNameIn, Collection<String> collectionListIn)
    throws SolrServerException, IOException {
    List<String> collectionToAdd = solrCollectionDao.getCollections(solrClient);
    collectionToAdd.retainAll(collectionListIn);

    String collectionsCSV = null;
    if (!collectionToAdd.isEmpty()) {
      collectionsCSV = StringUtils.join(collectionToAdd, ',');
      CollectionAdminRequest.CreateAlias aliasCreateRequest = new CollectionAdminRequest.CreateAlias();
      aliasCreateRequest.setAliasName(aliasNameIn);
      aliasCreateRequest.setAliasedCollections(collectionsCSV);
      CollectionAdminResponse createResponse = aliasCreateRequest.process(solrClient);
      if (createResponse.getStatus() != 0) {
        LOG.error("Error creating alias. alias=" + aliasNameIn + ", collectionList=" + collectionsCSV
          + ", response=" + createResponse);
        return 0;
      }
    }
    if ( collectionToAdd.size() == collectionListIn.size()) {
      LOG.info("Created alias for all collections. alias=" + aliasNameIn + ", collectionsCSV=" + collectionsCSV);
    } else {
      LOG.info("Created alias for " + collectionToAdd.size() + " out of " + collectionListIn.size() + " collections. " +
        "alias=" + aliasNameIn + ", collectionsCSV=" + collectionsCSV);
    }
    return collectionToAdd.size();
  }
}
