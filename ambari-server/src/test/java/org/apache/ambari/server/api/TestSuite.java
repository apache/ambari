package org.apache.ambari.server.api;

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

/**
 * All unit tests.
 */

import org.apache.ambari.server.api.handlers.*;
import org.apache.ambari.server.api.query.QueryImplTest;
import org.apache.ambari.server.api.resources.ResourceInstanceImplTest;
import org.apache.ambari.server.api.services.*;
import org.apache.ambari.server.api.services.parsers.JsonPropertyParserTest;
import org.apache.ambari.server.api.services.serializers.JsonSerializerTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({ClusterServiceTest.class, HostServiceTest.class, ServiceServiceTest.class,
    ComponentServiceTest.class, HostComponentServiceTest.class, ReadHandlerTest.class, QueryImplTest.class,
    JsonPropertyParserTest.class, CreateHandlerTest.class, UpdateHandlerTest.class, DeleteHandlerTest.class,
    PersistenceManagerImplTest.class, GetRequestTest.class, PutRequestTest.class, PostRequestTest.class,
    DeleteRequestTest.class, JsonSerializerTest.class, QueryCreateHandlerTest.class, ResourceInstanceImplTest.class})
public class TestSuite {
}
