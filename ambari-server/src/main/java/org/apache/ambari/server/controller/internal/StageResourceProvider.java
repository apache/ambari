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

import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
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
import org.apache.ambari.server.orm.dao.StageDAO;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.StageEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ResourceProvider for Stage
 */
@StaticallyInject
public class StageResourceProvider extends AbstractResourceProvider implements ExtendedResourceProvider {

  /**
   * Used for querying stage resources.
   */
  @Inject
  private static StageDAO dao = null;

  /**
   * Used to get cluster information.
   */
  private static Clusters clusters = null;

  @Inject
  private static Provider<Clusters> clustersProvider = null;

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
  public static final String STAGE_PROGRESS_PERCENT = "Stage/progress_percent";
  public static final String STAGE_STATUS = "Stage/status";
  public static final String STAGE_START_TIME = "Stage/start_time";
  public static final String STAGE_END_TIME = "Stage/end_time";

  /**
   * The property ids for a stage resource.
   */
  private static final Set<String> PROPERTY_IDS = new HashSet<String>();

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
    PROPERTY_IDS.add(STAGE_PROGRESS_PERCENT);
    PROPERTY_IDS.add(STAGE_STATUS);
    PROPERTY_IDS.add(STAGE_START_TIME);
    PROPERTY_IDS.add(STAGE_END_TIME);

    // keys
    KEY_PROPERTY_IDS.put(Resource.Type.Stage, STAGE_STAGE_ID);
    KEY_PROPERTY_IDS.put(Resource.Type.Cluster, STAGE_CLUSTER_NAME);
    KEY_PROPERTY_IDS.put(Resource.Type.Request, STAGE_REQUEST_ID);
  }


  // ----- Constructors ------------------------------------------------------

  /**
   * Constructor.
   */
  StageResourceProvider() {
    super(PROPERTY_IDS, KEY_PROPERTY_IDS);
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
    throw new UnsupportedOperationException();
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

    ensureClusters();

    Set<Resource> results     = new LinkedHashSet<Resource>();
    Set<String>   propertyIds = getRequestPropertyIds(request, predicate);

    List<StageEntity> entities = dao.findAll(request, predicate);
    for (StageEntity entity : entities) {
      results.add(toResource(entity, propertyIds));
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


  // ----- helper methods ----------------------------------------------------

  /**
   * Converts the {@link StageEntity} to a {@link Resource}.
   *
   * @param entity        the entity to convert (not {@code null})
   * @param requestedIds  the properties requested (not {@code null})
   *
   * @return the new resource
   */
  private Resource toResource(StageEntity entity,
                              Set<String> requestedIds) {

    Resource resource = new ResourceImpl(Resource.Type.Stage);

    Long clusterId = entity.getClusterId();
    if (clusterId != null) {
      try {
        Cluster cluster = clusters.getClusterById(clusterId);

        setResourceProperty(resource, STAGE_CLUSTER_NAME, cluster.getClusterName(), requestedIds);
      } catch (Exception e) {
        LOG.error("Can not get information for cluster " + clusterId + ".", e );
      }
    }

    setResourceProperty(resource, STAGE_STAGE_ID, entity.getStageId(), requestedIds);
    setResourceProperty(resource, STAGE_REQUEST_ID, entity.getRequestId(), requestedIds);
    setResourceProperty(resource, STAGE_CONTEXT, entity.getRequestContext(), requestedIds);
    setResourceProperty(resource, STAGE_CLUSTER_HOST_INFO, entity.getClusterHostInfo(), requestedIds);
    setResourceProperty(resource, STAGE_COMMAND_PARAMS, entity.getCommandParamsStage(), requestedIds);
    setResourceProperty(resource, STAGE_HOST_PARAMS, entity.getHostParamsStage(), requestedIds);

    Collection<HostRoleCommandEntity> tasks = entity.getHostRoleCommands();

    Long startTime = tasks.isEmpty() ? 0L : Long.MAX_VALUE;
    Long endTime   = 0L;

    for (HostRoleCommandEntity task : tasks) {
      startTime = Math.min(task.getStartTime(), startTime);
      endTime   = Math.max(task.getEndTime(), endTime);
    }

    setResourceProperty(resource, STAGE_START_TIME, startTime, requestedIds);
    setResourceProperty(resource, STAGE_END_TIME, endTime, requestedIds);

    int taskCount = tasks.size();

    Map<HostRoleStatus, Integer> taskStatusCounts = calculateTaskStatusCounts(tasks);

    setResourceProperty(resource, STAGE_PROGRESS_PERCENT, calculateProgressPercent(taskStatusCounts, taskCount),
        requestedIds);
    setResourceProperty(resource, STAGE_STATUS, calculateSummaryStatus(taskStatusCounts, taskCount).toString(),
        requestedIds);

    return resource;
  }

  /**
   * Calculate the percent complete for the stage based on its tasks.
   *
   * @param counters    counts of tasks that are in various states
   * @param totalTasks  total number of tasks in request
   *
   * @return the percent complete for the stage
   */
  private double calculateProgressPercent(Map<HostRoleStatus, Integer> counters, double totalTasks) {
    return ((counters.get(HostRoleStatus.QUEUED) * 0.09 +
        counters.get(HostRoleStatus.IN_PROGRESS) * 0.35 +
        counters.get(HostRoleStatus.COMPLETED)) / totalTasks) * 100.0;
  }

  /**
   * Calculate the stage status based on the status of its tasks.
   *
   * @param counters    counts of tasks that are in various states
   * @param totalTasks  total number of tasks in request
   *
   * @return summary request status based on statuses of tasks in different states.
   */
  private HostRoleStatus calculateSummaryStatus(Map<HostRoleStatus, Integer> counters, int totalTasks) {
    return counters.get(HostRoleStatus.FAILED) > 0 ? HostRoleStatus.FAILED :
        counters.get(HostRoleStatus.ABORTED) > 0 ? HostRoleStatus.ABORTED :
        counters.get(HostRoleStatus.TIMEDOUT) > 0 ? HostRoleStatus.TIMEDOUT :
        counters.get(HostRoleStatus.IN_PROGRESS) > 0 ? HostRoleStatus.IN_PROGRESS :
        counters.get(HostRoleStatus.COMPLETED) == totalTasks ? HostRoleStatus.COMPLETED : HostRoleStatus.PENDING;
  }

  /**
   * Returns counts of tasks that are in various states.
   *
   * @param tasks  the collection of tasks
   *
   * @return a map of counts of tasks keyed by the task status
   */
  private Map<HostRoleStatus, Integer> calculateTaskStatusCounts(Collection<HostRoleCommandEntity> tasks) {
    Map<HostRoleStatus, Integer> counters = new HashMap<HostRoleStatus, Integer>();
    // initialize
    for (HostRoleStatus hostRoleStatus : HostRoleStatus.values()) {
      counters.put(hostRoleStatus, 0);
    }
    // calculate counts
    for (HostRoleCommandEntity hostRoleCommand : tasks) {
      HostRoleStatus status = hostRoleCommand.getStatus();
      // count tasks where isCompletedState() == true as COMPLETED
      // but don't count tasks with COMPLETED status twice
      if (status.isCompletedState() && status != HostRoleStatus.COMPLETED) {
        // Increase total number of completed tasks;
        counters.put(HostRoleStatus.COMPLETED, counters.get(HostRoleStatus.COMPLETED) + 1);
      }
      // Increment counter for particular status
      counters.put(status, counters.get(status) + 1);
    }

    // We overwrite the value to have the sum converged
    counters.put(HostRoleStatus.IN_PROGRESS,
        tasks.size() -
        counters.get(HostRoleStatus.COMPLETED) -
        counters.get(HostRoleStatus.QUEUED) -
        counters.get(HostRoleStatus.PENDING));

    return counters;
  }

  /**
   * Ensure that cluster information is available.
   *
   * @return the clusters information
   */
  private synchronized Clusters ensureClusters() {
    if (clusters == null) {
      clusters = clustersProvider.get();
    }
    return clusters;
  }
}
