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
import org.apache.ambari.server.controller.predicate.BasePredicate;
import org.apache.ambari.server.controller.predicate.PredicateVisitorAcceptor;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.PropertyId;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PredicateHelper;
import org.apache.ambari.server.controller.utilities.PropertyHelper;

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
 * Generic JDBC based resource provider.
 */
public class JDBCResourceProvider implements ResourceProvider {

  private final Resource.Type type;

  private final Set<PropertyId> propertyIds;

  private final ConnectionFactory connectionFactory;

  /**
   * The schema for this provider's resource type.
   */
  private final Map<Resource.Type, PropertyId> keyPropertyIds;

  /**
   * Key mappings used for joins.
   */
  private final Map<String, Map<PropertyId, PropertyId>> importedKeys = new HashMap<String, Map<PropertyId, PropertyId>>();

  public JDBCResourceProvider(ConnectionFactory connectionFactory,
                              Resource.Type type,
                              Set<PropertyId> propertyIds,
                              Map<Resource.Type, PropertyId> keyPropertyIds) {
    this.connectionFactory = connectionFactory;
    this.type = type;
    this.propertyIds = propertyIds;
    this.keyPropertyIds = keyPropertyIds;
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate) {
    Set<Resource> resources = new HashSet<Resource>();
    Set<PropertyId> propertyIds = PropertyHelper.getRequestPropertyIds(this.propertyIds, request, predicate);

    try {
      Connection connection = connectionFactory.getConnection();

      try {

        for (String table : getTables(propertyIds)) {
          getImportedKeys(connection, table);
        }

        String sql = getSelectSQL(propertyIds, predicate);
        Statement statement = connection.createStatement();

        ResultSet rs = statement.executeQuery(sql);

        while (rs.next()) {
          ResultSetMetaData metaData = rs.getMetaData();
          int columnCount = metaData.getColumnCount();

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

  @Override
  public void createResources(Request request) {
    try {
      Connection connection = connectionFactory.getConnection();

      try {

        Set<Map<PropertyId, String>> propertySet = request.getProperties();

        for (Map<PropertyId, String> properties : propertySet) {
          String sql = getInsertSQL(properties);

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

  @Override
  public void updateResources(Request request, Predicate predicate) {

    try {
      Connection connection = connectionFactory.getConnection();
      try {
        Set<Map<PropertyId, String>> propertySet = request.getProperties();

        Map<PropertyId, String> properties = propertySet.iterator().next();

        String sql = getUpdateSQL(properties, predicate);

        Statement statement = connection.createStatement();

        statement.execute(sql);
      } finally {
        connection.close();
      }

    } catch (SQLException e) {
      throw new IllegalStateException("DB error : ", e);
    }
  }

  @Override
  public void deleteResources(Predicate predicate) {
    try {
      Connection connection = connectionFactory.getConnection();
      try {
        String sql = getDeleteSQL(predicate);

        Statement statement = connection.createStatement();
        statement.execute(sql);
      } finally {
        connection.close();
      }

    } catch (SQLException e) {
      throw new IllegalStateException("DB error : ", e);
    }
  }


  private String getInsertSQL(Map<PropertyId, String> properties) {

    StringBuilder columns = new StringBuilder();
    StringBuilder values = new StringBuilder();
    String table = null;


    for (Map.Entry<PropertyId, String> entry : properties.entrySet()) {
      PropertyId propertyId    = entry.getKey();
      String     propertyValue = entry.getValue();

      table = propertyId.getCategory();


      if (columns.length() > 0) {
        columns.append(", ");
      }
      columns.append(propertyId.getName());

      if (values.length() > 0) {
        values.append(", ");
      }
      values.append("'");
      values.append(propertyValue);
      values.append("'");
    }

    return "insert into " + table + " (" +
      columns + ") values (" +values + ")";
  }

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
    if (predicate != null &&
        propertyIds.containsAll(PredicateHelper.getPropertyIds(predicate)) &&
        predicate instanceof PredicateVisitorAcceptor) {

      SQLPredicateVisitor visitor = new SQLPredicateVisitor();
      ((PredicateVisitorAcceptor) predicate).accept(visitor);
      whereClause.append(visitor.getSQL());
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

  private String getDeleteSQL(Predicate predicate) {

    StringBuilder whereClause = new StringBuilder();
    if (predicate instanceof BasePredicate) {

      BasePredicate basePredicate = (BasePredicate) predicate;

      SQLPredicateVisitor visitor = new SQLPredicateVisitor();
      basePredicate.accept(visitor);
      whereClause.append(visitor.getSQL());

      String table = basePredicate.getPropertyIds().iterator().next().getCategory();

      return "delete from " + table + " where " + whereClause;
    }
    throw new IllegalStateException("Can't generate SQL.");
  }

  private String getUpdateSQL(Map<PropertyId, String> properties, Predicate predicate) {

    if (predicate instanceof BasePredicate) {

      StringBuilder whereClause = new StringBuilder();

      BasePredicate basePredicate = (BasePredicate) predicate;

      SQLPredicateVisitor visitor = new SQLPredicateVisitor();
      basePredicate.accept(visitor);
      whereClause.append(visitor.getSQL());

      String table = basePredicate.getPropertyIds().iterator().next().getCategory();


      StringBuilder setClause = new StringBuilder();
      for (Map.Entry<PropertyId, String> entry : properties.entrySet()) {

        if (setClause.length() > 0) {
          setClause.append(", ");
        }
        setClause.append(entry.getKey().getName());
        setClause.append(" = ");
        setClause.append("'");
        setClause.append(entry.getValue());
        setClause.append("'");
      }

      return "update " + table + " set " + setClause + " where " + whereClause;
    }
    throw new IllegalStateException("Can't generate SQL.");
  }

  @Override
  public Set<PropertyId> getPropertyIds() {
    return propertyIds;
  }

  @Override
  public Map<Resource.Type, PropertyId> getKeyPropertyIds() {
    return keyPropertyIds;
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
