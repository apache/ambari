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

package org.apache.ambari.server.api.services.parsers;

import java.util.Map;
import java.util.Set;

/**
 * Parse the provided String into a map of properties and associated values.
 */
public interface RequestBodyParser {
  /**
   * Parse the provided string into a map of properties and values.
   * The key contains both the category hierarchy and the property name.
   *
   * @param s  the string body to be parsed
   *
   * @return a set of maps of properties or an empty set if no properties exist
   */
  public Set<Map<String, Object>> parse(String s);
}
