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
package org.apache.ambari.view.huetoambarimigration.datasource.queryset.huequeryset.hive.historyqueryset;


public class OracleQuerySetHueDB extends QuerySetHueDB {

  @Override
  protected String fetchuserIdfromUsernameSql() {
    return "select id from auth_user where username=?";
  }
  @Override
  protected String fetchHueQueriesNoStartdateNoEnddateSql() {
    return "select query from beeswax_queryhistory where owner_id =?";
  }
  @Override
  protected String fetchHueQueriesNoStartdateYesEnddateSql() {
    return "select query from beeswax_queryhistory where owner_id =? AND submission_date <= date(?)";
  }
  @Override
  protected String fetchHueQueriesYesStartdateNoEnddateSql() {
    return "select query from beeswax_queryhistory where owner_id =? AND submission_date >= date(?)";
  }
  @Override
  protected String fetchHueQueriesYesStartdateYesEnddateSql() {
    return "select query from beeswax_queryhistory where owner_id =? AND submission_date >= date(?) AND submission_date <= date(?)";
  }
  @Override
  protected String fetchHueQueriesNoStartdateNoEnddateYesallUserSql() {
    return "select query from beeswax_queryhistory";
  }
  @Override
  protected String fetchHueQueriesNoStartdateYesEnddateYesallUserSql() {
    return "select query from beeswax_queryhistory where submission_date <= date(?)";
  }
  @Override
  protected String fetchHueQueriesYesStartdateNoEnddateYesallUserSql() {
    return "select query from beeswax_queryhistory where submission_date >= date(?)";

  }
  @Override
  protected String fetchHueQueriesYesStartdateYesEnddateYesallUserSql() {
    return "select query from beeswax_queryhistory where submission_date >= date(?) AND submission_date <= date(?)";

  }
  @Override
  protected String fetchUserNamSql() {
    return "select username from auth_user where id = ?";
  }

}
