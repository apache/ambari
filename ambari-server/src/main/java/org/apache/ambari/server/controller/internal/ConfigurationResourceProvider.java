package org.apache.ambari.server.controller.internal;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ConfigurationRequest;
import org.apache.ambari.server.controller.ConfigurationResponse;
import org.apache.ambari.server.controller.ServiceComponentHostRequest;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * Resource provider for configuration resources.
 */
class ConfigurationResourceProvider extends ResourceProviderImpl {

  // ----- Property ID constants ---------------------------------------------

  // Configurations (values are part of query strings and body post, so they don't have defined categories)
  protected static final String CONFIGURATION_CLUSTER_NAME_PROPERTY_ID    = PropertyHelper.getPropertyId("Config", "cluster_name");
  protected static final String CONFIGURATION_CONFIG_TYPE_PROPERTY_ID     = PropertyHelper.getPropertyId(null, "type");
  protected static final String CONFIGURATION_CONFIG_TAG_PROPERTY_ID      = PropertyHelper.getPropertyId(null, "tag");

  private static final String CONFIG_HOST_NAME = PropertyHelper.getPropertyId("Config", "host_name");
  private static final String CONFIG_COMPONENT_NAME = PropertyHelper.getPropertyId("Config", "component_name");


  private static Set<String> pkPropertyIds =
      new HashSet<String>(Arrays.asList(new String[]{
          CONFIGURATION_CLUSTER_NAME_PROPERTY_ID,
          CONFIGURATION_CONFIG_TYPE_PROPERTY_ID}));

  ConfigurationResourceProvider(Set<String> propertyIds,
                                Map<Resource.Type, String> keyPropertyIds,
                                AmbariManagementController managementController) {

    super(propertyIds, keyPropertyIds, managementController);

  }

  @Override
  public RequestStatus createResources(Request request) throws AmbariException, UnsupportedPropertyException {
    for (Map<String, Object> map : request.getProperties()) {

      String cluster = (String) map.get(CONFIGURATION_CLUSTER_NAME_PROPERTY_ID);
      String type = (String) map.get(PropertyHelper.getPropertyId("", "type"));
      String tag = (String) map.get(PropertyHelper.getPropertyId("", "tag"));
      Map<String, String> configMap = new HashMap<String, String>();

      Iterator<Map.Entry<String, Object>> it1 = map.entrySet().iterator();
      while (it1.hasNext()) {
        Map.Entry<String, Object> entry = it1.next();
        if (PropertyHelper.getPropertyCategory(entry.getKey()).equals("properties") && null != entry.getValue()) {
          configMap.put(PropertyHelper.getPropertyName(entry.getKey()), entry.getValue().toString());
        }
      }

      ConfigurationRequest configRequest = new ConfigurationRequest(cluster, type, tag, configMap);

      getManagementController().createConfiguration(configRequest);
    }
    return getRequestStatus(null);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws AmbariException, UnsupportedPropertyException {
    Map<String, Object> map = getProperties(predicate);
    
    if (map.containsKey(CONFIG_HOST_NAME) && map.containsKey(CONFIG_COMPONENT_NAME)) {
      ServiceComponentHostRequest hostComponentRequest = new ServiceComponentHostRequest(
          (String) map.get(CONFIGURATION_CLUSTER_NAME_PROPERTY_ID),
          null,
          (String) map.get(CONFIG_COMPONENT_NAME),
          (String) map.get(CONFIG_HOST_NAME),
          null, null);
      
      Map<String, String> mappints = getManagementController().getHostComponentDesiredConfigMapping(hostComponentRequest);
      
      Set<Resource> resources = new HashSet<Resource>();
      
      for (Entry<String, String> entry : mappints.entrySet()) {
      
        Resource resource = new ResourceImpl(Resource.Type.Configuration);
        
        resource.setProperty(CONFIGURATION_CLUSTER_NAME_PROPERTY_ID, map.get(CONFIGURATION_CLUSTER_NAME_PROPERTY_ID));
        resource.setProperty(CONFIG_COMPONENT_NAME, map.get(CONFIG_COMPONENT_NAME));
        resource.setProperty(CONFIG_HOST_NAME, map.get(CONFIG_HOST_NAME));

        resource.setProperty(CONFIGURATION_CONFIG_TYPE_PROPERTY_ID, entry.getKey());
        resource.setProperty(CONFIGURATION_CONFIG_TAG_PROPERTY_ID, entry.getValue());
        
        resources.add(resource);
      }


      return resources;
      
    } else {
      // TODO : handle multiple requests
      ConfigurationRequest configRequest = getRequest(map);
      
      Set<ConfigurationResponse> responses = getManagementController().getConfigurations(Collections.singleton(configRequest));

      Set<Resource> resources = new HashSet<Resource>();
      for (ConfigurationResponse response : responses) {
        Resource resource = new ResourceImpl(Resource.Type.Configuration);
        resource.setProperty(CONFIGURATION_CLUSTER_NAME_PROPERTY_ID, response.getClusterName());
        resource.setProperty(CONFIGURATION_CONFIG_TYPE_PROPERTY_ID, response.getType());
        resource.setProperty(CONFIGURATION_CONFIG_TAG_PROPERTY_ID, response.getVersionTag());
        
        if (null != response.getConfigs() && response.getConfigs().size() > 0) {
          Map<String, String> configs = response.getConfigs();

          for (Entry<String, String> entry : configs.entrySet()) {
            String id = PropertyHelper.getPropertyId("properties", entry.getKey());
            resource.setProperty(id, entry.getValue());
          }
        }

        resources.add(resource);
      }
      
      return resources;
    }
  }

  /**
   * Throws an exception, as Configurations cannot be updated.
   */
  @Override
  public RequestStatus updateResources(Request request, Predicate predicate)
      throws AmbariException, UnsupportedPropertyException {
    throw new AmbariException ("Cannot update a Configuration resource.");
  }

  /**
   * Throws an exception, as Configurations cannot be deleted.
   */
  @Override
  public RequestStatus deleteResources(Predicate predicate) throws AmbariException, UnsupportedPropertyException {
    throw new AmbariException ("Cannot delete a Configuration resource.");
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }

  public static void getConfigPropertyValues(Map<String, Object> propertyMap, Map<String, String> configMap) {
    for (Map.Entry<String,Object> entry : propertyMap.entrySet()) {
      String propertyId = entry.getKey();
      if (PropertyHelper.getPropertyCategory(propertyId).equals("config")) {
        configMap.put(PropertyHelper.getPropertyName(propertyId), (String) entry.getValue());
      }
    }
  }

  private ConfigurationRequest getRequest(Map<String, Object> properties) {
    String type = (String) properties.get(CONFIGURATION_CONFIG_TYPE_PROPERTY_ID);

    String tag = (String) properties.get(CONFIGURATION_CONFIG_TAG_PROPERTY_ID);

    return new ConfigurationRequest(
        (String) properties.get(CONFIGURATION_CLUSTER_NAME_PROPERTY_ID),
        type, tag, new HashMap<String, String>());
  }
}
