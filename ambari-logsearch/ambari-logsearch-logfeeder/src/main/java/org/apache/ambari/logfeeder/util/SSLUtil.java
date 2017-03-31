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

package org.apache.ambari.logfeeder.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.log4j.Logger;

import java.io.File;

public class SSLUtil {
  private static final Logger LOG = Logger.getLogger(SSLUtil.class);

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

  private static final String CREDENTIAL_STORE_PROVIDER_PATH = "hadoop.security.credential.provider.path";
  private static final String LOGFEEDER_CERT_DEFAULT_FOLDER = "/etc/ambari-logsearch-portal/conf/keys";
  private static final String LOGFEEDER_STORE_DEFAULT_PASSWORD = "bigdata";
  
  private SSLUtil() {
    throw new UnsupportedOperationException();
  }
  
  public static String getKeyStoreLocation() {
    return System.getProperty(KEYSTORE_LOCATION_ARG);
  }
  
  public static String getKeyStoreType() {
    return System.getProperty(KEYSTORE_TYPE_ARG);
  }
  
  public static String getKeyStorePassword() {
    return System.getProperty(KEYSTORE_PASSWORD_ARG);
  }
  
  public static String getTrustStoreLocation() {
    return System.getProperty(TRUSTSTORE_LOCATION_ARG);
  }
  
  public static String getTrustStoreType() {
    return System.getProperty(TRUSTSTORE_TYPE_ARG);
  }
  
  public static String getTrustStorePassword() {
    return System.getProperty(TRUSTSTORE_PASSWORD_ARG);
  }
  
  public static void ensureStorePasswords() {
    ensureStorePassword(KEYSTORE_LOCATION_ARG, KEYSTORE_PASSWORD_ARG, KEYSTORE_PASSWORD_PROPERTY_NAME, KEYSTORE_PASSWORD_FILE);
    ensureStorePassword(TRUSTSTORE_LOCATION_ARG, TRUSTSTORE_PASSWORD_ARG, TRUSTSTORE_PASSWORD_PROPERTY_NAME, TRUSTSTORE_PASSWORD_FILE);
  }
  
  private static void ensureStorePassword(String locationArg, String pwdArg, String propertyName, String fileName) {
    if (StringUtils.isNotEmpty(System.getProperty(locationArg)) && StringUtils.isEmpty(System.getProperty(pwdArg))) {
      String password = getPassword(propertyName, fileName);
      System.setProperty(pwdArg, password);
    }
  }

  private static String getPassword(String propertyName, String fileName) {
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
  
  private static String getPasswordFromCredentialStore(String propertyName) {
    try {
      String providerPath = LogFeederUtil.getStringProperty(CREDENTIAL_STORE_PROVIDER_PATH);
      if (providerPath == null) {
        return null;
      }
      
      Configuration config = new Configuration();
      config.set(CREDENTIAL_STORE_PROVIDER_PATH, providerPath);
      char[] passwordChars = config.getPassword(propertyName);
      return (ArrayUtils.isNotEmpty(passwordChars)) ? new String(passwordChars) : null;
    } catch (Exception e) {
      LOG.warn(String.format("Could not load password %s from credential store, using default password", propertyName));
      return null;
    }
  }

  private static String getPasswordFromFile(String fileName) {
    try {
      File pwdFile = new File(LOGFEEDER_CERT_DEFAULT_FOLDER, fileName);
      if (!pwdFile.exists()) {
        FileUtils.writeStringToFile(pwdFile, LOGFEEDER_STORE_DEFAULT_PASSWORD);
        return LOGFEEDER_STORE_DEFAULT_PASSWORD;
      } else {
        return FileUtils.readFileToString(pwdFile);
      }
    } catch (Exception e) {
      LOG.warn("Exception occurred during read/write password file for keystore/truststore.", e);
      return null;
    }
  }

}
