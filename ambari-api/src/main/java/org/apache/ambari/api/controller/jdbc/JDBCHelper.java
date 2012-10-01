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

import org.apache.ambari.api.controller.internal.PropertyIdImpl;
import org.apache.ambari.api.controller.internal.ResourceImpl;
import org.apache.ambari.api.controller.predicate.BasePredicate;
import org.apache.ambari.api.controller.predicate.PredicateVisitorAcceptor;
import org.apache.ambari.api.controller.spi.Predicate;
import org.apache.ambari.api.controller.spi.PropertyId;
import org.apache.ambari.api.controller.spi.Request;
import org.apache.ambari.api.controller.spi.Resource;
import org.apache.ambari.api.controller.utilities.PredicateHelper;
import org.apache.ambari.api.controller.utilities.Properties;

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
 * Helper class for JDBC related operations.
 */
public class JDBCHelper {

  public static void createResources(ConnectionFactory connectionFactory, Request request) {
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

  public static Set<Resource> getResources(ConnectionFactory connectionFactory, Resource.Type type, Request request, Predicate predicate) {

    Set<Resource> resources = new HashSet<Resource>();
    Set<PropertyId> propertyIds = new HashSet<PropertyId>(request.getPropertyIds());
    if (predicate != null) {
      propertyIds.addAll(PredicateHelper.getPropertyIds(predicate));
    }

    try {
      Connection connection = connectionFactory.getConnection();

      try {

        Map<PropertyId, PropertyId> joinKeys = getJoins(connection, propertyIds);
        String sql = getSelectSQL(propertyIds, predicate, joinKeys);
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

  public static void updateResources(ConnectionFactory connectionFactory, Request request, Predicate predicate) {

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

  public static void deleteResources(ConnectionFactory connectionFactory, Predicate predicate) {
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


  // ----- Helper methods ----------------------------------------------------

  private static String getInsertSQL(Map<PropertyId, String> properties) {

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

  private static String getSelectSQL(Set<PropertyId> propertyIds, Predicate predicate, Map<PropertyId, PropertyId> joinKeys) {

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
      for (Map.Entry<PropertyId, PropertyId> entry : joinKeys.entrySet()) {
        String category1 = entry.getKey().getCategory();
        String category2 = entry.getValue().getCategory();
        if (tableSet.contains(category1) && tableSet.contains(category2)){
          if (haveWhereClause || joinClause.length() > 0) {
            joinClause.append(" and ");
          }
          joinClause.append(category1).append(".").append(entry.getKey().getName());
          joinClause.append("=");
          joinClause.append(category2).append(".").append(entry.getValue().getName());
          tableSet.add(category1);
          tableSet.add(category2);
        }
      }
      haveWhereClause = true;
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

  private static String getDeleteSQL(Predicate predicate) {

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

  private static String getUpdateSQL(Map<PropertyId, String> properties, Predicate predicate) {

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

  private static Map<PropertyId, PropertyId> getJoins(Connection connection, Set<PropertyId> tables) throws SQLException {
    Map<PropertyId, PropertyId> joins = new HashMap<PropertyId, PropertyId>();
    DatabaseMetaData meta = connection.getMetaData();

    for (PropertyId propertyId : tables) {
      ResultSet rs = meta.getImportedKeys(connection.getCatalog(), null, propertyId.getCategory());

      while (rs.next()) {

        PropertyId pkPropertyId = Properties.getPropertyId(
            rs.getString("PKCOLUMN_NAME"), rs.getString("PKTABLE_NAME"));

        PropertyId fkPropertyId = Properties.getPropertyId(
            rs.getString("FKCOLUMN_NAME"), rs.getString("FKTABLE_NAME"));
        joins.put(pkPropertyId, fkPropertyId);
      }
    }
    return joins;
  }
}
