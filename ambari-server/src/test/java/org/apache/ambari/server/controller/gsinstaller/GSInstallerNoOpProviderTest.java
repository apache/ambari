package org.apache.ambari.server.controller.gsinstaller;

import junit.framework.Assert;
import org.apache.ambari.server.controller.spi.Resource;
import org.junit.Test;

import java.util.Collections;

/**
 * GSInstallerNoOpProvider tests.
 */
public class GSInstallerNoOpProviderTest {

  @Test
  public void testGetKeyPropertyIds() throws Exception {
    ClusterDefinition clusterDefinition = new ClusterDefinition(new TestGSInstallerStateProvider());
    GSInstallerNoOpProvider provider = new GSInstallerNoOpProvider(Resource.Type.Workflow, clusterDefinition);
    Assert.assertNotNull(provider.getKeyPropertyIds());
  }

  @Test
  public void testCheckPropertyIds() throws Exception {
    ClusterDefinition clusterDefinition = new ClusterDefinition(new TestGSInstallerStateProvider());
    GSInstallerNoOpProvider provider = new GSInstallerNoOpProvider(Resource.Type.Workflow, clusterDefinition);
    Assert.assertTrue(provider.checkPropertyIds(Collections.singleton("id")).isEmpty());
  }
}
