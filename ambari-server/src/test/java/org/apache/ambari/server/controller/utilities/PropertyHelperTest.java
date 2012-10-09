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
package org.apache.ambari.server.controller.utilities;

import org.apache.ambari.server.controller.spi.PropertyId;
import org.apache.ambari.server.controller.spi.Resource;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Map;
import java.util.Set;

/**
 *
 */
public class PropertyHelperTest {

  @Ignore
  @Test
  public void testGetPropertyIds() throws Exception {


    Set<PropertyId> propertyIds = PropertyHelper.getPropertyIds(Resource.Type.HostComponent, "DB");

    propertyIds = PropertyHelper.getPropertyIds(Resource.Type.HostComponent, "JMX");


    propertyIds = PropertyHelper.getPropertyIds(Resource.Type.HostComponent, "GANGLIA");

  }

  @Ignore
  @Test
  public void testGetKeyPropertyIds() throws Exception {
    Map<Resource.Type, PropertyId> keyProperties = PropertyHelper.getKeyPropertyIds(Resource.Type.Service);
  }
}

