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

import java.util.ArrayList;

public class ColumnDescriptionShort extends ArrayList<Object> implements ColumnDescription {
  private static final int INITIAL_CAPACITY = 3;
  private String name;
  private String type;
  private int position;

  public ColumnDescriptionShort(String name, String type, int position) {
    super(INITIAL_CAPACITY);
    add(name);
    add(type);
    add(position);
    this.name = name;
    this.type = type;
    this.position = position;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getType() {
    return type;
  }

  @Override
  public int getPosition() {
    return position;
  }
}
