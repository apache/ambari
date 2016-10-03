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

package org.apache.ambari.logsearch.util;

import javax.net.ssl.SSLContext;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.jetty.util.ssl.SslContextFactory;

public class SSLUtil {
  private static final Logger logger = Logger.getLogger(SSLUtil.class);
  
  private static final String KEYSTORE_LOCATION_ARG = "javax.net.ssl.keyStore";
  private static final String KEYSTORE_PASSWORD_ARG = "javax.net.ssl.keyStorePassword";
  private static final String KEYSTORE_TYPE_ARG = "javax.net.ssl.keyStoreType";
  private static final String DEFAULT_KEYSTORE_TYPE = "JKS";
  private static final String TRUSTSTORE_LOCATION_ARG = "javax.net.ssl.trustStore";
  private static final String TRUSTSTORE_PASSWORD_ARG = "javax.net.ssl.trustStorePassword";
  private static final String TRUSTSTORE_TYPE_ARG = "javax.net.ssl.trustStoreType";
  private static final String DEFAULT_TRUSTSTORE_TYPE = "JKS";
  
  private SSLUtil() {
    throw new UnsupportedOperationException();
  }
  
  public static String getKeyStoreLocation() {
    return System.getProperty(KEYSTORE_LOCATION_ARG);
  }
  
  public static String getKeyStorePassword() {
    return System.getProperty(KEYSTORE_PASSWORD_ARG);
  }
  
  public static String getKeyStoreType() {
    return System.getProperty(KEYSTORE_TYPE_ARG, DEFAULT_KEYSTORE_TYPE);
  }
  
  public static String getTrustStoreLocation() {
    return System.getProperty(TRUSTSTORE_LOCATION_ARG);
  }
  
  public static String getTrustStorePassword() {
    return System.getProperty(TRUSTSTORE_PASSWORD_ARG);
  }
  
  public static String getTrustStoreType() {
    return System.getProperty(TRUSTSTORE_TYPE_ARG, DEFAULT_TRUSTSTORE_TYPE);
  }
  
  public static boolean isKeyStoreSpecified() {
    return StringUtils.isNotEmpty(getKeyStoreLocation()) && StringUtils.isNotEmpty(getKeyStorePassword());
  }

  private static boolean isTrustStoreSpecified() {
    return StringUtils.isNotEmpty(getTrustStoreLocation()) && StringUtils.isNotEmpty(getTrustStorePassword());
  }
  
  public static SslContextFactory getSslContextFactory() {
    SslContextFactory sslContextFactory = new SslContextFactory();
    sslContextFactory.setKeyStorePath(getKeyStoreLocation());
    sslContextFactory.setKeyStorePassword(getKeyStorePassword());
    sslContextFactory.setKeyStoreType(getKeyStoreType());
    if (isTrustStoreSpecified()) {
      sslContextFactory.setTrustStorePath(getTrustStoreLocation());
      sslContextFactory.setTrustStorePassword(getTrustStorePassword());
      sslContextFactory.setTrustStoreType(getTrustStoreType());
    }
    
    return sslContextFactory;
  }
  
  public static SSLContext getSSLContext() {
    SslContextFactory sslContextFactory = getSslContextFactory();
    
    try {
      sslContextFactory.start();
      return sslContextFactory.getSslContext();
    } catch (Exception e) {
      logger.error("Could not create SSL Context", e);
      return null;
    } finally {
      try {
        sslContextFactory.stop();
      } catch (Exception e) {
        logger.error("Could not stop sslContextFactory", e);
      }
    }
  }
}
