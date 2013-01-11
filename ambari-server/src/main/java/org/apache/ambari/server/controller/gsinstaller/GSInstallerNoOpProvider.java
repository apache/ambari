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
package org.apache.ambari.server.controller.gsinstaller;

import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;

import java.util.Map;
import java.util.Set;

/**
 * A NO-OP resource provider for a gsInstaller defined cluster.
 */
public class GSInstallerNoOpProvider extends GSInstallerResourceProvider{

  private final Resource.Type type;

  // ----- Constructors ------------------------------------------------------

  public GSInstallerNoOpProvider(Resource.Type type, ClusterDefinition clusterDefinition) {
    super(clusterDefinition);
    this.type = type;
  }


  // ----- ResourceProvider --------------------------------------------------

  @Override
  public Set<String> getPropertyIdsForSchema() {
    return PropertyHelper.getPropertyIds(type);
  }

  @Override
  public Map<Resource.Type, String> getKeyPropertyIds() {
    return PropertyHelper.getKeyPropertyIds(type);
  }
}
