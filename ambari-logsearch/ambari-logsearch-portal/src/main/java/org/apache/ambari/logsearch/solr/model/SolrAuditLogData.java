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
package org.apache.ambari.logsearch.solr.model;

import org.apache.ambari.logsearch.model.response.AuditLogData;
import org.apache.solr.client.solrj.beans.Field;

import java.util.Date;
import java.util.List;

import static org.apache.ambari.logsearch.solr.SolrConstants.AuditLogConstants.*;

public class SolrAuditLogData extends SolrCommonLogData implements AuditLogData {

  @Field(AUDIT_LOG_TYPE)
  private String logType;

  @Field(AUDIT_POLICY)
  private String policy;

  @Field(AUDIT_ACCESS)
  private String access;

  @Field(AUDIT_ACTION)
  private String action;

  @Field(AUDIT_AGENT)
  private String agent;

  @Field(AUDIT_AGENT_HOST)
  private String agentHost;

  @Field(AUDIT_CLIENT_IP)
  private String clientIp;

  @Field(AUDIT_CLIENT_TYPE)
  private String clientType;

  @Field(AUDIT_REQEST_CONTEXT)
  private String requestContext;

  @Field(AUDIT_ENFORCER)
  private String enforcer;

  @Field(AUDIT_EVTTIME)
  private Date eventTime;

  @Field(AUDIT_REASON)
  private String reason;

  @Field(AUDIT_PROXY_USERS)
  private List<String> proxyUsers;

  @Field(AUDIT_COMPONENT)
  private String repo;

  @Field(AUDIT_REPO_TYPE)
  private Integer repoType;

  @Field(AUDIT_REQEST_DATA)
  private String requestData;

  @Field(AUDIT_REQUEST_USER)
  private String requestUser;

  @Field(AUDIT_RESPONSE_TYPE)
  private String responseType;

  @Field(AUDIT_RESOURCE)
  private String resource;

  @Field(AUDIT_RESULT)
  private Integer result;

  @Field(AUDIT_SESSION)
  private String session;

  @Field(AUDIT_TAGS)
  private List<String> tags;

  @Field(AUDIT_TAGS_STR)
  private String tagsStr;

  @Field(AUDIT_TEXT)
  private String text;

  @Override
  public String getText() {
    return text;
  }

  @Override
  public void setText(String text) {
    this.text = text;
  }

  @Override
  public String getTagsStr() {
    return tagsStr;
  }

  @Override
  public void setTagsStr(String tagsStr) {
    this.tagsStr = tagsStr;
  }

  @Override
  public List<String> getTags() {
    return tags;
  }

  @Override
  public void setTags(List<String> tags) {
    this.tags = tags;
  }

  @Override
  public String getSession() {
    return session;
  }

  @Override
  public void setSession(String session) {
    this.session = session;
  }

  @Override
  public Integer getResult() {
    return result;
  }

  @Override
  public void setResult(Integer result) {
    this.result = result;
  }

  @Override
  public String getResource() {
    return resource;
  }

  @Override
  public void setResource(String resource) {
    this.resource = resource;
  }

  @Override
  public String getResponseType() {
    return responseType;
  }

  public void setResponseType(String responseType) {
    this.responseType = responseType;
  }

  @Override
  public String getRequestUser() {
    return requestUser;
  }

  @Override
  public void setRequestUser(String requestUser) {
    this.requestUser = requestUser;
  }

  @Override
  public String getRequestData() {
    return requestData;
  }

  @Override
  public void setRequestData(String requestData) {
    this.requestData = requestData;
  }

  @Override
  public Integer getRepoType() {
    return repoType;
  }

  @Override
  public void setRepoType(Integer repoType) {
    this.repoType = repoType;
  }

  @Override
  public String getRepo() {
    return repo;
  }

  @Override
  public void setRepo(String repo) {
    this.repo = repo;
  }

  @Override
  public List<String> getProxyUsers() {
    return proxyUsers;
  }

  @Override
  public void setProxyUsers(List<String> proxyUsers) {
    this.proxyUsers = proxyUsers;
  }

  @Override
  public String getReason() {
    return reason;
  }

  @Override
  public void setReason(String reason) {
    this.reason = reason;
  }

  @Override
  public Date getEventTime() {
    return eventTime;
  }

  @Override
  public void setEventTime(Date eventTime) {
    this.eventTime = eventTime;
  }

  @Override
  public String getEnforcer() {
    return enforcer;
  }

  @Override
  public void setEnforcer(String enforcer) {
    this.enforcer = enforcer;
  }

  @Override
  public String getRequestContext() {
    return requestContext;
  }

  @Override
  public void setRequestContext(String requestContext) {
    this.requestContext = requestContext;
  }

  @Override
  public String getClientType() {
    return clientType;
  }

  @Override
  public void setClientType(String clientType) {
    this.clientType = clientType;
  }

  @Override
  public String getClientIp() {
    return clientIp;
  }

  @Override
  public void setClientIp(String clientIp) {
    this.clientIp = clientIp;
  }

  @Override
  public String getAgent() {
    return agent;
  }

  @Override
  public void setAgent(String agent) {
    this.agent = agent;
  }

  @Override
  public String getAgentHost() {
    return agentHost;
  }

  @Override
  public void setAgentHost(String agentHost) {
    this.agentHost = agentHost;
  }

  @Override
  public String getAction() {
    return action;
  }

  @Override
  public void setAction(String action) {
    this.action = action;
  }

  @Override
  public String getAccess() {
    return access;
  }

  @Override
  public void setAccess(String access) {
    this.access = access;
  }

  @Override
  public String getPolicy() {
    return policy;
  }

  @Override
  public void setPolicy(String policy) {
    this.policy = policy;
  }

  @Override
  public String getLogType() {
    return logType;
  }

  @Override
  public void setLogType(String logType) {
    this.logType = logType;
  }
}
