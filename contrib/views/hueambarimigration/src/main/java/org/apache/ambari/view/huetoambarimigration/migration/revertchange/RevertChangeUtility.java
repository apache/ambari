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

package org.apache.ambari.view.huetoambarimigration.migration.revertchange;

import java.beans.PropertyVetoException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.huetoambarimigration.resources.scripts.models.MigrationModel;
import org.apache.ambari.view.huetoambarimigration.persistence.utils.ItemNotFound;
import org.apache.ambari.view.huetoambarimigration.resources.PersonalCRUDResourceManager;
import org.apache.ambari.view.huetoambarimigration.resources.scripts.MigrationResourceManager;
import org.apache.ambari.view.huetoambarimigration.resources.scripts.models.MigrationResponse;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.security.UserGroupInformation;

import org.apache.ambari.view.huetoambarimigration.datasource.DataSourceAmbariDatabase;
import org.apache.ambari.view.huetoambarimigration.migration.configuration.ConfigurationCheckImplementation;


public class RevertChangeUtility  {



  protected MigrationResourceManager resourceManager = null;

  public synchronized PersonalCRUDResourceManager<MigrationResponse> getResourceManager(ViewContext view) {
    if (resourceManager == null) {
      resourceManager = new MigrationResourceManager(view);
    }
    return resourceManager;
  }

  public boolean stringtoDatecompare(String datefromservlet,
                                     String datefromfile) throws ParseException {

    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
    Date date1 = formatter.parse(datefromservlet);
    Date date2 = formatter.parse(datefromfile);
    if (date1.compareTo(date2) < 0) {
      return true;
    } else {
      return false;
    }

  }

  public void removedir(final String dir, final String namenodeuri)
    throws IOException, URISyntaxException {

    try {
      UserGroupInformation ugi = UserGroupInformation
        .createRemoteUser("hdfs");

      ugi.doAs(new PrivilegedExceptionAction<Void>() {

        public Void run() throws Exception {

          Configuration conf = new Configuration();
          conf.set("fs.hdfs.impl",
            org.apache.hadoop.hdfs.DistributedFileSystem.class
              .getName());
          conf.set("fs.file.impl",
            org.apache.hadoop.fs.LocalFileSystem.class
              .getName());
          conf.set("fs.defaultFS", namenodeuri);
          conf.set("hadoop.job.ugi", "hdfs");

          FileSystem fs = FileSystem.get(conf);
          Path src = new Path(dir);
          fs.delete(src, true);
          return null;
        }
      });
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public MigrationModel revertChangeUtility(String instance, String revertDate,String jobid,ViewContext view,MigrationResponse migrationresult) throws IOException, ItemNotFound {

    long startTime = System.currentTimeMillis();

    final Logger logger = Logger.getLogger(RevertChangeUtility.class);

    logger.info("------------------------------");
    logger.info("Reverting the changes Start:");
    logger.info("------------------------------");

    logger.info("Revert Date " + revertDate);
    logger.info("instance name " + instance);
    int i = 0;

    BufferedReader br = null;
    Connection connectionAmbariDatabase = null;

    try {
      connectionAmbariDatabase = DataSourceAmbariDatabase.getInstance(view.getProperties().get("ambaridrivername"), view.getProperties().get("ambarijdbcurl"), view.getProperties().get("ambaridbusername"), view.getProperties().get("ambaridbpassword")).getConnection();
      connectionAmbariDatabase.setAutoCommit(false);

      Statement stmt = null;
      stmt = connectionAmbariDatabase.createStatement();
      SAXBuilder builder = new SAXBuilder();
      File xmlFile = new File(ConfigurationCheckImplementation.getHomeDir() + "RevertChangesService.xml");
      try {

        Document document = (Document) builder.build(xmlFile);
        Element rootNode = document.getRootElement();
        List list = rootNode.getChildren("RevertRecord");
        logger.info("list size is = "+list.size());
        for (i = 0; i < list.size(); i++) {

          float calc = ((float) (i + 1)) / list.size() * 100;
          int progressPercentage = Math.round(calc);

          migrationresult.setIsNoQuerySelected("yes");
          migrationresult.setProgressPercentage(progressPercentage);
          migrationresult.setNumberOfQueryTransfered(i+1);
          migrationresult.setTotalNoQuery(list.size());

          getResourceManager(view).update(migrationresult, jobid);

          Element node = (Element) list.get(i);

          if (node.getChildText("instance").equals(instance)) {
            logger.info("instance matched");

            if (stringtoDatecompare(revertDate, node.getChildText("datetime").toString())) {
              logger.info("date is less query is sucess");
              String sql = node.getChildText("query");
              logger.info(sql);
              stmt.executeUpdate(sql);
              removedir(node.getChildText("dirname").toString(), view.getProperties().get("namenode_URI_Ambari"));
              logger.info(node.getChildText("dirname").toString() + " deleted");

            }
            else {
              logger.info("date is big query is failed");
            }

          }

        }

        connectionAmbariDatabase.commit();








        logger.info("------------------------------");
        logger.info("Reverting the changes End");
        logger.info("------------------------------");

      } catch (IOException e) {
        logger.error("IOException: ", e);
      } catch (ParseException e) {
        logger.error("ParseException: ", e);
      } catch (JDOMException e) {
        logger.error("JDOMException: ", e);
      } catch (URISyntaxException e) {
        logger.error("URISyntaxException:  ", e);
      }
    } catch (SQLException e1) {
      logger.error("SqlException  ", e1);
      try {
        connectionAmbariDatabase.rollback();
        logger.info("Rollback done");
      } catch (SQLException e2) {
        logger.error("SqlException in Rollback  ", e2);
      }
    } catch (PropertyVetoException e) {
      logger.error("PropertyVetoException: ", e);
    }

    long stopTime = System.currentTimeMillis();
    long elapsedTime = stopTime - startTime;

    MigrationModel model = new MigrationModel();
//    model.setInstanceName(instance);
//    model.setNumberofQueryTransfered(i + 1);
//    model.setTimeTakentotransfer(String.valueOf(elapsedTime));

    return model;
  }


}
