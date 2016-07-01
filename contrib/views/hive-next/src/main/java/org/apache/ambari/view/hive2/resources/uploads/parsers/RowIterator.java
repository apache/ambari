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

package org.apache.ambari.view.hive2.resources.uploads.parsers;

import org.apache.ambari.view.hive2.client.Row;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;

/**
 * Converts the Map of values created by JSON/XML Parser into ordered values in Row
 * Takes RowMapIterator as input
 */
public class RowIterator implements Iterator<Row> {

  private LinkedList<String> headers = null;
  private RowMapIterator iterator;

  /**
   * creates a row iterator for the map values in RowMapIterator
   * keeps the keys in map as header.
   * @param iterator
   */
  public RowIterator(RowMapIterator iterator) {
    this.iterator = iterator;
    LinkedHashMap<String, String> obj = iterator.peek();
    headers = new LinkedList<>();
    if (null != obj) {
      headers.addAll(obj.keySet());
    }
  }

  @Override
  public boolean hasNext() {
    return iterator.hasNext();
  }


  @Override
  public Row next() {
    LinkedHashMap<String, String> r = this.iterator.next();
    if (null == r) {
      return null;
    }

    return convertToRow(r);
  }

  @Override
  public void remove() {
    iterator.remove();
  }

  /**
   * @return : ordered collection of string of headers
   */
  public LinkedList<String> extractHeaders() {
    return headers;
  }

  /**
   * converts the map into a Row
   * @param lr
   * @return
   */
  private Row convertToRow(LinkedHashMap<String, String> lr) {
    Object[] data = new Object[headers.size()];
    int i = 0;
    for (String cd : headers) {
      String d = lr.get(cd);

      if (d != null)
        d = d.trim(); // trim to remove any \n etc which is used as a separator for rows in TableDataReader

      data[i++] = d;
    }

    return new Row(data);
  }

}