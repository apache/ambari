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

package org.apache.ambari.view.hive.resources.jobs;


import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.hive.client.ColumnDescription;
import org.apache.ambari.view.hive.client.HiveClientException;
import org.apache.ambari.view.hive.client.Cursor;
import org.apache.ambari.view.hive.utils.HiveClientFormattedException;
import org.apache.ambari.view.hive.utils.ServiceFormattedException;
import org.apache.commons.collections4.map.PassiveExpiringMap;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Results Pagination Controller
 * Persists cursors for result sets
 */
public class ResultsPaginationController {
  public static final String DEFAULT_SEARCH_ID = "default";
  private static Map<String, ResultsPaginationController> viewSingletonObjects = new HashMap<String, ResultsPaginationController>();
  public static ResultsPaginationController getInstance(ViewContext context) {
    if (!viewSingletonObjects.containsKey(context.getInstanceName()))
      viewSingletonObjects.put(context.getInstanceName(), new ResultsPaginationController());
    return viewSingletonObjects.get(context.getInstanceName());
  }

  public ResultsPaginationController() {
  }

  private static final long EXPIRING_TIME = 10*60*1000;  // 10 minutes
  private static final int DEFAULT_FETCH_COUNT = 50;
  private Map<String, Cursor> resultsCache;

  public static class CustomTimeToLiveExpirationPolicy extends PassiveExpiringMap.ConstantTimeToLiveExpirationPolicy<String, Cursor> {
    public CustomTimeToLiveExpirationPolicy(long timeToLiveMillis) {
      super(timeToLiveMillis);
    }

    @Override
    public long expirationTime(String key, Cursor value) {
      if (key.startsWith("$")) {
        return -1;  //never expire
      }
      return super.expirationTime(key, value);
    }
  }

  private Map<String, Cursor> getResultsCache() {
    if (resultsCache == null) {
      PassiveExpiringMap<String, Cursor> resultsCacheExpiringMap =
          new PassiveExpiringMap<String, Cursor>(new CustomTimeToLiveExpirationPolicy(EXPIRING_TIME));
      resultsCache = Collections.synchronizedMap(resultsCacheExpiringMap);
    }
    return resultsCache;
  }

  /**
   * Renew timer of cache entry.
   * @param key name/id of results request
   * @return false if entry not found; true if renew was ok
   */
  public boolean keepAlive(String key, String searchId) {
    if (searchId == null)
      searchId = DEFAULT_SEARCH_ID;
    String effectiveKey = key + "?" + searchId;
    if (!getResultsCache().containsKey(effectiveKey)) {
      return false;
    }
    Cursor cursor = getResultsCache().get(effectiveKey);
    getResultsCache().put(effectiveKey, cursor);
    return true;
  }

  private Cursor getResultsSet(String key, Callable<Cursor> makeResultsSet) {
    if (!getResultsCache().containsKey(key)) {
      Cursor resultSet = null;
      try {
        resultSet = makeResultsSet.call();
      } catch (HiveClientException ex) {
        throw new HiveClientFormattedException(ex);
      } catch (Exception ex) {
        throw new ServiceFormattedException(ex.getMessage(), ex);
      }
      getResultsCache().put(key, resultSet);
    }

    return getResultsCache().get(key);
  }

  public Response.ResponseBuilder request(String key, String searchId, boolean canExpire, String fromBeginning, Integer count, Callable<Cursor> makeResultsSet) throws HiveClientException {
    if (searchId == null)
      searchId = DEFAULT_SEARCH_ID;
    key = key + "?" + searchId;
    if (!canExpire)
      key = "$" + key;
    if (fromBeginning != null && fromBeginning.equals("true") && getResultsCache().containsKey(key))
      getResultsCache().remove(key);
    Cursor resultSet = getResultsSet(key, makeResultsSet);

    if (count == null)
      count = DEFAULT_FETCH_COUNT;

    ResultsResponse resultsResponse = new ResultsResponse();
    resultsResponse.setSchema(resultSet.getSchema());
    ArrayList<Object[]> rows = new ArrayList<Object[]>(count);
    int read = resultSet.readRaw(rows, count);
    resultsResponse.setRows(rows);
    resultsResponse.setReadCount(read);
    resultsResponse.setHasNext(resultSet.hasNext());
//      resultsResponse.setSize(resultSet.size());
    resultsResponse.setOffset(resultSet.getOffset());
    resultsResponse.setHasResults(true);
    return Response.ok(resultsResponse);
  }

  public static Response.ResponseBuilder emptyResponse() {
    ResultsResponse resultsResponse = new ResultsResponse();
    resultsResponse.setSchema(new ArrayList<ColumnDescription>());
    resultsResponse.setRows(new ArrayList<Object[]>());
    resultsResponse.setReadCount(0);
    resultsResponse.setHasNext(false);
    resultsResponse.setOffset(0);
    resultsResponse.setHasResults(false);
    return Response.ok(resultsResponse);
  }

  private static class ResultsResponse {
    private ArrayList<ColumnDescription> schema;
    private ArrayList<Object[]> rows;
    private int readCount;
    private boolean hasNext;
    private long offset;
    private boolean hasResults;

    public void setSchema(ArrayList<ColumnDescription> schema) {
      this.schema = schema;
    }

    public ArrayList<ColumnDescription> getSchema() {
      return schema;
    }

    public void setRows(ArrayList<Object[]> rows) {
      this.rows = rows;
    }

    public ArrayList<Object[]> getRows() {
      return rows;
    }

    public void setReadCount(int readCount) {
      this.readCount = readCount;
    }

    public int getReadCount() {
      return readCount;
    }

    public void setHasNext(boolean hasNext) {
      this.hasNext = hasNext;
    }

    public boolean isHasNext() {
      return hasNext;
    }

    public long getOffset() {
      return offset;
    }

    public void setOffset(long offset) {
      this.offset = offset;
    }

    public boolean getHasResults() {
      return hasResults;
    }

    public void setHasResults(boolean hasResults) {
      this.hasResults = hasResults;
    }
  }
}
