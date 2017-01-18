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

import java.io.File;

public class SSLUtil {
  private static final String KEYSTORE_LOCATION_ARG = "javax.net.ssl.keyStore";
  private static final String TRUSTSTORE_LOCATION_ARG = "javax.net.ssl.trustStore";
  private static final String KEYSTORE_PASSWORD_ARG = "javax.net.ssl.keyStorePassword";
  private static final String TRUSTSTORE_PASSWORD_ARG = "javax.net.ssl.trustStorePassword";
  private static final String KEYSTORE_PASSWORD_FILE = "ks_pass.txt";
  private static final String TRUSTSTORE_PASSWORD_FILE = "ts_pass.txt";
  
  private static final String LOGFEEDER_CERT_DEFAULT_FOLDER = "/etc/ambari-logsearch-portal/conf/keys";
  private static final String LOGFEEDER_STORE_DEFAULT_PASSWORD = "bigdata";
  
  private SSLUtil() {
    throw new UnsupportedOperationException();
  }
  
  public static void ensureStorePasswords() {
    ensureStorePassword(KEYSTORE_LOCATION_ARG, KEYSTORE_PASSWORD_ARG, KEYSTORE_PASSWORD_FILE);
    ensureStorePassword(TRUSTSTORE_LOCATION_ARG, TRUSTSTORE_PASSWORD_ARG, TRUSTSTORE_PASSWORD_FILE);
  }
  
  private static void ensureStorePassword(String locationArg, String pwdArg, String pwdFile) {
    if (StringUtils.isNotEmpty(System.getProperty(locationArg)) && StringUtils.isEmpty(System.getProperty(pwdArg))) {
      String password = getPasswordFromFile(pwdFile);
      System.setProperty(pwdArg, password);
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
      throw new RuntimeException("Exception occurred during read/write password file for keystore/truststore.", e);
    }
  }

}
