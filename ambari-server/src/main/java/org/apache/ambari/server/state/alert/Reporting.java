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
 * The {@link Reporting} class represents the OK/WARNING/CRITICAL structures in
 * an {@link AlertDefinition}.
 */
public class Reporting {

  /**
   *
   */
  @SerializedName("ok")
  private ReportTemplate m_ok;

  /**
   *
   */
  @SerializedName("warning")
  private ReportTemplate m_warning;

  /**
   *
   */
  @SerializedName("critical")
  private ReportTemplate m_critical;

  /**
   * @return the WARNING structure or {@code null} if none.
   */
  public ReportTemplate getWarning() {
    return m_warning;
  }

  /**
   * @return the CRITICAL structure or {@code null} if none.
   */
  public ReportTemplate getCritical() {
    return m_critical;
  }

  /**
   * @return the OK structure or {@code null} if none.
   */
  public ReportTemplate getOk() {
    return m_ok;
  }

  /**
   * The {@link ReportTemplate} class is used to pair a label and threshhold
   * value.
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
     * @return the threshold value for this template or {@code null} if none.
     */
    public Double getValue() {
      return m_value;
    }
  }
}
