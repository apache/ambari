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

package org.apache.ambari.server.api.query;

import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.controller.spi.*;

import java.util.Set;


/**
 * Responsible for querying the back end for read requests
 */
public interface Query {

  /**
   * Add a property to the query.
   * This is the select portion of the query.
   *
   * @param group         the group name that contains the property
   * @param property      the property name
   * @param temporalInfo  temporal information for the property
   */
  public void addProperty(String group, String property, TemporalInfo temporalInfo);

  /**
   * Add a local (not sub-resource) property to the query.
   * This is the select portion of the query.
   *
   * @param property the property id which contains the group, property name
   *                 and whether the property is temporal
   */
  public void addLocalProperty(String property);

  /**
   * Obtain the properties of the query.
   * These are the properties that make up the select portion of the query for which
   * values are to be retrieved.
   *
   * @return the query properties
   */
  public Set<String> getProperties();

  /**
   * Execute the query.
   *
   * @return the result of the query.
   *
   * @throws UnsupportedPropertyException if the query or query predicate contains invalid non-existent properties
   * @throws SystemException an internal error occurred
   * @throws NoSuchResourceException the query didn't match any resources
   * @throws NoSuchParentResourceException a specified parent resource doesn't exist
   */
  public Result execute()
      throws UnsupportedPropertyException, SystemException, NoSuchResourceException, NoSuchParentResourceException;

  /**
   * Return the predicate used to identify the associated resource.  This includes the primary key and
   * all parent id's;
   *
   * @return the predicate used to identify the associated resource
   */
  public Predicate getPredicate();

  /**
   * Set the user provided predicated on this query.
   * This predicate will be "AND'd" with the internal query to produce the final predicate.
   *
   * @param predicate  the user provided predicate
   */
  public void setUserPredicate(Predicate predicate);

  /**
   * Set the page request information for this query.
   *
   * @param pageRequest  the page request information
   */
  public void setPageRequest(PageRequest pageRequest);

}
