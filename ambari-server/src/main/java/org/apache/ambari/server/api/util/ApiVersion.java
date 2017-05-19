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

package org.apache.ambari.server.api.util;

import java.util.EnumSet;

public enum ApiVersion {
  v1,
  v2;

  public static ApiVersion Default = v1;
  public static EnumSet<ApiVersion> all = EnumSet.allOf(ApiVersion.class);
  public static EnumSet<ApiVersion> v1Only = EnumSet.of(v1);
  public static EnumSet<ApiVersion> v2Only = EnumSet.of(v2);
  public static EnumSet<ApiVersion> v2Plus = EnumSet.complementOf(v1Only);
}
