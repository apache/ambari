/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.api.resources;

import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.Resource.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resource Definition for Mpack Resource types.
 */
public class MpackResourceDefinition extends BaseResourceDefinition {

  private final static Logger LOG =
          LoggerFactory.getLogger(MpackResourceDefinition.class);

  public MpackResourceDefinition(Type resourceType) {
    super(Resource.Type.Mpack);
  }

  public MpackResourceDefinition() {
    super(Resource.Type.Mpack);
  }

  @Override
  public String getPluralName() {
    return "mpacks";
  }

  @Override
  public String getSingularName() {
    return "mpack";
  }
}
