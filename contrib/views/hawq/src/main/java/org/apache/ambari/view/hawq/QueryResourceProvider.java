/*
  Licensed to the Apache Software Foundation (ASF) under one
  or more contributor license agreements.  See the NOTICE file
  distributed with this work for additional information
  regarding copyright ownership.  The ASF licenses this file
  to you under the Apache License, Version 2.0 (the
  "License"); you may not use this file except in compliance
  with the License.  You may obtain a copy of the License at
  <p>
  http://www.apache.org/licenses/LICENSE-2.0
  <p>
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */

package org.apache.ambari.view.hawq;

import org.apache.ambari.view.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.sql.*;
import java.util.*;

/**
 * Resource provider for query resource.
 */
public class QueryResourceProvider implements ResourceProvider<QueryResource> {

    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(QueryResourceProvider.class);

    /**
     * The View Context Properties.
     */
    private static final String USER_PROP = "hawq.user.name";
    private static final String USER_DESC = "HAWQ User Name";

    private static final String PASSWORD_PROP = "hawq.user.password";
    private static final String PASSWORD_DESC = "HAWQ User Password";

    private static final String HOSTNAME_PROP = "hawq.master.host";
    private static final String HOSTNAME_DESC = "HAWQ Master Host";

    private static final String HOSTPORT_PROP = "hawq.master.port";
    private static final String HOSTPORT_DESC = "HAWQ Master Port";

    /**
     * The query to run in the database.
     */
    private static final String QUERY =
            "SELECT datid, datname, procpid, sess_id, usesysid, usename, application_name," +
                    "  current_query, waiting, waiting_resource, HOST(client_addr) as client_addr, client_port," +
                    "  TO_CHAR(query_start, 'YYYY-MM-DD HH24:MI:SS') AS query_start," +
                    "  TO_CHAR(backend_start, 'YYYY-MM-DD HH24:MI:SS') AS backend_start," +
                    "  TO_CHAR(xact_start, 'YYYY-MM-DD HH24:MI:SS') AS xact_start," +
                    "  ROUND(EXTRACT(EPOCH FROM DATE_TRUNC('second', NOW() - query_start))) AS query_duration " +
                    "FROM   pg_stat_activity " +
                    "WHERE  current_query NOT LIKE '<IDLE>' AND current_query NOT LIKE '%pg_stat_activity%';";


    /**
     * The view context.
     */
    @Inject
    ViewContext viewContext;

    /**
     * The datasource wrapper on top of connection pool used to connect to the HAWQ Master.
     */
    private HAWQDataSource dataSource = new HAWQDataSource();

    // ----- ResourceProvider --------------------------------------------------

    @Override
    public QueryResource getResource(String resourceId, Set<String> propertyIds) throws
            SystemException, NoSuchResourceException, UnsupportedPropertyException {
        throw new UnsupportedOperationException("Getting query resource is not currently supported.");
    }

    @Override
    public Set<QueryResource> getResources(ReadRequest request) throws
            SystemException, NoSuchResourceException, UnsupportedPropertyException {

        Set<QueryResource> resources;
        try {
            LOG.debug("Get queries: View Context Properties = {}", viewContext.getProperties());
            resources = getQueries();
            LOG.debug("Retrieved {} queries from HAWQ Master", resources.size());
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
            throw new SystemException("Can't get current queries.", e);
        }

        return resources;
    }

    @Override
    public void createResource(String resourceId, Map<String, Object> stringObjectMap) throws
            SystemException, ResourceAlreadyExistsException, NoSuchResourceException, UnsupportedPropertyException {
        throw new UnsupportedOperationException("Creating query resources is not currently supported.");
    }

    @Override
    public boolean updateResource(String resourceId, Map<String, Object> stringObjectMap) throws
            SystemException, NoSuchResourceException, UnsupportedPropertyException {
        throw new UnsupportedOperationException("Updating query resources is not currently supported.");
    }

    @Override
    public boolean deleteResource(String resourceId) throws
            SystemException, NoSuchResourceException, UnsupportedPropertyException {
        throw new UnsupportedOperationException("Deleting query resources is not currently supported.");
    }


    // ----- helper methods ----------------------------------------------------

    /**
     * Get the property from the view context.
     *
     * @param property    property name
     * @param description property description
     * @throws SystemException if the property value is not specified
     */
    private String getViewContextProperty(String property, String description) throws SystemException {
        String value = viewContext.getProperties().get(property);
        if (value == null || value.isEmpty()) {
            throw new SystemException(description + " property is not specified for the view instance.", new Exception());
        }
        return value;
    }

    /**
     * Retrieves a set of current queries from the HAWQ Master.
     *
     * @return a set of queries
     * @throws SystemException if configuration parameters are invalid
     * @throws SQLException    if data can't be retrieved from the database
     */
    private Set<QueryResource> getQueries() throws SystemException, SQLException {
        Set<QueryResource> result = new HashSet<>();

        Connection conn = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            conn = dataSource.getConnection(getViewContextProperty(HOSTNAME_PROP, HOSTNAME_DESC),
                    getViewContextProperty(HOSTPORT_PROP, HOSTPORT_DESC),
                    getViewContextProperty(USER_PROP, USER_DESC),
                    getViewContextProperty(PASSWORD_PROP, PASSWORD_DESC));
            st = conn.createStatement();

            LOG.debug("Executing query");
            rs = st.executeQuery(QUERY);

            ResultSetMetaData md = rs.getMetaData();
            int columns = md.getColumnCount();

            // iterate over ResultSet, creating QueryResource objects from the records
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>(columns);
                for (int i = 1; i <= columns; ++i) {
                    row.put(md.getColumnName(i), rs.getObject(i));
                }
                QueryResource query = new QueryResource();
                query.setId(row.get(QueryResource.ID_PROPERTY).toString());
                query.setAttributes(row);
                LOG.debug("row={}", row);
                result.add(query);
            }
        } finally {
            closeResultSet(rs);
            closeStatement(st);
            closeConnection(conn);
        }
        return result;
    }

    /**
     * Closes a result set, ignoring any exceptions.
     *
     * @param rs ResultSet to close
     */
    private void closeResultSet(ResultSet rs) {
        try {
            if (rs != null) rs.close();
        } catch (Exception e) {
            // ignore
        }
    }

    /**
     * Closes a statement ignoring any exceptions.
     *
     * @param st Statement to close
     */
    private void closeStatement(Statement st) {
        try {
            if (st != null) st.close();
        } catch (Exception e) {
            // ignore
        }
    }

    /**
     * Closes a connection ignoring any exceptions. Pooled connection would be returned to the pool.
     *
     * @param conn connection to close
     */
    private void closeConnection(Connection conn) {
        try {
            if (conn != null) conn.close();
        } catch (Exception e) {
            // ignore
        }
    }

}
