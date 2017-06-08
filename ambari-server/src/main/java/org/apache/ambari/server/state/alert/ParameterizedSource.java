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

import java.util.Collections;
import java.util.List;

import org.apache.ambari.server.state.AlertState;

import com.google.gson.annotations.SerializedName;


/**
 * The {@link ParameterizedSource} is used for alerts where the logic of
 * computing the {@link AlertState} is dependant on user-specified parameters.
 * For example, the parameters might be threshold values.
 */
public abstract class ParameterizedSource extends Source {

  /**
   * A list of all of the alert parameters, if any.
   */
  @SerializedName("parameters")
  List<AlertParameter> m_parameters;

  /**
   * Gets a list of the optional parameters which govern how a parameterized
   * alert behaves. These are usually threshold values.
   *
   * @return the list of parameters, or an empty list if none.
   */
  public List<AlertParameter> getParameters() {
    if (null == m_parameters) {
      return Collections.emptyList();
    }

    return m_parameters;
  }

  /**
   * The {@link AlertParameter} class represents a single parameter that can be
   * passed into an alert which takes parameters.
   */
  public static class AlertParameter {
    @SerializedName("name")
    private String m_name;

    @SerializedName("display_name")
    private String m_displayName;

    @SerializedName("units")
    private String m_units;

    @SerializedName("value")
    private Object m_value;

    @SerializedName("description")
    private String m_description;

    @SerializedName("type")
    private AlertParameterType m_type;

    @SerializedName("visibility")
    private AlertParameterVisibility m_visibility;

    /**
     * If this alert parameter controls a threshold, then its specified here,
     * otherwise it's {@code null}.
     */
    @SerializedName("threshold")
    private AlertState m_threshold;

    /**
     * Gets the unique name of the parameter.
     *
     * @return the name
     */
    public String getName() {
      return m_name;
    }

    /**
     * Gets the human readable name of the parameter.
     *
     * @return the displayName
     */
    public String getDisplayName() {
      return m_displayName;
    }

    /**
     * Gets the display units of the paramter.
     *
     * @return the units
     */
    public String getUnits() {
      return m_units;
    }

    /**
     * Gets the value of the parameter.
     *
     * @return the value
     */
    public Object getValue() {
      return m_value;
    }

    /**
     * Gets the description of the parameter.
     *
     * @return the description
     */
    public String getDescription() {
      return m_description;
    }

    /**
     * Gets the visibility of the parameter.
     *
     * @return the visibility
     */
    public AlertParameterVisibility getVisibility() {
      return m_visibility;
    }

    /**
     * Gets the threshold that this parameter directly controls, or {@code null}
     * for none.
     *
     * @return the threshold, or {@code null}.
     */
    public AlertState getThreshold() {
      return m_threshold;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((m_description == null) ? 0 : m_description.hashCode());
      result = prime * result + ((m_displayName == null) ? 0 : m_displayName.hashCode());
      result = prime * result + ((m_name == null) ? 0 : m_name.hashCode());
      result = prime * result + ((m_threshold == null) ? 0 : m_threshold.hashCode());
      result = prime * result + ((m_type == null) ? 0 : m_type.hashCode());
      result = prime * result + ((m_units == null) ? 0 : m_units.hashCode());
      result = prime * result + ((m_value == null) ? 0 : m_value.hashCode());
      result = prime * result + ((m_visibility == null) ? 0 : m_visibility.hashCode());
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
      AlertParameter other = (AlertParameter) obj;
      if (m_description == null) {
        if (other.m_description != null) {
          return false;
        }
      } else if (!m_description.equals(other.m_description)) {
        return false;
      }
      if (m_displayName == null) {
        if (other.m_displayName != null) {
          return false;
        }
      } else if (!m_displayName.equals(other.m_displayName)) {
        return false;
      }
      if (m_name == null) {
        if (other.m_name != null) {
          return false;
        }
      } else if (!m_name.equals(other.m_name)) {
        return false;
      }
      if (m_threshold != other.m_threshold) {
        return false;
      }
      if (m_type != other.m_type) {
        return false;
      }
      if (m_units == null) {
        if (other.m_units != null) {
          return false;
        }
      } else if (!m_units.equals(other.m_units)) {
        return false;
      }
      if (m_value == null) {
        if (other.m_value != null) {
          return false;
        }
      } else if (!m_value.equals(other.m_value)) {
        return false;
      }
      if (m_visibility == null) {
        if (other.m_visibility != null) {
          return false;
        }
      } else if (!m_visibility.equals(other.m_visibility)) {
        return false;
      }
      return true;
    }
  }

  /**
   * The {@link AlertParameterType} enum represents the value type.
   */
  public enum AlertParameterType {
    /**
     * String
     */
    STRING,

    /**
     * Integers, longs, floats, etc.
     */
    NUMERIC,

    /**
     * A percent value, expessed as a float.
     */
    PERCENT
  }

  /**
   * The {@link AlertParameterVisibility} enum represents the visibility of
   * alert parameters.
   */
  public enum AlertParameterVisibility {
    VISIBLE, HIDDEN, READ_ONLY
  }
}
