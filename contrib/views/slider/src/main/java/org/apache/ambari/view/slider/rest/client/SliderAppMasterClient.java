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

package org.apache.ambari.view.slider.rest.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.ambari.view.URLStreamProvider;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.slider.MetricsHolder;
import org.apache.ambari.view.slider.SliderAppType;
import org.apache.ambari.view.slider.SliderAppTypeComponent;
import org.apache.ambari.view.slider.TemporalInfo;
import org.apache.commons.httpclient.HttpException;
import org.apache.ambari.view.slider.SliderAppsViewController;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SliderAppMasterClient extends BaseHttpClient {

  private static final Logger logger = LoggerFactory
      .getLogger(SliderAppMasterClient.class);

  public SliderAppMasterClient(String url, ViewContext viewContext) {
    super(url, viewContext);
  }

  public SliderAppMasterData getAppMasterData() {
    try {
      String html = doGet("");
      if (html != null) {
        int from = html.lastIndexOf("<ul>");
        int to = html.lastIndexOf("</ul>");
        if (from < to && from > -1) {
          SliderAppMasterData data = new SliderAppMasterData();
          String content = html.substring(from, to);
          content = content.replaceAll("<[^>]*>", "\r\n");
          String[] splits = content.split("\r\n");
          for (int i = 0; i < splits.length; i++) {
            String split = splits[i].trim();
            if ("classpath:org.apache.slider.registry".equals(split)) {
              data.registryUrl = splits[i + 1].trim();
            } else if ("classpath:org.apache.http.UI".equals(split)) {
              data.uiUrl = splits[i + 1].trim();
            } else if ("classpath:org.apache.slider.management".equals(split)) {
              data.managementUrl = splits[i + 1].trim();
            } else if ("classpath:org.apache.slider.publisher".equals(split)) {
              data.publisherUrl = splits[i + 1].trim();
            }
          }
          return data;
        }
      }
    } catch (HttpException e) {
      logger.warn("Unable to determine Ambari clusters", e);
      throw new RuntimeException(e.getMessage(), e);
    } catch (IOException e) {
      logger.warn("Unable to determine Ambari clusters", e);
      throw new RuntimeException(e.getMessage(), e);
    }
    return null;
  }

  public Map<String, String> getQuickLinks(String providerUrl) {
    Map<String, String> quickLinks = new HashMap<String, String>();
    try {
      if (providerUrl == null || providerUrl.trim().length() < 1) {
        return quickLinks;
      }
      JsonElement json = super.doGetJson(providerUrl, "/slider/quicklinks");
      if (json != null && json.getAsJsonObject() != null
          && json.getAsJsonObject().has("entries")) {
        JsonObject jsonObject = json.getAsJsonObject().get("entries")
            .getAsJsonObject();
        for (Entry<String, JsonElement> entry : jsonObject.entrySet()) {
          if ("org.apache.slider.jmx".equals(entry.getKey())) {
            quickLinks.put("JMX", entry.getValue().getAsString());
          } else if ("org.apache.slider.monitor".equals(entry.getKey())) {
            quickLinks.put("UI", entry.getValue().getAsString());
          } else if ("org.apache.slider.metrics.ui".equals(entry.getKey())) {
            quickLinks.put("Metrics UI", entry.getValue().getAsString());
          } else if ("org.apache.slider.metrics".equals(entry.getKey())) {
            quickLinks.put(SliderAppsViewController.METRICS_API_NAME, entry.getValue().getAsString());
          } else {
            quickLinks.put(entry.getKey(), entry.getValue().getAsString());
          }
        }
      }
    } catch (HttpException e) {
      logger.warn("Unable to determine quicklinks from " + providerUrl, e);
    } catch (IOException e) {
      logger.warn("Unable to determine quicklinks from " + providerUrl, e);
    }
    return quickLinks;
  }

  public Map<String, Map<String, String>> getConfigs(String providerUrl) {
    Map<String, Map<String, String>> configsMap = new HashMap<String, Map<String, String>>();
    try {
      if (providerUrl == null || providerUrl.trim().length() < 1) {
        return configsMap;
      }
      JsonElement json = super.doGetJson(providerUrl, "/slider");
      if (json != null) {
        JsonObject configsJson = json.getAsJsonObject().get("configurations")
            .getAsJsonObject();
        for (Entry<String, JsonElement> entry : configsJson.entrySet()) {
          if ("complete-config".equals(entry.getKey())
              || "quicklinks".equals(entry.getKey())) {
            continue;
          }
          JsonElement entryJson = super.doGetJson(providerUrl, "/slider/"
              + entry.getKey());
          if (entryJson != null) {
            JsonObject configsObj = entryJson.getAsJsonObject().get("entries")
                .getAsJsonObject();
            if (configsObj != null) {
              Map<String, String> configs = new HashMap<String, String>();
              for (Entry<String, JsonElement> e : configsObj.entrySet()) {
                configs.put(e.getKey(), e.getValue().getAsString());
              }
              configsMap.put(entry.getKey(), configs);
            }
          }
        }
      }
    } catch (HttpException e) {
      logger.warn("Unable to determine quicklinks from " + providerUrl, e);
    } catch (IOException e) {
      logger.warn("Unable to determine quicklinks from " + providerUrl, e);
    }
    return configsMap;
  }

  public Map<String, Number[][]> getMetrics(String appName,
                                            String metricsUrl,
                                            Set<String> metricsRequested,
                                            TemporalInfo temporalInfo,
                                            ViewContext context,
                                            SliderAppType appType,
                                            MetricsHolder metricsHolder) {
    Map<String, Number[][]> retVal = new HashMap<String, Number[][]>();

    if (appType == null || metricsHolder == null
        || metricsHolder.getTimelineMetrics() == null) {
      logger.info("AppType must be provided and it must contain "
          + "timeline_metrics.json to extract jmx properties");
      return retVal;
    }

    Map<String, Number[][]> receivedMetrics = null;
    List<String> components = new ArrayList<String>();
    for (SliderAppTypeComponent appTypeComponent : appType.getTypeComponents()) {
      components.add(appTypeComponent.getName());
    }

    Map<String, Map<String, Map<String, Metric>>> metrics = metricsHolder
        .getTimelineMetrics();
    Map<String, Metric> relevantMetrics = getRelevantMetrics(metrics, components);
    Set<String> metricsToRead = new HashSet<String>();
    Map<String, String> reverseNameLookup = new HashMap<String, String>();
    for (String key : relevantMetrics.keySet()) {
      if (metricsRequested.contains(key)) {
        String metricName = relevantMetrics.get(key).getMetric();
        metricsToRead.add(metricName);
        reverseNameLookup.put(metricName, key);
      }
    }

    if (metricsToRead.size() != 0) {
      try {
        String specWithParams = SliderAppMetricsHelper.getUrlWithParams(
            appName, metricsUrl, metricsToRead, temporalInfo);
        logger.info("Using spec: " + specWithParams);
        if (specWithParams != null) {

          String spec = null;
          String params = null;
          String[] tokens = specWithParams.split("\\?", 2);

          try {
            spec = tokens[0];
            params = tokens[1];
          } catch (ArrayIndexOutOfBoundsException e) {
            logger.info(e.toString());
          }

          receivedMetrics = SliderAppMetricsHelper.getMetrics(context, spec,
              params);
        }
      } catch (Exception e) {
        logger.warn("Unable to retrieve metrics. " + e.getMessage());
      }
    }

    if (receivedMetrics != null) {
      for (Map.Entry<String, Number[][]> metric : receivedMetrics.entrySet()) {
        if (reverseNameLookup.containsKey(metric.getKey())) {
          retVal.put(reverseNameLookup.get(metric.getKey()), metric.getValue());
        }
      }
    }

    return retVal;
  }

  /**
   * Provides only the interesting JMX metric names and values.
   *
   * @param jmxUrl
   *
   * @return
   */
  public Map<String, String> getJmx(String jmxUrl,
                                    ViewContext context,
                                    SliderAppType appType,
                                    MetricsHolder metricsHolder) {
    Map<String, String> jmxProperties = new HashMap<String, String>();
    if (appType == null || metricsHolder == null || metricsHolder.getJmxMetrics() == null) {
      logger
          .info("AppType must be provided and it must contain jmx_metrics.json to extract jmx properties");
      return jmxProperties;
    }

    List<String> components = new ArrayList<String>();
    for (SliderAppTypeComponent appTypeComponent : appType.getTypeComponents()) {
      components.add(appTypeComponent.getName());
    }

    Map<String, Map<String, Map<String, Metric>>> metrics = metricsHolder.getJmxMetrics();
    Map<String, Metric> relevantMetrics = getRelevantMetrics(metrics, components);
    if (relevantMetrics.size() == 0) {
      logger.info("No metrics found for components defined in the app.");
      return jmxProperties;
    }

    SliderAppJmxHelper.JMXTypes jmxType = SliderAppJmxHelper.jmxTypeExpected(relevantMetrics);
    if (jmxType == null) {
      logger
          .info("jmx_metrics.json is malformed. It may have mixed metric key types of unsupported metric key types.");
      return jmxProperties;
    }

    try {
      URLStreamProvider streamProvider = context.getURLStreamProvider();
      InputStream jmxStream = null;
      Map<String, String> headers = new HashMap<String, String>();
      try {
        jmxStream = streamProvider.readFrom(jmxUrl, "GET", (String)null, headers);
      } catch (IOException e) {
        logger.error(String.format(
            "Unable to access JMX endpoint at %s. Error %s", jmxUrl,
            e.getMessage()));
      }

      if (jmxStream != null) {
        switch (jmxType) {
          case JMX_BEAN:
            SliderAppJmxHelper.extractMetricsFromJmxBean(jmxStream, jmxUrl,
                                                         jmxProperties, relevantMetrics);
            break;
          case JSON:
            SliderAppJmxHelper.extractMetricsFromJmxJson(jmxStream, jmxUrl,
                                                         jmxProperties, relevantMetrics);
            break;
          case XML:
            SliderAppJmxHelper.extractMetricsFromJmxXML(jmxStream, jmxUrl,
                                                        jmxProperties, relevantMetrics);
            break;
          default:
            logger.info("Unsupported jmx type.");
        }
      }
    } catch (Exception e) {
      logger.info("Failed to extract jmx metrics. " + e.getMessage());
    }

    return jmxProperties;
  }

  private Map<String, Metric> getRelevantMetrics(
      Map<String, Map<String, Map<String, Metric>>> metrics, List<String> comps) {
    Map<String, Metric> relevantMetrics = new HashMap<String, Metric>();
    for (String comp : comps) {
      for (Map<String, Map<String, Metric>> m : metrics.values()) {
        if (m.containsKey(comp)) {
          relevantMetrics.putAll(m.get(comp));
        }
      }
    }
    return relevantMetrics;
  }

  public static class SliderAppMasterData {
    public String registryUrl;
    public String uiUrl;
    public String managementUrl;
    public String publisherUrl;
  }

  public static class SliderAppContainerData {
    public String hostName;
    public String containerId;
  }

}
