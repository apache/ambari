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

package org.apache.ambari.scom;

import org.apache.ambari.server.controller.internal.AbstractPropertyProvider;
import org.apache.ambari.server.controller.internal.PropertyInfo;
import org.apache.ambari.server.controller.jdbc.ConnectionFactory;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.TemporalInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * SQL based property/metrics provider required for ambari-scom.
 */
public class SQLPropertyProvider extends AbstractPropertyProvider {

  private final HostInfoProvider hostProvider;

  private final String clusterNamePropertyId;

  private final String hostNamePropertyId;

  private final String componentNamePropertyId;

  private final ConnectionFactory connectionFactory;


  // ----- Constants ---------------------------------------------------------

  private static final String GET_METRICS_STATEMENT = "select * from dbo.ufGetMetrics(?, ?, ?, ?, ?, ?, ?)";

  protected final static Logger LOG =
      LoggerFactory.getLogger(SQLPropertyProvider.class);


  // ----- Constructors ------------------------------------------------------

  public SQLPropertyProvider(
      Map<String, Map<String, PropertyInfo>> componentPropertyInfoMap,
      HostInfoProvider hostProvider,
      String clusterNamePropertyId,
      String hostNamePropertyId,
      String componentNamePropertyId,
      ConnectionFactory connectionFactory) {
    super(componentPropertyInfoMap);
    this.hostProvider             = hostProvider;
    this.clusterNamePropertyId    = clusterNamePropertyId;
    this.hostNamePropertyId       = hostNamePropertyId;
    this.componentNamePropertyId  = componentNamePropertyId;
    this.connectionFactory        = connectionFactory;
  }


  // ----- PropertyProvider --------------------------------------------------

  @Override
  public Set<Resource> populateResources(Set<Resource> resources, Request request, Predicate predicate)
      throws SystemException {
    Set<Resource> keepers = new HashSet<Resource>();
    try {
      Connection connection = connectionFactory.getConnection();
      try {
        PreparedStatement preparedStatement = connection.prepareStatement(GET_METRICS_STATEMENT);
        try {
          for (Resource resource : resources) {
            if (populateResource(resource, request, predicate, preparedStatement)) {
              keepers.add(resource);
            }
          }
        } finally {
          preparedStatement.close();
        }
      } finally {
        connection.close();
      }
    } catch (SQLException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Error during populateResources call : caught exception", e);
      }
      throw new SystemException("Error during populateResources call : caught exception", e);
    }
    return keepers;
  }


  // ----- helper methods ----------------------------------------------------

  // Populate the given resource
  private boolean populateResource(Resource resource, Request request, Predicate predicate, PreparedStatement preparedStatement) throws SystemException {

    Set<String> ids = getRequestPropertyIds(request, predicate);
    if (ids.isEmpty()) {
      // no properties requested ... nothing to do.
      return true;
    }

    String componentName = (String) resource.getPropertyValue(componentNamePropertyId);

    if (getComponentMetrics().get(componentName) == null) {
      // no metrics defined for the given component ... nothing to do.
      return true;
    }

    String clusterName = (String) resource.getPropertyValue(clusterNamePropertyId);
    String hostName    = getHost(resource, clusterName, componentName);

    if (hostName == null) {
      throw new SystemException(
          "Unable to get metrics.  No host name for " + componentName, null);
    }

    for (String id : ids) {
      Map<String, PropertyInfo> propertyInfoMap = getPropertyInfoMap(componentName, id);

      for (Map.Entry<String, PropertyInfo> entry: propertyInfoMap.entrySet()) {
        String       propertyKey  = entry.getKey();
        PropertyInfo propertyInfo = entry.getValue();
        String       propertyId   = propertyInfo.getPropertyId();
        TemporalInfo temporalInfo = request.getTemporalInfo(id);

        if ((propertyInfo.isPointInTime() && temporalInfo == null) ||
            (propertyInfo.isTemporal()    && temporalInfo != null)) {

          long startTime;
          long endTime;

          if (temporalInfo != null) {
            Long endTimeSeconds = temporalInfo.getEndTime();

            endTime   = endTimeSeconds != -1 ? endTimeSeconds * 1000 : Long.MAX_VALUE;
            startTime = temporalInfo.getStartTime() * 1000;
          } else {
            startTime = 0L;
            endTime   = Long.MAX_VALUE;
          }

          String[] parts = propertyId.split("\\.");
          int      size  = parts.length;

          if (size >= 3) {
            List<DataPoint> dataPoints = getMetric(startTime, endTime, parts[size - 3], parts[size - 2], parts[size - 1],
                                                   componentName.toLowerCase(), hostName, preparedStatement);

            if (dataPoints != null) {
              if (temporalInfo == null){
                // return the value of the last data point
                int          length = dataPoints.size();
                Serializable value  = length > 0 ? dataPoints.get(length - 1).getValue() : 0;
                resource.setProperty(propertyKey, value);
              } else {

                Number[][] dp = new Number[dataPoints.size()][2];
                for (int i = 0; i < dp.length; i++) {
                  dp[i][0] = dataPoints.get(i).getValue();
                  dp[i][1] = dataPoints.get(i).getTimestamp();
                }
                resource.setProperty(propertyKey, dp);
              }
            }
          } else {
            if (LOG.isWarnEnabled()) {
              LOG.warn("Can't get metrics for " + id + " : " + propertyId);
            }
          }
        }
      }
    }

    return true;
  }

  // get a metric from a sql connection
  private List<DataPoint> getMetric(long startTime, long endTime, String recordTypeContext,
                        String recordTypeName, String metricName, String serviceName, String nodeName,
                        PreparedStatement preparedStatement) throws SystemException {

    if (recordTypeContext == null || recordTypeName == null || nodeName == null) {
      return null;
    }

    int columnId = 1;
    List<DataPoint> results;
    try {
      preparedStatement.clearParameters();

      preparedStatement.setLong(columnId++, startTime);
      preparedStatement.setLong(columnId++, endTime);
      preparedStatement.setNString(columnId++, recordTypeContext);
      preparedStatement.setNString(columnId++, recordTypeName);
      preparedStatement.setNString(columnId++, metricName);
      preparedStatement.setNString(columnId++, serviceName);
      preparedStatement.setNString(columnId, nodeName);

      ResultSet rs = preparedStatement.executeQuery();

      results = new LinkedList<DataPoint>();

      if (rs != null) {

        //(RecordTimeStamp bigint, MetricValue NVARCHAR(512))
        while (rs.next()) {

          ParsePosition parsePosition = new ParsePosition(0);
          NumberFormat  numberFormat  = NumberFormat.getInstance();
          Number        parsedNumber  = numberFormat.parse(rs.getNString("MetricValue"), parsePosition);

          results.add(new DataPoint(rs.getLong("RecordTimeStamp"), parsedNumber));
        }
      }
    } catch (SQLException e) {
      throw new SystemException("Error during getMetric call : caught exception - ", e);
    }
    return results;
  }

  // get the hostname for a given resource
  private String getHost(Resource resource, String clusterName, String componentName) throws SystemException {
    return hostNamePropertyId == null ?
        hostProvider.getHostName(clusterName, componentName) :
        hostProvider.getHostName((String) resource.getPropertyValue(hostNamePropertyId));
  }


  // ----- inner class : DataPoint -------------------------------------------

  /**
   * Structure to hold a single datapoint (value/timestamp pair) retrieved from the db.
   */
  private static class DataPoint {
    private final long timestamp;
    private final Number value;

    // ----- Constructor -------------------------------------------------

    /**
     * Construct a data point from the given value and timestamp.
     *
     * @param timestamp  the timestamp
     * @param value      the value
     */
    private DataPoint(long timestamp, Number value) {
      this.timestamp = timestamp;
      this.value = value;
    }

    // ----- DataPoint ---------------------------------------------------

    /**
     * Get the timestamp value.
     * @return the timestamp
     */
    public long getTimestamp() {
      return timestamp;
    }

    /**
     * Get the value.
     * @return the value
     */
    public Number getValue() {
      return value;
    }

    // ----- Object overrides --------------------------------------------

    @Override
    public String toString() {
      return "{" +value + " : " + timestamp + "}";
    }
  }
}
