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

package org.apache.ambari.api.resource;

import org.apache.ambari.api.query.Query;
import org.apache.ambari.api.services.formatters.ResultFormatter;
import org.apache.ambari.api.controller.spi.Resource;

import java.util.Map;
import java.util.Set;

/**
 *
 */
public interface ResourceDefinition {
  public String getPluralName();

  public String getSingularName();

  public String getId();

  public Set<ResourceDefinition> getChildren();

  public Set<ResourceDefinition> getRelations();

  public Map<Resource.Type, String> getResourceIds();

  public ResultFormatter getResultFormatter();

  public Resource.Type getType();

  public Query getQuery();
}
