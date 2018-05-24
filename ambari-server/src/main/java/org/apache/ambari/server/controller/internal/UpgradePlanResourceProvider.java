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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.dao.MpackDAO;
import org.apache.ambari.server.orm.dao.ServiceGroupDAO;
import org.apache.ambari.server.orm.dao.UpgradePlanDAO;
import org.apache.ambari.server.orm.entities.MpackEntity;
import org.apache.ambari.server.orm.entities.ServiceGroupEntity;
import org.apache.ambari.server.orm.entities.UpgradePlanConfigEntity;
import org.apache.ambari.server.orm.entities.UpgradePlanDetailEntity;
import org.apache.ambari.server.orm.entities.UpgradePlanEntity;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.stack.upgrade.Direction;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.annotate.JsonProperty;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;

/**
 * Manages upgrade plans.
 */
@StaticallyInject
public class UpgradePlanResourceProvider extends AbstractControllerResourceProvider {

  private static final String UPGRADE_PLAN = "UpgradePlan" + PropertyHelper.EXTERNAL_PATH_SEP;

  protected static final String UPGRADE_PLAN_ID = UPGRADE_PLAN + "id";
  protected static final String UPGRADE_PLAN_CLUSTER_NAME = UPGRADE_PLAN + "cluster_name";

  public static final String UPGRADE_PLAN_TYPE                        = UPGRADE_PLAN + "upgrade_type";
  public static final String UPGRADE_PLAN_DIRECTION                   = UPGRADE_PLAN + "direction";
  public static final String UPGRADE_PLAN_SKIP_FAILURES               = UPGRADE_PLAN + "skip_failures";
  public static final String UPGRADE_PLAN_SKIP_PREREQUISITE_CHECKS    = UPGRADE_PLAN + "skip_prerequisite_checks";
  public static final String UPGRADE_PLAN_SKIP_SERVICE_CHECKS         = UPGRADE_PLAN + "skip_service_checks";
  public static final String UPGRADE_PLAN_SKIP_SERVICE_CHECK_FAILURES = UPGRADE_PLAN + "skip_service_check_failures";
  public static final String UPGRADE_PLAN_FAIL_ON_CHECK_WARNINGS      = UPGRADE_PLAN + "fail_on_check_warnings";

  public static final String UPGRADE_PLAN_SERVICE_GROUPS              = UPGRADE_PLAN + "servicegroups";

  private static final Map<Resource.Type, String> KEY_PROPERTY_IDS = ImmutableMap.<Resource.Type, String>builder()
      .put(Resource.Type.UpgradePlan, UPGRADE_PLAN_ID)
      .put(Resource.Type.Cluster, UPGRADE_PLAN_CLUSTER_NAME)
      .build();

  private static final Set<String> PK_PROPERTY_IDS = Sets.newHashSet(KEY_PROPERTY_IDS.values());

  private static final Set<String> PROPERTY_IDS = Sets.newHashSet(
      UPGRADE_PLAN_ID,
      UPGRADE_PLAN_CLUSTER_NAME,
      UPGRADE_PLAN_TYPE,
      UPGRADE_PLAN_DIRECTION,
      UPGRADE_PLAN_SKIP_FAILURES,
      UPGRADE_PLAN_SKIP_PREREQUISITE_CHECKS,
      UPGRADE_PLAN_SKIP_SERVICE_CHECKS,
      UPGRADE_PLAN_SKIP_SERVICE_CHECK_FAILURES,
      UPGRADE_PLAN_FAIL_ON_CHECK_WARNINGS,
      UPGRADE_PLAN_SERVICE_GROUPS);

  /**
   * Used to deserialize the repository JSON into an object.
   */
  @Inject
  private static Gson s_gson;

  @Inject
  private static UpgradePlanDAO s_upgradePlanDAO;

  @Inject
  private static ServiceGroupDAO s_serviceGroupDAO;

  @Inject
  private static MpackDAO s_mpackDAO;

  /**
   * Constructor.
   *
   * @param controller the controller
   */
  UpgradePlanResourceProvider(AmbariManagementController controller) {
    super(Resource.Type.UpgradePlan, PROPERTY_IDS, KEY_PROPERTY_IDS, controller);

    setRequiredCreateAuthorizations(EnumSet.of(RoleAuthorization.CLUSTER_UPGRADE_DOWNGRADE_STACK));
  }

  @Override
  public RequestStatus createResourcesAuthorized(final Request request)
      throws SystemException,
      UnsupportedPropertyException, ResourceAlreadyExistsException,
      NoSuchParentResourceException {

    Set<Map<String, Object>> propertyMaps = request.getProperties();

    if (propertyMaps.size() > 1) {
      throw new IllegalArgumentException("Cannot create more than one Upgrade Plan at a time");
    }

    Map<String, Object> propertyMap = propertyMaps.iterator().next();

    // !!! i hate you, framework
    @SuppressWarnings("unchecked")
    Set<Map<String, Object>> groupMaps = (Set<Map<String, Object>>) propertyMap.get(
        UPGRADE_PLAN_SERVICE_GROUPS);
    String json = s_gson.toJson(groupMaps);

    java.lang.reflect.Type listType = new TypeToken<ArrayList<ServiceGroupJson>>(){}.getType();
    List<ServiceGroupJson> l = s_gson.fromJson(json, listType);

    UpgradePlanEntity entity = toEntity(propertyMap, l);

    s_upgradePlanDAO.create(entity);

    // cannot be null since toEntity() checks for it
    String clusterName = propertyMap.get(UPGRADE_PLAN_CLUSTER_NAME).toString();

    notifyCreate(Resource.Type.UpgradePlan, request);

    Resource res = toResource(entity, PROPERTY_IDS, clusterName);

    return new RequestStatusImpl(null, Collections.singleton(res));
  }

  @Override
  public Set<Resource> getResourcesAuthorized(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    Set<String> requestPropertyIds = getRequestPropertyIds(request, predicate);
    List<UpgradePlanEntity> entities = new ArrayList<>();
    Set<Resource> results = new LinkedHashSet<>();

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      String clusterName = (String) propertyMap.get(UPGRADE_PLAN_CLUSTER_NAME);
      String upgradePlanIdStr = (String) propertyMap.get(UPGRADE_PLAN_ID);

      if (StringUtils.isNotEmpty(upgradePlanIdStr)) {
        Long upgradePlanId = Long.valueOf(upgradePlanIdStr);
        UpgradePlanEntity entity = s_upgradePlanDAO.findByPK(upgradePlanId);

        if (null == entity) {
          throw new NoSuchResourceException(String.format("Could not find upgrade plan %s",
              upgradePlanIdStr));
        }

        results.add(toResource(entity, requestPropertyIds, clusterName));
      } else {
        // find all plans
        entities.addAll(s_upgradePlanDAO.findAll());

        entities.forEach(entity -> {
          results.add(toResource(entity, requestPropertyIds, clusterName));
        });
      }
    }

    return results;
  }

  @Override
  public RequestStatus updateResourcesAuthorized(final Request request,
      Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    throw new SystemException("Upgrade plans cannot be modified");
  }

  @Override
  public RequestStatus deleteResourcesAuthorized(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {
    throw new SystemException("Upgrade plans cannot be removed");
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return PK_PROPERTY_IDS;
  }

  /**
   * Creates an entity from a request
   * @param propertyMap
   *            the property map containing values
   * @param serviceGroupJsons
   *            the collection of service groups parsed from the request
   * @return the entity represeting the request
   *
   * @throws SystemException
   */
  private UpgradePlanEntity toEntity(Map<String, Object> propertyMap, Collection<ServiceGroupJson> serviceGroupJsons)
    throws SystemException {
    Arrays.asList(UPGRADE_PLAN_CLUSTER_NAME, UPGRADE_PLAN_TYPE, UPGRADE_PLAN_DIRECTION).stream()
      .forEach(property -> {
        if (!propertyMap.containsKey(property)) {
          throw new IllegalArgumentException(
              String.format("Property %s is required for an upgrade plan", property));
        }
      });

    if (CollectionUtils.isEmpty(serviceGroupJsons)) {
      throw new IllegalArgumentException("Service groups must be provided");
    }

    String clusterName = propertyMap.get(UPGRADE_PLAN_CLUSTER_NAME).toString();
    Cluster cluster = null;
    try {
      cluster = getManagementController().getClusters().getCluster(clusterName);
    } catch (AmbariException e) {
      throw new SystemException(String.format("Could not load cluster %s", clusterName), e);
    }

    String upgradeTypeName = propertyMap.get(UPGRADE_PLAN_TYPE).toString();
    UpgradeType upgradeType = UpgradeType.valueOf(upgradeTypeName);

    String directionName = propertyMap.get(UPGRADE_PLAN_DIRECTION).toString();
    Direction direction = Direction.valueOf(directionName);

    UpgradePlanEntity entity = new UpgradePlanEntity();

    entity.setClusterId(cluster.getClusterId());
    entity.setUpgradeType(upgradeType);
    entity.setDirection(direction);

    if (propertyMap.containsKey(UPGRADE_PLAN_FAIL_ON_CHECK_WARNINGS)) {
      boolean failOn = BooleanUtils.toBoolean(propertyMap.get(UPGRADE_PLAN_FAIL_ON_CHECK_WARNINGS).toString());
      entity.setFailOnPrerequisiteWarnings(failOn);
    }

    if (propertyMap.containsKey(UPGRADE_PLAN_SKIP_FAILURES)) {
      boolean skip = BooleanUtils.toBoolean(propertyMap.get(UPGRADE_PLAN_SKIP_FAILURES).toString());
      entity.setSkipFailures(skip);
    }

    if (propertyMap.containsKey(UPGRADE_PLAN_SKIP_PREREQUISITE_CHECKS)) {
      boolean skip = BooleanUtils.toBoolean(propertyMap.get(UPGRADE_PLAN_SKIP_PREREQUISITE_CHECKS).toString());
      entity.setSkipPrerequisiteChecks(skip);
    }

    if (propertyMap.containsKey(UPGRADE_PLAN_SKIP_SERVICE_CHECK_FAILURES)) {
      boolean skip = BooleanUtils.toBoolean(propertyMap.get(UPGRADE_PLAN_SKIP_SERVICE_CHECK_FAILURES).toString());
      entity.setSkipPrerequisiteChecks(skip);
    }

    if (propertyMap.containsKey(UPGRADE_PLAN_SKIP_SERVICE_CHECKS)) {
      boolean skip = BooleanUtils.toBoolean(propertyMap.get(UPGRADE_PLAN_SKIP_SERVICE_CHECKS).toString());
      entity.setSkipServiceChecks(skip);
    }

    Long clusterId = cluster.getClusterId();
    List<UpgradePlanDetailEntity> details = new ArrayList<>();

    serviceGroupJsons.stream()
      .filter(sgJson -> sgJson.mpackTargetId != null && sgJson.serviceGroupId != null)
      .forEach(sgJson -> {

        ServiceGroupEntity sgEntity = s_serviceGroupDAO.findByPK(sgJson.serviceGroupId);
        if (null == sgEntity) {
          throw new IllegalArgumentException(
              String.format("Cannot find service group identified by %s", sgJson.serviceGroupId));
        }

        MpackEntity mpackEntity = s_mpackDAO.findById(sgJson.mpackTargetId);
        if (null == mpackEntity) {
          throw new IllegalArgumentException(
              String.format("Cannot find mpack identified by %s", sgJson.mpackTargetId));
        }

        UpgradePlanDetailEntity detail = new UpgradePlanDetailEntity();
        detail.setServiceGroupId(sgJson.serviceGroupId);
        detail.setMpackTargetId(sgJson.mpackTargetId);

        // !!! TODO During create, we have to resolve the config changes and persist them
        // to allow the user to override them.  Ignore passed-in values, this has to come
        // from the mpack

        details.add(detail);
      });


    entity.setDetails(details);

    return entity;
  }

  /**
   * Converts an entity to a resource
   * @param upgradePlan
   *            the upgrade plan entity
   * @param requestedIds
   *            the requested ids
   * @param clusterName
   *            the cluster name
   * @return the resource
   */
  private Resource toResource(UpgradePlanEntity upgradePlan, Set<String> requestedIds,
      String clusterName) {
    ResourceImpl resource = new ResourceImpl(Resource.Type.UpgradePlan);

    setResourceProperty(resource, UPGRADE_PLAN_CLUSTER_NAME, clusterName, requestedIds);
    setResourceProperty(resource, UPGRADE_PLAN_ID, upgradePlan.getId(), requestedIds);
    setResourceProperty(resource, UPGRADE_PLAN_DIRECTION, upgradePlan.getDirection(), requestedIds);
    setResourceProperty(resource, UPGRADE_PLAN_FAIL_ON_CHECK_WARNINGS, upgradePlan.isFailOnPrerequisiteWarnings(), requestedIds);
    setResourceProperty(resource, UPGRADE_PLAN_SKIP_FAILURES, upgradePlan.isSkipFailures(), requestedIds);
    setResourceProperty(resource, UPGRADE_PLAN_SKIP_PREREQUISITE_CHECKS, upgradePlan.isSkipPrerequisiteChecks(), requestedIds);
    setResourceProperty(resource, UPGRADE_PLAN_SKIP_SERVICE_CHECK_FAILURES, upgradePlan.isSkipServiceCheckFailures(), requestedIds);
    setResourceProperty(resource, UPGRADE_PLAN_SKIP_SERVICE_CHECKS, upgradePlan.isSkipServiceChecks(), requestedIds);
    setResourceProperty(resource, UPGRADE_PLAN_SERVICE_GROUPS, toResourceObject(upgradePlan.getDetails()), requestedIds);

    return resource;
  }

  private List<ServiceGroupJson> toResourceObject(List<UpgradePlanDetailEntity> details) {
    return details.stream().map(ServiceGroupJson::new).collect(Collectors.toList());
  }

  /**
   * An object representing the service groups in a request.
   */
  private static class ServiceGroupJson {

    @SerializedName("service_group_id")
    @JsonProperty("service_group_id")
    private Long serviceGroupId;

    @SerializedName("mpack_target_id")
    @JsonProperty("mpack_target_id")
    private Long mpackTargetId;

    @SerializedName("configuration_changes")
    @JsonProperty("configuration_changes")
    private List<ConfigurationChangeJson> config_changes;

    private ServiceGroupJson(UpgradePlanDetailEntity detail) {
      serviceGroupId = detail.getServiceGroupId();
      mpackTargetId = detail.getMpackTargetId();

      config_changes = detail.getConfigChanges().stream()
          .map(ConfigurationChangeJson::new).collect(Collectors.toList());
    }
  }

  /**
   * An object representing a service group's configuration changes
   */
  private static class ConfigurationChangeJson {
    @SerializedName("config_type")
    @JsonProperty("config_type")
    private String type;

    @SerializedName("config_key")
    @JsonProperty("config_key")
    private String key;

    @SerializedName("config_value")
    @JsonProperty("config_value")
    private String value;

    private ConfigurationChangeJson(UpgradePlanConfigEntity change) {
      type = change.getType();
      key = change.getKey();
      value = change.getNewValue();
    }

  }


}
