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
package org.apache.ambari.server.api.services;

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.controller.internal.TemporalInfoImpl;
import org.apache.ambari.server.controller.predicate.*;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.TemporalInfo;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Request tests.
 */
public class RequestImplTest {

  @Test
  public void testGetQueryPredicate() {

    String uri = "http://foo.bar.com/api/v1/clusters?foo=bar&orProp1=5|orProp2!=6|orProp3<100&prop!=5&prop2>10&prop3>=20&prop4<500&prop5<=1&fields=field1,category/field2";
    Request request = new TestRequest(null, null, null, Request.Type.GET, null, uri);
    Predicate predicate = request.getQueryPredicate();

    Set<BasePredicate> setPredicates = new HashSet<BasePredicate>();
    setPredicates.add(new EqualsPredicate<String>("foo", "bar"));

    Set<BasePredicate> setOrPredicates = new HashSet<BasePredicate>();
    setOrPredicates.add(new EqualsPredicate<String>("orProp1", "5"));
    setOrPredicates.add(new NotPredicate(new EqualsPredicate<String>("orProp2", "6")));
    setOrPredicates.add(new LessPredicate<String>("orProp3", "100"));
    setPredicates.add(new OrPredicate(setOrPredicates.toArray(new BasePredicate[3])));

    setPredicates.add(new NotPredicate(new EqualsPredicate<String>("prop", "5")));
    setPredicates.add(new GreaterPredicate<String>("prop2", "10"));
    setPredicates.add(new GreaterEqualsPredicate<String>("prop3", "20"));
    setPredicates.add(new LessPredicate<String>("prop4", "500"));
    setPredicates.add(new LessEqualsPredicate<String>("prop5", "1"));
    Predicate expectedPredicate = new AndPredicate(setPredicates.toArray(new BasePredicate[6]));

    assertEquals(expectedPredicate, predicate);
  }

  @Test
  public void testGetFields() {
    UriInfo uriInfo = createMock(UriInfo.class);
    MultivaluedMap<String, String> mapQueryParams = createMock(MultivaluedMap.class);
    String fields = "prop,category/prop1,category2/category3/prop2[1,2,3],prop3[4,5,6],category4[7,8,9],sub-resource/*[10,11,12],finalProp";

    expect(uriInfo.getQueryParameters()).andReturn(mapQueryParams);
    expect(mapQueryParams.getFirst("fields")).andReturn(fields);

    replay(uriInfo, mapQueryParams);

    Request request =  new TestRequest(null, null, uriInfo, Request.Type.GET, null, null);
    Map<String, TemporalInfo> mapFields = request.getFields();

    assertEquals(7, mapFields.size());

    String prop = "prop";
    assertTrue(mapFields.containsKey(prop));
    assertNull(mapFields.get(prop));

    String prop1 = PropertyHelper.getPropertyId("category", "prop1");
    assertTrue(mapFields.containsKey(prop1));
    assertNull(mapFields.get(prop1));

    String prop2 = PropertyHelper.getPropertyId("category2/category3", "prop2");
    assertTrue(mapFields.containsKey(prop2));
    assertEquals(new TemporalInfoImpl(1, 2, 3), mapFields.get(prop2));

    String prop3 = "prop3";
    assertTrue(mapFields.containsKey(prop3));
    assertEquals(new TemporalInfoImpl(4, 5, 6), mapFields.get(prop3));

    String category4 = "category4";
    assertTrue(mapFields.containsKey(category4));
    assertEquals(new TemporalInfoImpl(7, 8, 9), mapFields.get(category4));

    String subResource = PropertyHelper.getPropertyId("sub-resource", "*");
    assertTrue(mapFields.containsKey(subResource));
    assertEquals(new TemporalInfoImpl(10, 11, 12), mapFields.get(subResource));

    String finalProp = "finalProp";
    assertTrue(mapFields.containsKey(finalProp));
    assertNull(mapFields.get(finalProp));


    verify(uriInfo, mapQueryParams);
  }


  private class TestRequest extends RequestImpl {
    private String m_uri;
    private TestRequest(HttpHeaders headers, String body, UriInfo uriInfo, Type requestType,
                        ResourceInstance resource, String uri) {

      super(headers, body, uriInfo, requestType, resource);
      m_uri = uri;
    }

    @Override
    public String getURI() {
      return m_uri;
    }
  }
}
