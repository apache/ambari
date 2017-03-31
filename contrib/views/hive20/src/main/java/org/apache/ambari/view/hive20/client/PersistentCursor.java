/*
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


import com.google.common.collect.Lists;

import java.util.Iterator;
import java.util.List;

/**
 * Wrapper over other iterables. Does not block and can be reset to start again from beginning.
 */
public class PersistentCursor<T, R> implements Cursor<T, R>  {
  private List<T> rows = Lists.newArrayList();
  private List<R> columns = Lists.newArrayList();
  private int offset = 0;

  public PersistentCursor(List<T> rows, List<R> columns) {
    this.rows = rows;
    this.columns = columns;
  }


  @Override
  public Iterator<T> iterator() {
    return this;
  }

  @Override
  public boolean hasNext() {
    return rows.size() > 0 && offset < rows.size();
  }

  @Override
  public T next() {
    T row = rows.get(offset);
    offset++;
    return row;
  }

  @Override
  public void remove() {
    throw new RuntimeException("Read only cursor. Method not supported");
  }

  @Override
  public boolean isResettable() {
    return true;
  }

  @Override
  public void reset() {
    this.offset = 0;
  }

  @Override
  public int getOffset() {
    return offset;
  }

  @Override
  public List<R> getDescriptions() {
    return columns;
  }

  @Override
  public void keepAlive() {
    // Do Nothing as we are pre-fetching everything.
  }
}
