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
package org.apache.ambari.infra.solr.domain.json;

import java.util.Map;

public class SolrCoreData {
  private String coreNodeName;
  private String hostName;
  private Map<String, String> properties;

  public SolrCoreData(String coreNodeName, String hostName, Map<String, String> properties) {
    this.coreNodeName = coreNodeName;
    this.hostName = hostName;
    this.properties = properties;
  }

  public String getCoreNodeName() {
    return coreNodeName;
  }

  public void setCoreNodeName(String coreNodeName) {
    this.coreNodeName = coreNodeName;
  }

  public String getHostName() {
    return hostName;
  }

  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }
}
