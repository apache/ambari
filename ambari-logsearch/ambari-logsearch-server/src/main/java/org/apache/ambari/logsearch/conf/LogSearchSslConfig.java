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
public class LogSearchSslConfig {

  public static final String LOGSEARCH_CERT_DEFAULT_FOLDER = "/etc/ambari-logsearch-portal/conf/keys";
  public static final String LOGSEARCH_CERT_DEFAULT_ALGORITHM = "sha256WithRSA";
  public static final String CREDENTIAL_STORE_PROVIDER_PATH = "hadoop.security.credential.provider.path";

  @LogSearchPropertyDescription(
    name = "logsearch.cert.algorithm",
    description = "Algorithm to generate certificates for SSL (if needed).",
    examples = {"sha256WithRSA"},
    defaultValue = LOGSEARCH_CERT_DEFAULT_ALGORITHM,
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  @Value("${logsearch.cert.algorithm:" + LOGSEARCH_CERT_DEFAULT_ALGORITHM + "}")
  private String certAlgorithm;

  @LogSearchPropertyDescription(
    name = "logsearch.cert.folder.location",
    description = "Folder where the generated certificates (SSL) will be located. Make sure the user of Log Search Server can access it.",
    examples = {"/etc/mypath/keys"},
    defaultValue = LOGSEARCH_CERT_DEFAULT_FOLDER,
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  @Value("${logsearch.cert.folder.location:" + LOGSEARCH_CERT_DEFAULT_FOLDER + "}")
  private String certFolder;

  @LogSearchPropertyDescription(
    name = CREDENTIAL_STORE_PROVIDER_PATH,
    description = "Path to interrogate for protected credentials. (see: https://hadoop.apache.org/docs/current/hadoop-project-dist/hadoop-common/CredentialProviderAPI.html)",
    examples = {"localjceks://file/home/mypath/my.jceks"},
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  @Value("${hadoop.security.credential.provider.path:}")
  private String credentialStoreProviderPath;

  public String getCertAlgorithm() {
    return certAlgorithm;
  }

  public void setCertAlgorithm(String certAlgorithm) {
    this.certAlgorithm = certAlgorithm;
  }

  public String getCertFolder() {
    return certFolder;
  }

  public void setCertFolder(String certFolder) {
    this.certFolder = certFolder;
  }

  public String getCredentialStoreProviderPath() {
    return credentialStoreProviderPath;
  }

  public void setCredentialStoreProviderPath(String credentialStoreProviderPath) {
    this.credentialStoreProviderPath = credentialStoreProviderPath;
  }
}
