/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.view.hive20.resources.upload;

import org.apache.ambari.view.hive20.client.ColumnDescription;
import org.apache.ambari.view.hive20.client.Row;
import org.apache.ambari.view.hive20.internal.dto.ColumnInfo;
import org.apache.ambari.view.hive20.resources.uploads.ColumnDescriptionImpl;
import org.apache.ambari.view.hive20.resources.uploads.TableDataReader;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class TableDataReaderTest {

  private class RowIter implements Iterator<Row> {
    int numberOfRows;
    int numberOfCols;
    int index = 0 ;
    ArrayList<Row> rows = new ArrayList<Row>();
    public RowIter(int numberOfRows, int numberOfCols){
      this.numberOfRows = numberOfRows;
      this.numberOfCols = numberOfCols;
      int x = 0 ;
      for(int i = 0; i < this.numberOfRows; i++ ){
        Object [] objArray = new Object[10];
        for(int j = 0; j < this.numberOfCols; j++ ){
          objArray[j] = x++ + "" ;
        }
        Row row = new Row(objArray);
        rows.add(row);
      }
    }
    @Override
    public boolean hasNext() {
      return index < numberOfRows;
    }

    @Override
    public Row next() {
      return rows.get(index++);
    }

    @Override
    public void remove() {
      throw new RuntimeException("Operation not supported.");
    }

    @Override
    public String toString() {
      return "RowIter{" +
              "index=" + index +
              ", rows=" + rows +
              '}';
    }
  }

  @Test
  public void testCSVReader() throws IOException {
    RowIter rowIter = new RowIter(10,10);
    List<ColumnInfo> colDescs = new LinkedList<>();
    for(int i = 0 ; i < 10 ; i++ ) {
      ColumnInfo cd = new ColumnInfo("col" + (i+1) , ColumnDescription.DataTypes.STRING.toString());
      colDescs.add(cd);
    }

    TableDataReader tableDataReader = new TableDataReader(rowIter, colDescs, false);

    char del = TableDataReader.CSV_DELIMITER;
    char[] first10 = {'0', del, '1', del, '2', del, '3', del, '4', del};
    char [] buf = new char[10];
    tableDataReader.read(buf,0,10);

    Assert.assertArrayEquals(first10,buf);

    char[] next11 = {'5', del, '6', del, '7', del, '8', del, '9', '\n', '1'}; //"5,6,7,8,9\n1".toCharArray();
    char [] buf1 = new char[11];
    tableDataReader.read(buf1,0,11);

    Assert.assertArrayEquals(next11,buf1);

    // read it fully
    while( tableDataReader.read(buf,0,10) != -1 );

    char [] last10 = {'9', '7', del, '9', '8', del, '9', '9', '\n', del}; //"97,98,99\n,".toCharArray(); // last comma is the left over of previous read.

    Assert.assertArrayEquals(last10,buf);
  }

  @Test
  public void testEmptyCSVReader() throws IOException {
    RowIter rowIter = new RowIter(0,0);

    TableDataReader tableDataReader = new TableDataReader(rowIter, null, false);

    char[] first10 = new char [10];
    char [] buf = new char[10];
    for( int i = 0 ; i < 10 ; i++ ){
      first10[i] = '\0';
      buf[i] = '\0';
    }

    tableDataReader.read(buf,0,10);

    Assert.assertArrayEquals(first10,buf);
  }
}
