package org.apache.ambari.server.checks;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Provider;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.apache.ambari.server.state.UpgradeState.IN_PROGRESS;
import static org.apache.ambari.server.state.UpgradeState.VERSION_MISMATCH;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Checks VersionMismatchCheck pre-upgrade check. Includes tests that emulate both
 * clusters with and without host components in VERSION_MISMATCH upgrade state.
 */
public class VersionMismatchCheckTest {
  private static final String CLUSTER_NAME = "cluster1";
  private static final String FIRST_SERVICE_NAME = "service1";
  private static final String FIRST_SERVICE_COMPONENT_NAME = "component1";
  private static final String FIRST_SERVICE_COMPONENT_HOST_NAME = "host1";
  private VersionMismatchCheck versionMismatchCheck;
  private Map<String, ServiceComponentHost> firstServiceComponentHosts;

  @Before
  public void setUp() throws Exception {
    final Clusters clusters = mock(Clusters.class);
    versionMismatchCheck = new VersionMismatchCheck();
    versionMismatchCheck.clustersProvider = new Provider<Clusters>() {
      @Override
      public Clusters get() {
        return clusters;
      }
    };

    Cluster cluster = mock(Cluster.class);
    when(clusters.getCluster(CLUSTER_NAME)).thenReturn(cluster);

    Service firstService = mock(Service.class);
    Map<String, Service> services = ImmutableMap.of(FIRST_SERVICE_NAME, firstService);
    when(cluster.getServices()).thenReturn(services);

    ServiceComponent firstServiceComponent = mock(ServiceComponent.class);
    Map<String, ServiceComponent> components = ImmutableMap.of(FIRST_SERVICE_COMPONENT_NAME, firstServiceComponent);
    when(firstService.getServiceComponents()).thenReturn(components);

    ServiceComponentHost firstServiceComponentHost = mock(ServiceComponentHost.class);
    firstServiceComponentHosts = ImmutableMap.of(FIRST_SERVICE_COMPONENT_HOST_NAME, firstServiceComponentHost);
    when(firstServiceComponent.getServiceComponentHosts()).thenReturn(firstServiceComponentHosts);
  }

  @Test
  public void testWarningWhenHostWithVersionMismatchExists() throws Exception {
    when(firstServiceComponentHosts.get(FIRST_SERVICE_COMPONENT_HOST_NAME).getUpgradeState()).thenReturn(VERSION_MISMATCH);

    PrerequisiteCheck check = new PrerequisiteCheck(null, CLUSTER_NAME);
    versionMismatchCheck.perform(check, new PrereqCheckRequest(CLUSTER_NAME));
    Assert.assertEquals(PrereqCheckStatus.WARNING, check.getStatus());
  }

  @Test
  public void testWarningWhenHostWithVersionMismatchDoesNotExist() throws Exception {
    when(firstServiceComponentHosts.get(FIRST_SERVICE_COMPONENT_HOST_NAME).getUpgradeState()).thenReturn(IN_PROGRESS);

    PrerequisiteCheck check = new PrerequisiteCheck(null, CLUSTER_NAME);
    versionMismatchCheck.perform(check, new PrereqCheckRequest(CLUSTER_NAME));
    Assert.assertEquals(PrereqCheckStatus.PASS, check.getStatus());
  }
}