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
package org.apache.ambari.view.huetoambarimigration.migration.pig.pigudf;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.huetoambarimigration.persistence.utils.ItemNotFound;
import org.apache.ambari.view.huetoambarimigration.resources.scripts.models.MigrationResponse;
import org.json.simple.JSONObject;

import java.io.IOException;


public class PigUdfStartJob extends Thread{

    String username;
    String instance;
    String startdate;
    String enddate;
    String jobid;
    ViewContext view;

    public PigUdfStartJob(String username, String instance, String jobid, ViewContext view) {
        this.username = username;
        this.instance=instance;
        this.startdate=startdate;
        this.enddate=enddate;
        this.jobid=jobid;
        this.view=view;
    }

    @Override
    public void run() {

        MigrationResponse migrationresult=new MigrationResponse();

        migrationresult.setId(jobid);
        migrationresult.setIntanceName(instance);
        migrationresult.setUserNameofhue(username);
        migrationresult.setProgressPercentage(0);
        migrationresult.setFlag(0);

        PigUdfMigrationUtility pigudfmigration=new PigUdfMigrationUtility();
        try {
            pigudfmigration.pigUdfMigration(username,instance,view,migrationresult,jobid);
        }
        catch (IOException e) {
            e.printStackTrace();
        } catch (ItemNotFound itemNotFound) {
            itemNotFound.printStackTrace();
        }

    }

}
