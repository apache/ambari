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

package org.apache.ambari.view.hive20.resources;

import org.apache.ambari.view.hive20.persistence.utils.FilteringStrategy;
import org.apache.ambari.view.hive20.persistence.utils.Indexed;
import org.apache.ambari.view.hive20.persistence.utils.ItemNotFound;

import java.util.List;

public interface IResourceManager<T extends Indexed> {
  T create(T object);

  T read(Object id) throws ItemNotFound;

  List<T> readAll(FilteringStrategy filteringStrategy);

  T update(T newObject, String id) throws ItemNotFound;

  void delete(Object resourceId) throws ItemNotFound;
}
