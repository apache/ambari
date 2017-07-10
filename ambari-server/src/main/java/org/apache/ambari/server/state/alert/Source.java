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
 * Abstract class that all known alert sources should extend.
 * <p/>
 * Equality checking for instances of this class should be executed on every
 * member to ensure that reconciling stack differences is correct.
 */
public abstract class Source {

  private SourceType type;

  @SerializedName("reporting")
  private Reporting reporting;

  /**
   * @return the type
   */
  public SourceType getType() {
    return type;
  }

  /**
   * @param type
   *          the type to set.
   */
  public void setType(SourceType type) {
    this.type = type;
  }

  /**
   * @return
   */
  public Reporting getReporting() {
    return reporting;
  }

  /**
   * Sets the OK/WARNING/CRTICAL structures.
   *
   * @param reporting
   *          the reporting structure or {@code null} for none.
   */
  public void setReporting(Reporting reporting) {
    this.reporting = reporting;
  }

  /**
   *
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((reporting == null) ? 0 : reporting.hashCode());
    result = prime * result + ((type == null) ? 0 : type.hashCode());

    return result;
  }

  /**
   *
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

    Source other = (Source) obj;
    if (reporting == null) {
      if (other.reporting != null) {
        return false;
      }
    } else if (!reporting.equals(other.reporting)) {
      return false;
    }

    if (type != other.type) {
      return false;
    }
    return true;
  }
}
