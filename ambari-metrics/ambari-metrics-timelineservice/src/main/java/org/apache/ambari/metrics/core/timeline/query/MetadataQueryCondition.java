/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.metrics.core.timeline.query;

import java.util.Collections;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

public class MetadataQueryCondition extends TransientMetricCondition {

  private boolean isMetricMetadataCondition = true;

  public MetadataQueryCondition(List<String> metricNames, String appId, String instanceId) {
    super(metricNames, Collections.EMPTY_LIST, appId, instanceId, null, null, null , null, true, null);
    this.hostnames = Collections.EMPTY_LIST;
  }

  public MetadataQueryCondition(List<String> hostnames) {
    super(Collections.EMPTY_LIST, hostnames, StringUtils.EMPTY, StringUtils.EMPTY, null, null, null , null, true, null);
    isMetricMetadataCondition = false;
  }

  public StringBuilder getConditionClause() {
    StringBuilder sb = new StringBuilder();

    boolean appendConjunction = false;
    if (CollectionUtils.isNotEmpty(metricNames)) {
      appendConjunction = appendMetricNameClause(sb);
    }

    if (CollectionUtils.isNotEmpty(hostnames)) {
      appendConjunction = appendHostnameClause(sb, appendConjunction);
    }

    String appId = getAppId();
    if (StringUtils.isNotEmpty(appId)) {
      if (appId.contains("%")) {
        appendConjunction = append(sb, appendConjunction, appId, " APP_ID LIKE ?");
      } else {
        appendConjunction = append(sb, appendConjunction, appId, " APP_ID = ?");
      }
    }

    String instanceId = getInstanceId();
    if (StringUtils.isNotEmpty(instanceId) && !"%".equals(instanceId)) {
      if (instanceId.contains("%")) {
        appendConjunction = append(sb, appendConjunction, instanceId, " INSTANCE_ID LIKE ?");
      } else {
        appendConjunction = append(sb, appendConjunction, instanceId, " INSTANCE_ID = ?");
      }
    }

    return sb;
  }

  @Override
  public String getInstanceId() {
    return instanceId == null || "%".equals(instanceId) || instanceId.isEmpty() ? null : instanceId;
  }

  public boolean isMetricMetadataCondition() {
    return isMetricMetadataCondition;
  }
}