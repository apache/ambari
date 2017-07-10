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
package org.apache.ambari.infra.security;

import java.security.Principal;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.security.authentication.server.AuthenticationToken;
import org.apache.solr.common.params.MapSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.Utils;
import org.apache.solr.security.AuthorizationContext;
import org.apache.solr.security.AuthorizationContext.RequestType;
import org.apache.solr.security.AuthorizationResponse;
import org.junit.Test;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.apache.solr.common.util.Utils.makeMap;
import static org.junit.Assert.assertEquals;

public class InfraRuleBasedAuthorizationPluginTest {

  private static final String PERMISSIONS = "{" +
    "  user-host : {" +
    "    'infra-solr@EXAMPLE.COM': [hostname, hostname2]" +
    "  }," +
    "  user-role : {" +
    "    'infra-solr@EXAMPLE.COM': [admin]," +
    "    'logsearch@EXAMPLE.COM': [logsearch_role,dev]," +
    "    'logfeeder@EXAMPLE.COM': [logsearch_role,dev]," +
    "    'atlas@EXAMPLE.COM': [atlas_role, audit_role, dev]," +
    "    'knox@EXAMPLE.COM': [audit_role,dev]," +
    "    'hdfs@EXAMPLE.COM': [audit_role,dev]," +
    "    'hbase@EXAMPLE.COM': [audit_role,dev]," +
    "    'yarn@EXAMPLE.COM': [audit_role,dev]," +
    "    'knox@EXAMPLE.COM': [audit_role,dev]," +
    "    'kafka@EXAMPLE.COM': [audit_role,dev]," +
    "    'kms@EXAMPLE.COM': [audit_role,dev]," +
    "    'storm@EXAMPLE.COM': [audit_role,dev]," +
    "    'rangeradmin@EXAMPLE.COM':[ranger_role, audit_role, dev]" +
    "  }," +
    "  permissions : [" +
    "    {name:'collection-admin-read'," +
    "    role:null}," +
    "    {name:collection-admin-edit ," +
    "    role:[logsearch_role, atlas_role, ranger_role, admin]}," +
    "    {name:mycoll_update," +
    "      collection:mycoll," +
    "      path:'/*'," +
    "      role:[logsearch_role,admin]" +
    "    }," +
    "    {name:mycoll2_update," +
    "      collection:mycoll2," +
    "      path:'/*'," +
    "      role:[ranger_role, audit_role, admin]" +
    "    }," +
    "{name:read , role:dev }]}";

  @Test
  public void testPermissions() {
    int STATUS_OK = 200;
    int FORBIDDEN = 403;
    int PROMPT_FOR_CREDENTIALS = 401;

    checkRules(makeMap("resource", "/#",
      "httpMethod", "POST",
      "userPrincipal", "unknownuser",
      "collectionRequests", "freeforall" )
      , STATUS_OK);

    checkRules(makeMap("resource", "/update/json/docs",
      "httpMethod", "POST",
      "userPrincipal", "tim",
      "collectionRequests", "mycoll")
      , FORBIDDEN);

    checkRules(makeMap("resource", "/update/json/docs",
      "httpMethod", "POST",
      "userPrincipal", "logsearch",
      "collectionRequests", "mycoll")
      , STATUS_OK);

    checkRules(makeMap("resource", "/update/json/docs",
      "httpMethod", "GET",
      "userPrincipal", "rangeradmin",
      "collectionRequests", "mycoll")
      , FORBIDDEN);

    checkRules(makeMap("resource", "/update/json/docs",
      "httpMethod", "GET",
      "userPrincipal", "rangeradmin",
      "collectionRequests", "mycoll2")
      , STATUS_OK);

    checkRules(makeMap("resource", "/update/json/docs",
      "httpMethod", "GET",
      "userPrincipal", "logsearch",
      "collectionRequests", "mycoll2")
      , FORBIDDEN);

    checkRules(makeMap("resource", "/update/json/docs",
      "httpMethod", "POST",
      "userPrincipal", "kms",
      "collectionRequests", "mycoll2")
      , STATUS_OK);

    checkRules(makeMap("resource", "/admin/collections",
      "userPrincipal", "tim",
      "requestType", RequestType.ADMIN,
      "collectionRequests", null,
      "params", new MapSolrParams(singletonMap("action", "CREATE")))
      , FORBIDDEN);

    checkRules(makeMap("resource", "/admin/collections",
      "userPrincipal", null,
      "requestType", RequestType.ADMIN,
      "collectionRequests", null,
      "params", new MapSolrParams(singletonMap("action", "CREATE")))
      , PROMPT_FOR_CREDENTIALS);

    checkRules(makeMap("resource", "/admin/collections",
      "userPrincipal", "rangeradmin",
      "requestType", RequestType.ADMIN,
      "collectionRequests", null,
      "params", new MapSolrParams(singletonMap("action", "CREATE")))
      , STATUS_OK);

    checkRules(makeMap("resource", "/admin/collections",
      "userPrincipal", "kms",
      "requestType", RequestType.ADMIN,
      "collectionRequests", null,
      "params", new MapSolrParams(singletonMap("action", "CREATE")))
      , FORBIDDEN);

    checkRules(makeMap("resource", "/admin/collections",
      "userPrincipal", "kms",
      "requestType", RequestType.ADMIN,
      "collectionRequests", null,
      "params", new MapSolrParams(singletonMap("action", "LIST")))
      , STATUS_OK);

    checkRules(makeMap("resource", "/admin/collections",
      "userPrincipal", "rangeradmin",
      "requestType", RequestType.ADMIN,
      "collectionRequests", null,
      "params", new MapSolrParams(singletonMap("action", "LIST")))
      , STATUS_OK);
  }

  private void checkRules(Map<String, Object> values, int expected) {
    checkRules(values,expected,(Map) Utils.fromJSONString(PERMISSIONS));
  }

  private void checkRules(Map<String, Object> values, int expected, Map<String ,Object> permissions) {
    AuthorizationContext context = new MockAuthorizationContext(values);
    InfraRuleBasedAuthorizationPlugin plugin = new InfraRuleBasedAuthorizationPlugin();
    plugin.init(permissions);
    AuthorizationResponse authResp = plugin.authorize(context);
    assertEquals(expected, authResp.statusCode);
  }

  private static class MockAuthorizationContext extends AuthorizationContext {
    private final Map<String,Object> values;

    private MockAuthorizationContext(Map<String, Object> values) {
      this.values = values;
    }

    @Override
    public SolrParams getParams() {
      SolrParams params = (SolrParams) values.get("params");
      return params == null ?  new MapSolrParams(new HashMap<String, String>()) : params;
    }

    @Override
    public Principal getUserPrincipal() {
      Object userPrincipal = values.get("userPrincipal");
      return userPrincipal == null ? null :
        new AuthenticationToken(String.valueOf(userPrincipal), String.format("%s%s", String.valueOf(userPrincipal), "/hostname@EXAMPLE.COM"), "kerberos");
    }

    @Override
    public String getHttpHeader(String header) {
      return null;
    }

    @Override
    public Enumeration getHeaderNames() {
      return null;
    }

    @Override
    public String getRemoteAddr() {
      return null;
    }

    @Override
    public String getRemoteHost() {
      return null;
    }

    @Override
    public List<CollectionRequest> getCollectionRequests() {
      Object collectionRequests = values.get("collectionRequests");
      if (collectionRequests instanceof String) {
        return singletonList(new CollectionRequest((String)collectionRequests));
      }
      return (List<CollectionRequest>) collectionRequests;
    }

    @Override
    public RequestType getRequestType() {
      return (RequestType) values.get("requestType");
    }

    @Override
    public String getHttpMethod() {
      return (String) values.get("httpMethod");
    }

    @Override
    public String getResource() {
      return (String) values.get("resource");
    }

    @Override
    public Object getHandler() {
      return null;
    }
  }

}
