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

package org.apache.ambari.server.controller.jmx;

import org.apache.ambari.server.controller.internal.AbstractPropertyProvider;
import org.apache.ambari.server.controller.internal.PropertyInfo;
import org.apache.ambari.server.controller.spi.*;
import org.apache.ambari.server.controller.utilities.StreamProvider;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Property provider implementation for JMX sources.
 */
public class JMXPropertyProvider extends AbstractPropertyProvider {

  private static final String NAME_KEY = "name";
  private static final String PORT_KEY = "tag.port";

  private static final long DEFAULT_POPULATE_TIMEOUT_MILLIS = 10000L;

  public static final String TIMED_OUT_MSG = "Timed out waiting for JMX metrics.";

  /**
   * Thread pool
   */
  private static final ExecutorService EXECUTOR_SERVICE;
  private static final int THREAD_POOL_CORE_SIZE = 20;
  private static final int THREAD_POOL_MAX_SIZE = 100;
  private static final long THREAD_POOL_TIMEOUT_MILLIS = 30000L;

  static {
    LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>(); // unlimited Queue

    ThreadPoolExecutor threadPoolExecutor =
        new ThreadPoolExecutor(
            THREAD_POOL_CORE_SIZE,
            THREAD_POOL_MAX_SIZE,
            THREAD_POOL_TIMEOUT_MILLIS,
            TimeUnit.MILLISECONDS,
            queue);

    threadPoolExecutor.allowCoreThreadTimeOut(true);

    EXECUTOR_SERVICE = threadPoolExecutor;
  }

  private final static ObjectReader objectReader;

  private static final Map<String, String> DEFAULT_JMX_PORTS = new HashMap<String, String>();

  static {
    DEFAULT_JMX_PORTS.put("NAMENODE",           "50070");
    DEFAULT_JMX_PORTS.put("DATANODE",           "50075");
    DEFAULT_JMX_PORTS.put("JOBTRACKER",         "50030");
    DEFAULT_JMX_PORTS.put("TASKTRACKER",        "50060");
    DEFAULT_JMX_PORTS.put("HBASE_MASTER",       "60010");
    DEFAULT_JMX_PORTS.put("HBASE_REGIONSERVER", "60030");
    DEFAULT_JMX_PORTS.put("RESOURCEMANAGER",     "8088");
    DEFAULT_JMX_PORTS.put("HISTORYSERVER",      "19888");
    DEFAULT_JMX_PORTS.put("NODEMANAGER",         "8042");

    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.configure(DeserializationConfig.Feature.USE_ANNOTATIONS, false);
    objectReader = objectMapper.reader(JMXMetricHolder.class);
  }

  protected final static Logger LOG =
      LoggerFactory.getLogger(JMXPropertyProvider.class);

  private final StreamProvider streamProvider;

  private final JMXHostProvider jmxHostProvider;

  private final String clusterNamePropertyId;

  private final String hostNamePropertyId;

  private final String componentNamePropertyId;

  private final String statePropertyId;

  private final Set<String> healthyStates;

  /**
   * The amount of time that this provider will wait for JMX metric values to be
   * returned from the JMX sources.  If no results are returned for this amount of
   * time then the request to populate the resources will fail.
   */
  protected long populateTimeout = DEFAULT_POPULATE_TIMEOUT_MILLIS;


  // ----- Constructors ------------------------------------------------------

  /**
   * Create a JMX property provider.
   *
   * @param componentMetrics         the map of supported metrics
   * @param streamProvider           the stream provider
   * @param jmxHostProvider          the host mapping
   * @param clusterNamePropertyId    the cluster name property id
   * @param hostNamePropertyId       the host name property id
   * @param componentNamePropertyId  the component name property id
   * @param statePropertyId          the state property id
   * @param healthyStates            the set of healthy state values
   */
  public JMXPropertyProvider(Map<String, Map<String, PropertyInfo>> componentMetrics,
                             StreamProvider streamProvider,
                             JMXHostProvider jmxHostProvider,
                             String clusterNamePropertyId,
                             String hostNamePropertyId,
                             String componentNamePropertyId,
                             String statePropertyId,
                             Set<String> healthyStates) {

    super(componentMetrics);

    this.streamProvider           = streamProvider;
    this.jmxHostProvider          = jmxHostProvider;
    this.clusterNamePropertyId    = clusterNamePropertyId;
    this.hostNamePropertyId       = hostNamePropertyId;
    this.componentNamePropertyId  = componentNamePropertyId;
    this.statePropertyId          = statePropertyId;
    this.healthyStates            = healthyStates;
  }


  // ----- PropertyProvider --------------------------------------------------

  @Override
  public Set<Resource> populateResources(Set<Resource> resources, Request request, Predicate predicate)
      throws SystemException {

    CompletionService<Resource> completionService =
        new ExecutorCompletionService<Resource>(EXECUTOR_SERVICE);

    // In a large cluster we could have thousands of resources to populate here.
    // Distribute the work across multiple threads.
    for (Resource resource : resources) {
      completionService.submit(getPopulateResourceCallable(resource, request, predicate));
    }

    Set<Resource> keepers = new HashSet<Resource>();
    try {
      for (int i = 0; i < resources.size(); ++ i) {
        Future<Resource> resourceFuture =
            completionService.poll(populateTimeout, TimeUnit.MILLISECONDS);

        if (resourceFuture == null) {
          // its been more than the populateTimeout since the last callable completed ...
          // don't wait any longer
          LOG.error(TIMED_OUT_MSG);
          break;
        } else {
          // future should already be completed... no need to wait on get
          Resource resource = resourceFuture.get();
          if (resource != null) {
            keepers.add(resource);
          }
        }
      }
    } catch (InterruptedException e) {
      logException(e);
    } catch (ExecutionException e) {
      rethrowSystemException(e.getCause());
    }
    return keepers;
  }


  // ----- helper methods ----------------------------------------------------

  /**
   * Set the populate timeout value for this provider.
   *
   * @param populateTimeout  the populate timeout value
   */
  protected void setPopulateTimeout(long populateTimeout) {
    this.populateTimeout = populateTimeout;
  }

  /**
   * Get the spec to locate the JMX stream from the given host and port
   *
   * @param hostName  the host name
   * @param port      the port
   *
   * @return the spec
   */
  protected String getSpec(String hostName, String port) {
    return "http://" + hostName + ":" + port + "/jmx";
  }

  /**
   * Get a callable that can be used to populate the given resource.
   *
   * @param resource  the resource to be populated
   * @param request   the request
   * @param predicate the predicate
   *
   * @return a callable that can be used to populate the given resource
   */
  private Callable<Resource> getPopulateResourceCallable(
      final Resource resource, final Request request, final Predicate predicate) {
    return new Callable<Resource>() {
      public Resource call() throws SystemException {
        return populateResource(resource, request, predicate);
      }
    };
  }

  /**
   * Populate a resource by obtaining the requested JMX properties.
   *
   * @param resource  the resource to be populated
   * @param request   the request
   * @param predicate the predicate
   *
   * @return the populated resource; null if the resource should NOT be part of the result set for the given predicate
   */
  private Resource populateResource(Resource resource, Request request, Predicate predicate)
      throws SystemException {

    Set<String> ids = getRequestPropertyIds(request, predicate);
    if (ids.isEmpty()) {
      // no properties requested
      return resource;
    }

    // Don't attempt to get the JMX properties if the resource is in an unhealthy state
    if (statePropertyId != null) {
      String state = (String) resource.getPropertyValue(statePropertyId);
      if (state != null && !healthyStates.contains(state)) {
        return resource;
      }
    }

    String componentName = (String) resource.getPropertyValue(componentNamePropertyId);

    if (getComponentMetrics().get(componentName) == null) {
      // If there are no metrics defined for the given component then there is nothing to do.
      return resource;
    }

    String clusterName = (String) resource.getPropertyValue(clusterNamePropertyId);

    String port = getPort(clusterName, componentName);
    if (port == null) {
      throw new SystemException(
          "Unable to get JMX metrics.  No port value for " + componentName, null);
    }

    String hostName = getHost(resource, clusterName, componentName);
    if (hostName == null) {
      throw new SystemException(
          "Unable to get JMX metrics.  No host name for " + componentName, null);
    }

    try {
      InputStream in = streamProvider.readFrom(getSpec(hostName, port));

      try {
        JMXMetricHolder metricHolder = objectReader.readValue(in);

        Map<String, Map<String, Object>> categories = new HashMap<String, Map<String, Object>>();

        for (Map<String, Object> bean : metricHolder.getBeans()) {
          String category = getCategory(bean);
          if (category != null) {
            categories.put(category, bean);
          }
        }

        for (String propertyId : ids) {
          Map<String, PropertyInfo> propertyInfoMap = getPropertyInfoMap(componentName, propertyId);

          for (Map.Entry<String, PropertyInfo> entry : propertyInfoMap.entrySet()) {

            PropertyInfo propertyInfo = entry.getValue();
            propertyId = entry.getKey();

            if (propertyInfo.isPointInTime()) {

              String property = propertyInfo.getPropertyId();
              String category = "";

              List<String> keyList = new LinkedList<String>();
              int keyStartIndex = property.indexOf('[', 0);
              int firstKeyIndex = keyStartIndex > -1 ? keyStartIndex : property.length();
              while (keyStartIndex > -1) {
                int keyEndIndex = property.indexOf(']', keyStartIndex);
                if (keyEndIndex > -1 & keyEndIndex > keyStartIndex) {
                  keyList.add(property.substring(keyStartIndex + 1, keyEndIndex));
                  keyStartIndex = property.indexOf('[', keyEndIndex);
                }
                else {
                  keyStartIndex = -1;
                }
              }

              int dotIndex = property.lastIndexOf('.', firstKeyIndex - 1);
              if (dotIndex != -1){
                category = property.substring(0, dotIndex);
                property = property.substring(dotIndex + 1, firstKeyIndex);
              }

              Map<String, Object> properties = categories.get(category);
              if (properties != null && properties.containsKey(property)) {
                Object value = properties.get(property);
                if (keyList.size() > 0 && value instanceof Map) {
                  Map map = (Map) value;
                  for (String key : keyList) {
                    value = map.get(key);
                    if (value instanceof Map) {
                      map = (Map) value;
                    }
                    else {
                      break;
                    }
                  }
                }
                resource.setProperty(propertyId, value);
              }
            }
          }
        }
      } finally {
        in.close();
      }
    } catch (IOException e) {
      logException(e);
    }
    return resource;
  }

  private String getPort(String clusterName, String componentName) throws SystemException {
    String port = jmxHostProvider.getPort(clusterName, componentName);
    return port == null ? DEFAULT_JMX_PORTS.get(componentName) : port;
  }

  private String getHost(Resource resource, String clusterName, String componentName) throws SystemException {
    return hostNamePropertyId == null ?
        jmxHostProvider.getHostName(clusterName, componentName) :
        (String) resource.getPropertyValue(hostNamePropertyId);
  }

  private String getCategory(Map<String, Object> bean) {
    if (bean.containsKey(NAME_KEY)) {
      String name = (String) bean.get(NAME_KEY);

      if (bean.containsKey(PORT_KEY)) {
        String port = (String) bean.get(PORT_KEY);
        name = name.replace("ForPort" + port, "");
      }
      return name;
    }
    return null;
  }

  /**
   * Log an error for the given exception.
   *
   * @param throwable  the caught exception
   *
   * @return the error message that was logged
   */
  private static String logException(Throwable throwable) {
    String msg = "Caught exception getting JMX metrics : " + throwable.getLocalizedMessage();

    LOG.error(msg);
    LOG.debug(msg, throwable);

    return msg;
  }

  /**
   * Rethrow the given exception as a System exception and log the message.
   *
   * @param throwable  the caught exception
   *
   * @throws SystemException always around the given exception
   */
  private static void rethrowSystemException(Throwable throwable) throws SystemException {
    String msg = logException(throwable);

    if (throwable instanceof SystemException) {
      throw (SystemException) throwable;
    }
    throw new SystemException (msg, throwable);
  }
}
