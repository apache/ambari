/**
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
package org.apache.ambari.server.state.alert;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link AlertUri} class is used to represent a complex URI structure where
 * there can be both a plaintext and SSL URI. This is used in cases where the
 * alert definition needs a way to expose which URL (http or https) should be
 * used to gather data. Currently, only {@link MetricSource} uses this, but it
 * can be swapped out in other source types where a plain string is used for the
 * URI.
 */
public class AlertUri {

  /**
   * The HTTP URI to use.
   */
  @SerializedName("http")
  private String m_httpUri;

  /**
   * The HTTPS URI to use.
   */
  @SerializedName("https")
  private String m_httpsUri;

  /**
   * The configuration property to check to determine if HTTP or HTTPS should be
   * used.
   */
  @SerializedName("https_property")
  private String m_httpsProperty;

  /**
   * The value to check {@link #m_httpsProperty} against to determine if HTTPS
   * should be used.
   */
  @SerializedName("https_property_value")
  private String m_httpsPropertyValue;

  /**
   * Gets the plaintext (HTTP) URI that can be used to retrieve alert
   * information.
   *
   * @return the httpUri the URI (or {@code null} to always use the secure URL).
   */
  public String getHttpUri() {
    return m_httpUri;
  }

  /**
   * Gets the secure (HTTPS) URI that can be used to retrieve alert information.
   *
   * @return the httpsUri the URI (or {@code null} to always use the insecure
   *         URL).
   */
  public String getHttpsUri() {
    return m_httpsUri;
  }

  /**
   * The configuration property that can be used to determine if the secure URL
   * should be used.
   *
   * @return the httpsProperty the configuration property, or {@code null} for
   *         none.
   */
  public String getHttpsProperty() {
    return m_httpsProperty;
  }

  /**
   * The literal value to use when comparing to the result from
   * {@link #getHttpsProperty()}.
   *
   * @return the httpsPropertyValue the literal value that indicates SSL mode is
   *         enabled, or {@code null} for none.
   */
  public String getHttpsPropertyValue() {
    return m_httpsPropertyValue;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;

    result = prime * result + ((m_httpUri == null) ? 0 : m_httpUri.hashCode());

    result = prime * result
        + ((m_httpsProperty == null) ? 0 : m_httpsProperty.hashCode());

    result = prime
        * result
        + ((m_httpsPropertyValue == null) ? 0 : m_httpsPropertyValue.hashCode());

    result = prime * result
        + ((m_httpsUri == null) ? 0 : m_httpsUri.hashCode());

    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null) {
      return false;
    }

    if (getClass() != obj.getClass()) {
      return false;
    }

    AlertUri other = (AlertUri) obj;
    if (m_httpUri == null) {
      if (other.m_httpUri != null) {
        return false;
      }
    } else if (!m_httpUri.equals(other.m_httpUri)) {
      return false;
    }

    if (m_httpsProperty == null) {
      if (other.m_httpsProperty != null) {
        return false;
      }
    } else if (!m_httpsProperty.equals(other.m_httpsProperty)) {
      return false;
    }

    if (m_httpsPropertyValue == null) {
      if (other.m_httpsPropertyValue != null) {
        return false;
      }
    } else if (!m_httpsPropertyValue.equals(other.m_httpsPropertyValue)) {
      return false;
    }

    if (m_httpsUri == null) {
      if (other.m_httpsUri != null) {
        return false;
      }
    } else if (!m_httpsUri.equals(other.m_httpsUri)) {
      return false;
    }

    return true;
  }
}
