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

/**
 * Override method specific to Mysql
 */

public class MysqlQuerySetAmbariDB extends QuerySetAmbariDB {

  @Override
  protected String getSqlMaxDSidFromTableId(int id) {
    return "select max( cast(ds_id as unsigned) ) as max from DS_PIGJOB_" + id + ";";
  }

  @Override
  protected String getTableIdSqlFromInstanceName() {
    return "select id from viewentity where class_name LIKE 'org.apache.ambari.view.pig.resources.jobs.models.PigJob' and view_instance_name=?;";
  }
  @Override
  protected String getSqlinsertToPigJob(int id) {
    return "INSERT INTO DS_PIGJOB_" + id + " values (?,?,0,'',0,'','',?,0,?,'',?,'','',?,?,'',?);";
  }
  @Override
  protected String getRevSql(int id, String maxcount) {
    return "delete from  DS_PIGJOB_" + id + " where ds_id='" + maxcount + "';";
  }
  @Override
  protected String getSqlSequenceNoFromAmbariSequence(int id) {
    return "select sequence_value from ambari_sequences where sequence_name ='ds_pigjob_"+id+"_id_seq';";
  }
  @Override
  protected String getSqlUpdateSequenceNo(int id) {
    return "update ambari_sequences set sequence_value=? where sequence_name='ds_pigjob_"+id+"_id_seq';";
  }
}
