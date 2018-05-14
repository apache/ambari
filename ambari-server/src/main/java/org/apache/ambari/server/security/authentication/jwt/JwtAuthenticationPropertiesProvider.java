/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.security.authentication.jwt;

import static org.apache.ambari.server.configuration.AmbariServerConfigurationCategory.SSO_CONFIGURATION;
import static org.apache.ambari.server.configuration.AmbariServerConfigurationKey.SSO_AUTHENTICATION_ENABLED;
import static org.apache.ambari.server.configuration.AmbariServerConfigurationKey.SSO_JWT_AUDIENCES;
import static org.apache.ambari.server.configuration.AmbariServerConfigurationKey.SSO_JWT_COOKIE_NAME;
import static org.apache.ambari.server.configuration.AmbariServerConfigurationKey.SSO_PROVIDER_CERTIFICATE;
import static org.apache.ambari.server.configuration.AmbariServerConfigurationKey.SSO_PROVIDER_ORIGINAL_URL_PARAM_NAME;
import static org.apache.ambari.server.configuration.AmbariServerConfigurationKey.SSO_PROVIDER_URL;

import java.io.UnsupportedEncodingException;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;

import org.apache.ambari.server.EagerSingleton;
import org.apache.ambari.server.configuration.AmbariServerConfigurationKey;
import org.apache.ambari.server.controller.internal.AmbariServerConfigurationHandler;
import org.apache.ambari.server.events.AmbariConfigurationChangedEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.security.encryption.CertificateUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;

/**
 * JwtAuthenticationPropertiesProvider manages a {@link JwtAuthenticationProperties} instance by
 * lazily loading the properties if needed and refreshing the properties if updates are made to the
 * sso-configuration category of the Ambari configuration data.
 * <p>
 * The {@link JwtAuthenticationProperties} are updated upon events received from the {@link AmbariEventPublisher}.
 */
@EagerSingleton
public class JwtAuthenticationPropertiesProvider {
  private static final Logger LOG = LoggerFactory.getLogger(JwtAuthenticationPropertiesProvider.class);

  private static final String PEM_CERTIFICATE_HEADER = "-----BEGIN CERTIFICATE-----";
  private static final String PEM_CERTIFICATE_FOOTER = "-----END CERTIFICATE-----";

  private final AmbariServerConfigurationHandler configurationHandler;

  private JwtAuthenticationProperties properties = null;

  @Inject
  public JwtAuthenticationPropertiesProvider(AmbariServerConfigurationHandler configurationHandler, AmbariEventPublisher eventPublisher) {
    this.configurationHandler = configurationHandler;

    eventPublisher.register(this);
  }

  /**
   * Retrieves the {@link JwtAuthenticationProperties} data.
   * <p>
   * This value is expected to contain the latest data from the Ambari configuration store in the
   * Ambari DB.
   * <p>
   * If the stored reference is <code>null</code>, the properites will be loaded.
   *
   * @return a JwtAuthenticationProperties
   */
  public JwtAuthenticationProperties getProperties() {
    if (properties == null) {
      loadProperties(false);
    }

    return properties;
  }

  /**
   * (Reloads the {@link JwtAuthenticationProperties} when the sso-configuration data is updated
   * and an {@link AmbariConfigurationChangedEvent} is triggered in the {@link AmbariEventPublisher}.
   *
   * @param event an {@link AmbariConfigurationChangedEvent}
   */
  @Subscribe
  public synchronized void reloadProperties(AmbariConfigurationChangedEvent event) {
    if (SSO_CONFIGURATION.getCategoryName().equalsIgnoreCase(event.getCategoryName())) {
      loadProperties(true);
    }
  }

  /**
   * If the {@link JwtAuthenticationProperties} reference is <code>null</code> or the caller requests
   * a force reload (forceReload == <code>true</code>), a new {@link JwtAuthenticationProperties}
   * is created and this members are set to data retrieved from the Ambari databases.
   * <p>
   * This method is synchronzed so that multiple threads cannot step on each other when requesting
   * the properties to be reloaded.
   *
   * @param forceReload <code>true</code> to force the properties to be reloaded; <code>false</code>
   *                    to only reload the data if the {@link JwtAuthenticationProperties} reference
   *                    is <code>null</code>
   */
  private synchronized void loadProperties(boolean forceReload) {
    if (forceReload || (properties == null)) {
      Map<String, String> ssoProperties = configurationHandler.getConfigurationProperties(SSO_CONFIGURATION.getCategoryName());
      JwtAuthenticationProperties properties = new JwtAuthenticationProperties();
      properties.setEnabledForAmbari("true".equalsIgnoreCase(getValue(SSO_AUTHENTICATION_ENABLED, ssoProperties)));
      properties.setAudiencesString(getValue(SSO_JWT_AUDIENCES, ssoProperties));
      properties.setAuthenticationProviderUrl(getValue(SSO_PROVIDER_URL, ssoProperties));
      properties.setCookieName(getValue(SSO_JWT_COOKIE_NAME, ssoProperties));
      properties.setOriginalUrlQueryParam(getValue(SSO_PROVIDER_ORIGINAL_URL_PARAM_NAME, ssoProperties));
      properties.setPublicKey(createPublicKey(getValue(SSO_PROVIDER_CERTIFICATE, ssoProperties)));

      this.properties = properties;
    }
  }

  /**
   * Given a String containing PEM-encode x509 certificate, an {@link RSAPublicKey} is created and
   * returned.
   * <p>
   * If the certificate data is does not contain the proper PEM-encoded x509 digital certificate
   * header and footer, they will be added.
   *
   * @param certificate a PEM-encode x509 certificate
   * @return an {@link RSAPublicKey}
   */
  private RSAPublicKey createPublicKey(String certificate) {
    RSAPublicKey publicKey = null;
    if (certificate != null) {
      certificate = certificate.trim();
    }
    if (!StringUtils.isEmpty(certificate)) {
      // Ensure the PEM data is properly formatted
      if (!certificate.startsWith(PEM_CERTIFICATE_HEADER)) {
        certificate = PEM_CERTIFICATE_HEADER + "/n" + certificate;
      }
      if (!certificate.endsWith(PEM_CERTIFICATE_FOOTER)) {
        certificate = certificate + "/n" + PEM_CERTIFICATE_FOOTER;
      }

      try {
        publicKey = CertificateUtils.getPublicKeyFromString(certificate);
      } catch (CertificateException | UnsupportedEncodingException e) {
        LOG.error("Unable to parse public certificate file. JTW authentication will fai", e);
      }
    }

    return publicKey;
  }

  /**
   * A helper method to retrieve the stored value or the default value for a given SSO confogiguration
   * value.
   *
   * @param key        the {@link AmbariServerConfigurationKey} to retrieve the data for
   * @param properties the map of properties to search
   * @return the value
   */
  private String getValue(AmbariServerConfigurationKey key, Map<String, String> properties) {
    if ((properties != null) && properties.containsKey(key.key())) {
      return properties.get(key.key());
    } else {
      return key.getDefaultValue();
    }
  }
}
