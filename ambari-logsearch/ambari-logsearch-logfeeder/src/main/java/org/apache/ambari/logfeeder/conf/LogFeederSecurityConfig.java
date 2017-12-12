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
package org.apache.ambari.logfeeder.conf;

import org.apache.ambari.logfeeder.common.LogFeederConstants;
import org.apache.ambari.logsearch.config.api.LogSearchPropertyDescription;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import java.io.File;
import java.nio.charset.Charset;

public class LogFeederSecurityConfig {

  private static final Logger LOG = LoggerFactory.getLogger(LogFeederSecurityConfig.class);

  private static final String KEYSTORE_LOCATION_ARG = "javax.net.ssl.keyStore";
  private static final String TRUSTSTORE_LOCATION_ARG = "javax.net.ssl.trustStore";
  private static final String KEYSTORE_TYPE_ARG = "javax.net.ssl.keyStoreType";
  private static final String TRUSTSTORE_TYPE_ARG = "javax.net.ssl.trustStoreType";
  private static final String KEYSTORE_PASSWORD_ARG = "javax.net.ssl.keyStorePassword";
  private static final String TRUSTSTORE_PASSWORD_ARG = "javax.net.ssl.trustStorePassword";
  private static final String KEYSTORE_PASSWORD_PROPERTY_NAME = "logfeeder_keystore_password";
  private static final String TRUSTSTORE_PASSWORD_PROPERTY_NAME = "logfeeder_truststore_password";
  private static final String KEYSTORE_PASSWORD_FILE = "ks_pass.txt";
  private static final String TRUSTSTORE_PASSWORD_FILE = "ts_pass.txt";

  private static final String LOGFEEDER_CERT_DEFAULT_FOLDER = "/etc/ambari-logsearch-portal/conf/keys";
  private static final String LOGFEEDER_STORE_DEFAULT_PASSWORD = "bigdata";

  private static final String CREDENTIAL_STORE_PROVIDER_PATH_PROPERTY = "hadoop.security.credential.provider.path";

  @LogSearchPropertyDescription(
    name = CREDENTIAL_STORE_PROVIDER_PATH_PROPERTY,
    description = "The jceks file that provides passwords.",
    examples = {"jceks://file/etc/ambari-logsearch-logfeeder/conf/logfeeder.jceks"},
    sources = {LogFeederConstants.LOGFEEDER_PROPERTIES_FILE}
  )
  @Value("${"+ CREDENTIAL_STORE_PROVIDER_PATH_PROPERTY + ":}")
  private String credentialStoreProviderPath;

  @LogSearchPropertyDescription(
    name = LogFeederConstants.SOLR_JAAS_FILE_PROPERTY,
    description = "The jaas file used for solr.",
    examples = {"/etc/ambari-logsearch-logfeeder/conf/logfeeder_jaas.conf"},
    defaultValue = LogFeederConstants.DEFAULT_SOLR_JAAS_FILE,
    sources = {LogFeederConstants.LOGFEEDER_PROPERTIES_FILE}
  )
  @Value("${" + LogFeederConstants.SOLR_JAAS_FILE_PROPERTY + ":" + LogFeederConstants.DEFAULT_SOLR_JAAS_FILE + "}")
  private String solrJaasFile;

  @LogSearchPropertyDescription(
    name = LogFeederConstants.SOLR_KERBEROS_ENABLE_PROPERTY,
    description = "Enables using kerberos for accessing solr.",
    examples = {"true"},
    defaultValue = LogFeederConstants.DEFAULT_SOLR_KERBEROS_ENABLE + "",
    sources = {LogFeederConstants.LOGFEEDER_PROPERTIES_FILE}
  )
  @Value("${"+ LogFeederConstants.SOLR_KERBEROS_ENABLE_PROPERTY + ":" + LogFeederConstants.DEFAULT_SOLR_KERBEROS_ENABLE + "}")
  private Boolean solrKerberosEnabled;

  public String getKeyStoreLocation() {
    return System.getProperty(KEYSTORE_LOCATION_ARG);
  }

  public String getKeyStoreType() {
    return System.getProperty(KEYSTORE_TYPE_ARG);
  }

  public String getKeyStorePassword() {
    return System.getProperty(KEYSTORE_PASSWORD_ARG);
  }

  public String getTrustStoreLocation() {
    return System.getProperty(TRUSTSTORE_LOCATION_ARG);
  }

  public String getTrustStoreType() {
    return System.getProperty(TRUSTSTORE_TYPE_ARG);
  }

  public String getTrustStorePassword() {
    return System.getProperty(TRUSTSTORE_PASSWORD_ARG);
  }

  public String getCredentialStoreProviderPath() {
    return credentialStoreProviderPath;
  }

  public void setCredentialStoreProviderPath(String credentialStoreProviderPath) {
    this.credentialStoreProviderPath = credentialStoreProviderPath;
  }

  public String getSolrJaasFile() {
    return solrJaasFile;
  }

  public void setSolrJaasFile(String solrJaasFile) {
    this.solrJaasFile = solrJaasFile;
  }

  public boolean isSolrKerberosEnabled() {
    return solrKerberosEnabled;
  }

  public void setSolrKerberosEnabled(Boolean solrKerberosEnabled) {
    this.solrKerberosEnabled = solrKerberosEnabled;
  }

  @PostConstruct
  public void ensureStorePasswords() {
    ensureStorePassword(KEYSTORE_LOCATION_ARG, KEYSTORE_PASSWORD_ARG, KEYSTORE_PASSWORD_PROPERTY_NAME, KEYSTORE_PASSWORD_FILE);
    ensureStorePassword(TRUSTSTORE_LOCATION_ARG, TRUSTSTORE_PASSWORD_ARG, TRUSTSTORE_PASSWORD_PROPERTY_NAME, TRUSTSTORE_PASSWORD_FILE);
  }

  private void ensureStorePassword(String locationArg, String pwdArg, String propertyName, String fileName) {
    if (StringUtils.isNotEmpty(System.getProperty(locationArg)) && StringUtils.isEmpty(System.getProperty(pwdArg))) {
      String password = getPassword(propertyName, fileName);
      System.setProperty(pwdArg, password);
    }
  }

  private String getPassword(String propertyName, String fileName) {
    String credentialStorePassword = getPasswordFromCredentialStore(propertyName);
    if (credentialStorePassword != null) {
      return credentialStorePassword;
    }

    String filePassword = getPasswordFromFile(fileName);
    if (filePassword != null) {
      return filePassword;
    }

    return LOGFEEDER_STORE_DEFAULT_PASSWORD;
  }

  private String getPasswordFromCredentialStore(String propertyName) {
    try {
      if (StringUtils.isEmpty(credentialStoreProviderPath)) {
        return null;
      }

      org.apache.hadoop.conf.Configuration config = new org.apache.hadoop.conf.Configuration();
      config.set(CREDENTIAL_STORE_PROVIDER_PATH_PROPERTY, credentialStoreProviderPath);
      char[] passwordChars = config.getPassword(propertyName);
      return (ArrayUtils.isNotEmpty(passwordChars)) ? new String(passwordChars) : null;
    } catch (Exception e) {
      LOG.warn(String.format("Could not load password %s from credential store, using default password", propertyName));
      return null;
    }
  }

  private String getPasswordFromFile(String fileName) {
    try {
      File pwdFile = new File(LOGFEEDER_CERT_DEFAULT_FOLDER, fileName);
      if (!pwdFile.exists()) {
        FileUtils.writeStringToFile(pwdFile, LOGFEEDER_STORE_DEFAULT_PASSWORD, Charset.defaultCharset());
        return LOGFEEDER_STORE_DEFAULT_PASSWORD;
      } else {
        return FileUtils.readFileToString(pwdFile, Charset.defaultCharset());
      }
    } catch (Exception e) {
      LOG.warn("Exception occurred during read/write password file for keystore/truststore.", e);
      return null;
    }
  }

}
