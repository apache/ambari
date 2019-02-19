/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.apache.ambari.server.security.authentication.tproxy;

import java.util.Map;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

public class AmbariTProxyConfiguration {
  private static final Logger LOGGER = LoggerFactory.getLogger(AmbariTProxyConfiguration.class);
  private static final String TPROXY_AUTHENTICATION_ENABLED = "ambari.tproxy.authentication.enabled";
  private static final String TEMPLATE_PROXY_USER_ALLOWED_HOSTS = "ambari.tproxy.proxyuser.%s.hosts";
  private static final String TEMPLATE_PROXY_USER_ALLOWED_USERS = "ambari.tproxy.proxyuser.%s.users";
  private static final String TEMPLATE_PROXY_USER_ALLOWED_GROUPS = "ambari.tproxy.proxyuser.%s.groups";
  private final ImmutableMap<String, String> configurationMap;

  public static AmbariTProxyConfiguration fromConfig(Configuration configuration) {
    return new AmbariTProxyConfiguration(configuration.getAmbariProperties());
  }

  /**
   * Constructor
   * <p>
   * Copies the given configuration propery map into an {@link ImmutableMap} and pulls out propery
   * values upon request.
   *
   * @param configurationMap a map of property names to values
   */
  AmbariTProxyConfiguration(Map<String, String> configurationMap) {
    ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();

    if (configurationMap != null) {
      builder.putAll(configurationMap);
    }

    this.configurationMap = builder.build();
  }

  /**
   * Returns an immutable map of the contained properties
   *
   * @return an immutable map property names to values
   */
  public Map<String, String> toMap() {
    return configurationMap;
  }

  /**
   * Determines of tristed proxy support is enabled based on the configuration data.
   *
   * @return <code>true</code> if trusted proxy support is enabed; <code>false</code> otherwise
   */
  public boolean isEnabled() {
    return Boolean.valueOf(getValue(TPROXY_AUTHENTICATION_ENABLED, configurationMap, "false"));
  }

  public String getAllowedHosts(String proxyUser) {
    return getValue(String.format(TEMPLATE_PROXY_USER_ALLOWED_HOSTS, proxyUser), configurationMap, "");
  }

  public String getAllowedUsers(String proxyUser) {
    return getValue(String.format(TEMPLATE_PROXY_USER_ALLOWED_USERS, proxyUser), configurationMap, "");
  }

  public String getAllowedGroups(String proxyUser) {
    return getValue(String.format(TEMPLATE_PROXY_USER_ALLOWED_GROUPS, proxyUser), configurationMap, "");
  }

  protected String getValue(String propertyName, Map<String, String> configurationMap, String defaultValue) {
    if ((configurationMap != null) && configurationMap.containsKey(propertyName)) {
      return configurationMap.get(propertyName);
    } else {
      LOGGER.debug("Ambari server configuration property [{}] hasn't been set; using default value", propertyName);
      return defaultValue;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    return new EqualsBuilder()
      .append(configurationMap, ((AmbariTProxyConfiguration) o).configurationMap)
      .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
      .append(configurationMap)
      .toHashCode();
  }
}
