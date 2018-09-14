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

import org.apache.ambari.logsearch.config.api.LogSearchPropertyDescription;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.ambari.logsearch.common.LogSearchConstants.AUDIT_COMPONENT_LABELS_DEFAULTS;
import static org.apache.ambari.logsearch.common.LogSearchConstants.AUDIT_FIELD_COMMON_LABELS_DEFAULTS;
import static org.apache.ambari.logsearch.common.LogSearchConstants.AUDIT_FIELD_EXCLUDES_COMMON_DEFAULTS;
import static org.apache.ambari.logsearch.common.LogSearchConstants.AUDIT_FIELD_EXCLUDES_DEFAULTS;
import static org.apache.ambari.logsearch.common.LogSearchConstants.AUDIT_FIELD_FALLBACK_PREFIX_DEFAULTS;
import static org.apache.ambari.logsearch.common.LogSearchConstants.AUDIT_FIELD_FALLBACK_SUFFIX_DEFAULTS;
import static org.apache.ambari.logsearch.common.LogSearchConstants.AUDIT_FIELD_FILTERABLE_EXCLUDES_COMMON_DEFAULTS;
import static org.apache.ambari.logsearch.common.LogSearchConstants.AUDIT_FIELD_FILTERABLE_EXCLUDES_DEFAULTS;
import static org.apache.ambari.logsearch.common.LogSearchConstants.AUDIT_FIELD_LABELS_DEFAULTS;
import static org.apache.ambari.logsearch.common.LogSearchConstants.AUDIT_FIELD_VISIBLE_COMMON_DEFAULTS;
import static org.apache.ambari.logsearch.common.LogSearchConstants.AUDIT_FIELD_VISIBLE_DEFAULTS;
import static org.apache.ambari.logsearch.common.LogSearchConstants.LOGSEARCH_PROPERTIES_FILE;
import static org.apache.ambari.logsearch.common.LogSearchConstants.SERVICE_FIELD_FALLBACK_PREFIX_DEFAULTS;
import static org.apache.ambari.logsearch.common.LogSearchConstants.SERVICE_FIELD_FALLBACK_SUFFIX_DEFAULTS;
import static org.apache.ambari.logsearch.common.LogSearchConstants.SERVICE_FIELD_FILTERABLE_EXLUDE_DEFAULTS;
import static org.apache.ambari.logsearch.common.LogSearchConstants.SERVICE_GROUP_LABELS_DEFAULTS;
import static org.apache.ambari.logsearch.common.LogSearchConstants.SERVICE_COMPONENT_LABELS_DEFAULTS;
import static org.apache.ambari.logsearch.common.LogSearchConstants.SERVICE_FIELD_LABELS_DEFAULTS;
import static org.apache.ambari.logsearch.common.LogSearchConstants.SERVICE_FIELD_EXCLUDES_DEFAULTS;
import static org.apache.ambari.logsearch.common.LogSearchConstants.SERVICE_FIELD_VISIBLE_DEFAULTS;

@Configuration
public class UIMappingConfig {

  @Value("#{propertiesSplitter.parseMap('${logsearch.web.service_logs.group.labels:" + SERVICE_GROUP_LABELS_DEFAULTS + "}')}")
  @LogSearchPropertyDescription(
    name = "logsearch.web.service_logs.group.labels",
    description = "Map of serivce group labels",
    examples = {"ambari:Ambari,yarn:YARN"},
    defaultValue = SERVICE_GROUP_LABELS_DEFAULTS,
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private Map<String, String> serviceGroupLabels;

  @Value("#{propertiesSplitter.parseMap('${logsearch.web.service_logs.component.labels:" + SERVICE_COMPONENT_LABELS_DEFAULTS + "}')}")
  @LogSearchPropertyDescription(
    name = "logsearch.web.service_logs.component.labels",
    description = "Map of serivce component labels.",
    examples = {"ambari_agent:Ambari Agent,ambari_server:Ambari Servcer"},
    defaultValue = SERVICE_COMPONENT_LABELS_DEFAULTS,
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private Map<String, String> serviceComponentLabels;

  @Value("#{propertiesSplitter.parseMap('${logsearch.web.service_logs.field.labels:" + SERVICE_FIELD_LABELS_DEFAULTS + "}')}")
  @LogSearchPropertyDescription(
    name = "logsearch.web.service_logs.field.labels",
    description = "Map of serivce field labels.",
    examples = {"log_message:Message,ip:IP Address"},
    defaultValue = SERVICE_FIELD_LABELS_DEFAULTS,
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private Map<String, String> serviceFieldLabels;

  @Value("#{propertiesSplitter.parseList('${logsearch.web.service_logs.field.excludes:" + SERVICE_FIELD_EXCLUDES_DEFAULTS + "}')}")
  @LogSearchPropertyDescription(
    name = "logsearch.web.service_logs.field.excludes",
    description = "List of fields that will be excluded from metadata schema responses.",
    examples = {"seq_num,tag"},
    defaultValue = SERVICE_FIELD_EXCLUDES_DEFAULTS,
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private List<String> serviceFieldExcludeList;

  @Value("#{propertiesSplitter.parseList('${logsearch.web.service_logs.field.visible:" + SERVICE_FIELD_VISIBLE_DEFAULTS + "}')}")
  @LogSearchPropertyDescription(
    name = "logsearch.web.service_logs.field.visible",
    description = "List of fields that will be displayed by default on the UI.",
    examples = {"log_message,path,logtime"},
    defaultValue = SERVICE_FIELD_VISIBLE_DEFAULTS,
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private List<String> serviceFieldVisibleList;

  @Value("#{propertiesSplitter.parseList('${logsearch.web.service_logs.field.filterable.excludes:" + SERVICE_FIELD_FILTERABLE_EXLUDE_DEFAULTS + "}')}")
  @LogSearchPropertyDescription(
    name = "logsearch.web.service_logs.field.filterable.excludes",
    description = "List of fields that will be excluded from filter selection on the UI.",
    examples = {"path,method,logger_name"},
    defaultValue = SERVICE_FIELD_FILTERABLE_EXLUDE_DEFAULTS,
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private List<String> serviceFieldFilterableExcludesList;

  @Value("#{propertiesSplitter.parseMap('${logsearch.web.audit_logs.component.labels:" + AUDIT_COMPONENT_LABELS_DEFAULTS + "}')}")
  @LogSearchPropertyDescription(
    name = "logsearch.web.audit_logs.component.labels",
    description = "Map of component component labels.",
    examples = {"ambari:Ambari,RangerAudit:ranger"},
    defaultValue = AUDIT_COMPONENT_LABELS_DEFAULTS,
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private Map<String, String> auditComponentLabels;

  @Value("#{propertiesSplitter.parseMapInMap('${logsearch.web.audit_logs.field.labels:" + AUDIT_FIELD_LABELS_DEFAULTS + "}')}")
  @LogSearchPropertyDescription(
    name = "logsearch.web.audit_logs.field.labels",
    description = "Map of fields (key-value pairs) labels for different component types.",
    examples = {"ambari#reqUser:Ambari User,ws_response:Response;RangerAudit#reqUser:Req User"},
    defaultValue = AUDIT_FIELD_LABELS_DEFAULTS,
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private Map<String, Map<String, String>> auditFieldLabels;

  @Value("#{propertiesSplitter.parseMap('${logsearch.web.audit_logs.field.common.labels:" + AUDIT_FIELD_COMMON_LABELS_DEFAULTS + "}')}")
  @LogSearchPropertyDescription(
    name = "logsearch.web.audit_logs.field.common.labels",
    description = "Map of fields labels for audits (common).",
    examples = {"reqUser:Req User,resp:Response"},
    defaultValue = AUDIT_FIELD_COMMON_LABELS_DEFAULTS,
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private Map<String, String> auditFieldCommonLabels;

  @Value("#{propertiesSplitter.parseListInMap('${logsearch.web.audit_logs.field.visible:" + AUDIT_FIELD_VISIBLE_DEFAULTS + "}')}")
  @LogSearchPropertyDescription(
    name = "logsearch.web.audit_logs.field.visible",
    description = "List of fields that will be displayed by default on the UI for different audit components.",
    examples = {"ambari:reqUser,resp;RangerAudit:reqUser,repo"},
    defaultValue = AUDIT_FIELD_VISIBLE_DEFAULTS,
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private Map<String, List<String>> auditFieldVisibleleMap;

  @Value("#{propertiesSplitter.parseList('${logsearch.web.audit_logs.field.common.visible:" + AUDIT_FIELD_VISIBLE_COMMON_DEFAULTS + "}')}")
  @LogSearchPropertyDescription(
    name = "logsearch.web.audit_logs.field.common.visible",
    description = "List of fields that will be displayed by default on the UI for every audit components.",
    examples = {"reqUser,resp"},
    defaultValue = AUDIT_FIELD_VISIBLE_COMMON_DEFAULTS,
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private List<String> auditFieldCommonVisibleList;

  @Value("#{propertiesSplitter.parseListInMap('${logsearch.web.audit_logs.field.excludes:" + AUDIT_FIELD_EXCLUDES_DEFAULTS + "}')}")
  @LogSearchPropertyDescription(
    name = "logsearch.web.audit_logs.field.excludes",
    description = "List of fields that will be excluded from metadata schema responses for different audit components.",
    examples = {"ambari:reqUser,resp,hdfs:ws_user,ws_role"},
    defaultValue = AUDIT_FIELD_EXCLUDES_DEFAULTS,
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private Map<String, List<String>> auditFieldExcludeMap;

  @Value("#{propertiesSplitter.parseList('${logsearch.web.audit_logs.field.common.excludes:" + AUDIT_FIELD_EXCLUDES_COMMON_DEFAULTS + "}')}")
  @LogSearchPropertyDescription(
    name = "logsearch.web.audit_logs.field.common.excludes",
    description = "List of fields that will be excluded from metadata schema responses for every audit components.",
    examples = {"reqUser,resp,tag_str"},
    defaultValue = AUDIT_FIELD_EXCLUDES_COMMON_DEFAULTS,
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private List<String> auditFieldCommonExcludeList;

  @Value("#{propertiesSplitter.parseListInMap('${logsearch.web.audit_logs.field.filterable.excludes:" + AUDIT_FIELD_FILTERABLE_EXCLUDES_DEFAULTS + "}')}")
  @LogSearchPropertyDescription(
    name = "logsearch.web.audit_logs.field.filterable.excludes",
    description = "List of fields that will be excluded from filter selection on the UI for different audit components.",
    examples = {"ambari:tag_str,resp,tag_str;RangerAudit:path,ip"},
    defaultValue = AUDIT_FIELD_FILTERABLE_EXCLUDES_DEFAULTS,
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private Map<String, List<String>> auditFieldFilterableExcludeMap;

  @Value("#{propertiesSplitter.parseList('${logsearch.web.audit_logs.field.common.filterable.common.excludes:" + AUDIT_FIELD_FILTERABLE_EXCLUDES_COMMON_DEFAULTS + "}')}")
  @LogSearchPropertyDescription(
    name = "logsearch.web.audit_logs.field.common.filterable.common.excludes",
    description = "List of fields that will be excluded from filter selection on the UI for every audit components.",
    examples = {"tag_str,resp,tag_str"},
    defaultValue = AUDIT_FIELD_FILTERABLE_EXCLUDES_COMMON_DEFAULTS,
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private List<String> auditFieldCommonFilterableExcludeList;

  @Value("${logsearch.web.labels.fallback.enabled:true}")
  @LogSearchPropertyDescription(
    name = "logsearch.web.audit_logs.field.filterable.excludes",
    description = "Enable label fallback. (replace _ with spaces and capitalize properly)",
    examples = {"false"},
    defaultValue = "true",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private boolean labelFallbackEnabled;

  @Value("#{propertiesSplitter.parseList('${logsearch.web.labels.service_logs.field.fallback.prefixes:" + SERVICE_FIELD_FALLBACK_PREFIX_DEFAULTS +"}')}")
  @LogSearchPropertyDescription(
    name = "logsearch.web.labels.service_logs.field.fallback.prefixes",
    description = "List of prefixes that should be removed during fallback of service field labels.",
    examples = {"ws_,std_,sdi_"},
    defaultValue = SERVICE_FIELD_FALLBACK_PREFIX_DEFAULTS,
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private List<String> serviceFieldFallbackPrefixes;

  @Value("#{propertiesSplitter.parseList('${logsearch.web.labels.audit_logs.field.fallback.prefixes:" + AUDIT_FIELD_FALLBACK_PREFIX_DEFAULTS + "}')}")
  @LogSearchPropertyDescription(
    name = "logsearch.web.labels.service_logs.field.fallback.prefixes",
    description = "List of prefixes that should be removed during fallback of audit field labels.",
    examples = {"ws_,std_,sdi_"},
    defaultValue = AUDIT_FIELD_FALLBACK_PREFIX_DEFAULTS,
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private List<String> auditFieldFallbackPrefixes;

  @Value("#{propertiesSplitter.parseList('${logsearch.web.labels.service_logs.field.fallback.suffixes:" + SERVICE_FIELD_FALLBACK_PREFIX_DEFAULTS +"}')}")
  @LogSearchPropertyDescription(
    name = "logsearch.web.labels.service_logs.field.fallback.suffixes",
    description = "List of suffixes that should be removed during fallback of service field labels.",
    examples = {"_i,_l,_s,_b"},
    defaultValue = SERVICE_FIELD_FALLBACK_SUFFIX_DEFAULTS,
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private List<String> serviceFieldFallbackSuffixes;

  @Value("#{propertiesSplitter.parseList('${logsearch.web.labels.audit_logs.field.fallback.suffixes:" + AUDIT_FIELD_FALLBACK_PREFIX_DEFAULTS + "}')}")
  @LogSearchPropertyDescription(
    name = "logsearch.web.labels.service_logs.field.fallback.suffixes",
    description = "List of suffixes that should be removed during fallback of audit field labels.",
    examples = {"_i,_l,_s,_b"},
    defaultValue = AUDIT_FIELD_FALLBACK_SUFFIX_DEFAULTS,
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private List<String> auditFieldFallbackSuffixes;

  private final Map<String, Map<String, String>> mergedAuditFieldLabelMap = new HashMap<>();

  private final Map<String, List<String>> mergedAuditFieldVisibleMap = new HashMap<>();

  private final Map<String, List<String>> mergedAuditFieldExcludeMap = new HashMap<>();

  private final Map<String, List<String>> mergedAuditFieldFilterableExcludesMap = new HashMap<>();

  public Map<String, String> getServiceGroupLabels() {
    return serviceGroupLabels;
  }

  public void setServiceGroupLabels(Map<String, String> serviceGroupLabels) {
    this.serviceGroupLabels = serviceGroupLabels;
  }

  public Map<String, String> getServiceComponentLabels() {
    return serviceComponentLabels;
  }

  public void setServiceComponentLabels(Map<String, String> serviceComponentLabels) {
    this.serviceComponentLabels = serviceComponentLabels;
  }

  public Map<String, String> getAuditComponentLabels() {
    return auditComponentLabels;
  }

  public void setAuditComponentLabels(Map<String, String> auditComponentLabels) {
    this.auditComponentLabels = auditComponentLabels;
  }

  public Map<String, String> getServiceFieldLabels() {
    return serviceFieldLabels;
  }

  public void setServiceFieldLabels(Map<String, String> serviceFieldLabels) {
    this.serviceFieldLabels = serviceFieldLabels;
  }

  public Map<String, Map<String, String>> getAuditFieldLabels() {
    return auditFieldLabels;
  }

  public void setAuditFieldLabels(Map<String, Map<String, String>> auditFieldLabels) {
    this.auditFieldLabels = auditFieldLabels;
  }

  public List<String> getServiceFieldExcludeList() {
    return serviceFieldExcludeList;
  }

  public void setServiceFieldExcludeList(List<String> serviceFieldExcludeList) {
    this.serviceFieldExcludeList = serviceFieldExcludeList;
  }

  public List<String> getServiceFieldVisibleList() {
    return serviceFieldVisibleList;
  }

  public void setServiceFieldVisibleList(List<String> serviceFieldVisibleList) {
    this.serviceFieldVisibleList = serviceFieldVisibleList;
  }

  public Map<String, List<String>> getAuditFieldVisibleleMap() {
    return auditFieldVisibleleMap;
  }

  public void setAuditFieldVisibleleMap(Map<String, List<String>> auditFieldVisibleleMap) {
    this.auditFieldVisibleleMap = auditFieldVisibleleMap;
  }

  public List<String> getAuditFieldCommonVisibleList() {
    return auditFieldCommonVisibleList;
  }

  public void setAuditFieldCommonVisibleList(List<String> auditFieldCommonVisibleList) {
    this.auditFieldCommonVisibleList = auditFieldCommonVisibleList;
  }

  public Map<String, List<String>> getAuditFieldExcludeMap() {
    return auditFieldExcludeMap;
  }

  public void setAuditFieldExcludeMap(Map<String, List<String>> auditFieldExcludeMap) {
    this.auditFieldExcludeMap = auditFieldExcludeMap;
  }

  public List<String> getAuditFieldCommonExcludeList() {
    return auditFieldCommonExcludeList;
  }

  public void setAuditFieldCommonExcludeList(List<String> auditFieldCommonExcludeList) {
    this.auditFieldCommonExcludeList = auditFieldCommonExcludeList;
  }

  public Map<String, String> getAuditFieldCommonLabels() {
    return auditFieldCommonLabels;
  }

  public void setAuditFieldCommonLabels(Map<String, String> auditFieldCommonLabels) {
    this.auditFieldCommonLabels = auditFieldCommonLabels;
  }

  public boolean isLabelFallbackEnabled() {
    return labelFallbackEnabled;
  }

  public void setLabelFallbackEnabled(boolean labelFallbackEnabled) {
    this.labelFallbackEnabled = labelFallbackEnabled;
  }

  public List<String> getServiceFieldFallbackPrefixes() {
    return serviceFieldFallbackPrefixes;
  }

  public void setServiceFieldFallbackPrefixes(List<String> serviceFieldFallbackPrefixes) {
    this.serviceFieldFallbackPrefixes = serviceFieldFallbackPrefixes;
  }

  public List<String> getAuditFieldFallbackPrefixes() {
    return auditFieldFallbackPrefixes;
  }

  public void setAuditFieldFallbackPrefixes(List<String> auditFieldFallbackPrefixes) {
    this.auditFieldFallbackPrefixes = auditFieldFallbackPrefixes;
  }

  public List<String> getServiceFieldFilterableExcludesList() {
    return serviceFieldFilterableExcludesList;
  }

  public void setServiceFieldFilterableExcludesList(List<String> serviceFieldFilterableExcludesList) {
    this.serviceFieldFilterableExcludesList = serviceFieldFilterableExcludesList;
  }

  public List<String> getServiceFieldFallbackSuffixes() {
    return serviceFieldFallbackSuffixes;
  }

  public void setServiceFieldFallbackSuffixes(List<String> serviceFieldFallbackSuffixes) {
    this.serviceFieldFallbackSuffixes = serviceFieldFallbackSuffixes;
  }

  public List<String> getAuditFieldFallbackSuffixes() {
    return auditFieldFallbackSuffixes;
  }

  public void setAuditFieldFallbackSuffixes(List<String> auditFieldFallbackSuffixes) {
    this.auditFieldFallbackSuffixes = auditFieldFallbackSuffixes;
  }

  public Map<String, List<String>> getMergedAuditFieldVisibleMap() {
    return mergedAuditFieldVisibleMap;
  }

  public Map<String, List<String>> getMergedAuditFieldExcludeMap() {
    return mergedAuditFieldExcludeMap;
  }

  public Map<String, Map<String, String>> getMergedAuditFieldLabelMap() {
    return mergedAuditFieldLabelMap;
  }

  public Map<String, List<String>> getMergedAuditFieldFilterableExcludesMap() {
    return mergedAuditFieldFilterableExcludesMap;
  }

  @PostConstruct
  public void init() {
    mergeCommonAndSpecMapValues(auditFieldLabels, auditFieldCommonLabels, mergedAuditFieldLabelMap);
    mergeCommonAndSpecListValues(auditFieldVisibleleMap, auditFieldCommonVisibleList, mergedAuditFieldVisibleMap);
    mergeCommonAndSpecListValues(auditFieldExcludeMap, auditFieldCommonExcludeList, mergedAuditFieldExcludeMap);
    mergeCommonAndSpecListValues(auditFieldFilterableExcludeMap, auditFieldCommonFilterableExcludeList, mergedAuditFieldFilterableExcludesMap);
  }

  private void mergeCommonAndSpecListValues(Map<String, List<String>> specMap, List<String> commonList,
                                            Map<String, List<String>> mergedMap) {
    Set<String> componentFilterableKeys = specMap.keySet();
    for (String component : componentFilterableKeys) {
      List<String> specAuditDataList = specMap.get(component);
      List<String> mergedDataList = new ArrayList<>();
      if (specAuditDataList != null) {
        mergedDataList.addAll(specAuditDataList);
        for (String commonData : commonList) {
          if (!specAuditDataList.contains(commonData)) {
            mergedDataList.add(commonData);
          }
        }
        mergedMap.put(component, mergedDataList);
      }
    }
  }

  private void mergeCommonAndSpecMapValues(Map<String, Map<String, String>> specMap, Map<String, String> commonMap,
                                           Map<String, Map<String, String>> mergedMap) {
    Set<String> componentFilterableKeys = specMap.keySet();
    for (String component : componentFilterableKeys) {
      Map<String, String> specAuditDataMap = specMap.get(component);
      Map<String, String> mergedAuditDataMap = new HashMap<>();
      if (specAuditDataMap != null) {
        mergedAuditDataMap.putAll(specAuditDataMap);
        for (Map.Entry<String, String> entry : commonMap.entrySet()) {
          if (!specAuditDataMap.containsKey(entry.getKey())) {
            mergedAuditDataMap.put(entry.getKey(), entry.getValue());
          }
        }
        mergedMap.put(component, mergedAuditDataMap);
      }
    }
  }

}
