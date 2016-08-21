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
package org.apache.ambari.view.huetoambarimigration.datasource.queryset.ambariqueryset.hive.instancedetail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Service class to get instance detail
 */

public abstract class QuerySetAmbariDB {

  public PreparedStatement getHiveInstanceDeatil(Connection connection) throws SQLException {
    PreparedStatement prSt = connection.prepareStatement(getHiveInstanceSql());
    return prSt;
  }

  public PreparedStatement getAllInstanceDeatil(Connection connection) throws SQLException {
    PreparedStatement prSt = connection.prepareStatement(getAllInstanceDetailSql());
    return prSt;
  }

  protected String getHiveInstanceSql(){
    return "select distinct(view_instance_name) as instancename from viewentity where view_name like '%HIVE%';";
  }

  protected String getAllInstanceDetailSql(){
    return "select distinct(view_instance_name) as instancename from viewentity where view_name like '%HIVE%' or view_name like '%PIG%';";
  }

}
