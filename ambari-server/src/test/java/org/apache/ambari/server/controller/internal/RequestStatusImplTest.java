package org.apache.ambari.server.controller.internal;

import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.Set;

/**
 * RequestStatusImpl Tests
 */
public class RequestStatusImplTest {
  @Test
  public void testGetAssociatedResources() throws Exception {
    RequestStatusImpl status = new RequestStatusImpl(null);
    Assert.assertEquals(Collections.emptySet(), status.getAssociatedResources());


    Resource associatedResource = new ResourceImpl(Resource.Type.Service);
    Set<Resource> associatedResources = Collections.singleton(associatedResource);
    status = new RequestStatusImpl(null, associatedResources);
    Assert.assertEquals(associatedResources, status.getAssociatedResources());
  }

  @Test
  public void testGetRequestResource() throws Exception {
    RequestStatusImpl status = new RequestStatusImpl(null);
    Assert.assertNull(status.getRequestResource());

    Resource requestResource = new ResourceImpl(Resource.Type.Request);
    status = new RequestStatusImpl(requestResource);

    Assert.assertEquals(requestResource, status.getRequestResource());
  }

  @Test
  public void testGetStatus() throws Exception {
    RequestStatusImpl status = new RequestStatusImpl(null);
    Assert.assertEquals(RequestStatus.Status.Complete, status.getStatus());

    Resource requestResource = new ResourceImpl(Resource.Type.Request);
    requestResource.setProperty("Requests/status", "InProgress");
    status = new RequestStatusImpl(requestResource);
    Assert.assertEquals(RequestStatus.Status.InProgress, status.getStatus());
  }
}
