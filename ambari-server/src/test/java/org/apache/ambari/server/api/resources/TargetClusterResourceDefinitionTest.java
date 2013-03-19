package org.apache.ambari.server.api.resources;

import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

/**
 * Tests for TargetClusterResourceDefinition.
 */
public class TargetClusterResourceDefinitionTest {
  @Test
  public void testGetPluralName() throws Exception {
    TargetClusterResourceDefinition definition = new TargetClusterResourceDefinition();
    Assert.assertEquals("targets", definition.getPluralName());
  }

  @Test
  public void testGetSingularName() throws Exception {
    TargetClusterResourceDefinition definition = new TargetClusterResourceDefinition();
    Assert.assertEquals("target", definition.getSingularName());
  }

  @Test
  public void testGetSubResourceDefinitions() throws Exception {
    TargetClusterResourceDefinition definition = new TargetClusterResourceDefinition();
    Set<SubResourceDefinition> subResourceDefinitions = definition.getSubResourceDefinitions ();
    Assert.assertTrue(subResourceDefinitions.isEmpty());
  }
}
