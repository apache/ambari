package org.apache.ambari.resource.statemachine;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;

import org.apache.ambari.common.rest.entities.ClusterDefinition;
import org.apache.ambari.components.ComponentPlugin;
import org.apache.ambari.controller.Cluster;
import org.testng.annotations.Test;

public class TestClusterImpl {

  /**
   * Create cluster with two components, both having active roles.
   * There should be two component objects in the ClusterImpl created
   * @throws IOException
   */
  @Test
  public void testClusterImplWithTwoActiveComponents() throws IOException {

    //set component plugin that returns one active role
    ComponentPlugin pluginWActiveRole = mock(ComponentPlugin.class);
    String[] servicesWithActive = {"abc"};
    when(pluginWActiveRole.getActiveRoles()).thenReturn(servicesWithActive);

    ClusterImpl clusterImpl = buildClusterImplWithComponents(pluginWActiveRole, pluginWActiveRole);
    assertEquals(clusterImpl.getServices().size(), 2, "number of components with active service");     

  }
  
  /**
   * Create cluster with two components, only one of which has active role(s)
   * There should be only one component object in the ClusterImpl created
   * @throws IOException
   */
  @Test
  public void testClusterImplWithOneActiveComponents() throws IOException {

    //set component plugin that returns one active role
    ComponentPlugin pluginWActiveRole = mock(ComponentPlugin.class);
    String[] servicesWithActive = {"abc"};
    when(pluginWActiveRole.getActiveRoles()).thenReturn(servicesWithActive);

    //set component plugin that returns NO active roles
    ComponentPlugin pluginWOActiveRole = mock(ComponentPlugin.class);
    String[] servicesNoActive = {};
    when(pluginWOActiveRole.getActiveRoles()).thenReturn(servicesNoActive);
    
    ClusterImpl clusterImpl = buildClusterImplWithComponents(pluginWActiveRole, pluginWOActiveRole);
    assertEquals(clusterImpl.getServices().size(), 1, "number of components with active service");     
    
  }
  
  

  /**
   * Create a mocked ClusterImpl that has two components, using the ComponentPlugins args 
   * @param componentPlugin1 - the ComponentPlugin for first component
   * @param componentPlugin2 - the ComponentPlugin for second component
   * @return the ClusterImpl 
   * @throws IOException
   */
  private ClusterImpl buildClusterImplWithComponents(
      ComponentPlugin componentPlugin1, ComponentPlugin componentPlugin2)
          throws IOException {
    //set list of components
    ClusterDefinition cdef = mock(ClusterDefinition.class);
    when(cdef.getEnabledServices()).thenReturn(Arrays.asList("comp1","comp2"));

    Cluster cluster = mock(Cluster.class);
    when(cluster.getClusterDefinition(anyInt())).thenReturn(cdef);

    when(cluster.getComponentDefinition("comp1")).thenReturn(componentPlugin1);
    when(cluster.getComponentDefinition("comp2")).thenReturn(componentPlugin2);

    ClusterImpl clusterImpl = new ClusterImpl(cluster, 1, null);
    return clusterImpl;
  }

}
