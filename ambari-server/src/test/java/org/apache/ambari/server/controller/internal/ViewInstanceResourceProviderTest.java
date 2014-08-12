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

package org.apache.ambari.server.controller.internal;

import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.orm.entities.ViewEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceDataEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.server.orm.entities.ViewInstancePropertyEntity;
import org.apache.ambari.server.orm.entities.ViewParameterEntity;
import org.easymock.EasyMock;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;
import static org.easymock.EasyMock.*;

public class ViewInstanceResourceProviderTest {

  @Test
  public void testToResource() throws Exception {
    ViewInstanceResourceProvider provider = new ViewInstanceResourceProvider();
    Set<String> propertyIds = new HashSet<String>();
    propertyIds.add(ViewInstanceResourceProvider.PROPERTIES_PROPERTY_ID);
    ViewInstanceEntity viewInstanceEntity = createNiceMock(ViewInstanceEntity.class);
    ViewEntity viewEntity = createNiceMock(ViewEntity.class);
    expect(viewInstanceEntity.getViewEntity()).andReturn(viewEntity);

    ViewInstancePropertyEntity propertyEntity1 = createNiceMock(ViewInstancePropertyEntity.class);
    expect(propertyEntity1.getName()).andReturn("par1").anyTimes();
    expect(propertyEntity1.getValue()).andReturn("val1").anyTimes();
    ViewInstancePropertyEntity propertyEntity3 = createNiceMock(ViewInstancePropertyEntity.class);
    expect(propertyEntity3.getName()).andReturn("par3").anyTimes();
    expect(propertyEntity3.getValue()).andReturn("val3").anyTimes();
    expect(viewInstanceEntity.getProperties()).andReturn(Arrays.asList(propertyEntity1, propertyEntity3));

    ViewParameterEntity parameter1 = createNiceMock(ViewParameterEntity.class);
    expect(parameter1.getName()).andReturn("par1").anyTimes();
    ViewParameterEntity parameter2 = createNiceMock(ViewParameterEntity.class);
    expect(parameter2.getName()).andReturn("par2").anyTimes();
    expect(viewEntity.getParameters()).andReturn(Arrays.asList(parameter1, parameter2));

    expect(viewInstanceEntity.getData()).andReturn(Collections.<ViewInstanceDataEntity>emptyList());

    replay(viewEntity, viewInstanceEntity, parameter1, parameter2, propertyEntity1, propertyEntity3);

    Resource resource = provider.toResource(viewInstanceEntity, propertyIds);
    Map<String, Map<String, Object>> properties = resource.getPropertiesMap();
    assertEquals(1, properties.size());
    Map<String, Object> props = properties.get("ViewInstanceInfo/properties");
    assertNotNull(props);
    assertEquals(3, props.size());
    assertEquals("val1", props.get("par1"));
    assertEquals("val3", props.get("par3"));
    assertNull(props.get("par2"));
  }
}