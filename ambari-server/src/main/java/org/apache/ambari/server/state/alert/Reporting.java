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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.ambari.server.alerts.Threshold;
import org.apache.ambari.server.state.AlertState;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link Reporting} class represents the OK/WARNING/CRITICAL structures in
 * an {@link AlertDefinition}.
 * <p/>
 * Equality checking for instances of this class should be executed on every
 * member to ensure that reconciling stack differences is correct.
 */
public class Reporting {

  /**
   * The OK template.
   */
  @SerializedName("ok")
  private ReportTemplate m_ok;

  /**
   * The WARNING template.
   */
  @SerializedName("warning")
  private ReportTemplate m_warning;

  /**
   * The CRITICAL template.
   */
  @SerializedName("critical")
  private ReportTemplate m_critical;

  /**
   * A label that identifies what units the value is in. For example, this could
   * be "s" for seconds or GB for "Gigabytes".
   */
  @SerializedName("units")
  private String m_units;

  @SerializedName("type")
  private ReportingType m_type;

  /**
   * @return the WARNING structure or {@code null} if none.
   */
  public ReportTemplate getWarning() {
    return m_warning;
  }

  /**
   * @param warning
   *          the WARNING structure or {@code null} if none.
   */
  public void setWarning(ReportTemplate warning) {
    m_warning = warning;
  }

  /**
   * @return the CRITICAL structure or {@code null} if none.
   */
  public ReportTemplate getCritical() {
    return m_critical;
  }

  /**
   * @param critical
   *          the CRITICAL structure or {@code null} if none.
   */
  public void setCritical(ReportTemplate critical) {
    m_critical = critical;
  }

  /**
   * @return the OK structure or {@code null} if none.
   */
  public ReportTemplate getOk() {
    return m_ok;
  }

  /**
   * @param ok
   *          the OK structure or {@code null} if none.
   */
  public void setOk(ReportTemplate ok) {
    m_ok = ok;
  }

  /**
   * Gets a label identifying the units that the values are in. For example,
   * this could be "s" for seconds or GB for "Gigabytes".
   *
   * @return the units, or {@code null} for none.
   */
  public String getUnits() {
    return m_units;
  }

  /**
   * Sets the label that identifies the units that the threshold values are in.
   * For example, this could be "s" for seconds or GB for "Gigabytes".
   *
   * @param units
   *          the units, or {@code null} for none.
   */
  public void setUnits(String units) {
    m_units = units;
  }

  public ReportingType getType() {
    return m_type;
  }

  public void setType(ReportingType m_type) {
    this.m_type = m_type;
  }

  /**
   *
   */
  @Override
  public int hashCode() {
    final int prime = 31;

    int result = 1;
    result = prime * result
        + ((m_critical == null) ? 0 : m_critical.hashCode());
    result = prime * result + ((m_ok == null) ? 0 : m_ok.hashCode());
    result = prime * result + ((m_warning == null) ? 0 : m_warning.hashCode());
    result = prime * result + ((m_type == null) ? 0 : m_type.hashCode());
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

    Reporting other = (Reporting) obj;
    if (m_critical == null) {
      if (other.m_critical != null) {
        return false;
      }
    } else if (!m_critical.equals(other.m_critical)) {
      return false;
    }

    if (m_ok == null) {
      if (other.m_ok != null) {
        return false;
      }
    } else if (!m_ok.equals(other.m_ok)) {
      return false;
    }

    if (m_warning == null) {
      if (other.m_warning != null) {
        return false;
      }
    } else if (!m_warning.equals(other.m_warning)) {
      return false;
    }

    if (m_type == null) {
      if (other.m_type != null) {
        return false;
      }
    } else if (!m_type.equals(other.m_type)) {
      return false;
    }

    return true;
  }

  public AlertState state(double value) {
    return getThreshold().state(value);
  }

  private Threshold getThreshold() {
    return new Threshold(getOk().getValue(), getWarning().getValue(), getCritical().getValue());
  }

  public String formatMessage(double value, List<Object> args) {
    List<Object> copy = new ArrayList<>(args);
    copy.add(value);
    return MessageFormat.format(message(value), copy.toArray());
  }

  private String message(double value) {
    switch (state(value)) {
      case OK:
        return getOk().getText();
      case WARNING:
        return getWarning().getText();
      case CRITICAL:
        return getCritical().getText();
      case UNKNOWN:
        return "Unknown";
      case SKIPPED:
        return "Skipped";
      default:
        throw new IllegalStateException("Invalid alert state: " + state(value));
    }
  }

  /**
   * The {@link ReportTemplate} class is used to pair a label and threshhold
   * value.
   * <p/>
   * Equality checking for instances of this class should be executed on every
   * member to ensure that reconciling stack differences is correct.
   */
  public static final class ReportTemplate {
    @SerializedName("text")
    private String m_text;

    @SerializedName("value")
    private Double m_value = null;

    /**
     * @return the parameterized text of this template or {@code null} if none.
     */
    public String getText() {
      return m_text;
    }

    /**
     * @param text
     *          the parameterized text of this template or {@code null} if none.
     */
    public void setText(String text) {
      m_text = text;
    }

    /**
     * @return the threshold value for this template or {@code null} if none.
     */
    public Double getValue() {
      return m_value;
    }

    /**
     * @param value
     *          the threshold value for this template or {@code null} if none.
     */
    public void setValue(Double value) {
      m_value = value;
    }

    /**
     *
     */
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((m_text == null) ? 0 : m_text.hashCode());
      result = prime * result + ((m_value == null) ? 0 : m_value.hashCode());
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

      ReportTemplate other = (ReportTemplate) obj;

      if (m_text == null) {
        if (other.m_text != null) {
          return false;
        }
      } else if (!m_text.equals(other.m_text)) {
        return false;
      }

      if (m_value == null) {
        if (other.m_value != null) {
          return false;
        }
      } else if (!m_value.equals(other.m_value)) {
        return false;
      }
      return true;
    }
  }

  public enum ReportingType {
    /**
     * Integers, longs, floats, etc.
     */
    NUMERIC,

    /**
     * A percent value, expessed as a float.
     */
    PERCENT
  }
}
