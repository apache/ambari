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
package org.apache.ambari.server.controller.spi;

import java.util.Map;
import java.util.Set;

/**
 * The request object carries the properties or property ids required to
 * satisfy a resource request.  The request object also contains any
 * temporal (date range) information, if any, for each requested property.
 */
public interface Request {

  /**
   * Get the set of property ids being requested.  Used for requests to get
   * resources.  An empty set signifies that all supported properties should
   * be returned (i.e. select * ).
   *
   * @return the set of property ids being requested
   */
  public Set<String> getPropertyIds();

  /**
   * Get the property values of the request.  Used
   * for requests to update or create resources.  Each value
   * in the set is a map of properties for a resource being
   * created/updated.  Each map contains property values keyed
   * by property ids.
   *
   * @return the set of properties being requested
   */
  public Set<Map<String, Object>> getProperties();

  /**
   * Get any request info properties of the request.  These are optional properties
   * that are specific to the request but not related to any resource.
   */
  public Map<String, String> getRequestInfoProperties();

  /**
   * Get the {@link TemporalInfo temporal information} for the given property
   * id for this request, if any.
   *
   * @param id the property id
   * @return the temporal information for the given property id; null if noe exists
   */
  public TemporalInfo getTemporalInfo(String id);

  /**
   * Obtain the pagination request information. This structure can be used for
   * the concrete {@link ResourceProvider} to instruct the caller that the
   * result set has already been paginated.
   *
   * @return the page request information.
   */
  public PageInfo getPageInfo();

  /**
   * Obtain information to order the results by. This structure can be used for
   * the concrete {@link ResourceProvider} to instruct the caller that the
   * result set has already been sorted.
   *
   * @return the sort request information.
   */
  public SortInfo getSortInfo();

  /**
   * The {@link PageInfo} class encapsulates the page request and optional
   * provider page response data. It is used so that a {@link ResourceProvider}
   * can indicate whether the provided {@link PageRequest} was already applied
   * to the result set.
   */
  public final class PageInfo {
    /**
     * {@code true} if the reponse is already paginated.
     */
    private boolean m_pagedResponse = false;

    /**
     * If {@link #m_pagedResponse} is {@code true} then this value will
     * represent the total items that the page slice originated from.
     */
    private int m_totalCount = 0;

    /**
     * The initial page request, not {@code null}.
     */
    private final PageRequest m_pageRequest;

    /**
     * Constructor.
     *
     * @param pageRequest
     */
    public PageInfo(PageRequest pageRequest) {
      m_pageRequest = pageRequest;
    }

    /**
     * Gets the page request.
     *
     * @return the page request (never {@code null}).
     */
    public PageRequest getPageRequest() {
      return m_pageRequest;
    }

    /**
     * Gets whether the response has already been paginated from a larger result
     * set.
     *
     * @return {@code true} if the response is already paged, {@code false}
     *         otherwise.
     */
    public boolean isResponsePaged() {
      return m_pagedResponse;
    }

    /**
     * Gets whether the response has already been paginated from a larger result
     * set.
     *
     * @param responsePaged
     *          {@code true} if the response is already paged, {@code false}
     *          otherwise.
     */
    public void setResponsePaged(boolean responsePaged) {
      m_pagedResponse = responsePaged;
    }

    /**
     * Gets the total count of items from which the paged result set was taken.
     *
     * @return the totalCount the total count of items in the un-paginated set.
     */
    public int getTotalCount() {
      return m_totalCount;
    }

    /**
     * Sets the total count of items from which the paged result set was taken.
     *
     * @param totalCount
     *          the totalCount the total count of items in the un-paginated set.
     */
    public void setTotalCount(int totalCount) {
      m_totalCount = totalCount;
    }
  }

  /**
   * The {@link SortInfo} class encapsulates the sort request and optional
   * provider sort response data. It is used so that a {@link ResourceProvider}
   * can indicate whether the provided {@link SortRequest} was already applied
   * to the result set.
   */
  public final class SortInfo {
    /**
     * {@code true} if the response is already sorted.
     */
    private boolean m_sortedResponse = false;

    /**
     * The initial sort request, not {@code null}.
     */
    private final SortRequest m_sortRequest;

    /**
     * Constructor.
     *
     * @param sortRequest
     */
    public SortInfo(SortRequest sortRequest) {
      m_sortRequest = sortRequest;
    }

    /**
     * Gets the sort request.
     *
     * @return the sort request (never {@code null}).
     */
    public SortRequest getSortRequest() {
      return m_sortRequest;
    }

    /**
     * Sets whether the response is already sorted.
     *
     * @param responseSorted
     *          {@code true} if the response is already sorted, {@code false}
     *          otherwise.
     */
    public void setResponseSorted(boolean responseSorted) {
      m_sortedResponse = responseSorted;
    }

    /**
     * Gets whether the response is already sorted.
     *
     * @return {@code true} if the response is already sorted, {@code false}
     *         otherwise.
     */
    public boolean isResponseSorted() {
      return m_sortedResponse;
    }
  }
}
