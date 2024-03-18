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

package org.apache.ambari.metrics.core.timeline.uuid;

import java.util.Arrays;

public class TimelineMetricUuid {
  public byte[] uuid;

  public TimelineMetricUuid(byte[] uuid) {
    this.uuid = uuid;
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(uuid);
  }

  @Override
  public boolean equals(Object o) {

    if (this == o) {
      return false;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    TimelineMetricUuid that = (TimelineMetricUuid) o;

    return Arrays.equals(this.uuid, that.uuid);
  }

  @Override
  public String toString() {
    return Arrays.toString(uuid);
  }
}
