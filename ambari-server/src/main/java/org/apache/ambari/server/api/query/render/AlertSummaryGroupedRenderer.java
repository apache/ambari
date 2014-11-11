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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultImpl;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.controller.internal.AlertResourceProvider;
import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.state.AlertState;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * The {@link AlertSummaryGroupedRenderer} is used to format the results of
 * queries to the alerts endpoint. Each alert instance returned from the backend
 * is grouped by its alert definition and its state is then aggregated into the
 * summary information for that definition.
 * <p/>
 * The finalized structure is:
 *
 * <pre>
 * {
 *   "alerts_summary_grouped" : [
 *     {
 *       "definition_id" : 1,
 *       "definition_name" : "datanode_process",
 *       "summary" : {
 *         "CRITICAL": {
 *           "count": 1,
 *           "original_timestamp": 1415372992337
 *         },
 *         "OK": {
 *           "count": 1,
 *           "original_timestamp": 1415372992337
 *         },
 *         "UNKNOWN": {
 *           "count": 0,
 *           "original_timestamp": 0
 *         },
 *        "WARN": {
 *          "count": 0,
 *          "original_timestamp": 0
 *         }
 *       }
 *     },
 *     {
 *       "definition_id" : 2,
 *       "definition_name" : "namenode_process",
 *       "summary" : {
 *         "CRITICAL": {
 *           "count": 1,
 *           "original_timestamp": 1415372992337
 *         },
 *         "OK": {
 *           "count": 1,
 *           "original_timestamp": 1415372992337
 *         },
 *         "UNKNOWN": {
 *           "count": 0,
 *           "original_timestamp": 0
 *         },
 *        "WARN": {
 *          "count": 0,
 *          "original_timestamp": 0
 *         }
 *       }
 *     }
 *   ]
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
public class AlertSummaryGroupedRenderer extends AlertSummaryRenderer {

  private static final String ALERTS_SUMMARY_GROUP = "alerts_summary_grouped";

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
    Map<String, AlertDefinitionSummary> summaries = new HashMap<String, AlertDefinitionSummary>();

    // iterate over all returned flattened alerts and build the summary info
    for (TreeNode<Resource> node : resultTree.getChildren()) {
      Resource resource = node.getObject();

      Long definitionId = (Long) resource.getPropertyValue(AlertResourceProvider.ALERT_ID);
      String definitionName = (String) resource.getPropertyValue(AlertResourceProvider.ALERT_NAME);
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

      // create the group summary info if it doesn't exist yet
      AlertDefinitionSummary groupSummaryInfo = summaries.get(definitionName);
      if (null == groupSummaryInfo) {
        groupSummaryInfo = new AlertDefinitionSummary();
        groupSummaryInfo.Id = definitionId;
        groupSummaryInfo.Name = definitionName;

        summaries.put(definitionName, groupSummaryInfo);
      }

      // set and increment the correct values based on state
      switch (state) {
        case CRITICAL: {
          groupSummaryInfo.State.Critical.Count++;

          if (originalTimestamp > groupSummaryInfo.State.Critical.Timestamp) {
            groupSummaryInfo.State.Critical.Timestamp = originalTimestamp;
          }

          break;
        }
        case OK: {
          groupSummaryInfo.State.Ok.Count++;

          if (originalTimestamp > groupSummaryInfo.State.Ok.Timestamp) {
            groupSummaryInfo.State.Ok.Timestamp = originalTimestamp;
          }

          break;
        }
        case WARNING: {
          groupSummaryInfo.State.Warning.Count++;

          if (originalTimestamp > groupSummaryInfo.State.Warning.Timestamp) {
            groupSummaryInfo.State.Warning.Timestamp = originalTimestamp;
          }

          break;
        }
        default:
        case UNKNOWN: {
          groupSummaryInfo.State.Unknown.Count++;

          if (originalTimestamp > groupSummaryInfo.State.Unknown.Timestamp) {
            groupSummaryInfo.State.Unknown.Timestamp = originalTimestamp;
          }

          break;
        }
      }
    }

    Set<Entry<String, AlertDefinitionSummary>> entrySet = summaries.entrySet();
    List<AlertDefinitionSummary> groupedResources = new ArrayList<AlertDefinitionSummary>(
        entrySet.size());

    // iterate over all summary groups, adding them to the final list
    for (Entry<String, AlertDefinitionSummary> entry : entrySet) {
      groupedResources.add(entry.getValue());
    }

    Result groupedSummary = new ResultImpl(true);
    TreeNode<Resource> summaryTree = groupedSummary.getResultTree();

    Resource resource = new ResourceImpl(Resource.Type.Alert);
    summaryTree.addChild(resource, ALERTS_SUMMARY_GROUP);

    resource.setProperty(ALERTS_SUMMARY_GROUP, groupedResources);
    return groupedSummary;
  }

  /**
   * {@inheritDoc}
   * <p/>
   * Additionally adds {@link AlertResourceProvider#ALERT_ID} and
   * {@link AlertResourceProvider#ALERT_NAME}.
   */
  @Override
  protected void addRequiredAlertProperties(Set<String> properties) {
    super.addRequiredAlertProperties(properties);

    properties.add(AlertResourceProvider.ALERT_ID);
    properties.add(AlertResourceProvider.ALERT_NAME);
  }

  /**
   * The {@link AlertDefinitionSummary} is a simple data structure for keeping
   * track of each alert definition's summary information as the result set is
   * being iterated over.
   */
  private final static class AlertDefinitionSummary {
    @JsonProperty(value = "definition_id")
    private long Id;

    @JsonProperty(value = "definition_name")
    private String Name;

    @JsonProperty(value = "summary")
    private final AlertStateSummary State = new AlertStateSummary();
  }

  /**
   * The {@link AlertStateSummary} class holds information about each possible
   * alert state.
   */
  private final static class AlertStateSummary {
    @JsonProperty(value = "OK")
    private final AlertStateValues Ok = new AlertStateValues();

    @JsonProperty(value = "WARNING")
    private final AlertStateValues Warning = new AlertStateValues();

    @JsonProperty(value = "CRITICAL")
    private final AlertStateValues Critical = new AlertStateValues();

    @JsonProperty(value = "UNKNOWN")
    private final AlertStateValues Unknown = new AlertStateValues();
  }

  /**
   * The {@link AlertStateValues} class holds various information about an alert
   * state, such as the number of instances of that state and the most recent
   * timestamp.
   */
  private final static class AlertStateValues {
    @JsonProperty(value = "count")
    private int Count = 0;

    @JsonProperty(value = "original_timestamp")
    private long Timestamp = 0;
  }
}
