/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.view.huetoambarimigration.datasource.queryset.ambariqueryset.hive.savedqueryset;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Prepared statement for Saved query
 */

public abstract class QuerySetAmbariDB {

  public PreparedStatement getTableIdFromInstanceNameSavedquery(Connection connection, String instance) throws SQLException {

    PreparedStatement prSt = connection.prepareStatement(getTableIdSqlFromInstanceNameSavedQuery());
    prSt.setString(1, instance);
    return prSt;
  }

  public PreparedStatement getTableIdFromInstanceNameHistoryquery(Connection connection, String instance) throws SQLException {

    PreparedStatement prSt = connection.prepareStatement(getTableIdSqlFromInstanceNameHistoryQuery());
    prSt.setString(1, instance);
    return prSt;
  }

  public PreparedStatement getMaxDsIdFromTableIdHistoryquery(Connection connection, int id) throws SQLException {

    PreparedStatement prSt = connection.prepareStatement(getSqlMaxDSidFromTableIdHistoryQuery(id));
    return prSt;
  }

  public PreparedStatement getMaxDsIdFromTableIdSavedquery(Connection connection, int id) throws SQLException {

    PreparedStatement prSt = connection.prepareStatement(getSqlMaxDSidFromTableIdSavedQuery(id));

    return prSt;
  }

  public PreparedStatement insertToHiveHistory(Connection connection, int id, String maxcount, long epochtime, String dirname) throws SQLException {

    String Logfile = dirname + "logs";
    String queryHqlFile = dirname + "query.hql";

    PreparedStatement prSt = connection.prepareStatement(getSqlInsertHiveHistory(id));

    prSt.setString(1, maxcount);
    prSt.setLong(2, epochtime);
    prSt.setString(3, Logfile);
    prSt.setString(4, queryHqlFile);
    prSt.setString(5, dirname);

    return prSt;
  }

  public PreparedStatement insertToHiveSavedQuery(Connection connection, int id, String maxcount, String database, String dirname, String query, String name) throws SQLException {

    String Logfile = dirname + "logs";
    String queryHqlFile = dirname + "query.hql";

    PreparedStatement prSt = connection.prepareStatement(getSqlInsertSavedQuery(id));

    prSt.setString(1, maxcount);
    prSt.setString(2, database);
    prSt.setString(3, queryHqlFile);
    prSt.setString(4, query);
    prSt.setString(5, name);

    return prSt;
  }

  public String revertSqlHistoryQuery(int id, String maxcount) throws SQLException {

    return getRevSqlHistoryQuery(id, maxcount);
  }

  public String revertSqlSavedQuery(int id, String maxcount) throws SQLException {

    return getRevSqlSavedQuery(id, maxcount);
  }

  protected String getSqlMaxDSidFromTableIdSavedQuery(int id) {
    return "select MAX(cast(ds_id as integer)) as max from ds_savedquery_" + id + ";";
  }

  protected String getTableIdSqlFromInstanceNameSavedQuery() {
    return "select id from viewentity where class_name LIKE 'org.apache.ambari.view.hive.resources.savedQueries.SavedQuery' and view_instance_name=?;";
  }

  protected String getSqlMaxDSidFromTableIdHistoryQuery(int id) {
    return "select MAX(cast(ds_id as integer)) as max from ds_jobimpl_" + id + ";";
  }

  protected String getTableIdSqlFromInstanceNameHistoryQuery() {
    return "select id from viewentity where class_name LIKE 'org.apache.ambari.view.hive.resources.jobs.viewJobs.JobImpl' and view_instance_name=?;";
  }

  protected String getSqlInsertHiveHistory(int id) {
    return "INSERT INTO ds_jobimpl_" + id + " values (?,'','','','','default',?,0,'','',?,'admin',?,'','job','','','Unknown',?,'','Worksheet');";
  }

  protected String getSqlInsertSavedQuery(int id) {
    return "INSERT INTO ds_savedquery_" + id + " values (?,?,'" + "admin" + "',?,?,?);";
  }

  protected String getRevSqlSavedQuery(int id, String maxcount) {
    return "delete from  ds_savedquery_" + id + " where ds_id='" + maxcount + "';";
  }

  protected String getRevSqlHistoryQuery(int id, String maxcount) {
    return "delete from  ds_jobimpl_" + id + " where ds_id='" + maxcount + "';";
  }

}
