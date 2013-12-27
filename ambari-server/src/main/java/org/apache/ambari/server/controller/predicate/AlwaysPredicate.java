package org.apache.ambari.server.controller.predicate;

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

import org.apache.ambari.server.controller.spi.Resource;

import java.util.Collections;
import java.util.Set;

/**
 * A predicate that always evaluates to true.
 */
public class AlwaysPredicate implements BasePredicate {
  public static final AlwaysPredicate INSTANCE = new AlwaysPredicate();

  @Override
  public boolean evaluate(Resource resource) {
    return true;
  }

  @Override
  public Set<String> getPropertyIds() {
    return Collections.emptySet();
  }

  @Override
  public void accept(PredicateVisitor visitor) {
    visitor.acceptAlwaysPredicate(this);
  }


  // ----- Object overrides --------------------------------------------------

  @Override
  public String toString() {
    return "TRUE";
  }
}
