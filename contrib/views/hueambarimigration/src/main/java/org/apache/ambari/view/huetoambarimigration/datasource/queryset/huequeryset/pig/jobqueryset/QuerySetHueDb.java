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
package org.apache.ambari.view.huetoambarimigration.datasource.queryset.huequeryset.pig.jobqueryset;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;


public abstract class QuerySetHueDb {

  public PreparedStatement getUseridfromUserName(Connection connection, String username) throws SQLException {
    PreparedStatement prSt = connection.prepareStatement(fetchuserIdfromUsernameSql());
    prSt.setString(1, username);
    return prSt;
  }

  public PreparedStatement getQueriesNoStartDateNoEndDate(Connection connection, int id) throws SQLException {
    PreparedStatement prSt = connection.prepareStatement(fetchHueQueriesNoStartdateNoEnddateSql());
    prSt.setInt(1, id);
    return prSt;
  }

  public PreparedStatement getQueriesNoStartDateYesEndDate(Connection connection, int id, String enddate) throws SQLException {
    PreparedStatement prSt = connection.prepareStatement(fetchHueQueriesNoStartdateYesEnddateSql());
    prSt.setInt(1, id);
    prSt.setString(2, enddate);
    return prSt;
  }

  public PreparedStatement getQueriesYesStartDateNoEndDate(Connection connection, int id, String startdate) throws SQLException {
    PreparedStatement prSt = connection.prepareStatement(fetchHueQueriesYesStartdateNoEnddateSql());
    prSt.setInt(1, id);
    prSt.setString(2, startdate);
    return prSt;
  }

  public PreparedStatement getQueriesYesStartDateYesEndDate(Connection connection, int id, String startdate, String endate) throws SQLException {
    PreparedStatement prSt = connection.prepareStatement(fetchHueQueriesYesStartdateYesEnddateSql());
    prSt.setInt(1, id);
    prSt.setString(2, startdate);
    prSt.setString(3, endate);
    return prSt;
  }

  /**
   * for all user
   */
  public PreparedStatement getQueriesNoStartDateNoEndDateAllUser(Connection connection) throws SQLException {
    PreparedStatement prSt = connection.prepareStatement(fetchHueQueriesNoStartdateNoEnddateYesallUserSql());
    return prSt;
  }

  public PreparedStatement getQueriesNoStartDateYesEndDateAllUser(Connection connection, String enddate) throws SQLException {
    PreparedStatement prSt = connection.prepareStatement(fetchHueQueriesNoStartdateYesEnddateYesallUserSql());
    prSt.setString(1, enddate);
    return prSt;
  }

  public PreparedStatement getQueriesYesStartDateNoEndDateAllUser(Connection connection, String startdate) throws SQLException {
    PreparedStatement prSt = connection.prepareStatement(fetchHueQueriesYesStartdateNoEnddateYesallUserSql());
    prSt.setString(1, startdate);
    return prSt;
  }

  public PreparedStatement getQueriesYesStartDateYesEndDateAllUser(Connection connection, String startdate, String endate) throws SQLException {
    PreparedStatement prSt = connection.prepareStatement(fetchHueQueriesYesStartdateYesEnddateYesallUserSql());
    prSt.setString(1, startdate);
    prSt.setString(2, endate);
    return prSt;
  }

  public PreparedStatement getUserName(Connection connection, int id) throws SQLException {
    PreparedStatement prSt = connection.prepareStatement(fetchUserNameSql());
    prSt.setInt(1, id);
    return prSt;
  }

  protected String fetchuserIdfromUsernameSql() {
    return "select id from auth_user where username=?;";

  }

  protected String fetchHueQueriesNoStartdateNoEnddateSql() {
    return "select status,start_time,statusdir,script_title,user_id from pig_job where user_id =?;";
  }

  protected String fetchHueQueriesNoStartdateYesEnddateSql() {
    return "select status,start_time,statusdir,script_title,user_id from pig_job where user_id =?  AND start_time <= date(?);";

  }

  protected String fetchHueQueriesYesStartdateNoEnddateSql() {
    return "select status,start_time,statusdir,script_title,user_id from pig_job where user_id =? AND start_time >= date(?);";

  }

  protected String fetchHueQueriesYesStartdateYesEnddateSql() {
    return "select status,start_time,statusdir,script_title,user_id from pig_job where user_id =? AND start_time >= date(?) AND start_time <= date(?);";

  }

  protected String fetchHueQueriesNoStartdateNoEnddateYesallUserSql() {
    return "select status,start_time,statusdir,script_title,user_id from pig_job ;";
  }

  protected String fetchHueQueriesNoStartdateYesEnddateYesallUserSql() {
    return "select status,start_time,statusdir,script_title,user_id from pig_job where  start_time <= date(?);";

  }

  protected String fetchHueQueriesYesStartdateNoEnddateYesallUserSql() {
    return "select status,start_time,statusdir,script_title,user_id from pig_job where  start_time >= date(?);";

  }

  protected String fetchHueQueriesYesStartdateYesEnddateYesallUserSql() {
    return "select status,start_time,statusdir,script_title,user_id from pig_job where  start_time >= date(?) AND start_time <= date(?);";

  }

  protected String fetchUserNameSql() {
    return "select username from auth_user where id = ?;";
  }

}
