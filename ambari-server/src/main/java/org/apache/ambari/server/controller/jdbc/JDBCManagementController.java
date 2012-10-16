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

import org.apache.ambari.server.controller.internal.PropertyIdImpl;
import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ClusterRequest;
import org.apache.ambari.server.controller.ClusterResponse;
import org.apache.ambari.server.controller.HostRequest;
import org.apache.ambari.server.controller.HostResponse;
import org.apache.ambari.server.controller.ServiceComponentHostRequest;
import org.apache.ambari.server.controller.ServiceComponentHostResponse;
import org.apache.ambari.server.controller.ServiceComponentRequest;
import org.apache.ambari.server.controller.ServiceComponentResponse;
import org.apache.ambari.server.controller.ServiceRequest;
import org.apache.ambari.server.controller.ServiceResponse;
import org.apache.ambari.server.controller.TrackActionResponse;
import org.apache.ambari.server.controller.predicate.AndPredicate;
import org.apache.ambari.server.controller.predicate.BasePredicate;
import org.apache.ambari.server.controller.predicate.EqualsPredicate;
import org.apache.ambari.server.controller.predicate.PredicateVisitorAcceptor;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.PropertyId;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PredicateHelper;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Generic JDBC implementation of a management controller.
 */
public class JDBCManagementController implements AmbariManagementController {
  /**
   * The connection factory.
   */
  private final ConnectionFactory connectionFactory;

  /**
   * Mapping of resource type to the name of the primary table for the resource.
   */
  private final Map<Resource.Type, String> resourceTables;

  /**
   * Primary key mappings.
   */
  private final Map<String, Set<PropertyId>> primaryKeys = new HashMap<String, Set<PropertyId>>();

  /**
   * Key mappings used for joins.
   */
  private final Map<String, Map<PropertyId, PropertyId>> importedKeys = new HashMap<String, Map<PropertyId, PropertyId>>();


  // ----- Constructors ------------------------------------------------------

  /**
   * Construct a new JDBC management controller with the given JDBC connection.
   *
   * @param connectionFactory  the connection factory
   */
  public JDBCManagementController(ConnectionFactory connectionFactory, Map<Resource.Type, String> resourceTables) {
    this.connectionFactory = connectionFactory;
    this.resourceTables = resourceTables;
  }

  // ----- AmbariManagementController ----------------------------------------

  @Override
  public TrackActionResponse createCluster(ClusterRequest request) throws AmbariException {
//    createResources(Resource.Type.Cluster, request);
    return null;
  }

  @Override
  public TrackActionResponse createService(ServiceRequest request) throws AmbariException {
    return null;
  }

  @Override
  public TrackActionResponse createComponent(ServiceComponentRequest request) throws AmbariException {
    return null;
  }

  @Override
  public TrackActionResponse createHost(HostRequest request) throws AmbariException {
    return null;
  }

  @Override
  public TrackActionResponse createHostComponent(ServiceComponentHostRequest request) throws AmbariException {
    return null;
  }

  @Override
  public Set<ClusterResponse> getClusters(ClusterRequest request) throws AmbariException {
//    return getResources(Resource.Type.Cluster, request, predicate);
    return null;
  }

  @Override
  public Set<ServiceResponse> getServices(ServiceRequest request) throws AmbariException {
    return null;
  }

  @Override
  public Set<ServiceComponentResponse> getComponents(ServiceComponentRequest request) throws AmbariException {
    return null;
  }

  @Override
  public Set<HostResponse> getHosts(HostRequest request) throws AmbariException {
    return null;
  }

  @Override
  public Set<ServiceComponentHostResponse> getHostComponents(ServiceComponentHostRequest request) throws AmbariException {
    return null;
  }

  @Override
  public TrackActionResponse updateCluster(ClusterRequest request) throws AmbariException {
//    updateResources(Resource.Type.Cluster, request, predicate);
    return null;
  }

  @Override
  public TrackActionResponse updateService(ServiceRequest request) throws AmbariException {
    return null;
  }

  @Override
  public TrackActionResponse updateComponent(ServiceComponentRequest request) throws AmbariException {
    return null;
  }

  @Override
  public TrackActionResponse updateHost(HostRequest request) throws AmbariException {
    return null;
  }

  @Override
  public TrackActionResponse updateHostComponent(ServiceComponentHostRequest request) throws AmbariException {
    return null;
  }

  @Override
  public TrackActionResponse deleteCluster(ClusterRequest request) throws AmbariException {
//    deleteResources(Resource.Type.Cluster, predicate);
    return null;
  }

  @Override
  public TrackActionResponse deleteService(ServiceRequest request) throws AmbariException {
    return null;
  }

  @Override
  public TrackActionResponse deleteComponent(ServiceComponentRequest request) throws AmbariException {
    return null;
  }

  @Override
  public TrackActionResponse deleteHost(HostRequest request) throws AmbariException {
    return null;
  }

  @Override
  public TrackActionResponse deleteHostComponent(ServiceComponentHostRequest request) throws AmbariException {
    return null;
  }


  // ----- Helper methods ----------------------------------------------------

  /**
   * Create the resources defined by the properties in the given request object.
   *
   * @param type     the resource type
   * @param request  the request object which defines the set of properties
   *                 for the resource to be created
   */
  private void createResources(Resource.Type type, Request request) {
    try {
      Connection connection = connectionFactory.getConnection();

      try {

        Set<Map<PropertyId, Object>> propertySet = request.getProperties();

        for (Map<PropertyId, Object> properties : propertySet) {
          String sql = getInsertSQL(resourceTables.get(type), properties);

          Statement statement = connection.createStatement();

          statement.execute(sql);
        }
      } finally {
        connection.close();
      }

    } catch (SQLException e) {
      throw new IllegalStateException("DB error : ", e);
    }
  }

  /**
   * Get a set of {@link Resource resources} based on the given request and predicate
   * information.
   *
   * @param type       the resource type
   * @param request    the request object which defines the desired set of properties
   * @param predicate  the predicate object which can be used to filter which
   *                   resources are returned
   * @return a set of resources based on the given request and predicate information
   */
  private Set<Resource> getResources(Resource.Type type, Request request, Predicate predicate) {

    Set<Resource> resources = new HashSet<Resource>();
    Set<PropertyId> propertyIds = new HashSet<PropertyId>(request.getPropertyIds());
    if (predicate != null) {
      propertyIds.addAll(PredicateHelper.getPropertyIds(predicate));
    }

    try {
      Connection connection = connectionFactory.getConnection();

      try {

        for (String table : getTables(propertyIds)) {
          getImportedKeys(connection, table);
        }
        String sql = getSelectSQL(propertyIds, predicate);
        Statement statement = connection.createStatement();

        ResultSet rs = statement.executeQuery(sql);
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        while (rs.next()) {
          final ResourceImpl resource = new ResourceImpl(type);
          for (int i = 1; i <= columnCount; ++i) {
            PropertyIdImpl propertyId = new PropertyIdImpl(metaData.getColumnName(i), metaData.getTableName(i), false);
            if (propertyIds.contains(propertyId)) {
              resource.setProperty(propertyId, rs.getString(i));
            }
          }
          resources.add(resource);
        }

      } finally {
        connection.close();
      }

    } catch (SQLException e) {
      throw new IllegalStateException("DB error : ", e);
    }

    return resources;
  }

  /**
   * Update the host resources selected by the given predicate with the properties
   * from the given request object.
   *
   * @param type       the resource type
   * @param request    the request object which defines the set of properties
   *                   for the resources to be updated
   * @param predicate  the predicate object which can be used to filter which
   *                   host resources are updated
   */
  private void updateResources(Resource.Type type, Request request, Predicate predicate) {
    try {
      Connection connection = connectionFactory.getConnection();
      try {
        Set<Map<PropertyId, Object>> propertySet = request.getProperties();

        Map<PropertyId, Object> properties = propertySet.iterator().next();

        String resourceTable = resourceTables.get(type);

        predicate = getPredicate(connection, resourceTable, predicate);

        if (predicate == null) {
          return;
        }

        String sql = getUpdateSQL(resourceTable, properties, predicate);

        Statement statement = connection.createStatement();

        statement.execute(sql);
      } finally {
        connection.close();
      }

    } catch (SQLException e) {
      throw new IllegalStateException("DB error : ", e);
    }
  }

  /**
   * Delete the resources selected by the given predicate.
   *
   * @param type      the resource type
   * @param predicate the predicate object which can be used to filter which
   *                  resources are deleted
   */
  private void deleteResources(Resource.Type type, Predicate predicate) {
    try {
      Connection connection = connectionFactory.getConnection();
      try {
        String resourceTable = resourceTables.get(type);

        predicate = getPredicate(connection, resourceTable, predicate);

        if (predicate == null) {
          return;
        }

        String sql = getDeleteSQL(resourceTable, predicate);

        Statement statement = connection.createStatement();
        statement.execute(sql);
      } finally {
        connection.close();
      }

    } catch (SQLException e) {
      throw new IllegalStateException("DB error : ", e);
    }
  }

  /**
   * Lazily populate the imported key mappings for the given table.
   *
   * @param connection  the connection to use to obtain the database meta data
   * @param table       the table
   *
   * @throws SQLException thrown if the meta data for the given connection cannot be obtained
   */
  private void getImportedKeys(Connection connection, String table) throws SQLException {
    if (!this.importedKeys.containsKey(table)) {

      Map<PropertyId, PropertyId> importedKeys = new HashMap<PropertyId, PropertyId>();
      this.importedKeys.put(table, importedKeys);

      DatabaseMetaData metaData = connection.getMetaData();

      ResultSet rs = metaData.getImportedKeys(connection.getCatalog(), null, table);

      while (rs.next()) {

        PropertyId pkPropertyId = PropertyHelper.getPropertyId(
            rs.getString("PKCOLUMN_NAME"), rs.getString("PKTABLE_NAME"));

        PropertyId fkPropertyId = PropertyHelper.getPropertyId(
            rs.getString("FKCOLUMN_NAME"), rs.getString("FKTABLE_NAME"));

        importedKeys.put(pkPropertyId, fkPropertyId);
      }
    }
  }

  /**
   * Lazily populate the primary key mappings for the given table.
   *
   * @param connection  the connection to use to obtain the database meta data
   * @param table       the table
   *
   * @throws SQLException thrown if the meta data for the given connection cannot be obtained
   */
  private void getPrimaryKeys(Connection connection, String table) throws SQLException {

    if (!this.primaryKeys.containsKey(table)) {

      Set<PropertyId> primaryKeys = new HashSet<PropertyId>();
      this.primaryKeys.put(table, primaryKeys);

      DatabaseMetaData metaData = connection.getMetaData();

      ResultSet rs = metaData.getPrimaryKeys(connection.getCatalog(), null, table);

      while (rs.next()) {

        PropertyId pkPropertyId = PropertyHelper.getPropertyId(
            rs.getString("COLUMN_NAME"), rs.getString("TABLE_NAME"));

        primaryKeys.add(pkPropertyId);
      }
    }
  }

  /**
   * Create a new predicate if the given predicate doesn't work for the given table.  Use the
   * given predicate and join to the given table to get the primary key values to create a new
   * predicate. (Could probably do this with INNER JOIN???)
   *
   * @param connection  the JDBC connection
   * @param table       the resource table
   * @param predicate   the predicate
   *
   * @return the new predicate
   *
   * @throws SQLException thrown if an exception occurred operating on the given connection
   */
  private Predicate getPredicate(Connection connection, String table, Predicate predicate) throws SQLException {

    Set<String> predicateTables = getTables(PredicateHelper.getPropertyIds(predicate));

    if (predicateTables.size() > 1 || !predicateTables.contains(table)) {
      for (String predicateTable : predicateTables){
        getImportedKeys(connection, predicateTable);
      }

      getPrimaryKeys(connection, table);
      getImportedKeys(connection, table);

      Set<PropertyId>   pkPropertyIds = primaryKeys.get(table);
      String            sql           = getSelectSQL(pkPropertyIds, predicate);
      Statement         statement     = connection.createStatement();
      ResultSet         rs            = statement.executeQuery(sql);
      ResultSetMetaData metaData      = rs.getMetaData();
      int               columnCount   = metaData.getColumnCount();

      Set<BasePredicate> predicates = new HashSet<BasePredicate>();
      while (rs.next()) {
        for (int i = 1; i <= columnCount; ++i) {
          PropertyIdImpl propertyId = new PropertyIdImpl(metaData.getColumnName(i), metaData.getTableName(i), false);
          if (pkPropertyIds.contains(propertyId)) {
            predicates.add(new EqualsPredicate(propertyId, rs.getString(i)));
          }
        }
      }

      predicate = predicates.size() == 0 ? null : predicates.size() > 1 ?
          new AndPredicate(predicates.toArray(new BasePredicate[2])) :
          predicates.iterator().next();
    }
    return predicate;
  }

  /**
   * Get an insert SQL statement based on the given properties.
   *
   * @param table      the table
   * @param properties  the properties
   *
   * @return the insert SQL
   */
  private String getInsertSQL(String table, Map<PropertyId, Object> properties) {

    StringBuilder columns = new StringBuilder();
    StringBuilder values = new StringBuilder();

    for (Map.Entry<PropertyId, Object> entry : properties.entrySet()) {
      PropertyId propertyId = entry.getKey();
      Object propertyValue = entry.getValue();

      if (columns.length() > 0) {
        columns.append(", ");
      }
      columns.append(propertyId.getName());

      if (values.length() > 0) {
        values.append(", ");
      }

      if (propertyValue instanceof String) {
        values.append("'");
        values.append(propertyValue);
        values.append("'");
      } else {
        values.append(propertyValue);
      }
    }

    return "insert into " + table + " (" +
        columns + ") values (" + values + ")";
  }

  /**
   * Get a select SQL statement based on the given property ids and predicate.
   *
   * @param propertyIds  the property ids
   * @param predicate    the predicate
   *
   * @return the select SQL
   */
  private String getSelectSQL(Set<PropertyId> propertyIds, Predicate predicate) {

    StringBuilder columns = new StringBuilder();
    Set<String> tableSet = new HashSet<String>();

    for (PropertyId propertyId : propertyIds) {
      if (columns.length() > 0) {
        columns.append(", ");
      }
      columns.append(propertyId.getCategory()).append(".").append(propertyId.getName());
      tableSet.add(propertyId.getCategory());
    }

    boolean haveWhereClause = false;
    StringBuilder whereClause = new StringBuilder();
    if (predicate != null && predicate instanceof PredicateVisitorAcceptor) {

      SQLPredicateVisitor visitor = new SQLPredicateVisitor();
      PredicateHelper.visit(predicate, visitor);
      whereClause.append(visitor.getSQL());

      for (PropertyId propertyId : PredicateHelper.getPropertyIds(predicate)) {
        tableSet.add(propertyId.getCategory());
      }
      haveWhereClause = true;
    }

    StringBuilder joinClause = new StringBuilder();

    if (tableSet.size() > 1) {

      for (String table : tableSet) {
        Map<PropertyId, PropertyId> joinKeys = importedKeys.get(table);
        if (joinKeys != null) {
          for (Map.Entry<PropertyId, PropertyId> entry : joinKeys.entrySet()) {
            String category1 = entry.getKey().getCategory();
            String category2 = entry.getValue().getCategory();
            if (tableSet.contains(category1) && tableSet.contains(category2)) {
              if (haveWhereClause) {
                joinClause.append(" AND ");
              }
              joinClause.append(category1).append(".").append(entry.getKey().getName());
              joinClause.append(" = ");
              joinClause.append(category2).append(".").append(entry.getValue().getName());
              tableSet.add(category1);
              tableSet.add(category2);

              haveWhereClause = true;
            }
          }
        }
      }
    }

    StringBuilder tables = new StringBuilder();

    for (String table : tableSet) {
      if (tables.length() > 0) {
        tables.append(", ");
      }
      tables.append(table);
    }

    String sql = "select " + columns + " from " + tables;

    if (haveWhereClause) {
      sql = sql + " where " + whereClause + joinClause;
    }
    return sql;
  }

  /**
   * Get a delete SQL statement based on the given predicate.
   *
   * @param table      the table
   * @param predicate  the predicate
   *
   * @return the delete SQL statement
   */
  private String getDeleteSQL(String table, Predicate predicate) {

    StringBuilder whereClause = new StringBuilder();
    if (predicate instanceof BasePredicate) {

      BasePredicate basePredicate = (BasePredicate) predicate;

      SQLPredicateVisitor visitor = new SQLPredicateVisitor();
      basePredicate.accept(visitor);
      whereClause.append(visitor.getSQL());

      return "delete from " + table + " where " + whereClause;
    }
    throw new IllegalStateException("Can't generate SQL.");
  }

  /**
   * Get an update SQL statement based on the given properties and predicate.
   *
   * @param table       the table
   * @param properties  the properties
   * @param predicate   the predicate
   *
   * @return the update SQL statement
   */
  private String getUpdateSQL(String table, Map<PropertyId, Object> properties, Predicate predicate) {

    if (predicate instanceof BasePredicate) {

      StringBuilder whereClause = new StringBuilder();

      BasePredicate basePredicate = (BasePredicate) predicate;

      SQLPredicateVisitor visitor = new SQLPredicateVisitor();
      basePredicate.accept(visitor);
      whereClause.append(visitor.getSQL());

      StringBuilder setClause = new StringBuilder();
      for (Map.Entry<PropertyId, Object> entry : properties.entrySet()) {

        if (setClause.length() > 0) {
          setClause.append(", ");
        }
        setClause.append(entry.getKey().getName());
        setClause.append(" = ");
        Object propertyValue = entry.getValue();

        if (propertyValue instanceof String) {
          setClause.append("'");
          setClause.append(propertyValue);
          setClause.append("'");
        } else {
          setClause.append(propertyValue);
        }
      }

      return "update " + table + " set " + setClause + " where " + whereClause;
    }
    throw new IllegalStateException("Can't generate SQL.");
  }

  /**
   * Get the set of tables associated with the given property ids.
   *
   * @param propertyIds  the property ids
   *
   * @return the set of tables
   */
  private static Set<String> getTables(Set<PropertyId> propertyIds) {
    Set<String> tables = new HashSet<String>();
    for (PropertyId propertyId : propertyIds) {
      tables.add(propertyId.getCategory());
    }
    return tables;
  }
}

