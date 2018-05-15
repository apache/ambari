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

package org.apache.ambari.view.hive2.resources.jobs;


import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.hive2.client.ColumnDescription;
import org.apache.ambari.view.hive2.client.Cursor;
import org.apache.ambari.view.hive2.client.HiveClientException;
import org.apache.ambari.view.hive2.client.Row;
import org.apache.ambari.view.hive2.utils.BadRequestFormattedException;
import org.apache.ambari.view.hive2.utils.HiveClientFormattedException;
import org.apache.ambari.view.hive2.utils.ResultFetchFormattedException;
import org.apache.ambari.view.hive2.utils.ResultNotReadyFormattedException;
import org.apache.ambari.view.hive2.utils.ServiceFormattedException;
import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.apache.hadoop.hbase.util.Strings;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
  private Map<String, Cursor<Row, ColumnDescription>> resultsCache;

  public static class CustomTimeToLiveExpirationPolicy extends PassiveExpiringMap.ConstantTimeToLiveExpirationPolicy<String, Cursor<Row, ColumnDescription>> {
    public CustomTimeToLiveExpirationPolicy(long timeToLiveMillis) {
      super(timeToLiveMillis);
    }

    @Override
    public long expirationTime(String key, Cursor<Row, ColumnDescription> value) {
      if (key.startsWith("$")) {
        return -1;  //never expire
      }
      return super.expirationTime(key, value);
    }
  }

  private Map<String, Cursor<Row, ColumnDescription>> getResultsCache() {
    if (resultsCache == null) {
      PassiveExpiringMap<String, Cursor<Row, ColumnDescription>> resultsCacheExpiringMap =
          new PassiveExpiringMap<>(new CustomTimeToLiveExpirationPolicy(EXPIRING_TIME));
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
    cursor.keepAlive();
    return true;
  }

  private Cursor<Row, ColumnDescription> getResultsSet(String key, Callable<Cursor<Row, ColumnDescription>> makeResultsSet) {
    if (!getResultsCache().containsKey(key)) {
      Cursor resultSet;
      try {
        resultSet = makeResultsSet.call();
        if (resultSet.isResettable()) {
          resultSet.reset();
        }
      } catch (ResultNotReadyFormattedException | ResultFetchFormattedException ex) {
        throw ex;
      } catch (Exception ex) {
        throw new ServiceFormattedException(ex.getMessage(), ex);
      }
      getResultsCache().put(key, resultSet);
    }

    return getResultsCache().get(key);
  }

  public Response.ResponseBuilder request(String key, String searchId, boolean canExpire, String fromBeginning, Integer count, String format, String requestedColumns, Callable<Cursor<Row, ColumnDescription>> makeResultsSet) throws HiveClientException {
    if (searchId == null)
      searchId = DEFAULT_SEARCH_ID;
    key = key + "?" + searchId;
    if (!canExpire)
      key = "$" + key;
    if (fromBeginning != null && fromBeginning.equals("true") && getResultsCache().containsKey(key)) {

      getResultsCache().remove(key);
    }

    Cursor<Row, ColumnDescription> resultSet = getResultsSet(key, makeResultsSet);

    if (count == null)
      count = DEFAULT_FETCH_COUNT;

    List<ColumnDescription> allschema = resultSet.getDescriptions();
    List<Row> allRowEntries = FluentIterable.from(resultSet)
      .limit(count).toList();

    List<ColumnDescription> schema = allschema;

    final Set<Integer> selectedColumns = getRequestedColumns(requestedColumns);
    if (!selectedColumns.isEmpty()) {
      schema = filter(allschema, selectedColumns);
    }

    List<Object[]> rows = FluentIterable.from(allRowEntries)
      .transform(new Function<Row, Object[]>() {
        @Override
        public Object[] apply(Row input) {
          if(!selectedColumns.isEmpty()) {
            return filter(Lists.newArrayList(input.getRow()), selectedColumns).toArray();
          } else {
            return input.getRow();
          }
        }
      }).toList();

    int read = rows.size();
    if(format != null && format.equalsIgnoreCase("d3")) {
      List<Map<String,Object>> results = new ArrayList<>();
      for(int i=0; i<rows.size(); i++) {
        Object[] row = rows.get(i);
        Map<String, Object> keyValue = new HashMap<>(row.length);
        for(int j=0; j<row.length; j++) {
          //Replace dots in schema with underscore
          String schemaName = schema.get(j).getName();
          keyValue.put(schemaName.replace('.','_'), row[j]);
        }
        results.add(keyValue);
      }
      return Response.ok(results);
    } else {
      ResultsResponse resultsResponse = new ResultsResponse();
      resultsResponse.setSchema(schema);
      resultsResponse.setRows(rows);
      resultsResponse.setReadCount(read);
      resultsResponse.setHasNext(resultSet.hasNext());
      //      resultsResponse.setSize(resultSet.size());
      resultsResponse.setOffset(resultSet.getOffset());
      resultsResponse.setHasResults(true);
      return Response.ok(resultsResponse);
    }
  }

  private <T> List<T> filter(List<T> list, Set<Integer> selectedColumns) {
    List<T> filtered = Lists.newArrayList();
    for(int i: selectedColumns) {
      if(list != null && list.get(i) != null)
        filtered.add(list.get(i));
    }

    return filtered;
  }

  private Set<Integer> getRequestedColumns(String requestedColumns) {
    if(Strings.isEmpty(requestedColumns)) {
      return new HashSet<>();
    }
    Set<Integer> selectedColumns = Sets.newHashSet();
    for (String columnRequested : requestedColumns.split(",")) {
      try {
        selectedColumns.add(Integer.parseInt(columnRequested));
      } catch (NumberFormatException ex) {
        throw new BadRequestFormattedException("Columns param should be comma-separated integers", ex);
      }
    }
    return selectedColumns;
  }

  private static class ResultsResponse {
    private List<ColumnDescription> schema;
    private List<String[]> rows;
    private int readCount;
    private boolean hasNext;
    private long offset;
    private boolean hasResults;

    public void setSchema(List<ColumnDescription> schema) {
      this.schema = schema;
    }

    public List<ColumnDescription> getSchema() {
      return schema;
    }

    public void setRows(List<Object[]> rows) {
      if( null == rows ){
        this.rows = null;
      }
      this.rows = new ArrayList<String[]>(rows.size());
      for(Object[] row : rows ){
        String[] strs = new String[row.length];
        for( int colNum = 0 ; colNum < row.length ; colNum++ ){
          String value = String.valueOf(row[colNum]);
          if(row[colNum] != null && (value.isEmpty() || value.equalsIgnoreCase("null"))){
            strs[colNum] = String.format("\"%s\"",value);
          }else{
            strs[colNum] = value;
          }
        }
        this.rows.add(strs);
      }
    }

    public List<String[]> getRows() {
      return rows;
    }

    public void setReadCount(int readCount) {
      this.readCount = readCount;
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
