package org.apache.ambari.server.api.services;

import org.apache.ambari.server.api.resources.ResourceDefinition;
import org.apache.ambari.server.controller.internal.TemporalInfoImpl;
import org.apache.ambari.server.controller.predicate.*;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.PropertyId;
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
 * Created with IntelliJ IDEA.
 * User: john
 * Date: 10/12/12
 * Time: 11:30 AM
 * To change this template use File | Settings | File Templates.
 */
public class RequestImplTest {

  @Test
  public void testGetQueryPredicate() {

    String uri = "http://foo.bar.com/api/v1/clusters?foo=bar&orProp1=5|orProp2!=6|orProp3<100&prop!=5&prop2>10&prop3>=20&prop4<500&prop5<=1&fields=field1,category/field2";
    Request request = new TestRequest(null, null, null, Request.Type.GET, null, uri);
    Predicate predicate = request.getQueryPredicate();

    Set<Predicate> setPredicates = new HashSet<Predicate>();
    setPredicates.add(new EqualsPredicate(PropertyHelper.getPropertyId("foo"), "bar"));

    Set<Predicate> setOrPredicates = new HashSet<Predicate>();
    setOrPredicates.add(new EqualsPredicate(PropertyHelper.getPropertyId("orProp1"), "5"));
    setOrPredicates.add(new NotPredicate(new EqualsPredicate(PropertyHelper.getPropertyId("orProp2"), "6")));
    setOrPredicates.add(new LessPredicate(PropertyHelper.getPropertyId("orProp3"), "100"));
    setPredicates.add(new OrPredicate(setOrPredicates.toArray(new BasePredicate[3])));

    setPredicates.add(new NotPredicate(new EqualsPredicate(PropertyHelper.getPropertyId("prop"), "5")));
    setPredicates.add(new GreaterPredicate(PropertyHelper.getPropertyId("prop2"), "10"));
    setPredicates.add(new GreaterEqualsPredicate(PropertyHelper.getPropertyId("prop3"), "20"));
    setPredicates.add(new LessPredicate(PropertyHelper.getPropertyId("prop4"), "500"));
    setPredicates.add(new LessEqualsPredicate(PropertyHelper.getPropertyId("prop5"), "1"));
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
    Map<PropertyId, TemporalInfo> mapFields = request.getFields();

    assertEquals(7, mapFields.size());

    PropertyId prop = PropertyHelper.getPropertyId("prop", null, false);
    assertTrue(mapFields.containsKey(prop));
    assertNull(mapFields.get(prop));

    PropertyId prop1 = PropertyHelper.getPropertyId("prop1", "category", false);
    assertTrue(mapFields.containsKey(prop1));
    assertNull(mapFields.get(prop1));

    PropertyId prop2 = PropertyHelper.getPropertyId("prop2", "category2/category3", true);
    assertTrue(mapFields.containsKey(prop2));
    assertEquals(new TemporalInfoImpl(1, 2, 3), mapFields.get(prop2));

    PropertyId prop3 = PropertyHelper.getPropertyId("prop3", null, true);
    assertTrue(mapFields.containsKey(prop3));
    assertEquals(new TemporalInfoImpl(4, 5, 6), mapFields.get(prop3));

    PropertyId category4 = PropertyHelper.getPropertyId("category4", null, true);
    assertTrue(mapFields.containsKey(category4));
    assertEquals(new TemporalInfoImpl(7, 8, 9), mapFields.get(category4));

    PropertyId subResource = PropertyHelper.getPropertyId("*", "sub-resource", true);
    assertTrue(mapFields.containsKey(subResource));
    assertEquals(new TemporalInfoImpl(10, 11, 12), mapFields.get(subResource));

    PropertyId finalProp = PropertyHelper.getPropertyId("finalProp", null, false);
    assertTrue(mapFields.containsKey(finalProp));
    assertNull(mapFields.get(finalProp));


    verify(uriInfo, mapQueryParams);
  }


  private class TestRequest extends RequestImpl {
    private String m_uri;
    private TestRequest(HttpHeaders headers, String body, UriInfo uriInfo, Type requestType, ResourceDefinition resourceDefinition, String uri) {
      super(headers, body, uriInfo, requestType, resourceDefinition);
      m_uri = uri;
    }

    @Override
    public String getURI() {
      return m_uri;
    }
  }
}
