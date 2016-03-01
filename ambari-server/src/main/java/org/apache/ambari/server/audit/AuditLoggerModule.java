/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.audit;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

public class AuditLoggerModule extends AbstractModule {

  private final static int BUFFERED_LOGGER_CAPACITY = 10000;

  @Override
  protected void configure() {
    bind(AuditLogger.class).to(BufferedAuditLogger.class);

    // set AuditLoggerDefaultImpl to be used by BufferedAuditLogger
    bind(AuditLogger.class).annotatedWith(Names.named(BufferedAuditLogger.InnerLogger)).to(AuditLoggerDefaultImpl.class);

    // set buffered audit logger capacity
    bindConstant().annotatedWith(Names.named(BufferedAuditLogger.Capacity)).to(BUFFERED_LOGGER_CAPACITY);

  }


}
