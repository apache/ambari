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
package org.apache.ambari.logsearch.conf;

import org.apache.zookeeper.data.ACL;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

public abstract class SolrConnectionPropsConfig implements SolrPropsConfig {
  @Value("${logsearch.solr.url:}")
  private String solrUrl;

  @Value("${logsearch.solr.zk_connect_string:}")
  private String zkConnectString;

  @Value("#{ACLPropertiesSplitter.parseAcls('${logsearch.solr.zk.acls:}')}")
  private List<ACL> zkAcls;

  @Value("${logsearch.solr.config_set.folder:/etc/ambari-logsearch-portal/conf/solr_configsets}")
  private String configSetFolder;

  @Override
  public String getSolrUrl() {
    return solrUrl;
  }

  @Override
  public void setSolrUrl(String solrUrl) {
    this.solrUrl = solrUrl;
  }

  @Override
  public String getZkConnectString() {
    return zkConnectString;
  }

  @Override
  public void setZkConnectString(String zkConnectString) {
    this.zkConnectString = zkConnectString;
  }

  @Override
  public List<ACL> getZkAcls() {
    return zkAcls;
  }

  @Override
  public void setZkAcls(List<ACL> zkAcls) {
    this.zkAcls = zkAcls;
  }

  @Override
  public String getConfigSetFolder() {
    return configSetFolder;
  }

  @Override
  public void setConfigSetFolder(String configSetFolder) {
    this.configSetFolder = configSetFolder;
  }
}
