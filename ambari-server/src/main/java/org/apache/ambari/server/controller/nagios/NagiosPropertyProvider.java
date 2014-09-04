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
package org.apache.ambari.server.controller.nagios;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Injector;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Pattern;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.ComponentSSLConfiguration;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.internal.BaseProvider;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.PropertyProvider;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.utilities.StreamProvider;
import org.apache.ambari.server.state.Alert;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used to populate resources that have Nagios alertable properties.
 */
public class NagiosPropertyProvider extends BaseProvider implements PropertyProvider {
  
  private static final Logger LOG = LoggerFactory.getLogger(NagiosPropertyProvider.class);
  private static final Set<String> NAGIOS_PROPERTY_IDS = new HashSet<String>();
  private static final String NAGIOS_TEMPLATE = "http://%s/ambarinagios/nagios/nagios_alerts.php?q1=alerts&alert_type=all";
  
  private static final String ALERT_DETAIL_PROPERTY_ID = "alerts/detail";
  private static final String ALERT_SUMMARY_OK_PROPERTY_ID = "alerts/summary/OK";
  private static final String ALERT_SUMMARY_WARNING_PROPERTY_ID = "alerts/summary/WARNING";
  private static final String ALERT_SUMMARY_CRITICAL_PROPERTY_ID = "alerts/summary/CRITICAL";
  private static final String ALERT_SUMMARY_PASSIVE_PROPERTY_ID = "alerts/summary/PASSIVE";
  private static final String PASSIVE_TOKEN = "AMBARIPASSIVE=";
  
  private static final List<String> DEFAULT_IGNORABLE_FOR_SERVICES = Collections.unmodifiableList(new ArrayList<String>(
      Arrays.asList("NodeManager health", "NodeManager process", "TaskTracker process",
      "RegionServer process", "DataNode process", "DataNode space",
      "ZooKeeper Server process", "Supervisors process")));
  
  private static List<String> IGNORABLE_FOR_SERVICES;
  
  private static final List<String> IGNORABLE_FOR_HOSTS = new ArrayList<String>(
    Collections.singletonList("percent"));

  private static final Pattern COMMA_PATTERN = Pattern.compile(",");

  // holds alerts for clusters.  clusterName is the key
  private static final Map<String, List<NagiosAlert>> CLUSTER_ALERTS =
      new ConcurrentHashMap<String, List<NagiosAlert>>();
  private static final ScheduledExecutorService scheduler;
  
  private static final Set<String> CLUSTER_NAMES = new CopyOnWriteArraySet<String>();
  
  static {
    NAGIOS_PROPERTY_IDS.add("alerts/summary");
    NAGIOS_PROPERTY_IDS.add("alerts/detail");
    IGNORABLE_FOR_SERVICES = new ArrayList<String>(DEFAULT_IGNORABLE_FOR_SERVICES);

    scheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
      @Override
      public Thread newThread(Runnable r) {
        return new Thread(r, "NagiosPropertyProvider Request Reset Thread");
      }
    });
  }

  @Inject
  private static Clusters clusters;
  private Resource.Type resourceType;
  private String clusterNameProperty;
  private String resourceTypeProperty;
  private StreamProvider urlStreamProvider;
  private boolean waitOnFirstCall = false;
  
  @Inject
  public static void init(Injector injector) {
    clusters = injector.getInstance(Clusters.class);
    Configuration config = injector.getInstance(Configuration.class);
    
    IGNORABLE_FOR_SERVICES = new ArrayList<String>(DEFAULT_IGNORABLE_FOR_SERVICES);
    String ignores = config.getProperty(Configuration.NAGIOS_IGNORE_FOR_SERVICES_KEY);
    if (null != ignores) {
      Collections.addAll(IGNORABLE_FOR_SERVICES, COMMA_PATTERN.split(ignores));
    }

    ignores = config.getProperty(Configuration.NAGIOS_IGNORE_FOR_HOSTS_KEY);
    if (null != ignores) {
      Collections.addAll(IGNORABLE_FOR_HOSTS, COMMA_PATTERN.split(ignores));
    }
    
  }  
  
  public NagiosPropertyProvider(Resource.Type type,
      StreamProvider streamProvider,
      String clusterPropertyId,
      String typeMatchPropertyId) {
    
    super(NAGIOS_PROPERTY_IDS);
    
    resourceType = type;
    clusterNameProperty = clusterPropertyId;
    resourceTypeProperty = typeMatchPropertyId;
    urlStreamProvider = streamProvider;
    
    scheduler.scheduleAtFixedRate(new Runnable() {
      @Override
      public void run() {
        for (String clusterName : CLUSTER_NAMES) {
          List<NagiosAlert> alerts = new LinkedList<NagiosAlert>();
          try {
            alerts = populateAlerts(clusterName);
          } catch (Exception e) {
            LOG.error("Could not load Nagios alerts: " + e.getMessage());
          }
          CLUSTER_ALERTS.put(clusterName, alerts);
        }
      }
    }, 0L, 30L, TimeUnit.SECONDS);    
  }

  
  /**
   * Use only for testing to remove all cached alerts.
   */
  public void forceReset() {
    CLUSTER_NAMES.clear();
    CLUSTER_ALERTS.clear();
    waitOnFirstCall = true;
  }
  
  @Override
  public Set<Resource> populateResources(Set<Resource> resources,
      Request request, Predicate predicate) throws SystemException {

    Set<String> propertyIds = getRequestPropertyIds(request, predicate);
    
    for (Resource res : resources) {
      String matchValue = res.getPropertyValue(resourceTypeProperty).toString();
      
      if (null == matchValue)
        continue;

      Object clusterPropertyValue = res.getPropertyValue(clusterNameProperty);
      if (null == clusterPropertyValue)
        continue;

      final String clusterName = clusterPropertyValue.toString();
      if (null == clusterName)
        continue;
      
      if (!CLUSTER_ALERTS.containsKey(clusterName)) {
        // prevent endless looping for the first-time collection
        CLUSTER_ALERTS.put(clusterName, Collections.<NagiosAlert>emptyList());
        
        Future<List<NagiosAlert>> f = scheduler.submit(new Callable<List<NagiosAlert>>() {
          @Override
          public List<NagiosAlert> call() throws Exception {
            return populateAlerts(clusterName);
          }
        });
        
        if (waitOnFirstCall) {
          try {
            CLUSTER_ALERTS.put(clusterName, f.get());
          } catch (Exception e) {
            LOG.error("Could not load metrics - Executor exception" +
             " (" + e.getMessage() + ")");
          }
        }
      }
      
      CLUSTER_NAMES.add(clusterName);
      
      List<NagiosAlert> alerts = CLUSTER_ALERTS.get(clusterName);
      if (null != alerts) {
        updateAlerts (res, matchValue, alerts, propertyIds);
      }

    }
    
    return resources;
  }
  
  /**
   * Aggregates and sets nagios properties on a resource.
   * @param res the resource
   * @param matchValue the value to match
   * @param allAlerts all alerts from Nagios
   * @param requestedIds the requested ids for the resource
   */
  private void updateAlerts(Resource res, String matchValue, List<NagiosAlert> allAlerts,
      Set<String> requestedIds) {
    if (null == allAlerts || 0 == allAlerts.size())
      return;
    
    int ok = 0;
    int warning = 0;
    int critical = 0;
    int passive = 0;
    
    List<Map<String, Object>> alerts = new ArrayList<Map<String, Object>>();

    Set<String> processedHosts = new HashSet<String>();
    
    for (NagiosAlert alert : allAlerts) {
      boolean match = false;

      switch (resourceType.getInternalType()) {
        case Service:
          match = alert.getService().equals(matchValue);
          if (match && null != alert.getDescription() &&
              IGNORABLE_FOR_SERVICES.contains(alert.getDescription())) {
            match = false;
          }          
          break;
        case Host:
          match = alert.getHost().equals(matchValue);
          if (match && null != alert.getDescription()) {
            String desc = alert.getDescription();
            Iterator<String> it = IGNORABLE_FOR_HOSTS.iterator();
            while (it.hasNext() && match) {
              if (-1 != desc.toLowerCase().indexOf(it.next()))
                match = false;
            }
          }
          break;
        case Cluster:
          if (!processedHosts.contains(alert.getHost())) {
            match = true;
            Iterator<String> it = IGNORABLE_FOR_HOSTS.iterator();
            String desc = alert.getDescription();
            while (it.hasNext() && match) {
              if (-1 != desc.toLowerCase().indexOf(it.next()))
                match = false;
            }
          }
          break;
        default:
          break;
      }

      if (match) {

        processedHosts.add(alert.getHost());

        // status = the return code from the plugin that controls
        // whether an alert is sent out (0 when using wrapper)
        // actual_status = the actual process result
        
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        
        map.put("description", alert.getDescription());
        map.put("host_name", alert.getHost());
        map.put("last_status", NagiosAlert.getStatusString(alert.getLastStatus()));
        map.put("last_status_time", Long.valueOf(alert.getLastStatusTime()));
        map.put("service_name", alert.getService());
        map.put("status", NagiosAlert.getStatusString(alert.getStatus()));
        map.put("status_time", Long.valueOf(alert.getStatusTime()));
        map.put("output", alert.getOutput());
        map.put("actual_status", NagiosAlert.getStatusString(alert.getStatus()));
        
        String longOut = alert.getLongPluginOutput();
        int index = (null == longOut) ? -1 : longOut.indexOf(PASSIVE_TOKEN);
        if (-1 != index) {
          int actualStatus = 3;
          try {
            int len = PASSIVE_TOKEN.length();
            
            actualStatus = Integer.parseInt(longOut.substring(
                index + len, index + len+1));
          } catch (Exception e) {
            // do nothing
          }
          
          map.put("status", "PASSIVE");
          map.put("actual_status", NagiosAlert.getStatusString(actualStatus));
          passive++;
        } else {
          switch (alert.getStatus()) {
            case 0:
              ok++;
              break;
            case 1:
              warning++;
              break;
            case 2:
              critical++;
              break;
            case 3:
              passive++;
              break;
            default:
              break;
          }
        }
        
        alerts.add(map);
      }
    }
    
    setResourceProperty(res, ALERT_SUMMARY_OK_PROPERTY_ID, Integer.valueOf(ok), requestedIds);
    setResourceProperty(res, ALERT_SUMMARY_WARNING_PROPERTY_ID, Integer.valueOf(warning), requestedIds);
    setResourceProperty(res, ALERT_SUMMARY_CRITICAL_PROPERTY_ID, Integer.valueOf(critical), requestedIds);
    setResourceProperty(res, ALERT_SUMMARY_PASSIVE_PROPERTY_ID, Integer.valueOf(passive), requestedIds);
    
    if (!alerts.isEmpty() &&
      (resourceType.getInternalType() != Resource.InternalType.Cluster)) {

      setResourceProperty(res, ALERT_DETAIL_PROPERTY_ID, alerts, requestedIds);
    }
  }

  /**
   * Contacts Nagios and loads/parses the response into Nagios alert instances.
   * @param clusterName the cluster name
   * @return a list of nagios alerts
   * @throws SystemException
   */
  private List<NagiosAlert> populateAlerts(String clusterName) throws Exception {
    
    String nagiosHost = null;
    
    List<NagiosAlert> results = new ArrayList<NagiosAlert>();
    
    try {
      Cluster cluster = clusters.getCluster(clusterName);
      Service service = cluster.getService("NAGIOS");
      Map<String, ServiceComponentHost> hosts = service.getServiceComponent("NAGIOS_SERVER").getServiceComponentHosts();
      
      if (!hosts.isEmpty())
        nagiosHost = hosts.keySet().iterator().next();
      
    } catch (AmbariException e) {
      LOG.debug("Cannot find a nagios service.  Skipping alerts.");
    }
    
    if (null == nagiosHost) {
      return results;
    } else {
      String template = NAGIOS_TEMPLATE;

      if (ComponentSSLConfiguration.instance().isNagiosSSL())
        template = template.replace("http", "https");
      
      String url = String.format(template, nagiosHost);  

      InputStream in = null;

      try {
        in = urlStreamProvider.readFrom(url);
        
        NagiosAlerts alerts = new Gson().fromJson(IOUtils.toString(in, "UTF-8"), NagiosAlerts.class);

        results.addAll(alerts.alerts);
        
        Collections.sort(results, new Comparator<NagiosAlert>() {
          @Override
          public int compare(NagiosAlert o1, NagiosAlert o2) {
            if (o2.getStatus() != o1.getStatus())
              return o2.getStatus()-o1.getStatus();
            else {
              return (int)(o2.getLastStatusTime()-o1.getLastStatusTime());
            }
          }
        });
        
        return results;
      } catch (Exception e) {
        throw new SystemException("Error reading HTTP response for cluster " + clusterName +
            ", nagios=" + url + " (" + e.getMessage() + ")", e);
      } finally {
        if (in != null) {
          try {
            in.close();
          }
          catch (IOException ioe) {
            LOG.error("Error closing HTTP response stream " + url);
          }
        }
      }      
    }
  }
  
  private static class NagiosAlerts {
    private List<NagiosAlert> alerts;
  }

}
