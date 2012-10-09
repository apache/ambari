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

package org.apache.ambari.api.query;

import org.apache.ambari.api.services.Result;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.PropertyId;


/**
 * Responsible for querying the back end for read requests
 */
public interface Query {

  /**
   * Add a property to the query.
   * This is the select portion of the query.
   *
   * @param group    the group name that contains the property
   * @param property the property name
   */
  public void addProperty(String group, String property);

  /**
   * Add a property to the query.
   * This is the select portion of the query.
   *
   * @param property the property id which contains the group, property name
   *                 and whether the property is temporal
   */
  public void addProperty(PropertyId property);

  /**
   * Execute the query.
   *
   * @return the result of the query.
   */
  public Result execute() throws AmbariException;

  /**
   * Return the predicate used to identify the associated resource.  This includes the primary key and
   * all parent id's;
   *
   * @return the predicate used to identify the associated resource
   */
  public Predicate getPredicate();
}
