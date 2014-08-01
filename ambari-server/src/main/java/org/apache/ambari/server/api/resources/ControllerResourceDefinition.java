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
package org.apache.ambari.server.api.resources;

import java.util.HashSet;
import java.util.Set;

import org.apache.ambari.server.controller.internal.ControllerType;
import org.apache.ambari.server.controller.spi.Resource;

/**
 * Controller Resource Definition
 */
public class ControllerResourceDefinition extends BaseResourceDefinition {

  private final ControllerType type;

  public ControllerResourceDefinition(ControllerType type) {
    super(Resource.Type.Controller);
    this.type = type;
  }

  @Override
  public String getPluralName() {
    return "controllers";
  }

  @Override
  public String getSingularName() {
    return "controller";
  }

  @Override
  public Set<SubResourceDefinition> getSubResourceDefinitions() {
    final Set<SubResourceDefinition> subResourceDefinitions = new HashSet<SubResourceDefinition>();
    if (type != null) {
      switch (type) {
      case LDAP:
        break;
      }
    }
    return subResourceDefinitions;
  }

}
