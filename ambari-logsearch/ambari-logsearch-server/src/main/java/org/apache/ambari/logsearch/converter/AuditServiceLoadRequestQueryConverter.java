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
package org.apache.ambari.logsearch.converter;

import org.apache.ambari.logsearch.common.LogType;
import org.apache.ambari.logsearch.model.request.impl.AuditServiceLoadRequest;
import org.springframework.data.solr.core.query.FacetOptions;

import javax.inject.Named;

import static org.apache.ambari.logsearch.solr.SolrConstants.AuditLogConstants.AUDIT_EVTTIME;
import static org.apache.ambari.logsearch.solr.SolrConstants.AuditLogConstants.AUDIT_COMPONENT;

@Named
public class AuditServiceLoadRequestQueryConverter extends AbstractLogRequestFacetQueryConverter<AuditServiceLoadRequest> {

  @Override
  public FacetOptions.FacetSort getFacetSort() {
    return FacetOptions.FacetSort.COUNT;
  }

  @Override
  public String getDateTimeField() {
    return AUDIT_EVTTIME;
  }

  @Override
  public LogType getLogType() {
    return LogType.AUDIT;
  }

  @Override
  public void appendFacetOptions(FacetOptions facetOptions, AuditServiceLoadRequest request) {
    facetOptions.addFacetOnField(AUDIT_COMPONENT);
    facetOptions.setFacetLimit(10);
  }
}
