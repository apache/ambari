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

package org.apache.ambari.api.controller.jdbc;

import org.apache.ambari.api.controller.internal.RequestImpl;
import org.apache.ambari.api.controller.spi.Predicate;
import org.apache.ambari.api.controller.spi.PropertyId;
import org.apache.ambari.api.controller.spi.Request;
import org.apache.ambari.api.controller.utilities.PredicateBuilder;
import org.apache.ambari.api.controller.utilities.Properties;
import org.junit.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

/**
 *
 */
public class JDBCManagementControllerTest {

  @Test
  public void testCreateCluster() throws Exception {
    ConnectionFactory connectionFactory = createNiceMock(ConnectionFactory.class);
    Connection connection = createNiceMock(Connection.class);
    Statement statement = createNiceMock(Statement.class);

    expect(connectionFactory.getConnection()).andReturn(connection).once();
    expect(connection.createStatement()).andReturn(statement).once();
    expect(statement.execute("insert into Clusters (state, version, cluster_name) values ('initial', '1.0', 'MyCluster')")).andReturn(true).once();

    replay(connectionFactory, connection, statement);

    JDBCManagementController provider =  new JDBCManagementController(connectionFactory);

    Map<PropertyId, String> properties = new HashMap<PropertyId, String>();

    PropertyId id = Properties.getPropertyId("cluster_name", "Clusters");
    properties.put(id, "MyCluster");

    id = Properties.getPropertyId("version", "Clusters");
    properties.put(id, "1.0");

    id = Properties.getPropertyId("state", "Clusters");
    properties.put(id, "initial");

    Set<Map<PropertyId, String>> propertySet = new HashSet<Map<PropertyId, String>>();
    propertySet.add(properties);

    Request request = new RequestImpl(null, propertySet);

    provider.createClusters(request);

    verify(connectionFactory, connection, statement);
  }

  @Test
  public void testCreateService() throws Exception{

    ConnectionFactory connectionFactory = createNiceMock(ConnectionFactory.class);
    Connection connection = createNiceMock(Connection.class);
    Statement statement = createNiceMock(Statement.class);

    expect(connectionFactory.getConnection()).andReturn(connection).once();
    expect(connection.createStatement()).andReturn(statement).once();
    expect(statement.execute("insert into ServiceInfo (service_name, cluster_name, state) values ('MyService', 'MyCluster', 'initial')")).andReturn(true).once();

    replay(connectionFactory, connection, statement);

    JDBCManagementController provider =  new JDBCManagementController(connectionFactory);

    Map<PropertyId, String> properties = new HashMap<PropertyId, String>();

    PropertyId id = Properties.getPropertyId("cluster_name", "ServiceInfo");
    properties.put(id, "MyCluster");

    id = Properties.getPropertyId("service_name", "ServiceInfo");
    properties.put(id, "MyService");

    id = Properties.getPropertyId("state", "ServiceInfo");
    properties.put(id, "initial");

    Set<Map<PropertyId, String>> propertySet = new HashSet<Map<PropertyId, String>>();
    propertySet.add(properties);

    Request request = new RequestImpl(null, propertySet);

    provider.createServices(request);

    verify(connectionFactory, connection, statement);
  }

  @Test
  public void testDeleteCluster() throws Exception{

    ConnectionFactory connectionFactory = createNiceMock(ConnectionFactory.class);
    Connection connection = createNiceMock(Connection.class);
    Statement statement = createNiceMock(Statement.class);

    expect(connectionFactory.getConnection()).andReturn(connection).once();
    expect(connection.createStatement()).andReturn(statement).once();
    expect(statement.execute("delete from Clusters where Clusters.cluster_name = \"MyCluster\"")).andReturn(true).once();

    replay(connectionFactory, connection, statement);

    JDBCManagementController provider =  new JDBCManagementController(connectionFactory);

    Predicate predicate = new PredicateBuilder().property("cluster_name", "Clusters").equals("MyCluster").toPredicate();

    provider.deleteServices(predicate);

    verify(connectionFactory, connection, statement);
  }

  @Test
  public void testUpdateCluster() throws Exception{

    ConnectionFactory connectionFactory = createNiceMock(ConnectionFactory.class);
    Connection connection = createNiceMock(Connection.class);
    Statement statement = createNiceMock(Statement.class);

    expect(connectionFactory.getConnection()).andReturn(connection).once();
    expect(connection.createStatement()).andReturn(statement).once();
    expect(statement.execute("update Clusters set state = 'running' where Clusters.cluster_name = \"MyCluster\"")).andReturn(true).once();

    replay(connectionFactory, connection, statement);

    JDBCManagementController provider =  new JDBCManagementController(connectionFactory);

    Map<PropertyId, String> properties = new HashMap<PropertyId, String>();

    PropertyId id = Properties.getPropertyId("state", "Clusters");
    properties.put(id, "running");

    Predicate predicate = new PredicateBuilder().property("cluster_name", "Clusters").equals("MyCluster").toPredicate();

    Set<Map<PropertyId, String>> propertySet = new HashSet<Map<PropertyId, String>>();
    propertySet.add(properties);

    Request request = new RequestImpl(null, propertySet);

    provider.updateClusters(request, predicate);

    verify(connectionFactory, connection, statement);
  }
}
