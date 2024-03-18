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
package org.apache.ambari.infra.conf;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.time.Duration;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.ambari.infra.conf.security.SslSecrets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;

@Named
public class InfraManagerWebServerCustomizer implements WebServerFactoryCustomizer<JettyServletWebServerFactory> {

  @Value("${infra-manager.server.port:61890}")
  private int port;

  @Value("${infra-manager.server.ssl.enabled:false}")
  private boolean sslEnabled;

  @Inject
  private ServerProperties serverProperties;

  @Inject
  private SslSecrets sslSecrets;

  private static final Integer SESSION_TIMEOUT = 60 * 30;
  private static final String INFRA_MANAGER_SESSION_ID = "INFRAMANAGER_SESSIONID";
  private static final String INFRA_MANAGER_APPLICATION_NAME = "infra-manager";

  @Override
  public void customize(JettyServletWebServerFactory factory) {
    factory.setPort(port);
    factory.setDisplayName(INFRA_MANAGER_APPLICATION_NAME);
    factory.getSession().getCookie().setName(INFRA_MANAGER_SESSION_ID);
    factory.getSession().setTimeout(Duration.ofSeconds(SESSION_TIMEOUT));

    Ssl ssl = new Ssl();
    String keyStore = System.getProperty("javax.net.ssl.keyStore");
    if (isNotBlank(keyStore)) {
      ssl.setKeyStore(keyStore);
      ssl.setKeyStoreType(System.getProperty("javax.net.ssl.keyStoreType"));
      String keyStorePassword = sslSecrets.getKeyStorePassword().get().orElseThrow(() -> new IllegalStateException("Password for keystore is not set!"));
      ssl.setKeyStorePassword(keyStorePassword);
      System.setProperty("javax.net.ssl.keyStorePassword", keyStorePassword);
    }

    String trustStore = System.getProperty("javax.net.ssl.trustStore");
    if (isNotBlank(trustStore)) {
      ssl.setTrustStore(trustStore);
      ssl.setTrustStoreType(System.getProperty("javax.net.ssl.trustStoreType"));
      String trustStorePassword = sslSecrets.getTrustStorePassword().get().orElseThrow(() -> new IllegalStateException("Password for truststore is not set!"));
      ssl.setKeyStorePassword(trustStorePassword);
      System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);
    }

    ssl.setEnabled(sslEnabled);

    factory.setSsl(ssl);
  }
}
