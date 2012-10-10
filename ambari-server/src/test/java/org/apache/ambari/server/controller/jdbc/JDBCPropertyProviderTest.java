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

package org.apache.ambari.server.controller.jdbc;

import static org.easymock.EasyMock.createNiceMock;

/**
*
*/
public class JDBCPropertyProviderTest {

//  @Test
//  public void testCreateClusters() throws Exception {
//    ConnectionFactory connectionFactory = createNiceMock(ConnectionFactory.class);
//    Connection connection = createNiceMock(Connection.class);
//    Statement statement = createNiceMock(Statement.class);
//
//    expect(connectionFactory.getConnection()).andReturn(connection).once();
//    expect(connection.createStatement()).andReturn(statement).once();
//    expect(statement.execute("insert into Clusters (cluster_name, version, state) values ('MyCluster', '1.0', 'initial')")).andReturn(true).once();
//
//    replay(connectionFactory, connection, statement);
//
//    JDBCManagementController controller =  new JDBCManagementController(connectionFactory, ClusterControllerHelper.RESOURCE_TABLES);
//
//    Map<PropertyId, Object> properties = new LinkedHashMap<PropertyId, Object>();
//
//    PropertyId id = PropertyHelper.getPropertyId("cluster_name", "Clusters");
//    properties.put(id, "MyCluster");
//
//    id = PropertyHelper.getPropertyId("version", "Clusters");
//    properties.put(id, "1.0");
//
//    id = PropertyHelper.getPropertyId("state", "Clusters");
//    properties.put(id, "initial");
//
//    Set<Map<PropertyId, Object>> propertySet = new LinkedHashSet<Map<PropertyId, Object>>();
//    propertySet.add(properties);
//
//    Request request = new RequestImpl(null, propertySet);
//
//    controller.createClusters(request);
//
//    verify(connectionFactory, connection, statement);
//  }
//
//  @Test
//  public void testCreateServices() throws Exception{
//
//    ConnectionFactory connectionFactory = createNiceMock(ConnectionFactory.class);
//    Connection connection = createNiceMock(Connection.class);
//    Statement statement = createNiceMock(Statement.class);
//
//    expect(connectionFactory.getConnection()).andReturn(connection).once();
//    expect(connection.createStatement()).andReturn(statement).once();
//    expect(statement.execute("insert into ServiceInfo (cluster_name, service_name, state) values ('MyCluster', 'MyService', 'initial')")).andReturn(true).once();
//
//    replay(connectionFactory, connection, statement);
//
//    JDBCManagementController controller =  new JDBCManagementController(connectionFactory, ClusterControllerHelper.RESOURCE_TABLES);
//
//    Map<PropertyId, Object> properties = new LinkedHashMap<PropertyId, Object>();
//
//    PropertyId id = PropertyHelper.getPropertyId("cluster_name", "ServiceInfo");
//    properties.put(id, "MyCluster");
//
//    id = PropertyHelper.getPropertyId("service_name", "ServiceInfo");
//    properties.put(id, "MyService");
//
//    id = PropertyHelper.getPropertyId("state", "ServiceInfo");
//    properties.put(id, "initial");
//
//    Set<Map<PropertyId, Object>> propertySet = new LinkedHashSet<Map<PropertyId, Object>>();
//    propertySet.add(properties);
//
//    Request request = new RequestImpl(null, propertySet);
//
//    controller.createServices(request);
//
//    verify(connectionFactory, connection, statement);
//  }
//
//  @Test
//  public void testCreateHosts() throws Exception{
//
//    ConnectionFactory connectionFactory = createNiceMock(ConnectionFactory.class);
//    Connection connection = createNiceMock(Connection.class);
//    Statement statement = createNiceMock(Statement.class);
//
//    expect(connectionFactory.getConnection()).andReturn(connection).once();
//    expect(connection.createStatement()).andReturn(statement).once();
//    expect(statement.execute("insert into Hosts (cluster_name, host_name, ip) values ('MyCluster', 'MyHost1', '10.68.18.171')")).andReturn(true).once();
//    expect(connection.createStatement()).andReturn(statement).once();
//    expect(statement.execute("insert into Hosts (cluster_name, host_name, ip) values ('MyCluster', 'MyHost2', '10.111.35.113')")).andReturn(true).once();
//
//    replay(connectionFactory, connection, statement);
//
//    JDBCManagementController controller =  new JDBCManagementController(connectionFactory, ClusterControllerHelper.RESOURCE_TABLES);
//
//    Set<Map<PropertyId, Object>> propertySet = new LinkedHashSet<Map<PropertyId, Object>>();
//
//    // first host
//    Map<PropertyId, Object> properties = new LinkedHashMap<PropertyId, Object>();
//
//    PropertyId id = PropertyHelper.getPropertyId("cluster_name", "Hosts");
//    properties.put(id, "MyCluster");
//
//    id = PropertyHelper.getPropertyId("host_name", "Hosts");
//    properties.put(id, "MyHost1");
//
//    id = PropertyHelper.getPropertyId("ip", "Hosts");
//    properties.put(id, "10.68.18.171");
//
//    propertySet.add(properties);
//
//    // second host
//    properties = new LinkedHashMap<PropertyId, Object>();
//
//    id = PropertyHelper.getPropertyId("cluster_name", "Hosts");
//    properties.put(id, "MyCluster");
//
//    id = PropertyHelper.getPropertyId("host_name", "Hosts");
//    properties.put(id, "MyHost2");
//
//    id = PropertyHelper.getPropertyId("ip", "Hosts");
//    properties.put(id, "10.111.35.113");
//
//    propertySet.add(properties);
//
//    Request request = new RequestImpl(null, propertySet);
//
//    controller.createHosts(request);
//
//    verify(connectionFactory, connection, statement);
//  }
//
//  @Test
//  public void testCreateComponents() throws Exception{
//
//    ConnectionFactory connectionFactory = createNiceMock(ConnectionFactory.class);
//    Connection connection = createNiceMock(Connection.class);
//    Statement statement = createNiceMock(Statement.class);
//
//    expect(connectionFactory.getConnection()).andReturn(connection).once();
//    expect(connection.createStatement()).andReturn(statement).once();
//    expect(statement.execute("insert into ServiceComponentInfo (cluster_name, service_name, component_name, description) values ('MyCluster', 'MyService', 'MyComponent', 'This is my component')")).andReturn(true).once();
//
//    replay(connectionFactory, connection, statement);
//
//    JDBCManagementController controller =  new JDBCManagementController(connectionFactory, ClusterControllerHelper.RESOURCE_TABLES);
//
//    Map<PropertyId, Object> properties = new LinkedHashMap<PropertyId, Object>();
//
//    PropertyId id = PropertyHelper.getPropertyId("cluster_name", "ServiceComponentInfo");
//    properties.put(id, "MyCluster");
//
//    id = PropertyHelper.getPropertyId("service_name", "ServiceComponentInfo");
//    properties.put(id, "MyService");
//
//    id = PropertyHelper.getPropertyId("component_name", "ServiceComponentInfo");
//    properties.put(id, "MyComponent");
//
//    id = PropertyHelper.getPropertyId("description", "ServiceComponentInfo");
//    properties.put(id, "This is my component");
//
//    Set<Map<PropertyId, Object>> propertySet = new LinkedHashSet<Map<PropertyId, Object>>();
//    propertySet.add(properties);
//
//    Request request = new RequestImpl(null, propertySet);
//
//    controller.createComponents(request);
//
//    verify(connectionFactory, connection, statement);
//  }
//
//  @Test
//  public void testCreateHostComponents() throws Exception{
//
//    ConnectionFactory connectionFactory = createNiceMock(ConnectionFactory.class);
//    Connection connection = createNiceMock(Connection.class);
//    Statement statement = createNiceMock(Statement.class);
//
//    expect(connectionFactory.getConnection()).andReturn(connection).once();
//    expect(connection.createStatement()).andReturn(statement).once();
//    expect(statement.execute("insert into HostRoles (cluster_name, host_name, component_name, role_id) values ('MyCluster', 'MyHost', 'MyComponent', 1)")).andReturn(true).once();
//
//    replay(connectionFactory, connection, statement);
//
//    JDBCManagementController controller =  new JDBCManagementController(connectionFactory, ClusterControllerHelper.RESOURCE_TABLES);
//
//    Map<PropertyId, Object> properties = new LinkedHashMap<PropertyId, Object>();
//
//    PropertyId id = PropertyHelper.getPropertyId("cluster_name", "HostRoles");
//    properties.put(id, "MyCluster");
//
//    id = PropertyHelper.getPropertyId("host_name", "HostRoles");
//    properties.put(id, "MyHost");
//
//    id = PropertyHelper.getPropertyId("component_name", "HostRoles");
//    properties.put(id, "MyComponent");
//
//    id = PropertyHelper.getPropertyId("role_id", "HostRoles");
//    properties.put(id, 1);
//
//    Set<Map<PropertyId, Object>> propertySet = new LinkedHashSet<Map<PropertyId, Object>>();
//    propertySet.add(properties);
//
//    Request request = new RequestImpl(null, propertySet);
//
//    controller.createHostComponents(request);
//
//    verify(connectionFactory, connection, statement);
//  }
//
//  @Test
//  public void testDeleteClusters() throws Exception{
//
//    ConnectionFactory connectionFactory = createNiceMock(ConnectionFactory.class);
//    Connection connection = createNiceMock(Connection.class);
//    Statement statement = createNiceMock(Statement.class);
//
//    expect(connectionFactory.getConnection()).andReturn(connection).once();
//    expect(connection.createStatement()).andReturn(statement).once();
//    expect(statement.execute("delete from Clusters where Clusters.cluster_name = \"MyCluster\"")).andReturn(true).once();
//
//    replay(connectionFactory, connection, statement);
//
//    JDBCManagementController controller =  new JDBCManagementController(connectionFactory, ClusterControllerHelper.RESOURCE_TABLES);
//
//    Predicate predicate = new PredicateBuilder().property("cluster_name", "Clusters").equals("MyCluster").toPredicate();
//
//    controller.deleteClusters(predicate);
//
//    verify(connectionFactory, connection, statement);
//  }
//
//  @Test
//  public void testDeleteServices() throws Exception{
//
//    ConnectionFactory connectionFactory = createNiceMock(ConnectionFactory.class);
//    Connection connection = createNiceMock(Connection.class);
//    DatabaseMetaData databaseMetaData = createNiceMock(DatabaseMetaData.class);
//    ResultSet metaDataResultSet = createNiceMock(ResultSet.class);
//    Statement statement = createNiceMock(Statement.class);
//    ResultSet resultSet = createNiceMock(ResultSet.class);
//    ResultSetMetaData resultSetMetaData = createNiceMock(ResultSetMetaData.class);
//
//    expect(connectionFactory.getConnection()).andReturn(connection).once();
//    expect(connection.getMetaData()).andReturn(databaseMetaData).atLeastOnce();
//    expect(databaseMetaData.getImportedKeys((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject())).andReturn(metaDataResultSet).atLeastOnce();
//    expect(metaDataResultSet.next()).andReturn(true).once();
//    expect(metaDataResultSet.getString("PKCOLUMN_NAME")).andReturn("service_name").once();
//    expect(metaDataResultSet.getString("PKTABLE_NAME")).andReturn("ServiceInfo").once();
//    expect(metaDataResultSet.getString("FKCOLUMN_NAME")).andReturn("service_name").once();
//    expect(metaDataResultSet.getString("FKTABLE_NAME")).andReturn("Services").once();
//    expect(metaDataResultSet.next()).andReturn(false).once();
//    expect(databaseMetaData.getPrimaryKeys((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject())).andReturn(metaDataResultSet).atLeastOnce();
//    expect(metaDataResultSet.next()).andReturn(true).once();
//    expect(metaDataResultSet.getString("COLUMN_NAME")).andReturn("service_name").once();
//    expect(metaDataResultSet.getString("TABLE_NAME")).andReturn("ServiceInfo").once();
//    expect(metaDataResultSet.next()).andReturn(true).once();
//    expect(metaDataResultSet.getString("COLUMN_NAME")).andReturn("cluster_name").once();
//    expect(metaDataResultSet.getString("TABLE_NAME")).andReturn("ServiceInfo").once();
//    expect(metaDataResultSet.next()).andReturn(false).once();
//    expect(connection.createStatement()).andReturn(statement).atLeastOnce();
//    expect(statement.executeQuery("select ServiceInfo.service_name, ServiceInfo.cluster_name from Services, ServiceInfo where Services.display_name = \"my service\" AND ServiceInfo.service_name = Services.service_name")).andReturn(resultSet).once();
//    expect(resultSet.getMetaData()).andReturn(resultSetMetaData).once();
//    expect(resultSetMetaData.getColumnCount()).andReturn(2).once();
//    expect(resultSet.next()).andReturn(true).once();
//    expect(resultSetMetaData.getColumnName(1)).andReturn("service_name").once();
//    expect(resultSetMetaData.getTableName(1)).andReturn("ServiceInfo").once();
//    expect(resultSet.getString(1)).andReturn("MyService").once();
//    expect(resultSetMetaData.getColumnName(2)).andReturn("cluster_name").once();
//    expect(resultSetMetaData.getTableName(2)).andReturn("ServiceInfo").once();
//    expect(resultSet.getString(2)).andReturn("MyCluster").once();
//    expect(resultSet.next()).andReturn(false).once();
//    expect(statement.execute("delete from ServiceInfo where (ServiceInfo.cluster_name = \"MyCluster\" AND ServiceInfo.service_name = \"MyService\")")).andReturn(true).once();
//
//    replay(connectionFactory, connection, databaseMetaData, metaDataResultSet, statement, resultSet, resultSetMetaData);
//
//    JDBCManagementController controller =  new JDBCManagementController(connectionFactory, ClusterControllerHelper.RESOURCE_TABLES);
//
//    Predicate predicate = new PredicateBuilder().property("display_name", "Services").equals("my service").toPredicate();
//
//    controller.deleteServices(predicate);
//
//    verify(connectionFactory, connection, databaseMetaData, metaDataResultSet, statement, resultSet, resultSetMetaData);
//  }
//
//  @Test
//  public void testDeleteHosts() throws Exception{
//
//    ConnectionFactory connectionFactory = createNiceMock(ConnectionFactory.class);
//    Connection connection = createNiceMock(Connection.class);
//    Statement statement = createNiceMock(Statement.class);
//
//    expect(connectionFactory.getConnection()).andReturn(connection).once();
//    expect(connection.createStatement()).andReturn(statement).once();
//    expect(statement.execute("delete from Hosts where (Hosts.host_name = \"MyHost1\" OR Hosts.host_name = \"MyHost2\")")).andReturn(true).once();
//
//    replay(connectionFactory, connection, statement);
//
//    JDBCManagementController controller =  new JDBCManagementController(connectionFactory, ClusterControllerHelper.RESOURCE_TABLES);
//
//    Predicate predicate = new PredicateBuilder().property("host_name", "Hosts").equals("MyHost1").or().
//                                                 property("host_name", "Hosts").equals("MyHost2").toPredicate();
//
//    controller.deleteHosts(predicate);
//
//    verify(connectionFactory, connection, statement);
//  }
//
//  @Test
//  public void testDeleteComponents() throws Exception{
//
//    ConnectionFactory connectionFactory = createNiceMock(ConnectionFactory.class);
//    Connection connection = createNiceMock(Connection.class);
//    Statement statement = createNiceMock(Statement.class);
//
//    expect(connectionFactory.getConnection()).andReturn(connection).once();
//    expect(connection.createStatement()).andReturn(statement).once();
//    expect(statement.execute("delete from ServiceComponentInfo where ServiceComponentInfo.service_name = \"MyService\"")).andReturn(true).once();
//
//    replay(connectionFactory, connection, statement);
//
//    JDBCManagementController controller =  new JDBCManagementController(connectionFactory, ClusterControllerHelper.RESOURCE_TABLES);
//
//    Predicate predicate = new PredicateBuilder().property("service_name", "ServiceComponentInfo").equals("MyService").toPredicate();
//
//    controller.deleteComponents(predicate);
//
//    verify(connectionFactory, connection, statement);
//  }
//
//  @Test
//  public void testDeleteHostComponents() throws Exception{
//
//    ConnectionFactory connectionFactory = createNiceMock(ConnectionFactory.class);
//    Connection connection = createNiceMock(Connection.class);
//    Statement statement = createNiceMock(Statement.class);
//
//    expect(connectionFactory.getConnection()).andReturn(connection).once();
//    expect(connection.createStatement()).andReturn(statement).once();
//    expect(statement.execute("delete from HostRoles where HostRoles.component_name = \"MyComponent\"")).andReturn(true).once();
//
//    replay(connectionFactory, connection, statement);
//
//    JDBCManagementController controller =  new JDBCManagementController(connectionFactory, ClusterControllerHelper.RESOURCE_TABLES);
//
//    Predicate predicate = new PredicateBuilder().property("component_name", "HostRoles").equals("MyComponent").toPredicate();
//
//    controller.deleteHostComponents(predicate);
//
//    verify(connectionFactory, connection, statement);
//  }
//
//  @Test
//  public void testUpdateClusters() throws Exception{
//
//    ConnectionFactory connectionFactory = createNiceMock(ConnectionFactory.class);
//    Connection connection = createNiceMock(Connection.class);
//    Statement statement = createNiceMock(Statement.class);
//
//    expect(connectionFactory.getConnection()).andReturn(connection).once();
//    expect(connection.createStatement()).andReturn(statement).once();
//    expect(statement.execute("update Clusters set state = 'running' where Clusters.cluster_name = \"MyCluster\"")).andReturn(true).once();
//
//    replay(connectionFactory, connection, statement);
//
//    JDBCManagementController controller =  new JDBCManagementController(connectionFactory, ClusterControllerHelper.RESOURCE_TABLES);
//
//    Map<PropertyId, Object> properties = new LinkedHashMap<PropertyId, Object>();
//
//    PropertyId id = PropertyHelper.getPropertyId("state", "Clusters");
//    properties.put(id, "running");
//
//    Predicate predicate = new PredicateBuilder().property("cluster_name", "Clusters").equals("MyCluster").toPredicate();
//
//    Set<Map<PropertyId, Object>> propertySet = new LinkedHashSet<Map<PropertyId, Object>>();
//    propertySet.add(properties);
//
//    Request request = new RequestImpl(null, propertySet);
//
//    controller.updateClusters(request, predicate);
//
//    verify(connectionFactory, connection, statement);
//  }
//
//  @Test
//  public void testUpdateServices() throws Exception{
//
//    ConnectionFactory connectionFactory = createNiceMock(ConnectionFactory.class);
//    Connection connection = createNiceMock(Connection.class);
//    DatabaseMetaData databaseMetaData = createNiceMock(DatabaseMetaData.class);
//    ResultSet metaDataResultSet = createNiceMock(ResultSet.class);
//    Statement statement = createNiceMock(Statement.class);
//    ResultSet resultSet = createNiceMock(ResultSet.class);
//    ResultSetMetaData resultSetMetaData = createNiceMock(ResultSetMetaData.class);
//
//    expect(connectionFactory.getConnection()).andReturn(connection).once();
//    expect(connection.getMetaData()).andReturn(databaseMetaData).atLeastOnce();
//    expect(databaseMetaData.getImportedKeys((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject())).andReturn(metaDataResultSet).atLeastOnce();
//    expect(metaDataResultSet.next()).andReturn(true).once();
//    expect(metaDataResultSet.getString("PKCOLUMN_NAME")).andReturn("service_name").once();
//    expect(metaDataResultSet.getString("PKTABLE_NAME")).andReturn("ServiceInfo").once();
//    expect(metaDataResultSet.getString("FKCOLUMN_NAME")).andReturn("service_name").once();
//    expect(metaDataResultSet.getString("FKTABLE_NAME")).andReturn("Services").once();
//    expect(metaDataResultSet.next()).andReturn(false).once();
//    expect(metaDataResultSet.next()).andReturn(false).once();
//    expect(databaseMetaData.getPrimaryKeys((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject())).andReturn(metaDataResultSet).atLeastOnce();
//    expect(metaDataResultSet.next()).andReturn(true).once();
//    expect(metaDataResultSet.getString("COLUMN_NAME")).andReturn("service_name").once();
//    expect(metaDataResultSet.getString("TABLE_NAME")).andReturn("ServiceInfo").once();
//    expect(metaDataResultSet.next()).andReturn(true).once();
//    expect(metaDataResultSet.getString("COLUMN_NAME")).andReturn("cluster_name").once();
//    expect(metaDataResultSet.getString("TABLE_NAME")).andReturn("ServiceInfo").once();
//    expect(metaDataResultSet.next()).andReturn(false).once();
//    expect(connection.createStatement()).andReturn(statement).atLeastOnce();
//    expect(statement.executeQuery("select ServiceInfo.service_name, ServiceInfo.cluster_name from Services, ServiceInfo where (ServiceInfo.service_name = \"MyService\" AND Services.display_name = \"my service\") AND ServiceInfo.service_name = Services.service_name")).andReturn(resultSet).once();
//    expect(resultSet.getMetaData()).andReturn(resultSetMetaData).once();
//    expect(resultSetMetaData.getColumnCount()).andReturn(2).once();
//    expect(resultSet.next()).andReturn(true).once();
//    expect(resultSetMetaData.getColumnName(1)).andReturn("service_name").once();
//    expect(resultSetMetaData.getTableName(1)).andReturn("ServiceInfo").once();
//    expect(resultSet.getString(1)).andReturn("MyService").once();
//    expect(resultSetMetaData.getColumnName(2)).andReturn("cluster_name").once();
//    expect(resultSetMetaData.getTableName(2)).andReturn("ServiceInfo").once();
//    expect(resultSet.getString(2)).andReturn("MyCluster").once();
//    expect(resultSet.next()).andReturn(false).once();
//    expect(statement.execute("update ServiceInfo set state = 'running' where (ServiceInfo.cluster_name = \"MyCluster\" AND ServiceInfo.service_name = \"MyService\")")).andReturn(true).once();
//
//    replay(connectionFactory, connection, databaseMetaData, metaDataResultSet, statement, resultSet, resultSetMetaData);
//
//    JDBCManagementController controller =  new JDBCManagementController(connectionFactory, ClusterControllerHelper.RESOURCE_TABLES);
//
//    Map<PropertyId, Object> properties = new LinkedHashMap<PropertyId, Object>();
//
//    PropertyId id = PropertyHelper.getPropertyId("state", "ServiceInfo");
//    properties.put(id, "running");
//
//    Predicate predicate = new PredicateBuilder().property("service_name", "ServiceInfo").equals("MyService").and().
//                                                 property("display_name", "Services").equals("my service").toPredicate();
//
//    Set<Map<PropertyId, Object>> propertySet = new LinkedHashSet<Map<PropertyId, Object>>();
//    propertySet.add(properties);
//
//    Request request = new RequestImpl(null, propertySet);
//
//    controller.updateServices(request, predicate);
//
//    verify(connectionFactory, connection, databaseMetaData, metaDataResultSet, statement, resultSet, resultSetMetaData);
//  }
//
//  @Test
//  public void testUpdateHosts() throws Exception{
//
//    ConnectionFactory connectionFactory = createNiceMock(ConnectionFactory.class);
//    Connection connection = createNiceMock(Connection.class);
//    Statement statement = createNiceMock(Statement.class);
//
//    expect(connectionFactory.getConnection()).andReturn(connection).once();
//    expect(connection.createStatement()).andReturn(statement).once();
//    expect(statement.execute("update Hosts set cpu_count = 4 where (Hosts.host_name = \"MyHost1\" OR Hosts.host_name = \"MyHost2\")")).andReturn(true).once();
//
//    replay(connectionFactory, connection, statement);
//
//    JDBCManagementController controller =  new JDBCManagementController(connectionFactory, ClusterControllerHelper.RESOURCE_TABLES);
//
//    Map<PropertyId, Object> properties = new LinkedHashMap<PropertyId, Object>();
//
//    PropertyId id = PropertyHelper.getPropertyId("cpu_count", "Hosts");
//    properties.put(id, 4);
//
//    Set<Map<PropertyId, Object>> propertySet = new LinkedHashSet<Map<PropertyId, Object>>();
//    propertySet.add(properties);
//
//    Predicate predicate = new PredicateBuilder().property("host_name", "Hosts").equals("MyHost1").or().
//                                                 property("host_name", "Hosts").equals("MyHost2").toPredicate();
//
//    Request request = new RequestImpl(null, propertySet);
//
//    controller.updateHosts(request, predicate);
//
//    verify(connectionFactory, connection, statement);
//  }
//
//  @Test
//  public void testUpdateComponents() throws Exception{
//
//    ConnectionFactory connectionFactory = createNiceMock(ConnectionFactory.class);
//    Connection connection = createNiceMock(Connection.class);
//    Statement statement = createNiceMock(Statement.class);
//
//    expect(connectionFactory.getConnection()).andReturn(connection).once();
//    expect(connection.createStatement()).andReturn(statement).once();
//    expect(statement.execute("update ServiceComponentInfo set description = 'new description' where ServiceComponentInfo.service_name = \"MyService\"")).andReturn(true).once();
//
//    replay(connectionFactory, connection, statement);
//
//    JDBCManagementController controller =  new JDBCManagementController(connectionFactory, ClusterControllerHelper.RESOURCE_TABLES);
//
//    Map<PropertyId, Object> properties = new LinkedHashMap<PropertyId, Object>();
//
//    PropertyId id = PropertyHelper.getPropertyId("description", "ServiceComponentInfo");
//    properties.put(id, "new description");
//
//    Predicate predicate = new PredicateBuilder().property("service_name", "ServiceComponentInfo").equals("MyService").toPredicate();
//
//    Set<Map<PropertyId, Object>> propertySet = new LinkedHashSet<Map<PropertyId, Object>>();
//    propertySet.add(properties);
//
//    Request request = new RequestImpl(null, propertySet);
//
//    controller.updateComponents(request, predicate);
//
//    verify(connectionFactory, connection, statement);
//  }
//
//  @Test
//  public void testUpdateHostComponents() throws Exception{
//
//    ConnectionFactory connectionFactory = createNiceMock(ConnectionFactory.class);
//    Connection connection = createNiceMock(Connection.class);
//    Statement statement = createNiceMock(Statement.class);
//
//    expect(connectionFactory.getConnection()).andReturn(connection).once();
//    expect(connection.createStatement()).andReturn(statement).once();
//    expect(statement.execute("update HostRoles set state = 'running' where HostRoles.component_name = \"MyComponent\"")).andReturn(true).once();
//
//    replay(connectionFactory, connection, statement);
//
//    JDBCManagementController controller =  new JDBCManagementController(connectionFactory, ClusterControllerHelper.RESOURCE_TABLES);
//
//    Map<PropertyId, Object> properties = new LinkedHashMap<PropertyId, Object>();
//
//    PropertyId id = PropertyHelper.getPropertyId("state", "HostRoles");
//    properties.put(id, "running");
//
//    Predicate predicate = new PredicateBuilder().property("component_name", "HostRoles").equals("MyComponent").toPredicate();
//
//    Set<Map<PropertyId, Object>> propertySet = new LinkedHashSet<Map<PropertyId, Object>>();
//    propertySet.add(properties);
//
//    Request request = new RequestImpl(null, propertySet);
//
//    controller.updateHostComponents(request, predicate);
//
//    verify(connectionFactory, connection, statement);
//  }
//
//  @Test
//  public void testGetClusters() throws Exception{
//
//    ConnectionFactory connectionFactory = createNiceMock(ConnectionFactory.class);
//    Connection connection = createNiceMock(Connection.class);
//    DatabaseMetaData databaseMetaData = createNiceMock(DatabaseMetaData.class);
//    ResultSet metaDataResultSet = createNiceMock(ResultSet.class);
//    Statement statement = createNiceMock(Statement.class);
//    ResultSet resultSet = createNiceMock(ResultSet.class);
//    ResultSetMetaData resultSetMetaData = createNiceMock(ResultSetMetaData.class);
//
//    expect(connectionFactory.getConnection()).andReturn(connection).once();
//    expect(connection.getMetaData()).andReturn(databaseMetaData).once();
//    expect(databaseMetaData.getImportedKeys((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject())).andReturn(metaDataResultSet).atLeastOnce();
//    expect(metaDataResultSet.next()).andReturn(false).atLeastOnce();
//    expect(connection.createStatement()).andReturn(statement).once();
//    expect(statement.executeQuery("select Clusters.state, Clusters.cluster_name from Clusters where Clusters.cluster_name = \"MyCluster\"")).andReturn(resultSet).once();
//    expect(resultSet.getMetaData()).andReturn(resultSetMetaData).once();
//    expect(resultSetMetaData.getColumnCount()).andReturn(2).once();
//    expect(resultSet.next()).andReturn(true).once();
//    expect(resultSetMetaData.getColumnName(1)).andReturn("state").once();
//    expect(resultSetMetaData.getTableName(1)).andReturn("Clusters").once();
//    expect(resultSet.getString(1)).andReturn("running").once();
//    expect(resultSetMetaData.getColumnName(2)).andReturn("cluster_name").once();
//    expect(resultSetMetaData.getTableName(2)).andReturn("Clusters").once();
//    expect(resultSet.getString(2)).andReturn("MyCluster").once();
//    expect(resultSet.next()).andReturn(false).once();
//
//    replay(connectionFactory, connection, databaseMetaData, metaDataResultSet, statement, resultSet, resultSetMetaData);
//
//    JDBCManagementController controller =  new JDBCManagementController(connectionFactory, ClusterControllerHelper.RESOURCE_TABLES);
//
//    Predicate predicate = new PredicateBuilder().property("cluster_name", "Clusters").equals("MyCluster").toPredicate();
//
//    Set<PropertyId> propertyIds = new LinkedHashSet<PropertyId>();
//    propertyIds.add(PropertyHelper.getPropertyId("state", "Clusters"));
//
//    Request request = new RequestImpl(propertyIds, null);
//
//    Set<Resource> resources = controller.getClusters(request, predicate);
//
//    Assert.assertEquals(1, resources.size());
//
//    Resource resource = resources.iterator().next();
//
//    Assert.assertEquals(Resource.Type.Cluster, resource.getType());
//
//    Assert.assertEquals("running", resource.getPropertyValue(PropertyHelper.getPropertyId("state", "Clusters")));
//
//    verify(connectionFactory, connection, databaseMetaData, metaDataResultSet, statement, resultSet, resultSetMetaData);
//  }
//
//  @Test
//  public void testGetServices() throws Exception{
//
//    ConnectionFactory connectionFactory = createNiceMock(ConnectionFactory.class);
//    Connection connection = createNiceMock(Connection.class);
//    DatabaseMetaData databaseMetaData = createNiceMock(DatabaseMetaData.class);
//    ResultSet metaDataResultSet = createNiceMock(ResultSet.class);
//    Statement statement = createNiceMock(Statement.class);
//    ResultSet resultSet = createNiceMock(ResultSet.class);
//    ResultSetMetaData resultSetMetaData = createNiceMock(ResultSetMetaData.class);
//
//    expect(connectionFactory.getConnection()).andReturn(connection).once();
//    expect(connection.getMetaData()).andReturn(databaseMetaData).atLeastOnce();
//    expect(databaseMetaData.getImportedKeys((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject())).andReturn(metaDataResultSet).atLeastOnce();
//    expect(metaDataResultSet.next()).andReturn(true).once();
//    expect(metaDataResultSet.getString("PKCOLUMN_NAME")).andReturn("service_name").once();
//    expect(metaDataResultSet.getString("PKTABLE_NAME")).andReturn("ServiceInfo").once();
//    expect(metaDataResultSet.getString("FKCOLUMN_NAME")).andReturn("service_name").once();
//    expect(metaDataResultSet.getString("FKTABLE_NAME")).andReturn("Services").once();
//    expect(metaDataResultSet.next()).andReturn(false).once();
//    expect(connection.createStatement()).andReturn(statement).once();
//    expect(statement.executeQuery("select ServiceInfo.service_name, Services.description from Services, ServiceInfo where ServiceInfo.service_name = \"MyService\" AND ServiceInfo.service_name = Services.service_name")).andReturn(resultSet).once();
//    expect(resultSet.getMetaData()).andReturn(resultSetMetaData).once();
//    expect(resultSetMetaData.getColumnCount()).andReturn(2).once();
//    expect(resultSet.next()).andReturn(true).once();
//    expect(resultSetMetaData.getColumnName(1)).andReturn("service_name").once();
//    expect(resultSetMetaData.getTableName(1)).andReturn("ServiceInfo").once();
//    expect(resultSet.getString(1)).andReturn("MyService").once();
//    expect(resultSetMetaData.getColumnName(2)).andReturn("description").once();
//    expect(resultSetMetaData.getTableName(2)).andReturn("Services").once();
//    expect(resultSet.getString(2)).andReturn("some description").once();
//    expect(resultSet.next()).andReturn(false).once();
//
//    replay(connectionFactory, connection, databaseMetaData, metaDataResultSet, statement, resultSet, resultSetMetaData);
//
//    JDBCManagementController controller =  new JDBCManagementController(connectionFactory, ClusterControllerHelper.RESOURCE_TABLES);
//
//    Predicate predicate = new PredicateBuilder().property("service_name", "ServiceInfo").equals("MyService").toPredicate();
//
//    Set<PropertyId> propertyIds = new LinkedHashSet<PropertyId>();
//    propertyIds.add(PropertyHelper.getPropertyId("service_name", "ServiceInfo"));
//    propertyIds.add(PropertyHelper.getPropertyId("description", "Services"));
//
//    Request request = new RequestImpl(propertyIds, null);
//
//    Set<Resource> resources = controller.getServices(request, predicate);
//
//    Assert.assertEquals(1, resources.size());
//
//    Resource resource = resources.iterator().next();
//
//    Assert.assertEquals(Resource.Type.Service, resource.getType());
//
//    Assert.assertEquals("MyService", resource.getPropertyValue(PropertyHelper.getPropertyId("service_name", "ServiceInfo")));
//    Assert.assertEquals("some description", resource.getPropertyValue(PropertyHelper.getPropertyId("description", "Services")));
//
//    verify(connectionFactory, connection, databaseMetaData, metaDataResultSet, statement, resultSet, resultSetMetaData);
//  }
//
//  @Test
//  public void testGetComponents() throws Exception{
//
//    ConnectionFactory connectionFactory = createNiceMock(ConnectionFactory.class);
//    Connection connection = createNiceMock(Connection.class);
//    DatabaseMetaData databaseMetaData = createNiceMock(DatabaseMetaData.class);
//    ResultSet metaDataResultSet = createNiceMock(ResultSet.class);
//    Statement statement = createNiceMock(Statement.class);
//    ResultSet resultSet = createNiceMock(ResultSet.class);
//    ResultSetMetaData resultSetMetaData = createNiceMock(ResultSetMetaData.class);
//
//    expect(connectionFactory.getConnection()).andReturn(connection).once();
//    expect(connection.getMetaData()).andReturn(databaseMetaData).atLeastOnce();
//    expect(databaseMetaData.getImportedKeys((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject())).andReturn(metaDataResultSet).atLeastOnce();
//    expect(metaDataResultSet.next()).andReturn(true).once();
//    expect(metaDataResultSet.getString("PKCOLUMN_NAME")).andReturn("component_name").once();
//    expect(metaDataResultSet.getString("PKTABLE_NAME")).andReturn("ServiceComponentInfo").once();
//    expect(metaDataResultSet.getString("FKCOLUMN_NAME")).andReturn("component_name").once();
//    expect(metaDataResultSet.getString("FKTABLE_NAME")).andReturn("ServiceComponents").once();
//    expect(metaDataResultSet.next()).andReturn(false).once();
//    expect(connection.createStatement()).andReturn(statement).once();
//    expect(statement.executeQuery("select ServiceComponentInfo.component_name, ServiceComponents.description from ServiceComponentInfo, ServiceComponents where ServiceComponentInfo.component_name = \"MyService\" AND ServiceComponentInfo.component_name = ServiceComponents.component_name")).andReturn(resultSet).once();
//    expect(resultSet.getMetaData()).andReturn(resultSetMetaData).once();
//    expect(resultSetMetaData.getColumnCount()).andReturn(2).once();
//    expect(resultSet.next()).andReturn(true).once();
//    expect(resultSetMetaData.getColumnName(1)).andReturn("component_name").once();
//    expect(resultSetMetaData.getTableName(1)).andReturn("ServiceComponentInfo").once();
//    expect(resultSet.getString(1)).andReturn("MyService").once();
//    expect(resultSetMetaData.getColumnName(2)).andReturn("description").once();
//    expect(resultSetMetaData.getTableName(2)).andReturn("ServiceComponents").once();
//    expect(resultSet.getString(2)).andReturn("some description").once();
//    expect(resultSet.next()).andReturn(false).once();
//
//    replay(connectionFactory, connection, databaseMetaData, metaDataResultSet, statement, resultSet, resultSetMetaData);
//
//    JDBCManagementController controller =  new JDBCManagementController(connectionFactory, ClusterControllerHelper.RESOURCE_TABLES);
//
//    Predicate predicate = new PredicateBuilder().property("component_name", "ServiceComponentInfo").equals("MyService").toPredicate();
//
//    Set<PropertyId> propertyIds = new LinkedHashSet<PropertyId>();
//    propertyIds.add(PropertyHelper.getPropertyId("component_name", "ServiceComponentInfo"));
//    propertyIds.add(PropertyHelper.getPropertyId("description", "ServiceComponents"));
//
//    Request request = new RequestImpl(propertyIds, null);
//
//    Set<Resource> resources = controller.getComponents(request, predicate);
//
//    Assert.assertEquals(1, resources.size());
//
//    Resource resource = resources.iterator().next();
//
//    Assert.assertEquals(Resource.Type.Component, resource.getType());
//
//    Assert.assertEquals("MyService", resource.getPropertyValue(PropertyHelper.getPropertyId("component_name", "ServiceComponentInfo")));
//    Assert.assertEquals("some description", resource.getPropertyValue(PropertyHelper.getPropertyId("description", "ServiceComponents")));
//
//    verify(connectionFactory, connection, databaseMetaData, metaDataResultSet, statement, resultSet, resultSetMetaData);
//  }
//
//  @Test
//  public void testGetHosts() throws Exception{
//
//    ConnectionFactory connectionFactory = createNiceMock(ConnectionFactory.class);
//    Connection connection = createNiceMock(Connection.class);
//    DatabaseMetaData databaseMetaData = createNiceMock(DatabaseMetaData.class);
//    ResultSet metaDataResultSet = createNiceMock(ResultSet.class);
//    Statement statement = createNiceMock(Statement.class);
//    ResultSet resultSet = createNiceMock(ResultSet.class);
//    ResultSetMetaData resultSetMetaData = createNiceMock(ResultSetMetaData.class);
//
//    expect(connectionFactory.getConnection()).andReturn(connection).once();
//    expect(connection.getMetaData()).andReturn(databaseMetaData).once();
//    expect(databaseMetaData.getImportedKeys((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject())).andReturn(metaDataResultSet).atLeastOnce();
//    expect(metaDataResultSet.next()).andReturn(false).atLeastOnce();
//    expect(connection.createStatement()).andReturn(statement).once();
//    expect(statement.executeQuery("select Hosts.cpu_count, Hosts.host_name from Hosts where Hosts.host_name = \"MyHost\"")).andReturn(resultSet).once();
//    expect(resultSet.getMetaData()).andReturn(resultSetMetaData).once();
//    expect(resultSetMetaData.getColumnCount()).andReturn(2).once();
//    expect(resultSet.next()).andReturn(true).once();
//    expect(resultSetMetaData.getColumnName(1)).andReturn("cpu_count").once();
//    expect(resultSetMetaData.getTableName(1)).andReturn("Hosts").once();
//    expect(resultSet.getString(1)).andReturn("4").once();
//    expect(resultSetMetaData.getColumnName(2)).andReturn("host_name").once();
//    expect(resultSetMetaData.getTableName(2)).andReturn("Hosts").once();
//    expect(resultSet.getString(2)).andReturn("MyHost").once();
//    expect(resultSet.next()).andReturn(false).once();
//
//    replay(connectionFactory, connection, databaseMetaData, metaDataResultSet, statement, resultSet, resultSetMetaData);
//
//    JDBCManagementController controller =  new JDBCManagementController(connectionFactory, ClusterControllerHelper.RESOURCE_TABLES);
//
//    Predicate predicate = new PredicateBuilder().property("host_name", "Hosts").equals("MyHost").toPredicate();
//
//    Set<PropertyId> propertyIds = new LinkedHashSet<PropertyId>();
//    propertyIds.add(PropertyHelper.getPropertyId("cpu_count", "Hosts"));
//
//    Request request = new RequestImpl(propertyIds, null);
//
//    Set<Resource> resources = controller.getHosts(request, predicate);
//
//    Assert.assertEquals(1, resources.size());
//
//    Resource resource = resources.iterator().next();
//
//    Assert.assertEquals(Resource.Type.Host, resource.getType());
//
//    Assert.assertEquals("4", resource.getPropertyValue(PropertyHelper.getPropertyId("cpu_count", "Hosts")));
//
//    verify(connectionFactory, connection, databaseMetaData, metaDataResultSet, statement, resultSet, resultSetMetaData);
//  }
//
//  @Test
//  public void testGetHostComponents() throws Exception{
//
//    ConnectionFactory connectionFactory = createNiceMock(ConnectionFactory.class);
//    Connection connection = createNiceMock(Connection.class);
//    DatabaseMetaData databaseMetaData = createNiceMock(DatabaseMetaData.class);
//    ResultSet metaDataResultSet = createNiceMock(ResultSet.class);
//    Statement statement = createNiceMock(Statement.class);
//    ResultSet resultSet = createNiceMock(ResultSet.class);
//    ResultSetMetaData resultSetMetaData = createNiceMock(ResultSetMetaData.class);
//
//    expect(connectionFactory.getConnection()).andReturn(connection).once();
//    expect(connection.getMetaData()).andReturn(databaseMetaData).once();
//    expect(databaseMetaData.getImportedKeys((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject())).andReturn(metaDataResultSet).atLeastOnce();
//    expect(metaDataResultSet.next()).andReturn(false).atLeastOnce();
//    expect(connection.createStatement()).andReturn(statement).once();
//    expect(statement.executeQuery("select HostRoles.host_name, HostRoles.state from HostRoles where HostRoles.host_name = \"MyHost\"")).andReturn(resultSet).once();
//    expect(resultSet.getMetaData()).andReturn(resultSetMetaData).once();
//    expect(resultSetMetaData.getColumnCount()).andReturn(2).once();
//    expect(resultSet.next()).andReturn(true).once();
//    expect(resultSetMetaData.getColumnName(1)).andReturn("state").once();
//    expect(resultSetMetaData.getTableName(1)).andReturn("HostRoles").once();
//    expect(resultSet.getString(1)).andReturn("running").once();
//    expect(resultSetMetaData.getColumnName(2)).andReturn("host_name").once();
//    expect(resultSetMetaData.getTableName(2)).andReturn("HostRoles").once();
//    expect(resultSet.getString(2)).andReturn("MyHost").once();
//    expect(resultSet.next()).andReturn(false).once();
//
//    replay(connectionFactory, connection, databaseMetaData, metaDataResultSet, statement, resultSet, resultSetMetaData);
//
//    JDBCManagementController controller =  new JDBCManagementController(connectionFactory, ClusterControllerHelper.RESOURCE_TABLES);
//
//    Predicate predicate = new PredicateBuilder().property("host_name", "HostRoles").equals("MyHost").toPredicate();
//
//    Set<PropertyId> propertyIds = new LinkedHashSet<PropertyId>();
//    propertyIds.add(PropertyHelper.getPropertyId("state", "HostRoles"));
//
//    Request request = new RequestImpl(propertyIds, null);
//
//    Set<Resource> resources = controller.getHostComponents(request, predicate);
//
//    Assert.assertEquals(1, resources.size());
//
//    Resource resource = resources.iterator().next();
//
//    Assert.assertEquals(Resource.Type.HostComponent, resource.getType());
//
//    Assert.assertEquals("running", resource.getPropertyValue(PropertyHelper.getPropertyId("state", "HostRoles")));
//
//    verify(connectionFactory, connection, databaseMetaData, metaDataResultSet, statement, resultSet, resultSetMetaData);
//  }
}

