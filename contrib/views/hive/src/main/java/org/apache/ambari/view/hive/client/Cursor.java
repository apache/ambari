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

package org.apache.ambari.view.hive.client;

import static org.apache.hive.service.cli.thrift.TCLIServiceConstants.TYPE_NAMES;

import org.apache.ambari.view.hive.utils.BadRequestFormattedException;
import org.apache.ambari.view.hive.utils.HiveClientFormattedException;
import org.apache.hive.service.cli.RowSet;
import org.apache.hive.service.cli.RowSetFactory;
import org.apache.hive.service.cli.thrift.*;
import org.apache.thrift.TException;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.*;

public class Cursor implements Iterator<Row>, Iterable<Row> {
  private final int FETCH_SIZE = 50;

  private TCLIService.Client client;
  private TOperationHandle opHandle;

  private RowSet fetched = null;
  private Iterator<Object[]> fetchedIterator = null;
  private Connection connection;
  private boolean resetCursor = false;
  private ArrayList<ColumnDescription> schema;
  private long offset;
  private HashSet<Integer> selectedColumns = new LinkedHashSet<Integer>();

  public Cursor(Connection connection, TOperationHandle opHandle) {
    this.connection = connection;
    this.client = connection.getClient();
    this.opHandle = opHandle;
  }

  public TOperationHandle getOpHandle() {
    return opHandle;
  }

  public void setOpHandle(TOperationHandle opHandle) {
    this.opHandle = opHandle;
  }

  private void fetchNextBlock() throws HiveClientException {
    //fetch another bunch
    TFetchResultsResp fetchResp = new HiveCall<TFetchResultsResp>(connection) {
      @Override
      public TFetchResultsResp body() throws HiveClientException {
        TFetchOrientation orientation = TFetchOrientation.FETCH_NEXT;
        if (resetCursor) {
          orientation = TFetchOrientation.FETCH_FIRST;
          resetCursor = false;
          offset = 0;
        }

        TFetchResultsReq fetchReq = getFetchResultsReq(orientation);
        try {
          return client.FetchResults(fetchReq);
        } catch (TException e) {
          throw new HiveClientException("H160 Unable to fetch results", e);
        }

      }
    }.call();
    Utils.verifySuccess(fetchResp.getStatus(), "H170 Unable to fetch results");
    TRowSet results = fetchResp.getResults();
    fetched = RowSetFactory.create(results, connection.getProtocol());
    fetchedIterator = fetched.iterator();
  }

  protected TFetchResultsReq getFetchResultsReq(TFetchOrientation orientation) {
    return new TFetchResultsReq(opHandle, orientation, FETCH_SIZE);
  }

  public ArrayList<ColumnDescription> getSchema() throws HiveClientException {
    if (this.schema == null) {
      TGetResultSetMetadataResp fetchResp = new HiveCall<TGetResultSetMetadataResp>(connection) {
        @Override
        public TGetResultSetMetadataResp body() throws HiveClientException {

          TGetResultSetMetadataReq fetchReq = new TGetResultSetMetadataReq(opHandle);
          try {
            return client.GetResultSetMetadata(fetchReq);
          } catch (TException e) {
            throw new HiveClientException("H180 Unable to fetch results metadata", e);
          }

        }
      }.call();
      Utils.verifySuccess(fetchResp.getStatus(), "H190 Unable to fetch results metadata");
      TTableSchema schema = fetchResp.getSchema();

      List<TColumnDesc> thriftColumns = schema.getColumns();
      ArrayList<ColumnDescription> columnDescriptions = new ArrayList<ColumnDescription>(thriftColumns.size());

      for (TColumnDesc columnDesc : thriftColumns) {
        String name = columnDesc.getColumnName();
        String type = TYPE_NAMES.get(columnDesc.getTypeDesc().getTypes().get(0).getPrimitiveEntry().getType());
        int position = columnDesc.getPosition();
        columnDescriptions.add(ColumnDescriptionShort.createShortColumnDescription(name, type, position));
      }
      if (selectedColumns.size() == 0)
        this.schema = columnDescriptions;
      else {
        ArrayList<ColumnDescription> selectedColumnsSchema = new ArrayList<ColumnDescription>();
        for (Integer selectedIndex : selectedColumns) {
          selectedColumnsSchema.add(columnDescriptions.get(selectedIndex));
        }
        this.schema = selectedColumnsSchema;
      }
    }
    return this.schema;
  }

  /**
   * Get list with all values in one column
   * @param column column index
   * @return list of objects in column
   */
  public <T> List<T> getValuesInColumn(int column) {
    LinkedList<T> list = new LinkedList<T>();
    for (Row row : this) {
      list.add((T) row.getRow()[column]);
    }
    return list;
  }

  /**
   * Get logs Result object
   * @return Result object configured to fetch logs
   */
  public Cursor getLogs() {
    return new LogsCursor(connection, opHandle);
  }

  public void reset() {
    fetchedIterator = null;
    fetched = null;
    resetCursor = true;
    offset = 0;
  }

  @Override
  public boolean hasNext() {
    fetchIfNeeded();
    return fetchedIterator.hasNext();
  }

  private void fetchIfNeeded() {
    if (fetchedIterator == null || !fetchedIterator.hasNext()) {
      try {
        fetchNextBlock();
      } catch (HiveClientException e) {
        throw new HiveClientFormattedException(e);
      }
    }
  }

  @Override
  public Row next() {
    if (!hasNext())
      throw new NoSuchElementException();
    Row row = new Row(fetchedIterator.next(), selectedColumns);
    offset ++;
    return row;
  }

  @Override
  public void remove() {
    throw new NotImplementedException();
  }

  @Override
  public Iterator<Row> iterator() {
    return this;
  }

//  public int size() {
//    fetchIfNeeded();
//    return fetched.numRows();
//  }
  public long getOffset() {
    return offset;
  }

  public int read(ArrayList<Row> rows, int count) {
    int read = 0;
    while(read < count && hasNext()) {
      rows.add(next());
      read ++;
    }
    return read;
  }

  public Row getHeadersRow() throws HiveClientException {
    ArrayList<ColumnDescription> schema = getSchema();

    Object[] row = new Object[schema.size()];
    for (ColumnDescription columnDescription : schema) {
      row[columnDescription.getPosition()-1] = columnDescription.getName();
    }
    return new Row(row, selectedColumns);
  }

  public int readRaw(ArrayList<Object[]> rows, int count) {
    int read = 0;
    while(read < count && hasNext()) {
      rows.add(next().getRow());
      read ++;
    }
    return read;
  }

  public void selectColumns(String columnsRequested) {
    selectedColumns.clear();
    if (columnsRequested != null) {
      for (String columnRequested : columnsRequested.split(",")) {
        try {
          selectedColumns.add(Integer.parseInt(columnRequested));
        } catch (NumberFormatException ex) {
          throw new BadRequestFormattedException("Columns param should be comma-separated integers", ex);
        }
      }
    }
  }
}
