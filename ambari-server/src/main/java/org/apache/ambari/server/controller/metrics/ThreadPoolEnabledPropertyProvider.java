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

package org.apache.ambari.server.controller.metrics;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.ambari.server.controller.internal.AbstractPropertyProvider;
import org.apache.ambari.server.controller.internal.PropertyInfo;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;

import java.util.Collections;
import java.util.HashSet;
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
 * Unites common functionality for multithreaded metrics providers
 * (JMX and REST as of now). Shares the same pool of executor threads.
 */
public abstract class ThreadPoolEnabledPropertyProvider extends AbstractPropertyProvider {

  /**
   * Host states that make available metrics collection
   */
  public static final Set<String> healthyStates = Collections.singleton("STARTED");
  protected final String hostNamePropertyId;
  private final MetricHostProvider metricHostProvider;

  /**
   * Executor service is shared between all childs of current class
   */
  private static final ExecutorService EXECUTOR_SERVICE = initExecutorService();
  private static final int THREAD_POOL_CORE_SIZE = 20;
  private static final int THREAD_POOL_MAX_SIZE = 100;
  private static final long THREAD_POOL_TIMEOUT_MILLIS = 30000L;

  private static final long DEFAULT_POPULATE_TIMEOUT_MILLIS = 10000L;
  /**
   * The amount of time that this provider will wait for JMX metric values to be
   * returned from the JMX sources.  If no results are returned for this amount of
   * time then the request to populate the resources will fail.
   */
  protected long populateTimeout = DEFAULT_POPULATE_TIMEOUT_MILLIS;
  public static final String TIMED_OUT_MSG = "Timed out waiting for metrics.";

  private static final Cache<String, Throwable> exceptionsCache = CacheBuilder.newBuilder()
          .expireAfterWrite(5, TimeUnit.MINUTES)
          .build();

  // ----- Constructors ------------------------------------------------------

  /**
   * Construct a provider.
   *
   * @param componentMetrics map of metrics for this provider
   */
  public ThreadPoolEnabledPropertyProvider(Map<String, Map<String, PropertyInfo>> componentMetrics,
                                           String hostNamePropertyId,
                                           MetricHostProvider metricHostProvider) {
    super(componentMetrics);
    this.hostNamePropertyId = hostNamePropertyId;
    this.metricHostProvider = metricHostProvider;
  }

  // ----- Thread pool -------------------------------------------------------

  /**
   * Generates thread pool with default parameters
   */


  private static ExecutorService initExecutorService() {
    LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>(); // unlimited Queue

    ThreadPoolExecutor threadPoolExecutor =
        new ThreadPoolExecutor(
            THREAD_POOL_CORE_SIZE,
            THREAD_POOL_MAX_SIZE,
            THREAD_POOL_TIMEOUT_MILLIS,
            TimeUnit.MILLISECONDS,
            queue);

    threadPoolExecutor.allowCoreThreadTimeOut(true);

    return threadPoolExecutor;
  }

  public static ExecutorService getExecutorService() {
    return EXECUTOR_SERVICE;
  }

  // ----- Common PropertyProvider implementation details --------------------

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
   * @return the populated resource; null if the resource should NOT be part of the result set for the given predicate
   */


  protected abstract Resource populateResource(Resource resource,
                                               Request request, Predicate predicate, Ticket ticket)

      throws SystemException;

  /**
   * Set the populate timeout value for this provider.
   *
   * @param populateTimeout the populate timeout value
   */


  protected void setPopulateTimeout(long populateTimeout) {
    this.populateTimeout = populateTimeout;

  }


  // ----- helper methods ----------------------------------------------------

  /**
   * Determine whether or not the given property id was requested.
   */
  protected static boolean isRequestedPropertyId(String propertyId, String requestedPropertyId, Request request) {
    return request.getPropertyIds().isEmpty() || propertyId.startsWith(requestedPropertyId);
  }

  /**
   * Log an error for the given exception.
   *
   * @param throwable  the caught exception
   *
   * @return the error message that was logged
   */
  protected static String logException(final Throwable throwable) {
    final String msg = "Caught exception getting JMX metrics : " + throwable.getLocalizedMessage();

    if (LOG.isDebugEnabled()) {
      LOG.debug(msg, throwable);
    } else {
      try {
        exceptionsCache.get(msg, new Callable<Throwable>() {
          @Override
          public Throwable call() {
            LOG.error(msg + ", skipping same exceptions for next 5 minutes", throwable);
            return throwable;
          }
        });
      } catch (ExecutionException ignored) {
      }
    }


    return msg;
  }

  /**
   * Rethrow the given exception as a System exception and log the message.
   *
   * @param throwable  the caught exception
   *
   * @throws org.apache.ambari.server.controller.spi.SystemException always around the given exception
   */
  protected static void rethrowSystemException(Throwable throwable) throws SystemException {
    String msg = logException(throwable);

    if (throwable instanceof SystemException) {
      throw (SystemException) throwable;
    }
    throw new SystemException (msg, throwable);
  }

  /**
   * Returns a hostname for component
   */


  public String getHost(Resource resource, String clusterName, String componentName) throws SystemException {
    return hostNamePropertyId == null ?
        metricHostProvider.getHostName(clusterName, componentName) :
        (String) resource.getPropertyValue(hostNamePropertyId);

  }


  /**
   * Get complete URL from parts
   */

  protected String getSpec(String protocol, String hostName,
                           String port, String url) {
    return protocol + "://" + hostName + ":" + port + url;

  }

  // ----- inner class : Ticket ----------------------------------------------

  /**
   * Ticket used to cancel provider threads.  The provider threads should
   * monitor the validity of the passed in ticket and bail out if it becomes
   * invalid (as in a timeout).
   */
  protected static class Ticket {
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
