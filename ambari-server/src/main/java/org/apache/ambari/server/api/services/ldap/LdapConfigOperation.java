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

package org.apache.ambari.server.api.services.ldap;

/**
 * Enumeration for supported operations related to LDAP configuration.
 */
public enum LdapConfigOperation {
  TEST_CONNECTION("test-connection"),
  TEST_ATTRIBUTES("test-attributes"),
  DETECT_ATTRIBUTES("detect-attributes");

  private String actionStr;

  LdapConfigOperation(String actionStr) {
    this.actionStr = actionStr;
  }

  public static LdapConfigOperation fromAction(String action) {
    for (LdapConfigOperation val : LdapConfigOperation.values()) {
      if (val.action().equals(action)) {
        return val;
      }
    }
    throw new IllegalStateException("Action [ " + action + " ] is not supported");
  }

  public String action() {
    return this.actionStr;
  }
}
