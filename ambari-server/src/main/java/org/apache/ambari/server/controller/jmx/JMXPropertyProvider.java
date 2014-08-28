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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ambari.server.controller.internal.AbstractPropertyProvider;
import org.apache.ambari.server.controller.internal.PropertyInfo;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.controller.utilities.StreamProvider;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectReader;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Property provider implementation for JMX sources.
 */
public class JMXPropertyProvider extends AbstractPropertyProvider {

  private static final String NAME_KEY = "name";
  private static final String PORT_KEY = "tag.port";
  private static final String DOT_REPLACEMENT_CHAR = "#";
  private static final long DEFAULT_POPULATE_TIMEOUT_MILLIS = 12000L;

  public static final String TIMED_OUT_MSG = "Timed out waiting for JMX metrics.";
  public static final String STORM_REST_API = "STORM_REST_API";

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

  private final static ObjectReader jmxObjectReader;
  private final static ObjectReader stormObjectReader;

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
    DEFAULT_JMX_PORTS.put("JOURNALNODE",         "8480");
    DEFAULT_JMX_PORTS.put("STORM_REST_API",      "8745");

    ObjectMapper jmxObjectMapper = new ObjectMapper();
    jmxObjectMapper.configure(DeserializationConfig.Feature.USE_ANNOTATIONS, false);
    jmxObjectReader = jmxObjectMapper.reader(JMXMetricHolder.class);

    TypeReference<HashMap<String,Object>> typeRef
            = new TypeReference<
            HashMap<String,Object>
            >() {};
    stormObjectReader = jmxObjectMapper.reader(typeRef);
  }

  protected final static Logger LOG =
      LoggerFactory.getLogger(JMXPropertyProvider.class);

  private static final Pattern dotReplacementCharPattern =
    Pattern.compile(DOT_REPLACEMENT_CHAR);

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

    // Get a valid ticket for the request.
    Ticket ticket = new Ticket();

    CompletionService<Resource> completionService =
        new ExecutorCompletionService<Resource>(EXECUTOR_SERVICE);

    // In a large cluster we could have thousands of resources to populate here.
    // Distribute the work across multiple threads.
    for (Resource resource : resources) {
      completionService.submit(getPopulateResourceCallable(resource, request, predicate, ticket));
    }

    Set<Resource> keepers = new HashSet<Resource>();
    try {
      for (int i = 0; i < resources.size(); ++ i) {
        Future<Resource> resourceFuture =
            completionService.poll(populateTimeout, TimeUnit.MILLISECONDS);

        if (resourceFuture == null) {
          // its been more than the populateTimeout since the last callable completed ...
          // invalidate the ticket to abort the threads and don't wait any longer
          ticket.invalidate();
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
   * @param protocol  the protocol, one of http or https
   * @param hostName  the host name
   * @param port      the port
   *
   * @return the spec
   */
  protected String getSpec(String protocol, String hostName,
                           String port, String componentName) {
      if (null == componentName || !componentName.equals(STORM_REST_API))
        return protocol + "://" + hostName + ":" + port + "/jmx";
      else
        return protocol + "://" + hostName + ":" + port + "/api/cluster/summary";
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
      return getSpec("http", hostName, port, null);
  }
  
  /**
   * Get a callable that can be used to populate the given resource.
   *
   * @param resource  the resource to be populated
   * @param request   the request
   * @param predicate the predicate
   * @param ticket    a valid ticket
   *
   * @return a callable that can be used to populate the given resource
   */
  private Callable<Resource> getPopulateResourceCallable(
      final Resource resource, final Request request, final Predicate predicate, final Ticket ticket) {
    return new Callable<Resource>() {
      public Resource call() throws SystemException {
        return populateResource(resource, request, predicate, ticket);
      }
    };
  }

  /**
   * Populate a resource by obtaining the requested JMX properties.
   *
   * @param resource  the resource to be populated
   * @param request   the request
   * @param predicate the predicate
   * @param ticket    a valid ticket
   *
   * @return the populated resource; null if the resource should NOT be part of the result set for the given predicate
   */
  private Resource populateResource(Resource resource, Request request, Predicate predicate, Ticket ticket)
      throws SystemException {

    Set<String> ids = getRequestPropertyIds(request, predicate);
    Set<String> unsupportedIds = new HashSet<String>();
    String componentName = (String) resource.getPropertyValue(componentNamePropertyId);

    if (getComponentMetrics().get(componentName) == null) {
      // If there are no metrics defined for the given component then there is nothing to do.
      return resource;
    }

    for (String id : ids) {
      if (request.getTemporalInfo(id) != null) {
        unsupportedIds.add(id);
      }
      if (!isSupportedPropertyId(componentName, id)) {
        unsupportedIds.add(id);
      }
    }
    ids.removeAll(unsupportedIds);

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

    String clusterName = (String) resource.getPropertyValue(clusterNamePropertyId);

    String port = getPort(clusterName, componentName);
    if (port == null) {
      LOG.warn("Unable to get JMX metrics.  No port value for " + componentName);
      return resource;
    }

    Set<String> hostNames = getHosts(resource, clusterName, componentName);
    if (hostNames == null || hostNames.isEmpty()) {
      LOG.warn("Unable to get JMX metrics.  No host name for " + componentName);
      return resource;
    }
    
    String protocol = getJMXProtocol(clusterName, componentName);
    InputStream in = null;

    try {
      try {
        for (String hostName : hostNames) {
          try {
            in = streamProvider.readFrom(getSpec(protocol, hostName, port, componentName));
            // if the ticket becomes invalid (timeout) then bail out
            if (!ticket.isValid()) {
              return resource;
            }
            if (null == componentName || !componentName.equals(STORM_REST_API)) {
              getHadoopMetricValue(in, ids, resource, request, ticket);
            } else {
              getStormMetricValue(in, ids, resource, ticket);
            }
          } catch (IOException e) {
            logException(e);
          }
        }
      } finally {
        if (in != null) {
          in.close();
        }
      }
    } catch (IOException e) {
      logException(e);
    }
    return resource;
  }

  /**
   * Hadoop-specific metrics fetching
   */
  private void getHadoopMetricValue(InputStream in, Set<String> ids,
                       Resource resource, Request request, Ticket ticket) throws IOException {
    JMXMetricHolder metricHolder = jmxObjectReader.readValue(in);

    Map<String, Map<String, Object>> categories = new HashMap<String, Map<String, Object>>();
    String componentName = (String) resource.getPropertyValue(componentNamePropertyId);

    for (Map<String, Object> bean : metricHolder.getBeans()) {
      String category = getCategory(bean);
      if (category != null) {
        categories.put(category, bean);
      }
    }

    for (String propertyId : ids) {
      Map<String, PropertyInfo> propertyInfoMap = getPropertyInfoMap(componentName, propertyId);

      String requestedPropertyId = propertyId;

      for (Map.Entry<String, PropertyInfo> entry : propertyInfoMap.entrySet()) {

        PropertyInfo propertyInfo = entry.getValue();
        propertyId = entry.getKey();

        if (propertyInfo.isPointInTime()) {

          String property = propertyInfo.getPropertyId();
          String category = "";

          List<String> keyList = new LinkedList<String>();

          int keyStartIndex = property.indexOf('[');
          if (-1 != keyStartIndex) {
            int keyEndIndex = property.indexOf(']', keyStartIndex);
            if (-1 != keyEndIndex && keyEndIndex > keyStartIndex) {
              keyList.add(property.substring(keyStartIndex+1, keyEndIndex));
            }
          }

          if (!containsArguments(propertyId)) {
            int dotIndex = property.indexOf('.', property.indexOf('='));
            if (-1 != dotIndex) {
              category = property.substring(0, dotIndex);
              property = (-1 == keyStartIndex) ?
                      property.substring(dotIndex+1) :
                      property.substring(dotIndex+1, keyStartIndex);
            }
          } else {
            int firstKeyIndex = keyStartIndex > -1 ? keyStartIndex : property.length();
            int dotIndex = property.lastIndexOf('.', firstKeyIndex);

            if (dotIndex != -1) {
              category = property.substring(0, dotIndex);
              property = property.substring(dotIndex + 1, firstKeyIndex);
            }
          }

          if (containsArguments(propertyId)) {
            Pattern pattern = Pattern.compile(category);

            // find all jmx categories that match the regex
            for (String jmxCat : categories.keySet()) {
              Matcher matcher = pattern.matcher(jmxCat);
              if (matcher.matches()) {
                String newPropertyId = propertyId;
                for (int i = 0; i < matcher.groupCount(); i++) {
                  newPropertyId = substituteArgument(newPropertyId, "$" + (i + 1), matcher.group(i + 1));
                }
                // We need to do the final filtering here, after the argument substitution
                if (isRequestedPropertyId(newPropertyId, requestedPropertyId, request)) {
                  if (!ticket.isValid()) {
                    return;
                  }
                  setResourceValue(resource, categories, newPropertyId, jmxCat, property, keyList);
                }
              }
            }
          } else {
            if (!ticket.isValid()) {
              return;
            }
            setResourceValue(resource, categories, propertyId, category, property, keyList);
          }
        }
      }
    }
  }

  /**
   * TODO: Refactor
   * Storm-specific metrics fetching
   */
  private void getStormMetricValue(InputStream in, Set<String> ids,
                                   Resource resource, Ticket ticket) throws IOException {
    HashMap<String, Object> metricHolder = stormObjectReader.readValue(in);
    for (String category : ids) {
      Map<String, PropertyInfo> defProps = getComponentMetrics().get(STORM_REST_API);
      for (Map.Entry<String, PropertyInfo> depEntry : defProps.entrySet()) {
        if (depEntry.getKey().startsWith(category)) {
          PropertyInfo propInfo = depEntry.getValue();
          String propName = propInfo.getPropertyId();
          Object propertyValue = metricHolder.get(propName);
          String absId = PropertyHelper.getPropertyId(category, propName);
          if (!ticket.isValid()) {
            return;
          }
          // TODO: Maybe cast to int
          resource.setProperty(absId, propertyValue);
        }
      }
    }
  }

  private void setResourceValue(Resource resource, Map<String, Map<String, Object>> categories, String propertyId,
                                String category, String property, List<String> keyList) {
    Map<String, Object> properties = categories.get(category);
    if (property.contains(DOT_REPLACEMENT_CHAR)) {
      property = dotReplacementCharPattern.matcher(property).replaceAll(".");
    }
    if (properties != null && properties.containsKey(property)) {
      Object value = properties.get(property);
      if (keyList.size() > 0 && value instanceof Map) {
        Map<?, ?> map = (Map<?, ?>) value;
        for (String key : keyList) {
          value = map.get(key);
          if (value instanceof Map) {
            map = (Map<?, ?>) value;
          }
          else {
            break;
          }
        }
      }
      resource.setProperty(propertyId, value);
    }
  }

  private String getPort(String clusterName, String componentName) throws SystemException {
    String port = jmxHostProvider.getPort(clusterName, componentName);
    return port == null ? DEFAULT_JMX_PORTS.get(componentName) : port;
  }

  private String getJMXProtocol(String clusterName, String componentName) {
    return jmxHostProvider.getJMXProtocol(clusterName, componentName);
  }
  
  private Set<String> getHosts(Resource resource, String clusterName, String componentName) {
    return hostNamePropertyId == null ?
            jmxHostProvider.getHostNames(clusterName, componentName) :
            Collections.singleton((String) resource.getPropertyValue(hostNamePropertyId));
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
   * Determine whether or not the given property id was requested.
   */
  private static boolean isRequestedPropertyId(String propertyId, String requestedPropertyId, Request request) {
    return request.getPropertyIds().isEmpty() || propertyId.startsWith(requestedPropertyId);
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

    LOG.debug(msg, throwable);

    return msg;
  }

  /**
   * Rethrow the given exception as a System exception and log the message.
   *
   * @param throwable  the caught exception
   *
   * @throws org.apache.ambari.server.controller.spi.SystemException always around the given exception
   */
  private static void rethrowSystemException(Throwable throwable) throws SystemException {
    String msg = logException(throwable);

    if (throwable instanceof SystemException) {
      throw (SystemException) throwable;
    }
    throw new SystemException (msg, throwable);
  }


  // ----- inner class : Ticket ----------------------------------------------

  /**
   * Ticket used to cancel provider threads.  The provider threads should
   * monitor the validity of the passed in ticket and bail out if it becomes
   * invalid (as in a timeout).
   */
  private static class Ticket {
    /**
     * Indicate whether or not the ticket is valid.
     */
    private volatile boolean valid = true;

    /**
     * Invalidate the ticket.
     */
    public void invalidate() {
      valid = false;
    }

    /**
     * Determine whether or not this ticket is valid.
     *
     * @return true if the ticket is valid
     */
    public boolean isValid() {
      return valid;
    }
  }
}
