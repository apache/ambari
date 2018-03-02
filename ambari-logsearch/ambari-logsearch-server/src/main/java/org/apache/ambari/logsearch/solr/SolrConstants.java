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
package org.apache.ambari.logsearch.solr;

public class SolrConstants {

  private SolrConstants() {
  }

  public class CommonLogConstants {
    private CommonLogConstants() {
    }

    public static final String ID = "id";
    public static final String SEQUENCE_ID = "seq_num";
    public static final String BUNDLE_ID = "bundle_id";
    public static final String CASE_ID = "case_id";
    public static final String CLUSTER = "cluster";
    public static final String LOG_MESSAGE = "log_message";
    public static final String LOGFILE_LINE_NUMBER = "logfile_line_number";
    public static final String EVENT_DURATION_MD5 = "event_dur_m5";
    public static final String FILE = "file";
    public static final String EVENT_COUNT = "event_count";
    public static final String EVENT_MD5 = "event_md5";
    public static final String MESSAGE_MD5= "message_md5";
    public static final String TTL = "_ttl_";
    public static final String EXPIRE_AT = "_expire_at_";
    public static final String VERSION = "_version_";
    public static final String ROUTER_FIELD = "_router_field_";
    public static final String TYPE = "type";
  }

  public class ServiceLogConstants {

    private ServiceLogConstants() {
    }

    public static final String BUNDLE_ID = "bundle_id";
    public static final String LOGTIME = "logtime";
    public static final String COMPONENT = "type";
    public static final String LOG_MESSAGE = "log_message";
    public static final String KEY_LOG_MESSAGE = "key_log_message";
    public static final String HOST = "host";
    public static final String GROUP = "group";
    public static final String LEVEL = "level";
    public static final String THREAD_NAME = "thread_name";
    public static final String LOGGER_NAME = "logger_name";
    public static final String LINE_NUMBER = "line_number";
    public static final String PATH = "path";
    public static final String IP = "ip";
    public static final String STORED_TOKEN_DYNAMIC_FIELDS = "std_*";
    public static final String KEY_DYNAMIC_FIELDS = "key_*";
    public static final String WS_DYNAMIC_FIELDS = "ws_*";
    public static final String SDI_DYNAMIC_FIELDS = "sdi_*";
  }

  public class AuditLogConstants {
    private AuditLogConstants() {
    }

    public static final String AUDIT_LOG_TYPE = "logType";
    public static final String AUDIT_POLICY = "policy";
    public static final String AUDIT_ACCESS = "access";
    public static final String AUDIT_ACTION = "action";
    public static final String AUDIT_AGENT = "agent";
    public static final String AUDIT_AGENT_HOST = "agentHost";
    public static final String AUDIT_CLIENT_IP = "cliIP";
    public static final String AUDIT_CLIENT_TYPE = "cliType";
    public static final String AUDIT_REQEST_CONTEXT = "reqContext";
    public static final String AUDIT_ENFORCER = "enforcer";
    public static final String AUDIT_REASON = "reason";
    public static final String AUDIT_PROXY_USERS = "proxyUsers";
    public static final String AUDIT_REPO_TYPE = "repoType";
    public static final String AUDIT_REQEST_DATA = "reqData";
    public static final String AUDIT_RESPONSE_TYPE = "resType";
    public static final String AUDIT_SESSION = "sess";
    public static final String AUDIT_TEXT = "text";
    public static final String AUDIT_RESULT = "result";
    public static final String AUDIT_COMPONENT = "repo";
    public static final String AUDIT_EVTTIME = "evtTime";
    public static final String AUDIT_REQUEST_USER = "reqUser";
    public static final String AUDIT_RESOURCE = "resource";
    public static final String AUDIT_TAGS = "tags";
    public static final String AUDIT_TAGS_STR = "tags_str";
  }

  public class EventHistoryConstants {
    private EventHistoryConstants() {
    }

    public static final String ID = "id";
    public static final String USER_NAME = "username";
    public static final String VALUES = "jsons";
    public static final String FILTER_NAME = "filtername";
    public static final String ROW_TYPE = "rowtype";
    public static final String SHARE_NAME_LIST = "share_username_list";
  }
}
