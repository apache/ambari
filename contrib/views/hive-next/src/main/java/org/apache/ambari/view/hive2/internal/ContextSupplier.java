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

package org.apache.ambari.view.hive2.internal;

import org.apache.ambari.view.ViewContext;

/**
 * A class that can supply objects of same type.
 * @param <T>
 */
public interface ContextSupplier<T> {
  /**
   * Retrieves an instance of appropriate type. The returned object could be a new instance
   * or an exiting instance. No guarantee on that.
   * @param context View Context to be used to create the instance
   * @return instance of appropriateType
   */
  T get(ViewContext context);


}
