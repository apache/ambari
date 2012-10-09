package org.apache.ambari.api;

/**
 * All unit tests.
 */

import org.apache.ambari.api.handlers.*;
import org.apache.ambari.api.services.*;
import org.apache.ambari.api.services.parsers.JsonPropertyParserTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({ClusterServiceTest.class, HostServiceTest.class, ServiceServiceTest.class,
    ComponentServiceTest.class, HostComponentServiceTest.class, DelegatingRequestHandlerTest.class,
    ReadHandlerTest.class, /*QueryImplTest.class*/ JsonPropertyParserTest.class, CreateHandlerTest.class,
    UpdateHandlerTest.class, DeleteHandlerTest.class, CreatePersistenceManagerTest.class,
    UpdatePersistenceManagerTest.class})
public class TestSuite {
}
