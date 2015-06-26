/**
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

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.ExtendedResourceProvider;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.QueryResponse;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.HostRoleCommandStatusSummaryDTO;
import org.apache.ambari.server.orm.dao.StageDAO;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.StageEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.topology.TopologyManager;

/**
 * ResourceProvider for Stage
 */
@StaticallyInject
public class StageResourceProvider extends AbstractControllerResourceProvider implements ExtendedResourceProvider {

  /**
   * Used for querying stage resources.
   */
  @Inject
  private static StageDAO dao = null;

  /**
   * Used for querying task resources.
   */
  @Inject
  private static HostRoleCommandDAO hostRoleCommandDAO = null;

  @Inject
  private static Provider<Clusters> clustersProvider = null;

  @Inject
  private static TopologyManager topologyManager;

  /**
   * Stage property constants.
   */
  public static final String STAGE_STAGE_ID = "Stage/stage_id";
  public static final String STAGE_CLUSTER_NAME = "Stage/cluster_name";
  public static final String STAGE_REQUEST_ID = "Stage/request_id";
  public static final String STAGE_LOG_INFO = "Stage/log_info";
  public static final String STAGE_CONTEXT = "Stage/context";
  public static final String STAGE_CLUSTER_HOST_INFO = "Stage/cluster_host_info";
  public static final String STAGE_COMMAND_PARAMS = "Stage/command_params";
  public static final String STAGE_HOST_PARAMS = "Stage/host_params";
  public static final String STAGE_SKIPPABLE = "Stage/skippable";
  public static final String STAGE_PROGRESS_PERCENT = "Stage/progress_percent";
  public static final String STAGE_STATUS = "Stage/status";
  public static final String STAGE_START_TIME = "Stage/start_time";
  public static final String STAGE_END_TIME = "Stage/end_time";

  /**
   * The property ids for a stage resource.
   */
  static final Set<String> PROPERTY_IDS = new HashSet<String>();

  /**
   * The key property ids for a stage resource.
   */
  private static final Map<Resource.Type, String> KEY_PROPERTY_IDS =
      new HashMap<Resource.Type, String>();

  static {
    // properties
    PROPERTY_IDS.add(STAGE_STAGE_ID);
    PROPERTY_IDS.add(STAGE_CLUSTER_NAME);
    PROPERTY_IDS.add(STAGE_REQUEST_ID);
    PROPERTY_IDS.add(STAGE_LOG_INFO);
    PROPERTY_IDS.add(STAGE_CONTEXT);
    PROPERTY_IDS.add(STAGE_CLUSTER_HOST_INFO);
    PROPERTY_IDS.add(STAGE_COMMAND_PARAMS);
    PROPERTY_IDS.add(STAGE_HOST_PARAMS);
    PROPERTY_IDS.add(STAGE_SKIPPABLE);
    PROPERTY_IDS.add(STAGE_PROGRESS_PERCENT);
    PROPERTY_IDS.add(STAGE_STATUS);
    PROPERTY_IDS.add(STAGE_START_TIME);
    PROPERTY_IDS.add(STAGE_END_TIME);

    // keys
    KEY_PROPERTY_IDS.put(Resource.Type.Stage, STAGE_STAGE_ID);
    KEY_PROPERTY_IDS.put(Resource.Type.Cluster, STAGE_CLUSTER_NAME);
    KEY_PROPERTY_IDS.put(Resource.Type.Request, STAGE_REQUEST_ID);
  }

  /**
   * Mapping of valid status transitions that that are driven by manual input.
   */
  private static Map<HostRoleStatus, EnumSet<HostRoleStatus>> manualTransitionMap = new HashMap<HostRoleStatus, EnumSet<HostRoleStatus>>();

  static {
    manualTransitionMap.put(HostRoleStatus.HOLDING, EnumSet.of(HostRoleStatus.COMPLETED, HostRoleStatus.ABORTED));
    manualTransitionMap.put(HostRoleStatus.HOLDING_FAILED, EnumSet.of(HostRoleStatus.PENDING, HostRoleStatus.FAILED, HostRoleStatus.ABORTED));
    manualTransitionMap.put(HostRoleStatus.HOLDING_TIMEDOUT, EnumSet.of(HostRoleStatus.PENDING, HostRoleStatus.TIMEDOUT, HostRoleStatus.ABORTED));
    //todo: perhaps add a CANCELED status that just affects a stage and wont abort the request
    //todo: so, if I scale 10 nodes and actually provision 5 and then later decide I don't want those
    //todo: additional 5 nodes I can cancel them and the corresponding request will have a status of COMPLETED
  }


  // ----- Constructors ------------------------------------------------------

  /**
   * Constructor.
   *
   * @param managementController  the Ambari management controller
   */
  StageResourceProvider(AmbariManagementController managementController) {
    super(PROPERTY_IDS, KEY_PROPERTY_IDS, managementController);
  }

  // ----- AbstractResourceProvider ------------------------------------------

  @Override
  protected Set<String> getPKPropertyIds() {
    return new HashSet<String>(KEY_PROPERTY_IDS.values());
  }


  // ----- ResourceProvider --------------------------------------------------

  @Override
  public RequestStatus createResources(Request request) throws SystemException,
      UnsupportedPropertyException, ResourceAlreadyExistsException,
      NoSuchParentResourceException {
    throw new UnsupportedOperationException();
  }

  @Override
  public RequestStatus updateResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    Iterator<Map<String,Object>> iterator = request.getProperties().iterator();
    if (iterator.hasNext()) {

      Map<String,Object> updateProperties = iterator.next();

      List<StageEntity> entities = dao.findAll(request, predicate);
      for (StageEntity entity : entities) {

        String stageStatus = (String) updateProperties.get(STAGE_STATUS);
        if (stageStatus != null) {
          HostRoleStatus desiredStatus = HostRoleStatus.valueOf(stageStatus);
          updateStageStatus(entity, desiredStatus, getManagementController());
        }
      }
    }
    notifyUpdate(Resource.Type.Stage, request, predicate);

    return getRequestStatus(null);
  }

  @Override
  public RequestStatus deleteResources(Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    Set<Resource> results     = new LinkedHashSet<Resource>();
    Set<String>   propertyIds = getRequestPropertyIds(request, predicate);

    // !!! poor mans cache.  toResource() shouldn't be calling the db
    // every time, when the request id is likely the same for each stageEntity
    Map<Long, Map<Long, HostRoleCommandStatusSummaryDTO>> cache =
        new HashMap<Long, Map<Long, HostRoleCommandStatusSummaryDTO>>();

    List<StageEntity> entities = dao.findAll(request, predicate);
    for (StageEntity entity : entities) {
      results.add(toResource(cache, entity, propertyIds));
    }

    cache.clear();

    Collection<StageEntity> topologyManagerStages = topologyManager.getStages();
    for (StageEntity entity : topologyManagerStages) {
      Resource stageResource = toResource(entity, propertyIds);
      if (predicate.evaluate(stageResource)) {
        results.add(stageResource);
      }
    }

    return results;
  }


  // ----- ExtendedResourceProvider ------------------------------------------

  @Override
  public QueryResponse queryForResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    Set<Resource> results = getResources(request, predicate);

    return new QueryResponseImpl(results, request.getSortRequest() != null, false, results.size());
  }

  // ----- StageResourceProvider ---------------------------------------------

  /**
   * Update the stage identified by the given stage id with the desired status.
   *
   * @param requestId      the request id
   * @param stageId        the stage id
   * @param desiredStatus  the desired stage status
   * @param controller     the ambari management controller
   */
  public static void updateStageStatus(long requestId, long stageId, HostRoleStatus desiredStatus,
                                       AmbariManagementController controller) {
    Predicate predicate = new PredicateBuilder().property(STAGE_STAGE_ID).equals(stageId).and().
        property(STAGE_REQUEST_ID).equals(requestId).toPredicate();

    List<StageEntity> entityList = dao.findAll(PropertyHelper.getReadRequest(), predicate);
    for (StageEntity stageEntity : entityList) {
      updateStageStatus(stageEntity, desiredStatus, controller);
    }
  }


  // ----- helper methods ----------------------------------------------------

  /**
   * Update the given stage entity with the desired status.
   *
   * @param stage          the stage entity to update
   * @param desiredStatus  the desired stage status
   * @param controller     the ambari management controller
   *
   * @throws java.lang.IllegalArgumentException if the transition to the desired status is not a
   *         legal transition
   */
  private static void updateStageStatus(StageEntity stage, HostRoleStatus desiredStatus,
                                        AmbariManagementController controller) {
    Collection<HostRoleCommandEntity> tasks = stage.getHostRoleCommands();

    HostRoleStatus currentStatus = CalculatedStatus.statusFromTaskEntities(tasks, stage.isSkippable()).getStatus();

    if (!isValidManualTransition(currentStatus, desiredStatus)) {
      throw new IllegalArgumentException("Can not transition a stage from " +
          currentStatus + " to " + desiredStatus);
    }
    if (desiredStatus == HostRoleStatus.ABORTED) {
      controller.getActionManager().cancelRequest(stage.getRequestId(), "User aborted.");
    } else {
      for (HostRoleCommandEntity hostRoleCommand : tasks) {
        HostRoleStatus hostRoleStatus = hostRoleCommand.getStatus();
        if (hostRoleStatus.equals(currentStatus)) {
          hostRoleCommand.setStatus(desiredStatus);

          if (desiredStatus == HostRoleStatus.PENDING) {
            hostRoleCommand.setStartTime(-1L);
          }
          hostRoleCommandDAO.merge(hostRoleCommand);
        }
      }
    }
  }

  /**
   * Converts the {@link StageEntity} to a {@link Resource}.
   *
   * @param entity        the entity to convert (not {@code null})
   * @param requestedIds  the properties requested (not {@code null})
   *
   * @return the new resource
   */
  private Resource toResource(
      Map<Long, Map<Long, HostRoleCommandStatusSummaryDTO>> cache,
      StageEntity entity,
      Set<String> requestedIds) {

    Resource resource = new ResourceImpl(Resource.Type.Stage);

    Long clusterId = entity.getClusterId();
    if (clusterId != null && !clusterId.equals(Long.valueOf(-1L))) {
      try {
        Cluster cluster = clustersProvider.get().getClusterById(clusterId);

        setResourceProperty(resource, STAGE_CLUSTER_NAME, cluster.getClusterName(), requestedIds);
      } catch (Exception e) {
        LOG.error("Can not get information for cluster " + clusterId + ".", e );
      }
    }

    if (!cache.containsKey(entity.getRequestId())) {
      cache.put(entity.getRequestId(), hostRoleCommandDAO.findAggregateCounts(entity.getRequestId()));
    }

    Map<Long, HostRoleCommandStatusSummaryDTO> summary = cache.get(entity.getRequestId());

    setResourceProperty(resource, STAGE_STAGE_ID, entity.getStageId(), requestedIds);
    setResourceProperty(resource, STAGE_REQUEST_ID, entity.getRequestId(), requestedIds);
    setResourceProperty(resource, STAGE_CONTEXT, entity.getRequestContext(), requestedIds);

    // this property is lazy loaded in JPA; don't use it unless requested
    if (isPropertyRequested(STAGE_CLUSTER_HOST_INFO, requestedIds)) {
      resource.setProperty(STAGE_CLUSTER_HOST_INFO, entity.getClusterHostInfo());
    }

    // this property is lazy loaded in JPA; don't use it unless requested
    if (isPropertyRequested(STAGE_COMMAND_PARAMS, requestedIds)) {
      resource.setProperty(STAGE_COMMAND_PARAMS, entity.getCommandParamsStage());
    }

    // this property is lazy loaded in JPA; don't use it unless requested
    if (isPropertyRequested(STAGE_HOST_PARAMS, requestedIds)) {
      resource.setProperty(STAGE_HOST_PARAMS, entity.getHostParamsStage());
    }

    setResourceProperty(resource, STAGE_SKIPPABLE, entity.isSkippable(), requestedIds);

    Long startTime = Long.MAX_VALUE;
    Long endTime = 0L;
    if (summary.containsKey(entity.getStageId())) {
      startTime = summary.get(entity.getStageId()).getStartTime();
      endTime = summary.get(entity.getStageId()).getEndTime();
    }

    setResourceProperty(resource, STAGE_START_TIME, startTime, requestedIds);
    setResourceProperty(resource, STAGE_END_TIME, endTime, requestedIds);

    CalculatedStatus status = CalculatedStatus.statusFromStageSummary(summary, Collections.singleton(entity.getStageId()));

    setResourceProperty(resource, STAGE_PROGRESS_PERCENT, status.getPercent(), requestedIds);
    setResourceProperty(resource, STAGE_STATUS, status.getStatus().toString(), requestedIds);

    return resource;
  }

  /**
   * Converts the {@link StageEntity} to a {@link Resource}.
   *
   * @param entity        the entity to convert (not {@code null})
   * @param requestedIds  the properties requested (not {@code null})
   *
   * @return the new resource
   */
  //todo: almost exactly the same as other toResource except how summaries are obtained
  //todo: refactor to combine the two with the summary logic extracted
  private Resource toResource(StageEntity entity, Set<String> requestedIds) {

    Resource resource = new ResourceImpl(Resource.Type.Stage);

    Long clusterId = entity.getClusterId();
    if (clusterId != null && !clusterId.equals(Long.valueOf(-1L))) {
      try {
        Cluster cluster = clustersProvider.get().getClusterById(clusterId);

        setResourceProperty(resource, STAGE_CLUSTER_NAME, cluster.getClusterName(), requestedIds);
      } catch (Exception e) {
        LOG.error("Can not get information for cluster " + clusterId + ".", e );
      }
    }

    Map<Long, HostRoleCommandStatusSummaryDTO> summary =
        topologyManager.getStageSummaries(entity.getRequestId());

    setResourceProperty(resource, STAGE_STAGE_ID, entity.getStageId(), requestedIds);
    setResourceProperty(resource, STAGE_REQUEST_ID, entity.getRequestId(), requestedIds);
    setResourceProperty(resource, STAGE_CONTEXT, entity.getRequestContext(), requestedIds);

    // this property is lazy loaded in JPA; don't use it unless requested
    if (isPropertyRequested(STAGE_CLUSTER_HOST_INFO, requestedIds)) {
      resource.setProperty(STAGE_CLUSTER_HOST_INFO, entity.getClusterHostInfo());
    }

    // this property is lazy loaded in JPA; don't use it unless requested
    if (isPropertyRequested(STAGE_COMMAND_PARAMS, requestedIds)) {
      resource.setProperty(STAGE_COMMAND_PARAMS, entity.getCommandParamsStage());
    }

    // this property is lazy loaded in JPA; don't use it unless requested
    if (isPropertyRequested(STAGE_HOST_PARAMS, requestedIds)) {
      resource.setProperty(STAGE_HOST_PARAMS, entity.getHostParamsStage());
    }

    setResourceProperty(resource, STAGE_SKIPPABLE, entity.isSkippable(), requestedIds);

    Long startTime = Long.MAX_VALUE;
    Long endTime = 0L;
    if (summary.containsKey(entity.getStageId())) {
      startTime = summary.get(entity.getStageId()).getStartTime();
      endTime = summary.get(entity.getStageId()).getEndTime();
    }

    setResourceProperty(resource, STAGE_START_TIME, startTime, requestedIds);
    setResourceProperty(resource, STAGE_END_TIME, endTime, requestedIds);

    CalculatedStatus status = CalculatedStatus.statusFromStageSummary(summary, Collections.singleton(entity.getStageId()));

    setResourceProperty(resource, STAGE_PROGRESS_PERCENT, status.getPercent(), requestedIds);
    setResourceProperty(resource, STAGE_STATUS, status.getStatus().toString(), requestedIds);

    return resource;
  }

  /**
   * Determine whether or not it is valid to transition from this stage status to the given status.
   *
   * @param status  the stage status being transitioned to
   *
   * @return true if it is valid to transition to the given stage status
   */
  private static boolean isValidManualTransition(HostRoleStatus status, HostRoleStatus desiredStatus) {
    EnumSet<HostRoleStatus> stageStatusSet = manualTransitionMap.get(status);
    return stageStatusSet != null && stageStatusSet.contains(desiredStatus);
  }
}
