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

import java.util.List;

import org.apache.ambari.server.state.AlertState;

import com.google.gson.annotations.SerializedName;

/**
 * Alert when the source type is defined as {@link SourceType#SCRIPT}
 * <p/>
 * Equality checking for instances of this class should be executed on every
 * member to ensure that reconciling stack differences is correct.
 */
public class ScriptSource extends Source {

  @SerializedName("path")
  private String m_path = null;

  /**
   * A list of all of the script parameters, if any.
   */
  @SerializedName("parameters")
  private List<ScriptParameter> m_parameters;

  /**
   * @return the path to the script file.
   */
  public String getPath() {
    return m_path;
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

  /**
   * The {@link ScriptParameter} class represents a single parameter that can be
   * passed into a script alert.
   */
  public static class ScriptParameter {
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
    private ScriptParameterType m_type;

    /**
     * If this script parameter controls a threshold, then its specified here,
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
      ScriptParameter other = (ScriptParameter) obj;
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
      return true;
    }


    /**
     * The {@link ScriptParameterType} enum represents the value type.
     */
    public enum ScriptParameterType {
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
  }
}
