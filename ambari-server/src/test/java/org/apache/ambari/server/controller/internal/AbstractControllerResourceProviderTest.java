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

import junit.framework.Assert;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.codehaus.jackson.map.ser.PropertyBuilder;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.easymock.EasyMock.and;
import static org.easymock.EasyMock.createMock;

/**
 * Abstract controller resource provider test.
 */
public class AbstractControllerResourceProviderTest {
  @Test
  public void testGetResourceProvider() throws Exception {
    Set<String> propertyIds = new HashSet<String>();
    propertyIds.add("foo");
    propertyIds.add("cat1/foo");
    propertyIds.add("cat2/bar");
    propertyIds.add("cat2/baz");
    propertyIds.add("cat3/sub1/bam");
    propertyIds.add("cat4/sub2/sub3/bat");
    propertyIds.add("cat5/subcat5/map");

    Map<Resource.Type, String> keyPropertyIds = new HashMap<Resource.Type, String>();

    AmbariManagementController managementController = createMock(AmbariManagementController.class);

    AbstractResourceProvider provider =
        (AbstractResourceProvider) AbstractControllerResourceProvider.getResourceProvider(
            Resource.Type.Service,
            propertyIds,
            keyPropertyIds,
            managementController);

    Assert.assertTrue(provider instanceof ServiceResourceProvider);
  }

  @Test
  public void testGetQueryParameterValue() {

    String queryParameterId1 = "qp/variable1";
    String queryParameterValue1 = "value1";
    String queryParameterId2 = "qp/variable2";
    String queryParameterValue2 = "value2";

    //Array of predicates
    Predicate  predicate = new PredicateBuilder().property(queryParameterId1).equals(queryParameterValue1).
        and().property(queryParameterId2).equals(queryParameterValue2).toPredicate();

    Assert.assertEquals(queryParameterValue1, AbstractControllerResourceProvider.getQueryParameterValue(queryParameterId1, predicate));
    Assert.assertFalse(queryParameterValue2.equals(AbstractControllerResourceProvider.getQueryParameterValue(queryParameterId1, predicate)));
    Assert.assertNull(AbstractControllerResourceProvider.getQueryParameterValue("queryParameterIdNotFound", predicate));

    String queryParameterId3 = "qp/variable3";
    String queryParameterValue3 = "value3";

    // tests ServiceInfo/state=INSTALLED&params/run_smoke_test=true
    //Array of arrays of predicates
    predicate = new PredicateBuilder().property(queryParameterId3).equals(queryParameterValue3).
        and().begin().property(queryParameterId1).equals(queryParameterValue1).
        and().property(queryParameterId2).equals(queryParameterValue2).end().toPredicate();

    Assert.assertEquals(queryParameterValue1, AbstractControllerResourceProvider.
        getQueryParameterValue(queryParameterId1, predicate));
    Assert.assertFalse(queryParameterValue2.equals(AbstractControllerResourceProvider.
        getQueryParameterValue(queryParameterId1, predicate)));
    Assert.assertNull(AbstractControllerResourceProvider.
        getQueryParameterValue("queryParameterIdNotFound", predicate));

    Assert.assertEquals(queryParameterValue3, AbstractControllerResourceProvider.
        getQueryParameterValue(queryParameterId3, predicate));

  }

  }
