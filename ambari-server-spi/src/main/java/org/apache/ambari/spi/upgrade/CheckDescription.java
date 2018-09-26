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
package org.apache.ambari.spi.upgrade;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.base.MoreObjects;

/**
 * The {@link CheckDescription} is used to provide information about an upgrade
 * check, such as its description, applicable upgrade types, and the various
 * reasons which could cause it to fail.
 */
public class CheckDescription {

  /**
   * All of the instantiated {@link CheckDescription}s.
   */
  private static final Set<CheckDescription> s_values = new LinkedHashSet<>();

  /**
   * A unique identifier.
   */
  private final String m_name;
  private final UpgradeCheckType m_type;
  private final String m_description;
  private Map<String, String> m_fails;

  /**
   * Constructor.
   *
   * @param name
   * @param type
   * @param description
   * @param fails
   */
  public CheckDescription(String name, UpgradeCheckType type, String description, Map<String, String> fails) {
    m_name = name;
    m_type = type;
    m_description = description;
    m_fails = fails;

    if (s_values.contains(this)) {
      throw new RuntimeException("Unable to add the upgrade check description named " + m_name
          + " because it already is registered");
    }

    s_values.add(this);
  }

  /**
   * @return the name of check
   */
  public String name() {
    return m_name;
  }

  /**
   * Gets all of the registered check descriptions.
   *
   * @return
   */
  public Set<CheckDescription> values() {
    return s_values;
  }

  /**
   * @return the type of check
   */
  public UpgradeCheckType getType() {
    return m_type;
  }

  /**
   * @return the text associated with the description
   */
  public String getText() {
    return m_description;
  }

  /**
   * @param key the failure text key
   * @return the fail text template.  Never {@code null}
   */
  public String getFail(String key) {
    return m_fails.containsKey(key) ? m_fails.get(key) : "";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return Objects.hash(m_name);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object object) {
    if (null == object) {
      return false;
    }

    if (this == object) {
      return true;
    }

    if (object.getClass() != getClass()) {
      return false;
    }

    CheckDescription that = (CheckDescription) object;

    return Objects.equals(m_name, that.m_name);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("name", m_name).toString();
  }
}