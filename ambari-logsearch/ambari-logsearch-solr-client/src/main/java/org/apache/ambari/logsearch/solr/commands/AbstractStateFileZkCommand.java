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
package org.apache.ambari.logsearch.solr.commands;

import org.apache.ambari.logsearch.solr.AmbariSolrCloudClient;
import org.apache.ambari.logsearch.solr.domain.AmbariSolrState;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

public abstract class AbstractStateFileZkCommand extends AbstractZookeeperRetryCommand<AmbariSolrState>{

  public static final String STATE_FILE = "ambari-solr-state.json";
  public static final String STATE_FIELD = "ambari_solr_security_state";

  public AbstractStateFileZkCommand(int maxRetries, int interval) {
    super(maxRetries, interval);
  }

  public AmbariSolrState getStateFromJson(AmbariSolrCloudClient client, String fileName) throws Exception {
    byte[] data = client.getSolrZkClient().getData(fileName, null, null, true);
    String input = new String(data);
    ObjectMapper mapper = new ObjectMapper();
    JsonNode rootNode = mapper.readValue(input.getBytes(), JsonNode.class);
    return AmbariSolrState.valueOf(rootNode.get(STATE_FIELD).asText());
  }
}
