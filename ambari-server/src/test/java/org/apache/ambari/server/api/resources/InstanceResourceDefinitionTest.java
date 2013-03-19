package org.apache.ambari.server.api.resources;

import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

/**
 * Tests for InstanceResourceDefinition.
 */
public class InstanceResourceDefinitionTest {
  @Test
  public void testGetPluralName() throws Exception {
    InstanceResourceDefinition definition = new InstanceResourceDefinition();
    Assert.assertEquals("instances", definition.getPluralName());
  }

  @Test
  public void testGetSingularName() throws Exception {
    InstanceResourceDefinition definition = new InstanceResourceDefinition();
    Assert.assertEquals("instance", definition.getSingularName());
  }

  @Test
  public void testGetSubResourceDefinitions() throws Exception {
    InstanceResourceDefinition definition = new InstanceResourceDefinition();
    Set<SubResourceDefinition> subResourceDefinitions = definition.getSubResourceDefinitions ();
    Assert.assertTrue(subResourceDefinitions.isEmpty());
  }
}
