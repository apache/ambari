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
package org.apache.ambari.infra.solr.commands;

import org.apache.ambari.infra.solr.AmbariSolrCloudClient;
import org.apache.ambari.infra.solr.domain.AmbariSolrState;
import org.apache.solr.common.cloud.SolrZkClient;
import org.apache.solr.common.cloud.SolrZooKeeper;
import org.apache.zookeeper.CreateMode;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class UpdateStateFileZkCommand extends AbstractStateFileZkCommand {

  private static final Logger LOG = LoggerFactory.getLogger(UpdateStateFileZkCommand.class);

  private String unsecureZnode;

  public UpdateStateFileZkCommand(int maxRetries, int interval, String unsecureZnode) {
    super(maxRetries, interval);
    this.unsecureZnode = unsecureZnode;
  }

  @Override
  protected AmbariSolrState executeZkCommand(AmbariSolrCloudClient client, SolrZkClient zkClient, SolrZooKeeper solrZooKeeper) throws Exception {
    boolean secure = client.isSecure();
    String stateFile = String.format("%s/%s", unsecureZnode, AbstractStateFileZkCommand.STATE_FILE);
    AmbariSolrState result = null;
    if (secure) {
      LOG.info("Update state file in secure mode.");
      updateStateFile(client, zkClient, AmbariSolrState.SECURE, stateFile);
      result = AmbariSolrState.SECURE;
    } else {
      LOG.info("Update state file in unsecure mode.");
      updateStateFile(client, zkClient, AmbariSolrState.UNSECURE, stateFile);
      result = AmbariSolrState.UNSECURE;
    }
    return result;
  }

  private void updateStateFile(AmbariSolrCloudClient client, SolrZkClient zkClient, AmbariSolrState stateToUpdate,
                               String stateFile) throws Exception {
    if (!zkClient.exists(stateFile, true)) {
      LOG.info("State file does not exits. Initializing it as '{}'", stateToUpdate);
      zkClient.create(stateFile, createStateJson(stateToUpdate).getBytes(StandardCharsets.UTF_8),
        CreateMode.PERSISTENT, true);
    } else {
      AmbariSolrState stateOnSecure = getStateFromJson(client, stateFile);
      if (stateToUpdate.equals(stateOnSecure)) {
        LOG.info("State file is in '{}' mode. No update.", stateOnSecure);
      } else {
        LOG.info("State file is in '{}' mode. Updating it to '{}'", stateOnSecure, stateToUpdate);
        zkClient.setData(stateFile, createStateJson(stateToUpdate).getBytes(StandardCharsets.UTF_8), true);
      }
    }
  }

  private String createStateJson(AmbariSolrState state) throws Exception {
    Map<String, String> secStateMap = new HashMap<>();
    secStateMap.put(AbstractStateFileZkCommand.STATE_FIELD, state.toString());
    return new ObjectMapper().writeValueAsString(secStateMap);
  }
}
