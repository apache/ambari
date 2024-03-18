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
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.response.CollectionAdminResponse;
import org.apache.solr.client.solrj.response.SolrResponseBase;

public abstract class AbstractSolrRetryCommand<REQUEST extends CollectionAdminRequest, RESPONSE>
  extends AbstractRetryCommand<RESPONSE> {

  public AbstractSolrRetryCommand(int maxRetries, int interval) {
    super(maxRetries, interval);
  }

  public abstract RESPONSE handleResponse(CollectionAdminResponse response, AmbariSolrCloudClient client) throws Exception;

  public abstract REQUEST createRequest(AmbariSolrCloudClient client);

  public abstract String errorMessage(AmbariSolrCloudClient client);

  @Override
  public RESPONSE createAndProcessRequest(AmbariSolrCloudClient client) throws Exception {
    REQUEST request = createRequest(client);
    CollectionAdminResponse response = (CollectionAdminResponse) request.process(client.getSolrCloudClient());
    handleErrorIfExists(response, errorMessage(client));
    return handleResponse(response, client);
  }

  private void handleErrorIfExists(SolrResponseBase response, String message) throws AmbariSolrCloudClientException {
    if (response.getStatus() != 0) {
      throw new AmbariSolrCloudClientException(message);
    }
  }
}
