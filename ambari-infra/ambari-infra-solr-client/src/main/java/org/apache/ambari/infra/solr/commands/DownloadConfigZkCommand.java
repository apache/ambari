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
import org.apache.ambari.infra.solr.AmbariSolrCloudClientException;
import org.apache.solr.common.cloud.ZkConfigManager;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DownloadConfigZkCommand extends AbstractZookeeperConfigCommand<String> {

  public DownloadConfigZkCommand(int maxRetries, int interval) {
    super(maxRetries, interval);
  }

  @Override
  protected String executeZkConfigCommand(ZkConfigManager zkConfigManager, AmbariSolrCloudClient client) throws Exception {
    Path configDir = Paths.get(client.getConfigDir());
    String configSet = client.getConfigSet();
    try {
      zkConfigManager.downloadConfigDir(configSet, configDir);
      return configDir.toString();
    } catch (IOException e){
      throw new AmbariSolrCloudClientException("Error downloading configuration set, check Solr Znode has started or not " +
        "(starting Solr (for Log Search) is responsible to create the Znode)" ,e);
    }
  }
}
