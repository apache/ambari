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
package org.apache.ambari.view.huetoambarimigration.datasource.queryset.ambariqueryset.pig.jobqueryset;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 *  Pig Job Prepare statement
 */

public abstract class QuerySetAmbariDB {

  public PreparedStatement getTableIdFromInstanceName(Connection connection, String instance) throws SQLException {

    PreparedStatement prSt = connection.prepareStatement(getTableIdSqlFromInstanceName());
    prSt.setString(1, instance);
    return prSt;
  }

  public PreparedStatement getSequenceNoFromAmbariSequence(Connection connection,int id) throws SQLException {
    PreparedStatement prSt = connection.prepareStatement(getSqlSequenceNoFromAmbariSequence(id));
    return prSt;
  }

  public PreparedStatement getMaxDsIdFromTableId(Connection connection, int id) throws SQLException {

    PreparedStatement prSt = connection.prepareStatement(getSqlMaxDSidFromTableId(id));
    return prSt;
  }

  public PreparedStatement updateSequenceNoInAmbariSequence(Connection connection, int seqNo, int id) throws SQLException {

    PreparedStatement prSt = connection.prepareStatement(getSqlUpdateSequenceNo(id));

    prSt.setInt(1, seqNo);

    return prSt;
  }

  public PreparedStatement insertToPigJob(String dirname, String maxcountforpigjob, long epochtime1, String title, Connection connection, int id, String status,String username) throws SQLException {

    String pigScriptFile = dirname + "script.pig";

    PreparedStatement prSt = connection.prepareStatement(getSqlinsertToPigJob(id));

    prSt.setString(1, maxcountforpigjob);
    prSt.setLong(2, epochtime1);
    prSt.setString(3, username);
    prSt.setString(4, pigScriptFile);
    prSt.setString(5, maxcountforpigjob);
    prSt.setString(6, status);
    prSt.setString(7, dirname);
    prSt.setString(8, title);

    return prSt;
  }

  public String revertSql(int id, String maxcount) throws SQLException {
    return getRevSql(id, maxcount);
  }

  protected String getSqlMaxDSidFromTableId(int id) {
    return "select MAX(cast(ds_id as integer)) as max from ds_pigjob_" + id + ";";
  }

  protected String getTableIdSqlFromInstanceName() {
    return "select id from viewentity where class_name LIKE 'org.apache.ambari.view.pig.resources.jobs.models.PigJob' and view_instance_name=?;";
  }

  protected String getSqlinsertToPigJob(int id) {
    return "INSERT INTO ds_pigjob_" + id + " values (?,?,0,'','0','','',?,0,?,'',?,'','',?,?,'',?);";
  }

  protected String getRevSql(int id, String maxcount) {
    return "delete from  ds_pigjob_" + id + " where ds_id='" + maxcount + "';";
  }

  protected String getSqlSequenceNoFromAmbariSequence(int id) {
    return "select sequence_value from ambari_sequences where sequence_name ='ds_pigjob_"+id+"_id_seq';";
  }

  protected String getSqlUpdateSequenceNo(int id) {
    return "update ambari_sequences set sequence_value=? where sequence_name='ds_pigjob_"+id+"_id_seq';";
  }

}
