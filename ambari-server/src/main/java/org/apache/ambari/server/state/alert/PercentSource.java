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
 * Alert when the source type is defined as {@link SourceType#PERCENT}
 * <p/>
 * Equality checking for instances of this class should be executed on every
 * member to ensure that reconciling stack differences is correct.
 */
public class PercentSource extends Source {

  @SerializedName("numerator")
  private MetricFractionPart m_numerator = null;

  @SerializedName("denominator")
  private MetricFractionPart m_denominator = null;

  /**
   * Gets the numerator for the percent calculation.
   *
   * @return a metric value representing the numerator (never {@code null}).
   */
  public MetricFractionPart getNumerator() {
    return m_numerator;
  }

  /**
   * Gets the denomintor for the percent calculation.
   *
   * @return a metric value representing the denominator (never {@code null}).
   */
  public MetricFractionPart getDenominator() {
    return m_denominator;
  }

  /**
   *
   */
  @Override
  public int hashCode() {
    final int prime = 31;

    int result = super.hashCode();
    result = prime * result
        + ((m_denominator == null) ? 0 : m_denominator.hashCode());
    result = prime * result
        + ((m_numerator == null) ? 0 : m_numerator.hashCode());

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

    if (!super.equals(obj)) {
      return false;
    }

    if (getClass() != obj.getClass()) {
      return false;
    }

    PercentSource other = (PercentSource) obj;

    if (m_denominator == null) {
      if (other.m_denominator != null) {
        return false;
      }
    } else if (!m_denominator.equals(other.m_denominator)) {
      return false;
    }

    if (m_numerator == null) {
      if (other.m_numerator != null) {
        return false;
      }
    } else if (!m_numerator.equals(other.m_numerator)) {
      return false;
    }

    return true;
  }

  /**
   * The {@link MetricFractionPart} class represents either the numerator or the
   * denominator of a fraction.
   * <p/>
   * Equality checking for instances of this class should be executed on every
   * member to ensure that reconciling stack differences is correct.
   */
  public static final class MetricFractionPart {
    @SerializedName("jmx")
    private String m_jmxInfo = null;

    @SerializedName("ganglia")
    private String m_gangliaInfo = null;

    /**
     * @return the jmx info, if this metric is jmx-based
     */
    public String getJmxInfo() {
      return m_jmxInfo;
    }

    /**
     * @return the ganglia info, if this metric is ganglia-based
     */
    public String getGangliaInfo() {
      return m_gangliaInfo;
    }

    /**
     *
     */
    @Override
    public int hashCode() {
      final int prime = 31;

      int result = 1;
      result = prime * result
          + ((m_gangliaInfo == null) ? 0 : m_gangliaInfo.hashCode());

      result = prime * result
          + ((m_jmxInfo == null) ? 0 : m_jmxInfo.hashCode());

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

      MetricFractionPart other = (MetricFractionPart) obj;
      if (m_gangliaInfo == null) {
        if (other.m_gangliaInfo != null) {
          return false;
        }
      } else if (!m_gangliaInfo.equals(other.m_gangliaInfo)) {
        return false;
      }

      if (m_jmxInfo == null) {
        if (other.m_jmxInfo != null) {
          return false;
        }
      } else if (!m_jmxInfo.equals(other.m_jmxInfo)) {
        return false;
      }

      return true;
    }

  }
}
