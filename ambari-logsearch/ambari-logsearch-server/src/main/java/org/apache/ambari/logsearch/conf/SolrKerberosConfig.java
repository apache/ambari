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

import static org.apache.ambari.logsearch.common.LogSearchConstants.LOGSEARCH_PROPERTIES_FILE;

@Configuration
public class SolrKerberosConfig {

  @Value("${logsearch.solr.jaas.file:/usr/lib/ambari-logsearch-portal/logsearch_solr_jaas.conf}")
  @LogSearchPropertyDescription(
    name = "logsearch.solr.jaas.file",
    description = "Path of the JAAS file for Kerberos based Solr Cloud authentication.",
    examples = {"/my/path/jaas_file.conf"},
    defaultValue = "/usr/lib/ambari-logsearch-portal/logsearch_solr_jaas.conf",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private String jaasFile;

  @Value("${logsearch.solr.kerberos.enable:false}")
  @LogSearchPropertyDescription(
    name = "logsearch.solr.kerberos.enable",
    description = "Enable Kerberos Authentication for Solr Cloud.",
    examples = {"true", "false"},
    defaultValue = "false",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private boolean enabled;

  public String getJaasFile() {
    return jaasFile;
  }

  public void setJaasFile(String jaasFile) {
    this.jaasFile = jaasFile;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }
}
