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

import static org.apache.ambari.server.configuration.AmbariServerConfigurationKey.SSO_AUTHENTICATION_ENABLED;
import static org.apache.ambari.server.configuration.AmbariServerConfigurationKey.SSO_JWT_AUDIENCES;
import static org.apache.ambari.server.configuration.AmbariServerConfigurationKey.SSO_JWT_COOKIE_NAME;
import static org.apache.ambari.server.configuration.AmbariServerConfigurationKey.SSO_PROVIDER_CERTIFICATE;
import static org.apache.ambari.server.configuration.AmbariServerConfigurationKey.SSO_PROVIDER_ORIGINAL_URL_PARAM_NAME;
import static org.apache.ambari.server.configuration.AmbariServerConfigurationKey.SSO_PROVIDER_URL;

import java.io.UnsupportedEncodingException;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.configuration.AmbariServerConfiguration;
import org.apache.ambari.server.security.encryption.CertificateUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class describes parameters of external JWT authentication provider
 */
public class JwtAuthenticationProperties extends AmbariServerConfiguration {
  private static final Logger LOG = LoggerFactory.getLogger(JwtAuthenticationPropertiesProvider.class);

  private static final String PEM_CERTIFICATE_HEADER = "-----BEGIN CERTIFICATE-----";
  private static final String PEM_CERTIFICATE_FOOTER = "-----END CERTIFICATE-----";

  private String authenticationProviderUrl = null;
  private RSAPublicKey publicKey = null;
  private List<String> audiences = null;
  private String cookieName = "hadoop-jwt";
  private String originalUrlQueryParam = null;
  private boolean enabledForAmbari;

  JwtAuthenticationProperties(Map<String, String> configurationMap) {
    setEnabledForAmbari(Boolean.valueOf(getValue(SSO_AUTHENTICATION_ENABLED, configurationMap)));
    setAudiencesString(getValue(SSO_JWT_AUDIENCES, configurationMap));
    setAuthenticationProviderUrl(getValue(SSO_PROVIDER_URL, configurationMap));
    setCookieName(getValue(SSO_JWT_COOKIE_NAME, configurationMap));
    setOriginalUrlQueryParam(getValue(SSO_PROVIDER_ORIGINAL_URL_PARAM_NAME, configurationMap));
    setPublicKey(getValue(SSO_PROVIDER_CERTIFICATE, configurationMap));
  }

  public String getAuthenticationProviderUrl() {
    return authenticationProviderUrl;
  }

  public void setAuthenticationProviderUrl(String authenticationProviderUrl) {
    this.authenticationProviderUrl = authenticationProviderUrl;
  }

  public RSAPublicKey getPublicKey() {
    return publicKey;
  }

  public void setPublicKey(String publicKey) {
    setPublicKey(createPublicKey(publicKey));
  }

  public void setPublicKey(RSAPublicKey publicKey) {
    this.publicKey = publicKey;
  }

  public List<String> getAudiences() {
    return audiences;
  }

  public void setAudiences(List<String> audiences) {
    this.audiences = audiences;
  }

  public void setAudiencesString(String audiencesString) {
    if (StringUtils.isNotEmpty(audiencesString)) {
      // parse into the list
      String[] audArray = audiencesString.split(",");
      audiences = new ArrayList<>();
      Collections.addAll(audiences, audArray);
    } else {
      audiences = null;
    }
  }

  public String getCookieName() {
    return cookieName;
  }

  public void setCookieName(String cookieName) {
    this.cookieName = cookieName;
  }

  public String getOriginalUrlQueryParam() {
    return originalUrlQueryParam;
  }

  public void setOriginalUrlQueryParam(String originalUrlQueryParam) {
    this.originalUrlQueryParam = originalUrlQueryParam;
  }

  public boolean isEnabledForAmbari() {
    return enabledForAmbari;
  }

  public void setEnabledForAmbari(boolean enabledForAmbari) {
    this.enabledForAmbari = enabledForAmbari;
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
        LOG.error("Unable to parse public certificate file. JTW authentication will fail.", e);
      }
    }

    return publicKey;
  }
}
