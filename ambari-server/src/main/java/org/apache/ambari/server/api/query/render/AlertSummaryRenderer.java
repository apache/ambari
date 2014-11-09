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

package org.apache.ambari.server.api.query.render;

import java.util.Set;

import org.apache.ambari.server.api.query.QueryInfo;
import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultImpl;
import org.apache.ambari.server.api.services.ResultPostProcessor;
import org.apache.ambari.server.api.services.ResultPostProcessorImpl;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.api.util.TreeNodeImpl;
import org.apache.ambari.server.controller.internal.AlertResourceProvider;
import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.state.AlertState;

/**
 * The {@link AlertSummaryRenderer} is used to format the results of queries to
 * the alerts endpoint. Each item returned from the query represents an
 * individual current alert which is then aggregated into a summary structure
 * based on the alert state.
 * <p/>
 * The finalized structure is:
 *
 * <pre>
 * {
 *   "href" : "http://localhost:8080/api/v1/clusters/c1/alerts?format=summary",
 *   "alerts_summary" : {
 *     "CRITICAL" : {
 *       "count" : 3,
 *       "original_timestamp" : 1415372828182
 *     },
 *     "OK" : {
 *       "count" : 37,
 *       "original_timestamp" : 1415375364937
 *     },
 *     "UNKNOWN" : {
 *       "count" : 1,
 *       "original_timestamp" : 1415372632261
 *     },
 *     "WARN" : {
 *       "count" : 0,
 *       "original_timestamp" : 0
 *     }
 *   }
 * }
 * </pre>
 * <p/>
 * The nature of a {@link Renderer} is that it manipulates the dataset returned
 * by a query. In the case of alert data, the query could potentially return
 * thousands of results if there are thousands of nodes in the cluster. This
 * could present a performance issue that can only be addressed by altering the
 * incoming query and modifying it to instruct the backend to return a JPA SUM
 * instead of a collection of entities.
 */
public class AlertSummaryRenderer extends BaseRenderer implements Renderer {

  private static final String OK_COUNT_PROPERTY = "alerts_summary/OK/count";
  private static final String OK_TIMESTAMP_PROPERTY = "alerts_summary/OK/original_timestamp";

  private static final String WARN_COUNT_PROPERTY = "alerts_summary/WARNING/count";
  private static final String WARN_TIMESTAMP_PROPERTY = "alerts_summary/WARNING/original_timestamp";

  private static final String CRITICAL_COUNT_PROPERTY = "alerts_summary/CRITICAL/count";
  private static final String CRITICAL_TIMESTAMP_PROPERTY = "alerts_summary/CRITICAL/original_timestamp";

  private static final String UNKNOWN_COUNT_PROPERTY = "alerts_summary/UNKNOWN/count";
  private static final String UNKNOWN_TIMESTAMP_PROPERTY = "alerts_summary/UNKNOWN/original_timestamp";

  /**
   * {@inheritDoc}
   */
  @Override
  public TreeNode<Set<String>> finalizeProperties(
      TreeNode<QueryInfo> queryTree, boolean isCollection) {

    QueryInfo queryInfo = queryTree.getObject();
    TreeNode<Set<String>> resultTree = new TreeNodeImpl<Set<String>>(
        null, queryInfo.getProperties(), queryTree.getName());

    copyPropertiesToResult(queryTree, resultTree);

    boolean addKeysToEmptyResource = true;
    if (!isCollection && isRequestWithNoProperties(queryTree)) {
      addSubResources(queryTree, resultTree);
      addKeysToEmptyResource = false;
    }

    ensureRequiredProperties(resultTree, addKeysToEmptyResource);

    // ensure that state and original_timestamp are on the request since these
    // are required by the finalization process of this renderer
    Set<String> properties = resultTree.getObject();
    properties.add(AlertResourceProvider.ALERT_STATE);
    properties.add(AlertResourceProvider.ALERT_ORIGINAL_TIMESTAMP);

    return resultTree;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ResultPostProcessor getResultPostProcessor(Request request) {
    // simply return the native rendering
    return new ResultPostProcessorImpl(request);
  }

  /**
   * {@inheritDoc}
   * <p/>
   * This will iterate over all of the nodes in the result tree and combine
   * their {@link AlertResourceProvider#ALERT_STATE} into a single summary
   * structure.
   */
  @Override
  public Result finalizeResult(Result queryResult) {
    TreeNode<Resource> resultTree = queryResult.getResultTree();
    Result summary = new ResultImpl(true);

    // counts
    int ok = 0;
    int warning = 0;
    int critical = 0;
    int unknown = 0;

    // keeps track of the most recent state change
    // (not the most recent alert received)
    long mostRecentOK = 0;
    long mostRecentWarning = 0;
    long mostRecentCritical = 0;
    long mostRecentUnknown = 0;

    // iterate over all returned flattened alerts and build the summary info
    for (TreeNode<Resource> node : resultTree.getChildren()) {
      Resource resource = node.getObject();
      AlertState state = (AlertState) resource.getPropertyValue(AlertResourceProvider.ALERT_STATE);
      Long originalTimestampObject = (Long) resource.getPropertyValue(AlertResourceProvider.ALERT_ORIGINAL_TIMESTAMP);

      // NPE sanity
      if (null == state) {
        state = AlertState.UNKNOWN;
      }

      // NPE sanity
      long originalTimestamp = 0;
      if (null != originalTimestampObject) {
        originalTimestamp = originalTimestampObject.longValue();
      }

      switch (state) {
        case CRITICAL: {
          critical++;

          if (originalTimestamp > mostRecentCritical) {
            mostRecentCritical = originalTimestamp;
          }

          break;
        }
        case OK: {
          ok++;

          if (originalTimestamp > mostRecentOK) {
            mostRecentOK = originalTimestamp;
          }

          break;
        }
        case WARNING: {
          warning++;

          if (originalTimestamp > mostRecentWarning) {
            mostRecentWarning = originalTimestamp;
          }

          break;
        }
        default:
        case UNKNOWN: {
          unknown++;

          if (originalTimestamp > mostRecentUnknown) {
            mostRecentUnknown = originalTimestamp;
          }

          break;
        }
      }
    }

    Resource resource = new ResourceImpl(Resource.Type.Alert);
    resource.setProperty(OK_COUNT_PROPERTY, ok);
    resource.setProperty(WARN_COUNT_PROPERTY, warning);
    resource.setProperty(CRITICAL_COUNT_PROPERTY, critical);
    resource.setProperty(UNKNOWN_COUNT_PROPERTY, unknown);

    resource.setProperty(OK_TIMESTAMP_PROPERTY, mostRecentOK);
    resource.setProperty(WARN_TIMESTAMP_PROPERTY, mostRecentWarning);
    resource.setProperty(CRITICAL_TIMESTAMP_PROPERTY, mostRecentCritical);
    resource.setProperty(UNKNOWN_TIMESTAMP_PROPERTY, mostRecentUnknown);

    TreeNode<Resource> summaryTree = summary.getResultTree();
    summaryTree.addChild(resource, "alerts_summary");

    return summary;
  }
}
