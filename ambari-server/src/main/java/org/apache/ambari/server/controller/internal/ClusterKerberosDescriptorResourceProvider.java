/*
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

import com.google.inject.Inject;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.ClusterController;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.Resource.Type;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.ClusterControllerHelper;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptorFactory;
import org.apache.commons.lang.StringUtils;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A read-only resource provider to get a the Kerberos Descriptor relevant to the cluster.
 * <p/>
 * The following types are available
 * <ul>
 * <li>STACK - the default descriptor for the relevant stack</li>
 * <li>USER - the user-supplied updates to be applied to the default descriptor</li>
 * <li>COMPOSITE - the default descriptor for the relevant stack with the the user-supplied updates applied</li>
 * </ul>
 */
@StaticallyInject
public class ClusterKerberosDescriptorResourceProvider extends ReadOnlyResourceProvider {

  // ----- Property ID constants ---------------------------------------------

  public static final String CLUSTER_KERBEROS_DESCRIPTOR_CLUSTER_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("KerberosDescriptor", "cluster_name");
  public static final String CLUSTER_KERBEROS_DESCRIPTOR_TYPE_PROPERTY_ID = PropertyHelper.getPropertyId("KerberosDescriptor", "type");
  public static final String CLUSTER_KERBEROS_DESCRIPTOR_DESCRIPTOR_PROPERTY_ID = PropertyHelper.getPropertyId("KerberosDescriptor", "kerberos_descriptor");

  private static final Set<String> PK_PROPERTY_IDS;
  private static final Set<String> PROPERTY_IDS;
  private static final Map<Type, String> KEY_PROPERTY_IDS;

  private static final Set<RoleAuthorization> REQUIRED_GET_AUTHORIZATIONS = EnumSet.of(RoleAuthorization.CLUSTER_TOGGLE_KERBEROS,
      RoleAuthorization.CLUSTER_VIEW_CONFIGS,
      RoleAuthorization.HOST_VIEW_CONFIGS,
      RoleAuthorization.SERVICE_VIEW_CONFIGS);

  static {
    Set<String> set;
    set = new HashSet<String>();
    set.add(CLUSTER_KERBEROS_DESCRIPTOR_CLUSTER_NAME_PROPERTY_ID);
    set.add(CLUSTER_KERBEROS_DESCRIPTOR_TYPE_PROPERTY_ID);
    PK_PROPERTY_IDS = Collections.unmodifiableSet(set);

    set = new HashSet<String>();
    set.add(CLUSTER_KERBEROS_DESCRIPTOR_CLUSTER_NAME_PROPERTY_ID);
    set.add(CLUSTER_KERBEROS_DESCRIPTOR_TYPE_PROPERTY_ID);
    set.add(CLUSTER_KERBEROS_DESCRIPTOR_DESCRIPTOR_PROPERTY_ID);
    PROPERTY_IDS = Collections.unmodifiableSet(set);

    HashMap<Type, String> map = new HashMap<Type, String>();
    map.put(Type.Cluster, CLUSTER_KERBEROS_DESCRIPTOR_CLUSTER_NAME_PROPERTY_ID);
    map.put(Type.ClusterKerberosDescriptor, CLUSTER_KERBEROS_DESCRIPTOR_TYPE_PROPERTY_ID);
    KEY_PROPERTY_IDS = Collections.unmodifiableMap(map);
  }

  @Inject
  private static KerberosDescriptorFactory kerberosDescriptorFactory;

  /**
   * Used to get kerberos descriptors associated with the cluster or stack.
   * Currently not available via injection.
   */
  private static ClusterController clusterController = null;


  /**
   * Create a new resource provider.
   */
  public ClusterKerberosDescriptorResourceProvider(AmbariManagementController managementController) {
    super(PROPERTY_IDS, KEY_PROPERTY_IDS, managementController);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    // Ensure the authenticated use has access to this data for any cluster...
    AuthorizationHelper.verifyAuthorization(ResourceType.CLUSTER, null, REQUIRED_GET_AUTHORIZATIONS);

    Set<String> requestedIds = getRequestPropertyIds(request, predicate);
    Set<Resource> resources = new HashSet<Resource>();

    AmbariManagementController managementController = getManagementController();
    Clusters clusters = managementController.getClusters();

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      String clusterName = getClusterName(propertyMap);

      Cluster cluster;
      try {
        cluster = clusters.getCluster(clusterName);

        if (cluster == null) {
          throw new NoSuchParentResourceException(String.format("A cluster with the name %s does not exist.", clusterName));
        }
      } catch (AmbariException e) {
        throw new NoSuchParentResourceException(String.format("A cluster with the name %s does not exist.", clusterName));
      }

      // Ensure the authenticated use has access to this data for the requested cluster...
      AuthorizationHelper.verifyAuthorization(ResourceType.CLUSTER, cluster.getResourceId(), REQUIRED_GET_AUTHORIZATIONS);

      KerberosDescriptorType kerberosDescriptorType = getKerberosDescriptorType(propertyMap);
      if (kerberosDescriptorType == null) {
        for (KerberosDescriptorType type : KerberosDescriptorType.values()) {
          resources.add(toResource(clusterName, type, null, requestedIds));
        }
      } else {
        KerberosDescriptor kerberosDescriptor;
        try {
          kerberosDescriptor = getKerberosDescriptor(cluster, kerberosDescriptorType);
        } catch (AmbariException e) {
          throw new SystemException("An unexpected error occurred building the cluster's composite Kerberos Descriptor", e);
        }

        if (kerberosDescriptor != null) {
          resources.add(toResource(clusterName, kerberosDescriptorType, kerberosDescriptor, requestedIds));
        }
      }
    }

    return resources;
  }

  /**
   * Retrieves the cluster name from the request property map.
   *
   * @param propertyMap the request property map
   * @return a cluster name
   * @throws IllegalArgumentException if the cluster name value is missing or empty.
   */
  private String getClusterName(Map<String, Object> propertyMap) {
    String clusterName = (String) propertyMap.get(CLUSTER_KERBEROS_DESCRIPTOR_CLUSTER_NAME_PROPERTY_ID);

    if (StringUtils.isEmpty(clusterName)) {
      throw new IllegalArgumentException("Invalid argument, cluster name is required");
    }

    return clusterName;
  }

  /**
   * Retrieves the Kerberos descriptor type from the request property map, if one was specified.
   * <p/>
   * See {@link org.apache.ambari.server.controller.internal.ClusterKerberosDescriptorResourceProvider.KerberosDescriptorType}
   * for expected values.
   *
   * @param propertyMap the request property map
   * @return a KerberosDescriptorType; or null is not specified in the request propery map
   * @throws IllegalArgumentException if the Kerberos descriptor type value is specified but not an expected value.
   */
  private KerberosDescriptorType getKerberosDescriptorType(Map<String, Object> propertyMap) {
    String type = (String) propertyMap.get(CLUSTER_KERBEROS_DESCRIPTOR_TYPE_PROPERTY_ID);
    KerberosDescriptorType kerberosDescriptorType = null;

    if (!StringUtils.isEmpty(type)) {
      try {
        kerberosDescriptorType = KerberosDescriptorType.valueOf(type.trim().toUpperCase());
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("Invalid argument, kerberos descriptor type of 'STACK', 'USER', or 'COMPOSITE' is required");
      }
    }

    return kerberosDescriptorType;
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return PK_PROPERTY_IDS;
  }

  protected KerberosDescriptor getKerberosDescriptor(Cluster cluster, KerberosDescriptorType type) throws AmbariException {

    KerberosDescriptor stackDescriptor = (type == KerberosDescriptorType.STACK || type == KerberosDescriptorType.COMPOSITE)
        ? getKerberosDescriptorFromStack(cluster.getCurrentStackVersion())
        : null;

    KerberosDescriptor userDescriptor = (type == KerberosDescriptorType.USER || type == KerberosDescriptorType.COMPOSITE)
        ? getKerberosDescriptorUpdates(cluster.getClusterName())
        : null;

    if (stackDescriptor == null) {
      if (userDescriptor == null) {
        return new KerberosDescriptor();
      } else {
        return userDescriptor;
      }
    } else {
      if (userDescriptor != null) {
        stackDescriptor.update(userDescriptor);
      }
      return stackDescriptor;
    }
  }

  /**
   * Get the user-supplied Kerberos descriptor from <code>cluster/:clusterName/artifacts/kerberos_descriptor</code>
   *
   * @param clusterName the cluster name
   * @return a Kerberos descriptor
   */
  private KerberosDescriptor getKerberosDescriptorUpdates(String clusterName) throws AmbariException {
    //
    KerberosDescriptor descriptor = null;

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate = pb.begin().property("Artifacts/cluster_name").equals(clusterName).and().
        property(ArtifactResourceProvider.ARTIFACT_NAME_PROPERTY).equals("kerberos_descriptor").
        end().toPredicate();

    synchronized (ClusterKerberosDescriptorResourceProvider.class) {
      if (clusterController == null) {
        clusterController = ClusterControllerHelper.getClusterController();
      }
    }

    ResourceProvider artifactProvider = clusterController.ensureResourceProvider(Resource.Type.Artifact);

    Request request = new RequestImpl(Collections.<String>emptySet(),
        Collections.<Map<String, Object>>emptySet(), Collections.<String, String>emptyMap(), null);

    Set<Resource> response = null;
    try {
      response = artifactProvider.getResources(request, predicate);
    } catch (AuthorizationException e) {
      throw new AmbariException(e.getMessage(), e);
    } catch (SystemException | UnsupportedPropertyException | NoSuchParentResourceException e) {
      throw new AmbariException("An unknown error occurred while trying to obtain the cluster's kerberos descriptor artifact", e);
    } catch (NoSuchResourceException e) {
      // no descriptor registered, use the default from the stack
    }

    if (response != null && !response.isEmpty()) {
      Resource descriptorResource = response.iterator().next();
      Map<String, Map<String, Object>> propertyMap = descriptorResource.getPropertiesMap();
      if (propertyMap != null) {
        Map<String, Object> artifactData = propertyMap.get(ArtifactResourceProvider.ARTIFACT_DATA_PROPERTY);
        Map<String, Object> artifactDataProperties = propertyMap.get(ArtifactResourceProvider.ARTIFACT_DATA_PROPERTY + "/properties");
        HashMap<String, Object> data = new HashMap<String, Object>();

        if (artifactData != null) {
          data.putAll(artifactData);
        }

        if (artifactDataProperties != null) {
          data.put("properties", artifactDataProperties);
        }

        descriptor = kerberosDescriptorFactory.createInstance(data);
      }
    }

    return descriptor;
  }

  /**
   * Get the default Kerberos descriptor from the stack, which is the same as the value from
   * <code>stacks/:stackName/versions/:version/artifacts/kerberos_descriptor</code>
   *
   * @param stackId the stack id
   * @return a Kerberos Descriptor
   * @throws AmbariException if an error occurs while retrieving the Kerberos descriptor
   */
  private KerberosDescriptor getKerberosDescriptorFromStack(StackId stackId) throws AmbariException {
    AmbariManagementController managementController = getManagementController();
    AmbariMetaInfo metaInfo = managementController.getAmbariMetaInfo();
    return metaInfo.getKerberosDescriptor(stackId.getStackName(), stackId.getStackVersion());
  }

  /**
   * Creates a new resource from the given cluster name, alias, and persist values.
   *
   * @param clusterName            a cluster name
   * @param kerberosDescriptorType a Kerberos descriptor type
   * @param kerberosDescriptor     a Kerberos descriptor
   * @param requestedIds           the properties to include in the resulting resource instance
   * @return a resource
   */
  private Resource toResource(String clusterName, KerberosDescriptorType kerberosDescriptorType,
                              KerberosDescriptor kerberosDescriptor, Set<String> requestedIds) {
    Resource resource = new ResourceImpl(Type.ClusterKerberosDescriptor);

    setResourceProperty(resource, CLUSTER_KERBEROS_DESCRIPTOR_CLUSTER_NAME_PROPERTY_ID, clusterName, requestedIds);

    if (kerberosDescriptorType != null) {
      setResourceProperty(resource, CLUSTER_KERBEROS_DESCRIPTOR_TYPE_PROPERTY_ID, kerberosDescriptorType.name(), requestedIds);
    }

    if (kerberosDescriptor != null) {
      setResourceProperty(resource, CLUSTER_KERBEROS_DESCRIPTOR_DESCRIPTOR_PROPERTY_ID, kerberosDescriptor.toMap(), requestedIds);
    }

    return resource;
  }

  public enum KerberosDescriptorType {
    STACK,
    USER,
    COMPOSITE
  }
}
