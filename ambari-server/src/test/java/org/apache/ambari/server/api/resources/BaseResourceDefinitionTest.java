package org.apache.ambari.server.api.resources;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.anyObject;

import org.apache.ambari.server.api.handlers.BaseManagementHandler;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.api.util.TreeNodeImpl;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ResourceProviderFactory;
import org.apache.ambari.server.controller.internal.AbstractControllerResourceProvider;
import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.internal.ServiceResourceProvider;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * BaseResourceDefinition tests.
 */
public class BaseResourceDefinitionTest {


  @Test
  public void testGetPostProcessors() {
    BaseResourceDefinition resourceDefinition = getResourceDefinition();

    List<ResourceDefinition.PostProcessor> postProcessors = resourceDefinition.getPostProcessors();

    Assert.assertEquals(1, postProcessors.size());

    ResourceDefinition.PostProcessor processor = postProcessors.iterator().next();

    Resource service = new ResourceImpl(Resource.Type.Service);
    service.setProperty("ServiceInfo/service_name", "Service1");

    TreeNode<Resource> parentNode  = new TreeNodeImpl<Resource>(null, null, "services");
    TreeNode<Resource> serviceNode = new TreeNodeImpl<Resource>(parentNode, service, "service1");

    parentNode.setProperty("isCollection", "true");
    
    ResourceProviderFactory factory = createMock(ResourceProviderFactory.class);
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    
    ResourceProvider serviceResourceProvider = new ServiceResourceProvider(PropertyHelper
        .getPropertyIds(Resource.Type.Service),
        PropertyHelper.getKeyPropertyIds(Resource.Type.Service),
        managementController);
    
    expect(factory.getServiceResourceProvider(anyObject(Set.class),
        anyObject(Map.class),
        anyObject(AmbariManagementController.class))).andReturn(serviceResourceProvider);
    
    AbstractControllerResourceProvider.init(factory);
    
    replay(factory, managementController);
    
    processor.process(null, serviceNode, "http://c6401.ambari.apache.org:8080/api/v1/clusters/c1/services");

    String href = serviceNode.getProperty("href");

    Assert.assertEquals("http://c6401.ambari.apache.org:8080/api/v1/clusters/c1/services/Service1", href);


    Resource configGroup = new ResourceImpl(Resource.Type.ConfigGroup);
    configGroup.setProperty("ConfigGroup/id", "2");

    TreeNode<Resource> resourcesNode   = new TreeNodeImpl<Resource>(null, null, BaseManagementHandler.RESOURCES_NODE_NAME);
    TreeNode<Resource> configGroupNode = new TreeNodeImpl<Resource>(resourcesNode, configGroup, "configGroup1");

    resourcesNode.setProperty("isCollection", "true");

    processor.process(null, configGroupNode, "http://c6401.ambari.apache.org:8080/api/v1/clusters/c1/config_groups");

    href = configGroupNode.getProperty("href");

    Assert.assertEquals("http://c6401.ambari.apache.org:8080/api/v1/clusters/c1/config_groups/2", href);
  }

  private BaseResourceDefinition getResourceDefinition() {
    return new BaseResourceDefinition(Resource.Type.Service) {
      @Override
      public String getPluralName() {
        return "pluralName";
      }

      @Override
      public String getSingularName() {
        return "singularName";
      }
    };
  }
}
