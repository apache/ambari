package org.apache.ambari.server.api.services;

import org.apache.ambari.server.api.resources.ResourceDefinition;
import org.apache.ambari.server.controller.predicate.*;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;
import java.util.HashSet;
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

    Request request = new TestRequest(null, null, null, Request.Type.GET, null);
    Predicate predicate = request.getQueryPredicate();

    Set<Predicate> setPredicates = new HashSet<Predicate>();
    setPredicates.add(new EqualsPredicate(PropertyHelper.getPropertyId("foo"), "bar"));
    setPredicates.add(new NotPredicate(new EqualsPredicate(PropertyHelper.getPropertyId("prop"), "5")));
    setPredicates.add(new GreaterPredicate(PropertyHelper.getPropertyId("prop2"), "10"));
    setPredicates.add(new GreaterEqualsPredicate(PropertyHelper.getPropertyId("prop3"), "20"));
    setPredicates.add(new LessPredicate(PropertyHelper.getPropertyId("prop4"), "500"));
    setPredicates.add(new LessEqualsPredicate(PropertyHelper.getPropertyId("prop5"), "1"));
    Predicate expectedPredicate = new AndPredicate(setPredicates.toArray(new BasePredicate[6]));

    assertEquals(expectedPredicate, predicate);
  }

  private class TestRequest extends RequestImpl {
    private TestRequest(HttpHeaders headers, String body, UriInfo uriInfo, Type requestType, ResourceDefinition resourceDefinition) {
      super(headers, body, uriInfo, requestType, resourceDefinition);
    }

    @Override
    public String getURI() {
      return ("http://foo.bar.com/api/v1/clusters?foo=bar&prop!=5&prop2>10&prop3>=20&prop4<500&prop5<=1&fields=field1,category/field2");
    }
  }
}
