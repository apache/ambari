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
package org.apache.ambari.view.huetoambarimigration.migration.configuration;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.huetoambarimigration.datasource.DataSourceHueDatabase;
import org.apache.ambari.view.huetoambarimigration.resources.scripts.models.UserModel;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import  org.apache.ambari.view.huetoambarimigration.datasource.queryset.huequeryset.userdetails.*;


public class UserDetailsUtility {


  public List<UserModel> getUserDetails(ViewContext view) throws PropertyVetoException, SQLException, IOException {

    List<UserModel> userlist=new ArrayList<>();
    Connection conn = null;
    Statement stmt = null;
    conn = DataSourceHueDatabase.getInstance(view.getProperties().get("huedrivername"),view.getProperties().get("huejdbcurl"),view.getProperties().get("huedbusername"),view.getProperties().get("huedbpassword")).getConnection();
    conn.setAutoCommit(false);
    stmt = conn.createStatement();
    UserModel all=new UserModel();
    all.setId(-1);
    all.setUsername("all");
    PreparedStatement prSt;
    userlist.add(all);
    ResultSet rs1=null;

    QuerySet huedatabase = null;

    if (view.getProperties().get("huedrivername").contains("mysql")) {
      huedatabase = new MysqlQuerySet();
    } else if (view.getProperties().get("huedrivername").contains("postgresql")) {
      huedatabase = new PostgressQuerySet();
    } else if (view.getProperties().get("huedrivername").contains("sqlite")) {
      huedatabase = new SqliteQuerySet();
    } else if (view.getProperties().get("huedrivername").contains("oracle")) {
      huedatabase = new OracleQuerySet();
    }

    prSt = huedatabase.getUserDetails(conn);

    rs1 = prSt.executeQuery();

    while (rs1.next()) {
      UserModel I=new UserModel();
      I.setUsername(rs1.getString(2));
      I.setId(rs1.getInt(1));
      System.out.println(rs1.getString(2));
      userlist.add(I);
    }
    rs1.close();
    stmt.close();
    return userlist;

  }


}
