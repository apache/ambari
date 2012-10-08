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

package org.apache.ambari.server.controller.predicate;

import org.apache.ambari.server.controller.spi.PropertyId;

/**
 * Simple PropertyId implementation.
 */
public class PropertyIdImpl implements PropertyId {
  /**
   * The property name.
   */
  private String name;

  /**
   * The category name.
   */
  private String category;

  /**
   * Indicates whether or not this property is temporal.
   */
  private boolean temporal;


  // ----- Constructors ------------------------------------------------------

  /**
   * Create a property id.  Required for JSON serialization.
   */
  public PropertyIdImpl() {
  }

  /**
   * Create a property id.
   *
   * @param name      the property name.
   * @param category  the property category.
   * @param temporal  a temporal indicator
   */
  public PropertyIdImpl(String name, String category, boolean temporal) {
    this.name     = name;
    this.category = category;
    this.temporal = temporal;
  }


  // ----- PropertyId --------------------------------------------------------

  public String getName() {
    return name;
  }

  public String getCategory() {
    return category;
  }

  public boolean isTemporal() {
    return temporal;
  }


  // ----- Object overrides --------------------------------------------------

  @Override
  public int hashCode() {
    return name.hashCode() +
        (category == null ? 0 : category.hashCode()) +
        (temporal ? 1 : 0);
  }

  @Override
  public boolean equals(Object o) {

    if (this == o) {
      return true;
    }

    if (!(o.getClass().equals(PropertyIdImpl.class))) {
      return false;
    }

    PropertyIdImpl that = (PropertyIdImpl) o;

    return this.name.equals(that.getName()) &&
        equals(this.category, that.getCategory()) &&
        this.isTemporal() == that.isTemporal();
  }

  @Override
  public String toString() {
    return "PropertyId[" + category + ", " + name + "]";
  }


  // ----- helper methods ----------------------------------------------------

  private static boolean equals(Object o1, Object o2) {
    if (o1 == null) {
      return o2 == null;
    }
    return o2 != null && o1.equals(o2);
  }
}
