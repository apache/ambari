/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.ldap.domain;

import java.util.Map;

/**
 * Factory interface for AmbariLdapConfiguration instances.
 * It's registered as a factory in the GUICE context (so no implementations required)
 *
 * To be extended with other factory methods upon needs.
 */
public interface AmbariLdapConfigurationFactory {

  /**
   * Creates an AmbariLdapConfiguration instance with the provided map of configuration settings.
   *
   * @param configuration a map where keys are the configuration properties and values are the configuration values
   * @return an AmbariLdapConfiguration instance
   */
  AmbariLdapConfiguration createLdapConfiguration(Map<String, String> configuration);
}