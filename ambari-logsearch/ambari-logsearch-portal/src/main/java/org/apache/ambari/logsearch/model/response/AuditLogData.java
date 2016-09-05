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
package org.apache.ambari.logsearch.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public interface AuditLogData extends CommonLogData {

  @JsonProperty("logType")
  String getLogType();

  void setLogType(String logType);

  @JsonProperty("policy")
  String getPolicy();

  void setPolicy(String policy);

  @JsonProperty("access")
  String getAccess();

  void setAccess(String access);

  @JsonProperty("action")
  String getAction();

  void setAction(String action);

  @JsonProperty("agent")
  String getAgent();

  void setAgent(String agent);

  @JsonProperty("agentHost")
  String getAgentHost();

  void setAgentHost(String agentHost);

  @JsonProperty("cliIP")
  String getClientIp();

  void setClientIp(String clientIp);

  @JsonProperty("cliType")
  String getClientType();

  public void setClientType(String clientType);

  @JsonProperty("reqContext")
  String getRequestContext();

  void setRequestContext(String requestContext);

  @JsonProperty("enforcer")
  String getEnforcer();

  void setEnforcer(String enforcer);

  @JsonProperty("evtTime")
  Date getEventTime();

  void setEventTime(Date eventTime);

  @JsonProperty("reason")
  String getReason();

  void setReason(String reason);

  @JsonProperty("proxyUsers")
  List<String> getProxyUsers();

  void setProxyUsers(List<String> proxyUsers);

  @JsonProperty("repo")
  String getRepo();

  void setRepo(String repo);

  @JsonProperty("repoType")
  String getRepoType();

  void setRepoType(String repoType);

  @JsonProperty("reqData")
  String getRequestData();

  void setRequestData(String requestData);

  @JsonProperty("reqUser")
  String getRequestUser();

  void setRequestUser(String requestUser);

  @JsonProperty("resType")
  String getResponseType();

  void setResponseType(String requestType);

  @JsonProperty("resource")
  String getResource();

  void setResource(String resource);

  @JsonProperty("result")
  Integer getResult();

  void setResult(Integer result);

  @JsonProperty("sess")
  String getSession();

  void setSession(String session);

  @JsonProperty("tags")
  List<String> getTags();

  void setTags(List<String> tags);

  @JsonProperty("tags_str")
  String getTagsStr();

  void setTagsStr(String tagsStr);

  @JsonProperty("text")
  String getText();

  void setText(String text);
}
