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


import com.google.gson.annotations.SerializedName;

/**
 * Request object wrapping information for LDAP configuration related request calls.
 */
public class LdapConfigurationRequest {

  @SerializedName("AmbariConfiguration")
  private AmbariConfiguration ambariConfiguration;

  @SerializedName("RequestInfo")
  private LdapRequestInfo requestInfo;

  public LdapConfigurationRequest() {
  }

  public AmbariConfiguration getAmbariConfiguration() {
    return ambariConfiguration;
  }

  public void setAmbariConfiguration(AmbariConfiguration ambariConfiguration) {
    this.ambariConfiguration = ambariConfiguration;
  }

  public LdapRequestInfo getRequestInfo() {
    return requestInfo;
  }

  public void setRequestInfo(LdapRequestInfo requestInfo) {
    this.requestInfo = requestInfo;
  }
}
