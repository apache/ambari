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

package org.apache.ambari.server.api.resources;

import java.util.Collection;

import org.apache.ambari.server.controller.internal.BlueprintResourceProvider;
import org.apache.ambari.server.controller.spi.Resource;


/**
 * Blueprint resource definition.
 */
public class BlueprintResourceDefinition extends BaseResourceDefinition {
  /**
   * Constructor.
   *
   */
  public BlueprintResourceDefinition() {
    super(Resource.Type.Blueprint);
  }

  @Override
  public String getPluralName() {
    return "blueprints";
  }

  @Override
  public String getSingularName() {
    return "blueprint";
  }

  @Override
  public Collection<String> getCreateDirectives() {
    Collection<String> directives = super.getCreateDirectives();
    directives.add(BlueprintResourceProvider.VALIDATE_TOPOLOGY_PROPERTY_ID);
    return directives;
  }
}
