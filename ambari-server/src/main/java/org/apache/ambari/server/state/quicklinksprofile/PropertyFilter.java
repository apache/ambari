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

package org.apache.ambari.server.state.quicklinksprofile;

import java.util.Objects;

import org.apache.ambari.server.state.quicklinks.Link;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * A quicklink filter based on property-match (the filter's property is contained by the links set of properties)
 */
public class PropertyFilter extends Filter {
  static final String PROPERTY_NAME = "property_name";

  @JsonProperty(PROPERTY_NAME)
  private String propertyName;

  public String getPropertyName() {
    return propertyName;
  }

  public void setPropertyName(String propertyName) {
    this.propertyName = propertyName;
  }

  @Override
  public boolean accept(Link link) {
    return link.getProperties().contains(propertyName);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PropertyFilter that = (PropertyFilter) o;
    return isVisible() == that.isVisible() && Objects.equals(propertyName, that.propertyName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(isVisible(), propertyName);
  }
}
