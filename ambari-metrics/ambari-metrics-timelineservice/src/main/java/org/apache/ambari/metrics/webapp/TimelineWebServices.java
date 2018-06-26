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

package org.apache.ambari.metrics.webapp;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.ambari.metrics.core.timeline.TimelineMetricServiceSummary;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.classification.InterfaceAudience.Public;
import org.apache.hadoop.classification.InterfaceStability.Unstable;
import org.apache.hadoop.metrics2.sink.timeline.AggregationResult;
import org.apache.hadoop.metrics2.sink.timeline.ContainerMetric;
import org.apache.hadoop.metrics2.sink.timeline.Precision;
import org.apache.hadoop.metrics2.sink.timeline.PrecisionLimitExceededException;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetricMetadata;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.apache.hadoop.metrics2.sink.timeline.TopNConfig;
import org.apache.hadoop.yarn.api.records.timeline.TimelinePutResponse;
import org.apache.ambari.metrics.core.timeline.TimelineMetricStore;
import org.apache.ambari.metrics.timeline.GenericObjectMapper;
import org.apache.ambari.metrics.timeline.NameValuePair;
import org.apache.ambari.metrics.timeline.TimelineReader.Field;
import org.apache.hadoop.yarn.util.timeline.TimelineUtils;
import org.apache.hadoop.yarn.webapp.BadRequestException;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
@Path("/ws/v1/timeline")
public class TimelineWebServices {
  private static final Log LOG = LogFactory.getLog(TimelineWebServices.class);
  
  private TimelineMetricStore timelineMetricStore;
  private static final String SMOKETEST_METRIC_APP_ID = "amssmoketestfake";

  @Inject
  public TimelineWebServices(TimelineMetricStore timelineMetricStore) {
    this.timelineMetricStore = timelineMetricStore;
  }

  @XmlRootElement(name = "about")
  @XmlAccessorType(XmlAccessType.NONE)
  @Public
  @Unstable
  public static class AboutInfo {

    private String about;

    public AboutInfo() {

    }

    public AboutInfo(String about) {
      this.about = about;
    }

    @XmlElement(name = "About")
    public String getAbout() {
      return about;
    }

    public void setAbout(String about) {
      this.about = about;
    }

  }

  /**
   * Return the description of the timeline web services.
   */
  @GET
  @Produces({ MediaType.APPLICATION_JSON /* , MediaType.APPLICATION_XML */})
  public AboutInfo about(
      @Context HttpServletRequest req,
      @Context HttpServletResponse res) {
    init(res);
    return new AboutInfo("AMS API");
  }

  /**
   * Store the given metrics into the timeline store, and return errors that
   * happened during storing.
   */
  @Path("/metrics")
  @POST
  @Consumes({ MediaType.APPLICATION_JSON /* , MediaType.APPLICATION_XML */})
  public TimelinePutResponse postMetrics(
    @Context HttpServletRequest req,
    @Context HttpServletResponse res,
    TimelineMetrics metrics) {

    init(res);
    if (metrics == null) {
      return new TimelinePutResponse();
    }

    try {

      // TODO: Check ACLs for MetricEntity using the TimelineACLManager.
      // TODO: Save owner of the MetricEntity.

      if (LOG.isTraceEnabled()) {
        LOG.trace("Storing metrics: " +
          TimelineUtils.dumpTimelineRecordtoJSON(metrics, true));
      }

      if (CollectionUtils.isNotEmpty(metrics.getMetrics()) && metrics.getMetrics().get(0).getAppId().equals(SMOKETEST_METRIC_APP_ID)) {
        return timelineMetricStore.putMetricsSkipCache(metrics);
      } else {
        return timelineMetricStore.putMetrics(metrics);
      }

    } catch (Exception e) {
      LOG.error("Error saving metrics.", e);
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Store the given metrics into the timeline store, and return errors that
   * happened during storing.
   */
  @Path("/metrics/aggregated")
  @POST
  @Consumes({ MediaType.APPLICATION_JSON /* , MediaType.APPLICATION_XML */})
  public TimelinePutResponse postAggregatedMetrics(
    @Context HttpServletRequest req,
    @Context HttpServletResponse res,
    AggregationResult metrics) {

    init(res);
    if (metrics == null) {
      return new TimelinePutResponse();
    }

    try {
      if (LOG.isTraceEnabled()) {
        LOG.trace("Storing aggregated metrics: " +
                TimelineUtils.dumpTimelineRecordtoJSON(metrics, true));
      }

      return timelineMetricStore.putHostAggregatedMetrics(metrics);
    } catch (Exception e) {
      LOG.error("Error saving metrics.", e);
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @Path("/containermetrics")
  @POST
  @Consumes({ MediaType.APPLICATION_JSON /* , MediaType.APPLICATION_XML */})
  public TimelinePutResponse postContainerMetrics(
      @Context HttpServletRequest req,
      @Context HttpServletResponse res,
      List<ContainerMetric> metrics) {
    init(res);
    if (metrics == null || metrics.isEmpty()) {
      return new TimelinePutResponse();
    }

    try {
      if (LOG.isTraceEnabled()) {
        LOG.trace("Storing container metrics: " + TimelineUtils
            .dumpTimelineRecordtoJSON(metrics, true));
      }

      return timelineMetricStore.putContainerMetrics(metrics);

    } catch (Exception e) {
      LOG.error("Error saving metrics.", e);
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Query for a set of different metrics satisfying the filter criteria.
   * All query params are optional. The default limit will apply if none
   * specified.
   *
   * @param metricNames Comma separated list of metrics to retrieve.
   * @param appId Application Id for the requested metrics.
   * @param instanceId Application instance id.
   * @param hostname Hostname where the metrics originated.
   * @param startTime Start time for the metric records retrieved.
   * @param precision Precision [ seconds, minutes, hours ]
   * @param limit limit on total number of {@link TimelineMetric} records
   *              retrieved.
   * @return {@link @TimelineMetrics}
   */
  @GET
  @Path("/metrics")
  @Produces({ MediaType.APPLICATION_JSON })
  public TimelineMetrics getTimelineMetrics(
    @Context HttpServletRequest req,
    @Context HttpServletResponse res,
    @QueryParam("metricNames") String metricNames,
    @QueryParam("appId") String appId,
    @QueryParam("instanceId") String instanceId,
    @QueryParam("hostname") String hostname,
    @QueryParam("startTime") String startTime,
    @QueryParam("endTime") String endTime,
    @QueryParam("precision") String precision,
    @QueryParam("limit") String limit,
    @QueryParam("grouped") String grouped,
    @QueryParam("topN") String topN,
    @QueryParam("topNFunction") String topNFunction,
    @QueryParam("isBottomN") String isBottomN,
    @QueryParam("seriesAggregateFunction") String seriesAggregateFunction
  ) {
    init(res);
    try {
      if (LOG.isTraceEnabled()) {
        LOG.trace("Request for metrics => metricNames: " + metricNames + ", " +
          "appId: " + appId + ", instanceId: " + instanceId + ", " +
          "hostname: " + hostname + ", startTime: " + startTime + ", " +
          "endTime: " + endTime + ", " +
          "precision: " + precision + "seriesAggregateFunction: " + seriesAggregateFunction);
      }

      return timelineMetricStore.getTimelineMetrics(
        parseListStr(metricNames, ","), parseListStr(hostname, ","), appId, parseStr(instanceId),
        parseLongStr(startTime), parseLongStr(endTime),
        Precision.getPrecision(precision), parseIntStr(limit),
        parseBoolean(grouped), parseTopNConfig(topN, topNFunction, isBottomN),
        seriesAggregateFunction);

    } catch (NumberFormatException ne) {
      throw new BadRequestException("startTime and limit should be numeric " +
        "values");
    } catch (Precision.PrecisionFormatException pfe) {
      throw new BadRequestException("precision should be seconds, minutes " +
        "or hours");
    } catch (PrecisionLimitExceededException iae) {
      throw new PrecisionLimitExceededException(iae.getMessage());
    } catch (IllegalArgumentException iae) {
      throw new BadRequestException(iae.getMessage());
    } catch (SQLException | IOException e) {
      throw new WebApplicationException(e,
        Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Path("/metrics/summary")
  @Produces({ MediaType.APPLICATION_JSON })
  public TimelineMetricServiceSummary getTimelineMetricServiceSummary(
    @Context HttpServletRequest req,
    @Context HttpServletResponse res) {
    init(res);

    try {
      return timelineMetricStore.getTimelineMetricServiceSummary();
    } catch (Exception e) {
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Path("/metrics/metadata")
  @Produces({ MediaType.APPLICATION_JSON })
  public Map<String, List<TimelineMetricMetadata>> getTimelineMetricMetadata(
    @Context HttpServletRequest req,
    @Context HttpServletResponse res,
    @QueryParam("appId") String appId,
    @QueryParam("metricName") String metricPattern,
    @QueryParam("includeAll") String includeBlacklistedMetrics
    ) {
    init(res);

    try {
      return timelineMetricStore.getTimelineMetricMetadata(
        parseStr(appId),
        parseStr(metricPattern),
        parseBoolean(includeBlacklistedMetrics));
    } catch (Exception e) {
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Path("/metrics/hosts")
  @Produces({ MediaType.APPLICATION_JSON })
  public Map<String, Set<String>> getHostedAppsMetadata(
    @Context HttpServletRequest req,
    @Context HttpServletResponse res
  ) {
    init(res);

    try {
      return timelineMetricStore.getHostAppsMetadata();
    } catch (Exception e) {
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Path("/metrics/instances")
  @Produces({ MediaType.APPLICATION_JSON })
  public Map<String, Map<String, Set<String>>> getClusterHostsMetadata(
    @Context HttpServletRequest req,
    @Context HttpServletResponse res,
    @QueryParam("appId") String appId,
    @QueryParam("instanceId") String instanceId
  ) {
    init(res);

    try {
      return timelineMetricStore.getInstanceHostsMetadata(instanceId, appId);
    } catch (Exception e) {
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Path("/metrics/uuid")
  @Produces({ MediaType.APPLICATION_JSON })
  public byte[] getUuid(
    @Context HttpServletRequest req,
    @Context HttpServletResponse res,
    @QueryParam("metricName") String metricName,
    @QueryParam("appId") String appId,
    @QueryParam("instanceId") String instanceId,
    @QueryParam("hostname") String hostname
    ) {
    init(res);

    if (metricName == null || appId == null) {
      throw new WebApplicationException(new IllegalArgumentException("Non null values needed for metricName and appId")
        , Response.Status.BAD_REQUEST);
    }
    try {
      return timelineMetricStore.getUuid(metricName, appId, instanceId, hostname);
    } catch (Exception e) {
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * This is a discovery endpoint that advertises known live collector
   * instances. Note: It will always answer with current instance as live.
   * This can be utilized as a liveliness pinger endpoint since the instance
   * names are cached and thereby no synchronous calls result from this API
   *
   * @return List<String> hostnames</String>
   */
  @GET
  @Path("/metrics/livenodes")
  @Produces({ MediaType.APPLICATION_JSON })
  public List<String> getLiveCollectorNodes(
    @Context HttpServletRequest req,
    @Context HttpServletResponse res
  ) {
    init(res);

    return timelineMetricStore.getLiveInstances();
  }

  private void init(HttpServletResponse response) {
    response.setContentType(null);
  }

  private static SortedSet<String> parseArrayStr(String str, String delimiter) {
    if (str == null) {
      return null;
    }
    SortedSet<String> strSet = new TreeSet<String>();
    String[] strs = str.split(delimiter);
    for (String aStr : strs) {
      strSet.add(aStr.trim());
    }
    return strSet;
  }

  private static NameValuePair parsePairStr(String str, String delimiter) {
    if (str == null) {
      return null;
    }
    String[] strs = str.split(delimiter, 2);
    try {
      return new NameValuePair(strs[0].trim(),
          GenericObjectMapper.OBJECT_READER.readValue(strs[1].trim()));
    } catch (Exception e) {
      // didn't work as an Object, keep it as a String
      return new NameValuePair(strs[0].trim(), strs[1].trim());
    }
  }

  private static Collection<NameValuePair> parsePairsStr(
      String str, String aDelimiter, String pDelimiter) {
    if (str == null) {
      return null;
    }
    String[] strs = str.split(aDelimiter);
    Set<NameValuePair> pairs = new HashSet<NameValuePair>();
    for (String aStr : strs) {
      pairs.add(parsePairStr(aStr, pDelimiter));
    }
    return pairs;
  }

  private static EnumSet<Field> parseFieldsStr(String str, String delimiter) {
    if (str == null) {
      return null;
    }
    String[] strs = str.split(delimiter);
    List<Field> fieldList = new ArrayList<Field>();
    for (String s : strs) {
      s = s.trim().toUpperCase();
      if (s.equals("EVENTS")) {
        fieldList.add(Field.EVENTS);
      } else if (s.equals("LASTEVENTONLY")) {
        fieldList.add(Field.LAST_EVENT_ONLY);
      } else if (s.equals("RELATEDENTITIES")) {
        fieldList.add(Field.RELATED_ENTITIES);
      } else if (s.equals("PRIMARYFILTERS")) {
        fieldList.add(Field.PRIMARY_FILTERS);
      } else if (s.equals("OTHERINFO")) {
        fieldList.add(Field.OTHER_INFO);
      } else {
        throw new IllegalArgumentException("Requested nonexistent field " + s);
      }
    }
    if (fieldList.size() == 0) {
      return null;
    }
    Field f1 = fieldList.remove(fieldList.size() - 1);
    if (fieldList.size() == 0) {
      return EnumSet.of(f1);
    } else {
      return EnumSet.of(f1, fieldList.toArray(new Field[fieldList.size()]));
    }
  }

  private static Long parseLongStr(String str) {
    return str == null ? null : Long.parseLong(str.trim());
  }

  private static Integer parseIntStr(String str) {
    return str == null ? null : Integer.parseInt(str.trim());
  }

  private static boolean parseBoolean(String booleanStr) {
    return booleanStr == null || Boolean.parseBoolean(booleanStr);
  }

  private static TopNConfig parseTopNConfig(String topN, String topNFunction,
                                            String bottomN) {
    if (topN == null || topN.isEmpty()) {
      return null;
    }
    Integer topNValue = parseIntStr(topN);

    if (topNValue == 0) {
      LOG.info("Invalid Input for TopN query. Ignoring TopN Request.");
      return null;
    }

    Boolean isBottomN = (bottomN != null && Boolean.parseBoolean(bottomN));
    return new TopNConfig(topNValue, topNFunction, isBottomN);
  }

  /**
   * Parses delimited string to list of strings. It skips strings that are
   * effectively empty (i.e. only whitespace).
   *
   */
  private static List<String> parseListStr(String str, String delimiter) {
    if (str == null || str.trim().isEmpty()){
      return null;
    }

    String[] split = str.trim().split(delimiter);
    List<String> list = new ArrayList<String>(split.length);
    for (String s : split) {
      if (!s.trim().isEmpty()){
        list.add(s);
      }
    }

    return list;
  }

  private static String parseStr(String str) {
    String trimmedInstance = (str == null) ? null : str.trim();
    if (trimmedInstance != null) {
      if (trimmedInstance.isEmpty() || trimmedInstance.equalsIgnoreCase("undefined")) {
        trimmedInstance = null;
      }
    }
    return trimmedInstance;
  }
}
