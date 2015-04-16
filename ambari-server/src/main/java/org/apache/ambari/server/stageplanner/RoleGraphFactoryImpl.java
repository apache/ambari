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
package org.apache.ambari.server.stageplanner;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.metadata.RoleCommandOrder;


@Singleton
public class RoleGraphFactoryImpl implements RoleGraphFactory {
  private Injector injector;

  @Inject
  public RoleGraphFactoryImpl(Injector injector) {
    this.injector = injector;
  }

  /**
   *
   * @return
   */
  @Override
  public RoleGraph createNew() {
    return new RoleGraph(this.injector.getInstance(StageFactory.class));
  }

  /**
   *
   * @param rd
   * @return
   */
  @Override
  public RoleGraph createNew(RoleCommandOrder rd) {
    return new RoleGraph(rd, this.injector.getInstance(StageFactory.class));
  }
}
