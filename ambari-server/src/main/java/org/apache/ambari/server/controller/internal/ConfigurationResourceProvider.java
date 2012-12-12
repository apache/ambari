package org.apache.ambari.server.controller.internal;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ConfigurationRequest;
import org.apache.ambari.server.controller.ConfigurationResponse;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.PropertyId;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Resource provider for configuration resources.
 */
class ConfigurationResourceProvider extends ResourceProviderImpl {

  // ----- Property ID constants ---------------------------------------------

  // Configurations (values are part of query strings and body post, so they don't have defined categories)
  protected static final PropertyId CONFIGURATION_CLUSTER_NAME_PROPERTY_ID    = PropertyHelper.getPropertyId("cluster_name","Config");
  protected static final PropertyId CONFIGURATION_CONFIG_TYPE_PROPERTY_ID     = PropertyHelper.getPropertyId("type");
  protected static final PropertyId CONFIGURATION_CONFIG_TAG_PROPERTY_ID      = PropertyHelper.getPropertyId("tag");


  private static Set<PropertyId> pkPropertyIds =
      new HashSet<PropertyId>(Arrays.asList(new PropertyId[]{
          CONFIGURATION_CLUSTER_NAME_PROPERTY_ID,
          CONFIGURATION_CONFIG_TYPE_PROPERTY_ID}));

  ConfigurationResourceProvider(Set<PropertyId> propertyIds,
                                Map<Resource.Type, PropertyId> keyPropertyIds,
                                AmbariManagementController managementController) {

    super(propertyIds, keyPropertyIds, managementController);

  }

  @Override
  public RequestStatus createResources(Request request) throws AmbariException {
    for (Map<PropertyId, Object> map : request.getProperties()) {

      String cluster = (String) map.get(CONFIGURATION_CLUSTER_NAME_PROPERTY_ID);
      String type = (String) map.get(PropertyHelper.getPropertyId("type", ""));
      String tag = (String) map.get(PropertyHelper.getPropertyId("tag", ""));
      Map<String, String> configMap = new HashMap<String, String>();

      Iterator<Map.Entry<PropertyId, Object>> it1 = map.entrySet().iterator();
      while (it1.hasNext()) {
        Map.Entry<PropertyId, Object> entry = it1.next();
        if (entry.getKey().getCategory().equals("properties") && null != entry.getValue()) {
          configMap.put(entry.getKey().getName(), entry.getValue().toString());
        }
      }

      ConfigurationRequest configRequest = new ConfigurationRequest(cluster, type, tag, configMap);

      getManagementController().createConfiguration(configRequest);
    }
    return getRequestStatus(null);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws AmbariException {

    ConfigurationRequest configRequest = getRequest(getProperties(predicate));

    // TODO : handle multiple requests
    Set<ConfigurationResponse> responses = getManagementController().getConfigurations(Collections.singleton(configRequest));

    Set<Resource> resources = new HashSet<Resource>();
    for (ConfigurationResponse response : responses) {
      Resource resource = new ResourceImpl(Resource.Type.Configuration);
      resource.setProperty(CONFIGURATION_CLUSTER_NAME_PROPERTY_ID, response.getClusterName());
      resource.setProperty(CONFIGURATION_CONFIG_TYPE_PROPERTY_ID, response.getType());
      resource.setProperty(CONFIGURATION_CONFIG_TAG_PROPERTY_ID, response.getVersionTag());
      if (null != response.getConfigs() && response.getConfigs().size() > 0) {
        Map<String, String> configs = response.getConfigs();

        for (Map.Entry<String, String> entry : configs.entrySet()) {
          PropertyId id = PropertyHelper.getPropertyId(entry.getKey(), "properties");
          resource.setProperty(id, entry.getValue());
        }
      }

      resources.add(resource);
    }
    return resources;
  }

  /**
   * Throws an exception, as Configurations cannot be updated.
   */
  @Override
  public RequestStatus updateResources(Request request, Predicate predicate)
      throws AmbariException {
    throw new AmbariException ("Cannot update a Configuration resource.");
  }

  /**
   * Throws an exception, as Configurations cannot be deleted.
   */
  @Override
  public RequestStatus deleteResources(Predicate predicate) throws AmbariException {
    throw new AmbariException ("Cannot delete a Configuration resource.");
  }

  @Override
  protected Set<PropertyId> getPKPropertyIds() {
    return pkPropertyIds;
  }

  private ConfigurationRequest getRequest(Map<PropertyId, Object> properties) {
    String type = (String) properties.get(CONFIGURATION_CONFIG_TYPE_PROPERTY_ID);

    String tag = (String) properties.get(CONFIGURATION_CONFIG_TAG_PROPERTY_ID);

    return new ConfigurationRequest(
        (String) properties.get(CONFIGURATION_CLUSTER_NAME_PROPERTY_ID),
        type, tag, new HashMap<String, String>());
  }
}
