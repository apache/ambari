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

import java.util.ArrayList;

public class ColumnDescriptionShort extends ArrayList<Object> implements ColumnDescription {
  public static final int INITIAL_CAPACITY = 3;
  public static final int NAME_INDEX = 0;
  public static final int TYPE_INDEX = 1;
  public static final int POSITION_INDEX = 2;

  private ColumnDescriptionShort(String name, String type, int position) {
    super(INITIAL_CAPACITY);
    this.add(null);
    this.add(null);
    this.add(null);
    setName(name);
    setType(type);
    setPosition(position);
  }

  public static ColumnDescription createShortColumnDescription(String name, String type, int position) {
    return new ColumnDescriptionShort(name, type, position);
  }

  @Override
  public String getName() {
    return (String) this.get(NAME_INDEX);
  }

  @Override
  public void setName(String name) {
    this.set(NAME_INDEX, name);
  }

  @Override
  public String getType() {
    return (String) this.get(TYPE_INDEX);
  }

  @Override
  public void setType(String type) {
    this.set(TYPE_INDEX, type);
  }

  @Override
  public int getPosition() {
    return (Integer) this.get(POSITION_INDEX);
  }

  @Override
  public void setPosition(int position) {
    this.set(POSITION_INDEX, position);
  }
}
