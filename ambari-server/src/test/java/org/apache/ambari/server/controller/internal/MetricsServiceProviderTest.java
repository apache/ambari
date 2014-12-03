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
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.apache.ambari.server.controller.internal.ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID;
import static org.apache.ambari.server.controller.internal.HostComponentResourceProvider.HOST_COMPONENT_CLUSTER_NAME_PROPERTY_ID;
import static org.apache.ambari.server.controller.internal.HostComponentResourceProvider.HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID;
import static org.apache.ambari.server.controller.internal.HostComponentResourceProvider.HOST_COMPONENT_HOST_NAME_PROPERTY_ID;
import static org.apache.ambari.server.controller.metrics.MetricsServiceProvider.MetricsService;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MetricsServiceProviderTest {

  @SuppressWarnings("unchecked")
  @Test
  public void testResetMetricProviderMap() throws Exception {
    TestMetricProviderModule providerModule = new TestMetricProviderModule();

    ResourceProvider clusterResourceProvider = providerModule.getResourceProvider(Resource.Type.Cluster);

    final Resource cluster = mock(Resource.class);
    final Set<Resource> resources = new HashSet<Resource>() {{ add(cluster); }};
    when(clusterResourceProvider.getResources((Request) any(),
      (Predicate) any())).thenReturn(resources);
    when(cluster.getPropertyValue(CLUSTER_NAME_PROPERTY_ID)).thenReturn("c1");

    // Assert requests and predicates used
    ResourceProvider hostComponentResourceProvider = providerModule
      .getResourceProvider(Resource.Type.HostComponent);

    Set<String> propertyIds = new HashSet<String>();
    propertyIds.add(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID);
    propertyIds.add(HOST_COMPONENT_HOST_NAME_PROPERTY_ID);

    Request expectedRequest = PropertyHelper.getReadRequest(propertyIds);

    Predicate expectedPredicate1 =
      new PredicateBuilder().property(HOST_COMPONENT_CLUSTER_NAME_PROPERTY_ID)
        .equals("c1").and().property(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID)
        .equals("GANGLIA_SERVER").toPredicate();

    Predicate expectedPredicate2 =
      new PredicateBuilder().property(HOST_COMPONENT_CLUSTER_NAME_PROPERTY_ID)
        .equals("c1").and().property(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID)
        .equals("METRIC_COLLECTOR").toPredicate();

    ArgumentCaptor<Request> requestCapture = ArgumentCaptor.forClass(Request.class);
    ArgumentCaptor<Predicate> predicateCapture = ArgumentCaptor.forClass(Predicate.class);

    when(hostComponentResourceProvider.getResources(any(Request.class),
      any(Predicate.class))).thenReturn(Collections.<Resource>emptySet());

    // Assert default null
    MetricsService service = providerModule.getMetricsServiceType();
    verify(hostComponentResourceProvider, times(3)).getResources(requestCapture
      .capture(), predicateCapture.capture());

    List<Request> requests = requestCapture.getAllValues();
    Request request1 = requests.get(requests.size() - 2); // GANGLIA check
    Request request2 = requests.get(requests.size() - 1); // COLLECTOR check

    List<Predicate> predicates = predicateCapture.getAllValues();
    Predicate predicate1 = predicates.get(predicates.size() - 2);
    Predicate predicate2 = predicates.get(predicates.size() - 1);

    Assert.assertEquals(expectedRequest, request1);
    Assert.assertEquals(expectedRequest, request2);
    Assert.assertEquals(expectedPredicate1, predicate1);
    Assert.assertEquals(expectedPredicate2, predicate2);

    Assert.assertEquals(null, service);

    // Assert change to actual service

    final Resource hostComponent = mock(Resource.class);
    Set<Resource> hostComponents = new HashSet<Resource>() {{ add(hostComponent); }};
    when(hostComponentResourceProvider.getResources(any(Request.class),
      any(Predicate.class))).thenReturn(hostComponents);
    when(hostComponent.getPropertyValue
      (HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID)).thenReturn("METRIC_COLLECTOR");
    when(hostComponent.getPropertyValue(HOST_COMPONENT_HOST_NAME_PROPERTY_ID)).thenReturn("h1");

    service = providerModule.getMetricsServiceType();
    Assert.assertEquals(MetricsService.TIMELINE_METRICS, service);
  }

  private static class TestMetricProviderModule extends AbstractProviderModule {
    ResourceProvider clusterResourceProvider = mock(ClusterResourceProvider.class);

    ResourceProvider hostCompResourceProvider = mock(HostComponentResourceProvider.class);

    @Override
    protected ResourceProvider createResourceProvider(Resource.Type type) {
      if (type == Resource.Type.Cluster)
        return clusterResourceProvider;
      else if (type == Resource.Type.HostComponent)
        return hostCompResourceProvider;
      return null;
    }
  }
}
