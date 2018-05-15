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

package org.apache.ambari.view.hive2.resources.jobs.rm;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.utils.ambari.AmbariApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RMParserFactory {
  protected final static Logger LOG =
      LoggerFactory.getLogger(RMParserFactory.class);

  private final ViewContext context;
  private final AmbariApi ambariApi;

  public RMParserFactory(ViewContext context) {
    this.context = context;
    this.ambariApi = new AmbariApi(context);
  }

  public RMParser getRMParser() {
    String rmUrl = getRMUrl();

    RMRequestsDelegate delegate = new RMRequestsDelegateImpl(context, rmUrl);
    return new RMParser(delegate);
  }

  public String getRMUrl() {
    return ambariApi.getServices().getRMUrl();
  }
}
