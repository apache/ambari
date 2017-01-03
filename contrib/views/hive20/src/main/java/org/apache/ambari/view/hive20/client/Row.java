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

package org.apache.ambari.view.hive20.client;

import java.util.Arrays;
import java.util.HashSet;

public class Row {
  private Object[] row;

  public Row(Object[] row) {
    this(row, null);
  }

  public Row(Object[] row, HashSet<Integer> selectedColumns) {
    if (selectedColumns == null || selectedColumns.size() == 0)
      this.row = row.clone();
    else {
      this.row = new Object[selectedColumns.size()];
      int rowIndex = 0;
      for (Integer selectedIndex : selectedColumns) {
        this.row[rowIndex] = row[selectedIndex];
        rowIndex ++;
      }
    }
  }

  public Object[] getRow() {
    return row;
  }

  public void setRow(Object[] row) {
    this.row = row;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Row row1 = (Row) o;

    boolean retValue = Arrays.equals(row, row1.row);
    return retValue;
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(row);
  }

  @Override
  public String toString() {
    return "Row{" +
            "row=" + Arrays.toString(row) +
            '}';
  }
}
