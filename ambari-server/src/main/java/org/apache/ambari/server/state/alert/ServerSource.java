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
package org.apache.ambari.server.state.alert;

import com.google.gson.annotations.SerializedName;


/**
 * Alert when the source type is defined as {@link SourceType#SERVER}
 */
public class ServerSource extends ParameterizedSource {

  @SerializedName("class")
  private String m_class;

  @SerializedName("uri")
  private AlertUri uri = null;

  @SerializedName("jmx")
  private MetricSource.JmxInfo jmxInfo = null;


  /**
   * Gets the fully qualified classname specified in the source.
   */
  public String getSourceClass() {
    return m_class;
  }

  public MetricSource.JmxInfo getJmxInfo() {
    return jmxInfo;
  }

  public AlertUri getUri() {
    return uri;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((m_class == null) ? 0 : m_class.hashCode());
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

    if (!super.equals(obj)) {
      return false;
    }

    if (getClass() != obj.getClass()) {
      return false;
    }

    ServerSource other = (ServerSource) obj;
    if (m_class == null) {
      if (other.m_class != null) {
        return false;
      }
    } else if (!m_class.equals(other.m_class)) {
      return false;
    }
    return true;
  }

}
