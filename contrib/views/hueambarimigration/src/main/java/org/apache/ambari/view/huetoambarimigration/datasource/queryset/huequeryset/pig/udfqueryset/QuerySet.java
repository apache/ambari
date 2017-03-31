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
package org.apache.ambari.view.huetoambarimigration.datasource.queryset.huequeryset.pig.udfqueryset;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;


public abstract class QuerySet {

  public PreparedStatement getUserNamefromUserId(Connection connection,int id) throws SQLException {
    PreparedStatement prSt = connection.prepareStatement(fetchuserNamefromUserIdSql());
    prSt.setInt(1, id);
    return prSt;
  }

  public PreparedStatement getUseridfromUserName(Connection connection,String username) throws SQLException {
    PreparedStatement prSt = connection.prepareStatement(fetchuserIdfromUsernameSql());
    prSt.setString(1, username);
    return prSt;
  }

  public PreparedStatement getUserQueries(Connection connection,int id) throws SQLException {
    PreparedStatement prSt = connection.prepareStatement(fetchHueQueriesSql());
    prSt.setInt(1, id);
    return prSt;
  }

  /**
   * for all users
   * */
  public PreparedStatement getAllQueries(Connection connection) throws SQLException {
    PreparedStatement prSt = connection.prepareStatement(fetchHueQueriesAllUserSql());
    return prSt;
  }


  protected String fetchuserIdfromUsernameSql() {
    return "select id from auth_user where username=?;";

  }

  protected String fetchuserNamefromUserIdSql(){
    return "select username from auth_user where id=?;";
  }

  protected String fetchHueQueriesSql() {
    return "select url, file_name, owner_id from pig_udf where owner_id =?;";
  }


  protected String fetchHueQueriesAllUserSql() {
    return "select url, file_name, owner_id from pig_udf;";
  }

}



