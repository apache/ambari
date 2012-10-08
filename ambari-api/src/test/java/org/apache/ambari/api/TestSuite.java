package org.apache.ambari.api;

/**
 * All unit tests.
 */

import org.apache.ambari.api.handlers.DelegatingRequestHandlerTest;
import org.apache.ambari.api.handlers.ReadHandlerTest;
import org.apache.ambari.api.services.ClusterServiceTest;
import org.apache.ambari.api.services.ComponentServiceTest;
import org.apache.ambari.api.services.HostComponentServiceTest;
import org.apache.ambari.api.services.HostServiceTest;
import org.apache.ambari.api.services.ServiceServiceTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({ClusterServiceTest.class, HostServiceTest.class, ServiceServiceTest.class,
    ComponentServiceTest.class, HostComponentServiceTest.class, DelegatingRequestHandlerTest.class,
    ReadHandlerTest.class})
public class TestSuite {
}
