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
 * Alert when the source type is defined as {@link SourceType#SCRIPT}
 * <p/>
 * Equality checking for instances of this class should be executed on every
 * member to ensure that reconciling stack differences is correct.
 */
public class ScriptSource extends ParameterizedSource {

  @SerializedName("path")
  private String m_path = null;

  /**
   * @return the path to the script file.
   */
  public String getPath() {
    return m_path;
  }

  public void setPath(String path) {
    m_path = path;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((m_path == null) ? 0 : m_path.hashCode());

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

    ScriptSource other = (ScriptSource) obj;

    if (m_path == null) {
      if (other.m_path != null) {
        return false;
      }
    } else if (!m_path.equals(other.m_path)) {
      return false;
    }

    return true;
  }
}
