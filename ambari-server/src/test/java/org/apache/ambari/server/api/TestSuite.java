package org.apache.ambari.server.api;

/**
 * All unit tests.
 */

import org.apache.ambari.server.api.handlers.*;
import org.apache.ambari.server.api.query.QueryImplTest;
import org.apache.ambari.server.api.services.*;
import org.apache.ambari.server.api.services.parsers.JsonPropertyParserTest;
import org.apache.ambari.server.api.services.serializers.JsonSerializerTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({ClusterServiceTest.class, HostServiceTest.class, ServiceServiceTest.class,
    ComponentServiceTest.class, HostComponentServiceTest.class, DelegatingRequestHandlerTest.class,
    ReadHandlerTest.class, QueryImplTest.class, JsonPropertyParserTest.class, CreateHandlerTest.class,
    UpdateHandlerTest.class, DeleteHandlerTest.class, CreatePersistenceManagerTest.class,
    UpdatePersistenceManagerTest.class, RequestImplTest.class, JsonSerializerTest.class})
public class TestSuite {
}
