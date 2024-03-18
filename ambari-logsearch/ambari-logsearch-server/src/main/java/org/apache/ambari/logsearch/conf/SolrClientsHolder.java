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

import org.apache.solr.client.solrj.SolrClient;

import java.util.EnumMap;
import java.util.Map;

public class SolrClientsHolder {

  private Map<CollectionType, SolrClient> clientsMap = new EnumMap<>(CollectionType.class);

  public enum CollectionType {
    SERVICE,
    AUDIT,
    HISTORY
  }

  public SolrClientsHolder() {
    clientsMap.put(CollectionType.SERVICE, null);
    clientsMap.put(CollectionType.AUDIT, null);
    clientsMap.put(CollectionType.HISTORY, null);
  }

  public SolrClient getSolrClient(CollectionType type) {
    return clientsMap.get(type);
  }

  public synchronized void setSolrClient(SolrClient solrClient, CollectionType type) {
    clientsMap.put(type, solrClient);
  }
}
