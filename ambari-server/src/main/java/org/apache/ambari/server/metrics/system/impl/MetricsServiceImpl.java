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
package org.apache.ambari.server.metrics.system.impl;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.internal.AbstractControllerResourceProvider;
import org.apache.ambari.server.controller.internal.ServiceConfigVersionResourceProvider;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.metrics.system.AmbariMetricSink;
import org.apache.ambari.server.metrics.system.MetricsService;
import org.apache.ambari.server.security.authorization.internal.InternalAuthenticationToken;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Singleton
public class MetricsServiceImpl implements MetricsService {
  private static Logger LOG = LoggerFactory.getLogger(MetricsServiceImpl.class);
  private Map<String, AbstractMetricsSource> sources = new HashMap<>();
  private AmbariMetricSink sink = new AmbariMetricSinkImpl();
  private String collectorUri = "";
  private String collectorProtocol = "";
  private Configuration configuration;

  @Inject
  AmbariManagementController amc;

  @Override
  public void init() {
    try {
      configuration = new Configuration();
      if (collectorUri.isEmpty() || collectorProtocol.isEmpty()) {
        setCollectorUri();
      }
      configureSourceAndSink();
    } catch (Exception e) {
      LOG.info("Error initializing MetricsService", e);
    }

  }
  @Override
  public void run() {
    final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    for (Map.Entry<String, AbstractMetricsSource> entry : sources.entrySet()) {
      publishMetrics(executor, entry);
    }
  }


  private void setCollectorUri() {
    InternalAuthenticationToken authenticationToken = new InternalAuthenticationToken("admin");
    authenticationToken.setAuthenticated(true);
    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
    Clusters clusters = amc.getClusters();
    for (Map.Entry<String, Cluster> kv : clusters.getClusters().entrySet()) {
      String clusterName = kv.getKey();
      Resource.Type type = Resource.Type.ServiceConfigVersion;

      Set<String> propertyIds = new HashSet<String>();
      propertyIds.add(ServiceConfigVersionResourceProvider.SERVICE_CONFIG_VERSION_CONFIGURATIONS_PROPERTY_ID);

      Predicate predicate = new PredicateBuilder().property(
        ServiceConfigVersionResourceProvider.SERVICE_CONFIG_VERSION_CLUSTER_NAME_PROPERTY_ID).equals(clusterName).and().property(
        ServiceConfigVersionResourceProvider.SERVICE_CONFIG_VERSION_SERVICE_NAME_PROPERTY_ID).equals("AMBARI_METRICS").and().property(
        ServiceConfigVersionResourceProvider.SERVICE_CONFIG_VERSION_IS_CURRENT_PROPERTY_ID).equals("true").toPredicate();

      Request request = PropertyHelper.getReadRequest(propertyIds);

      ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        amc);

      try {
        Set<Resource> resources = provider.getResources(request, predicate);

        // get collector uri
        for (Resource resource : resources) {
          if (resource != null) {
            ArrayList<LinkedHashMap<Object, Object>> configs = (ArrayList<LinkedHashMap<Object, Object>>)
              resource.getPropertyValue(ServiceConfigVersionResourceProvider.SERVICE_CONFIG_VERSION_CONFIGURATIONS_PROPERTY_ID);
            for (LinkedHashMap<Object, Object> config : configs) {
              if (config != null && config.get("type").equals("ams-site")) {
                TreeMap<Object, Object> properties = (TreeMap<Object, Object>) config.get("properties");
                collectorUri = (String) properties.get("timeline.metrics.service.webapp.address");
                String which_protocol = (String) properties.get("timeline.metrics.service.http.policy");
                collectorProtocol = which_protocol.equals("HTTP_ONLY") ? "http" : "https";
                break;
              }
            }
          }
        }
      } catch (Exception e) {
        LOG.info("Throwing exception when retrieving Collector URI", e);
      }
    }
  }

  private void configureSourceAndSink() {
    try {
      LOG.info("********* Configuring Ambari Metrics Sink and Source**********");
      int frequency = Integer.parseInt(configuration.getProperty("sink.frequency", "10")); // default value 10
      sink.init(collectorProtocol, collectorUri, frequency);
      String[] sourceNames = configuration.getProperty("metrics.sources").split(",");
      for (String sourceName: sourceNames) {
        String className = configuration.getProperty("source." + sourceName + ".class");
        Class t = Class.forName(className);
        AbstractMetricsSource src = (AbstractMetricsSource)t.newInstance();
        src.init(sink);
        sources.put(sourceName, src);
      }
    }
    catch (Exception e) {
      LOG.info("Throwing exception when registering metric sink and source", e);
    }
  }

  private void publishMetrics(ScheduledExecutorService executor, Map.Entry<String, AbstractMetricsSource> entry) {
    String className = entry.getKey();
    AbstractMetricsSource source = entry.getValue();
    String interval = "source." + className + ".interval";
    int duration = Integer.parseInt(configuration.getProperty(interval, "5")); // default value 5
    try {
      executor.scheduleWithFixedDelay(source, 0, duration, TimeUnit.SECONDS);

    } catch (Exception e) {
      LOG.info("Throwing exception when failing scheduling source", e);
    }
  }
}
