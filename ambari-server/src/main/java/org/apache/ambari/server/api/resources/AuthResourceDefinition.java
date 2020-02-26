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

import java.util.HashSet;
import java.util.Set;

import org.apache.ambari.server.controller.spi.Resource;

/**
 * Auth Resource Definition
 */
public class AuthResourceDefinition extends BaseResourceDefinition {

  public AuthResourceDefinition() {
    super(Resource.Type.Auth);
  }

  @Override
  public String getPluralName() {
    return "auths";
  }

  @Override
  public String getSingularName() {
    return "auth";
  }

  @Override
  public Set<SubResourceDefinition> getSubResourceDefinitions() {
    final Set<SubResourceDefinition> subResourceDefinitions = new HashSet<>();
    subResourceDefinitions.add(new SubResourceDefinition(Resource.Type.UserAuthenticationSource));
    subResourceDefinitions.add(new SubResourceDefinition(Resource.Type.UserPrivilege));
    subResourceDefinitions.add(new SubResourceDefinition(Resource.Type.ActiveWidgetLayout));
    subResourceDefinitions.add(new SubResourceDefinition(Resource.Type.User));
    return subResourceDefinitions;
  }

}
