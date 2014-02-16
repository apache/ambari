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

package org.apache.ambari.view.weather;


import java.util.Map;

/**
 * City resource bean.
 * Represents city for the weather view.
 */
public class CityResource {

  /**
   * The city id.
   */
  private String id;

  /**
   * The city weather properties.
   */
  private Map<String, Object> weather;

  /**
   * The weather units (imperial or metric).
   */
  private String units;


  /**
   * Get the city id.
   *
   * @return the id
   */
  public String getId() {
    return id;
  }

  /**
   * Set the city id.
   *
   * @param id  the id
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * Get the weather properties.
   *
   * @return the weather properties
   */
  public Map<String, Object> getWeather() {
    return weather;
  }

  /**
   * Set the weather properties.
   *
   * @param weather  the weather properties
   */
  public void setWeather(Map<String, Object> weather) {
    this.weather = weather;
  }

  /**
   * Get the weather units (imperial or metric).
   *
   * @return the weather units
   */
  public String getUnits() {
    return units;
  }

  /**
   * Set the weather units (imperial or metric).
   *
   * @param units  the weather units
   */
  public void setUnits(String units) {
    this.units = units;
  }
}
