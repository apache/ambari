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


package org.apache.ambari.view.huetoambarimigration.migration.pig.pigjob;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;

import org.apache.ambari.view.ViewContext;

import org.apache.ambari.view.huetoambarimigration.persistence.utils.ItemNotFound;
import org.apache.ambari.view.huetoambarimigration.resources.PersonalCRUDResourceManager;
import org.apache.ambari.view.huetoambarimigration.resources.scripts.MigrationResourceManager;
import org.apache.ambari.view.huetoambarimigration.resources.scripts.models.MigrationResponse;
import org.apache.ambari.view.huetoambarimigration.resources.scripts.models.PigModel;
import org.apache.log4j.Logger;

import org.apache.ambari.view.huetoambarimigration.datasource.DataSourceAmbariDatabase;
import org.apache.ambari.view.huetoambarimigration.datasource.DataSourceHueDatabase;
import org.apache.ambari.view.huetoambarimigration.datasource.queryset.huequeryset.pig.jobqueryset.*;
import org.apache.ambari.view.huetoambarimigration.datasource.queryset.ambariqueryset.pig.jobqueryset.*;

public class PigJobMigrationUtility {

  protected MigrationResourceManager resourceManager = null;

  public synchronized PersonalCRUDResourceManager<MigrationResponse> getResourceManager(ViewContext view) {
    if (resourceManager == null) {
      resourceManager = new MigrationResourceManager(view);
    }
    return resourceManager;
  }

  public void pigJobMigration(String username, String instance, String startDate, String endDate, ViewContext view, MigrationResponse migrationresult, String jobid) throws IOException, ItemNotFound {

    long startTime = System.currentTimeMillis();

    final Logger logger = Logger.getLogger(PigJobMigrationUtility.class);
    Connection connectionHuedb = null;
    Connection connectionAmbaridb = null;

    logger.info("------------------------------");
    logger.info("pig Jobs Migration started");
    logger.info("------------------------------");
    logger.info("start date: " + startDate);
    logger.info("enddate date: " + endDate);
    logger.info("instance is: " + username);
    logger.info("hue username is : " + instance);

    PigJobMigrationImplementation pigjobimpl = new PigJobMigrationImplementation();// creating the implementation object

    QuerySetHueDb huedatabase = null;

    if (view.getProperties().get("huedrivername").contains("mysql")) {
      huedatabase = new MysqlQuerySetHueDb();
    } else if (view.getProperties().get("huedrivername").contains("postgresql")) {
      huedatabase = new PostgressQuerySetHueDb();
    } else if (view.getProperties().get("huedrivername").contains("sqlite")) {
      huedatabase = new SqliteQuerySetHueDb();
    } else if (view.getProperties().get("huedrivername").contains("oracle")) {
      huedatabase = new OracleQuerySetHueDb();
    }

    QuerySetAmbariDB ambaridatabase = null;

    if (view.getProperties().get("ambaridrivername").contains("mysql")) {
      ambaridatabase = new MysqlQuerySetAmbariDB();
    } else if (view.getProperties().get("ambaridrivername").contains("postgresql")) {
      ambaridatabase = new PostgressQuerySetAmbariDB();
    } else if (view.getProperties().get("ambaridrivername").contains("oracle")) {
      ambaridatabase = new OracleQuerySetAmbariDB();
    }
    int maxCountforPigScript = 0, i = 0;

    String time = null, timeIndorder = null;
    Long epochtime = null;
    String pigJobDirName;
    ArrayList<PigModel> pigJobDbPojo = new ArrayList<PigModel>();

    try {

      String[] usernames = username.split(",");
      int totalQueries = 0;
      for (int k = 0; k < usernames.length; k++) {

        connectionHuedb = DataSourceHueDatabase.getInstance(view.getProperties().get("huedrivername"), view.getProperties().get("huejdbcurl"), view.getProperties().get("huedbusername"), view.getProperties().get("huedbpassword")).getConnection();//connecting to hue database
        username = usernames[k];
        migrationresult.setProgressPercentage(0);
        logger.info("Migration started for user " + username);
        pigJobDbPojo = pigjobimpl.fetchFromHueDB(username, startDate, endDate, connectionHuedb, huedatabase);// fetching the PigJobs details from hue
        totalQueries += pigJobDbPojo.size();
        for (int j = 0; j < pigJobDbPojo.size(); j++) {
          logger.info("the query fetched from hue=" + pigJobDbPojo.get(i).getScript());

        }

                /*No pig Job details has been fetched accordring to search criteria*/
        if (pigJobDbPojo.size() == 0) {

          logger.info("No queries has been selected for the user " + username + " between dates: " + startDate + " - " + endDate);
        } else {

          connectionAmbaridb = DataSourceAmbariDatabase.getInstance(view.getProperties().get("ambaridrivername"), view.getProperties().get("ambarijdbcurl"), view.getProperties().get("ambaridbusername"), view.getProperties().get("ambaridbpassword")).getConnection();
          connectionAmbaridb.setAutoCommit(false);

          int fetchPigTablenameInstance = pigjobimpl.fetchInstanceTablename(connectionAmbaridb, instance, ambaridatabase);
          int sequence = pigjobimpl.fetchSequenceno(connectionAmbaridb, fetchPigTablenameInstance, ambaridatabase);

          for (i = 0; i < pigJobDbPojo.size(); i++) {

            float calc = ((float) (i + 1)) / pigJobDbPojo.size() * 100;
            int progressPercentage = Math.round(calc);

            migrationresult.setProgressPercentage(progressPercentage);
            migrationresult.setNumberOfQueryTransfered(i + 1);
            getResourceManager(view).update(migrationresult, jobid);

            logger.info("Loop No." + (i + 1));
            logger.info("________________");
            logger.info("the title of script " + pigJobDbPojo.get(i).getTitle());

            maxCountforPigScript = i + sequence + 1;

            time = pigjobimpl.getTime();
            timeIndorder = pigjobimpl.getTimeInorder();
            epochtime = pigjobimpl.getEpochTime();

            if (usernames[k].equals("all")) {
              username = pigJobDbPojo.get(i).getUserName();
            }

            pigJobDirName = "/user/" + username + "/pig/jobs/" + pigJobDbPojo.get(i).getTitle() + "_" + time + "/";

            pigjobimpl.insertRowPigJob(pigJobDirName, maxCountforPigScript, time, timeIndorder, epochtime, pigJobDbPojo.get(i).getTitle(), connectionAmbaridb, fetchPigTablenameInstance, pigJobDbPojo.get(i).getStatus(), instance, i, ambaridatabase, username);

            if (view.getProperties().get("KerberoseEnabled").equals("y")) {
              pigjobimpl.createDirPigJobSecured(pigJobDirName, view.getProperties().get("namenode_URI_Ambari"), username, view.getProperties().get("PrincipalUserName"));
              pigjobimpl.copyFileBetweenHdfsSecured(pigJobDbPojo.get(i).getDir() + "/script.pig", pigJobDirName, view.getProperties().get("namenode_URI_Ambari"), view.getProperties().get("namenode_URI_Hue"), username, view.getProperties().get("PrincipalUserName"));
              pigjobimpl.copyFileBetweenHdfsSecured(pigJobDbPojo.get(i).getDir() + "/stderr", pigJobDirName, view.getProperties().get("namenode_URI_Ambari"), view.getProperties().get("namenode_URI_Hue"), username, view.getProperties().get("PrincipalUserName"));
              pigjobimpl.copyFileBetweenHdfsSecured(pigJobDbPojo.get(i).getDir() + "/stdout", pigJobDirName, view.getProperties().get("namenode_URI_Ambari"), view.getProperties().get("namenode_URI_Hue"), username, view.getProperties().get("PrincipalUserName"));

            } else {

              pigjobimpl.createDirPigJob(pigJobDirName, view.getProperties().get("namenode_URI_Ambari"), username);
              pigjobimpl.copyFileBetweenHdfs(pigJobDbPojo.get(i).getDir() + "/script.pig", pigJobDirName, view.getProperties().get("namenode_URI_Ambari"), view.getProperties().get("namenode_URI_Hue"), username);
              pigjobimpl.copyFileBetweenHdfs(pigJobDbPojo.get(i).getDir() + "/stderr", pigJobDirName, view.getProperties().get("namenode_URI_Ambari"), view.getProperties().get("namenode_URI_Hue"), username);
              pigjobimpl.copyFileBetweenHdfs(pigJobDbPojo.get(i).getDir() + "/stdout", pigJobDirName, view.getProperties().get("namenode_URI_Ambari"), view.getProperties().get("namenode_URI_Hue"), username);

            }

            logger.info(pigJobDbPojo.get(i).getTitle() + "has been migrated to Ambari");

          }
          pigjobimpl.updateSequenceno(connectionAmbaridb, maxCountforPigScript, fetchPigTablenameInstance, ambaridatabase);
          connectionAmbaridb.commit();
        }
        logger.info("Migration completed for user " + username);
      }
      migrationresult.setFlag(1);
      if (totalQueries == 0) {
        migrationresult.setNumberOfQueryTransfered(0);
        migrationresult.setTotalNoQuery(0);
      } else {
        migrationresult.setNumberOfQueryTransfered(totalQueries);
        migrationresult.setTotalNoQuery(totalQueries);
        migrationresult.setProgressPercentage(100);
      }
      getResourceManager(view).update(migrationresult, jobid);
    } catch (SQLException e) {
      logger.error("sql exception in ambari database:", e);
      migrationresult.setError("SQL Exception: " + e.getMessage());
      try {
        connectionAmbaridb.rollback();
        logger.info("roll back done");
      } catch (SQLException e1) {
        logger.error("roll back  exception:", e1);
      }
    } catch (ClassNotFoundException e2) {
      logger.error("class not found exception:", e2);
      migrationresult.setError("Class Not Found Exception: " + e2.getMessage());
    } catch (ParseException e) {
      logger.error("ParseException: ", e);
      migrationresult.setError("ParseException: " + e.getMessage());
    } catch (URISyntaxException e) {
      logger.error("URISyntaxException", e);
      migrationresult.setError("URI Syntax Exception: " + e.getMessage());
    } catch (PropertyVetoException e) {
      logger.error("PropertyVetoException", e);
      migrationresult.setError("Property Veto Exception: " + e.getMessage());
    } catch (Exception e) {
      logger.error("Generic Exception: ", e);
      migrationresult.setError("Exception: " + e.getMessage());
    } finally {
      if (null != connectionAmbaridb)
        try {
          connectionAmbaridb.close();
        } catch (SQLException e) {
          logger.error("connection closing exception ", e);
          migrationresult.setError("Error closing connection: " + e.getMessage());
        }
      getResourceManager(view).update(migrationresult, jobid);
    }

    logger.info("------------------------------");
    logger.info("pig Job Migration End");
    logger.info("------------------------------");

    long stopTime = System.currentTimeMillis();
    long elapsedTime = stopTime - startTime;

    migrationresult.setJobtype("pigjobmigration");
    migrationresult.setTotalTimeTaken(String.valueOf(elapsedTime));
    getResourceManager(view).update(migrationresult, jobid);


  }

}