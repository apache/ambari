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

package org.apache.ambari.view.hive2.resources.uploads.parsers.csv.commonscsv;

import org.apache.ambari.view.hive2.client.Row;
import org.apache.commons.csv.CSVRecord;

import java.util.Iterator;

/**
 * iterates over the input CSV records and generates Row objects
 */
class CSVIterator implements Iterator<Row> {

  private Iterator<CSVRecord> iterator;

  public CSVIterator(Iterator<CSVRecord> iterator) {
    this.iterator = iterator;
  }

  @Override
  public boolean hasNext() {
    return iterator.hasNext();
  }

  @Override
  public Row next() {
    CSVRecord row = iterator.next();
    Object[] values = new Object[row.size()];
    for (int i = 0; i < values.length; i++) {
      values[i] = row.get(i);
    }
    Row r = new Row(values);
    return r;
  }

  @Override
  public void remove() {
    this.iterator.remove();
  }
}
