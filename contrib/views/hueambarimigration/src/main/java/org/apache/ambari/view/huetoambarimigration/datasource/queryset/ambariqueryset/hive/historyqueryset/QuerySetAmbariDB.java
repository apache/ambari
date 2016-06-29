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
package org.apache.ambari.view.huetoambarimigration.datasource.queryset.ambariqueryset.hive.historyqueryset;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * History Query Prepared statemets
 */

public abstract class QuerySetAmbariDB {

  public PreparedStatement getTableIdFromInstanceName(Connection connection, String instance) throws SQLException {
    PreparedStatement prSt = connection.prepareStatement(getTableIdSqlFromInstanceName());
    prSt.setString(1, instance);
    return prSt;
  }

  public PreparedStatement getMaxDsIdFromTableId(Connection connection, int id) throws SQLException {

    PreparedStatement prSt = connection.prepareStatement(getSqlMaxDSidFromTableId(id));

    return prSt;
  }

  public PreparedStatement insertToHiveHistory(Connection connection, int id, String maxcount, long epochtime, String dirname) throws SQLException {

    String Logfile=  dirname + "logs";
    String queryHqlFile= dirname + "query.hql";

    PreparedStatement prSt = connection.prepareStatement(getSqlInsertHiveHistory(id));

    prSt.setString(1, maxcount);
    prSt.setLong(2, epochtime);
    prSt.setString(3, Logfile);
    prSt.setString(4, queryHqlFile);
    prSt.setString(5, dirname);

    return prSt;
  }

  public String RevertSql(int id,String maxcount) throws SQLException {
    return getRevSql(id,maxcount);
  }

  protected String getSqlMaxDSidFromTableId(int id) {
    return "select MAX(cast(ds_id as integer)) as max from ds_jobimpl_" + id + ";";
  }

  protected String getTableIdSqlFromInstanceName() {
    return "select id from viewentity where class_name LIKE 'org.apache.ambari.view.hive.resources.jobs.viewJobs.JobImpl' and view_instance_name=?;";
  }

  protected String getSqlInsertHiveHistory(int id) {
    return "INSERT INTO ds_jobimpl_" + id + " values (?,'','','','','default',?,0,'','',?,'admin',?,'','job','','','Unknown',?,'','Worksheet');";
  }

  protected String getRevSql(int id,String maxcount){
    return "delete from  ds_jobimpl_" + id + " where ds_id='" + maxcount + "';";
  }

}
