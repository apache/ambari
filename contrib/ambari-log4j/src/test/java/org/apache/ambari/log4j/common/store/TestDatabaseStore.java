/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.log4j.common.store;

import junit.framework.TestCase;
import org.apache.ambari.log4j.common.LogStoreUpdateProvider;
import org.apache.log4j.spi.LoggingEvent;

import java.io.IOException;
import java.sql.Connection;

public class TestDatabaseStore extends TestCase {

  class SampleLogStoreUpdateProvider implements LogStoreUpdateProvider {
    public void init(Connection connection) throws IOException {
    }

    public void update(LoggingEvent originalEvent, Object parsedEvent)
        throws IOException {
    }
  }

  public void testDatabaseStore() throws IOException {
    DatabaseStore store = new DatabaseStore(TestDatabaseStore.class.getName(), "", "", "",
        new SampleLogStoreUpdateProvider());
    store.close();
  }
}
