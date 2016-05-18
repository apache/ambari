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


package org.apache.ambari.view.huetoambarimigration.controller.pig;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.huetoambarimigration.controller.configurationcheck.ProgressBarStatus;
import org.apache.log4j.Logger;

import org.apache.ambari.view.huetoambarimigration.datasource.DataSourceAmbariDatabase;
import org.apache.ambari.view.huetoambarimigration.datasource.DataSourceHueDatabase;
import org.apache.ambari.view.huetoambarimigration.model.*;
import org.apache.ambari.view.huetoambarimigration.service.configurationcheck.ConfFileReader;
import org.apache.ambari.view.huetoambarimigration.service.pig.PigScriptImpl;

public class PigScriptMigration extends HttpServlet {


  private static final long serialVersionUID = 1031422249396784970L;
  ViewContext view;
  private String startDate;
  private String endDate;
  private String instance;
  private String userName;

  @Override
  public void init(ServletConfig config) throws ServletException {

    super.init(config);
    ServletContext context = config.getServletContext();
    view = (ViewContext) context.getAttribute(ViewContext.CONTEXT_ATTRIBUTE);

  }

  public void doGet(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException {

    HttpSession session = req.getSession(true);
    final Logger logger = Logger.getLogger(PigScriptMigration.class);
    Connection connectionHuedb = null;
    Connection connectionAmbaridb = null;

    logger.info("-------------------------------------");
    logger.info("Pig saved script Migration started");
    logger.info("-------------------------------------");

    //fethcing data from client

    userName = req.getParameter("username");
    startDate = req.getParameter("startdate");
    endDate = req.getParameter("enddate");
    instance = req.getParameter("instance");
    int i = 0;

    logger.info("start date: " + startDate);
    logger.info("enddate date: " + endDate);
    logger.info("instance is: " + userName);
    logger.info("hue username is : " + instance);

    //Reading the configuration file
    PigScriptImpl pigsavedscriptmigration = new PigScriptImpl();

    int maxcountforsavequery = 0, maxcountforpigsavedscript;
    String time = null, timetobeInorder = null;
    Long epochTime = null;
    String dirNameForPigScript, completeDirandFilePath, pigscriptFilename="";
    int pigInstanceTableName;

    ArrayList<PojoPig> dbpojoPigSavedscript = new ArrayList<PojoPig>();

    try {
      connectionHuedb = DataSourceHueDatabase.getInstance(view.getProperties().get("huedrivername"), view.getProperties().get("huejdbcurl"), view.getProperties().get("huedbusername"), view.getProperties().get("huedbpassword")).getConnection();//connection to Hue DB
      dbpojoPigSavedscript = pigsavedscriptmigration.fetchFromHueDatabase(userName, startDate, endDate, connectionHuedb, view.getProperties().get("huedrivername"));// Fetching Pig script details from Hue DB

      /* If No Pig Script has been fetched from Hue db according to our search criteria*/
      if (dbpojoPigSavedscript.size() == 0) {

        logger.info("no Pig script has been selected from hue according to your criteria of searching");
        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();
        out.println("<br>");
        out.println("<h4>No Pig Script selected according to your criteria</h4>");

      } else {

        connectionAmbaridb = DataSourceAmbariDatabase.getInstance(view.getProperties().get("ambaridrivername"), view.getProperties().get("ambarijdbcurl"), view.getProperties().get("ambaridbusername"), view.getProperties().get("ambaridbpassword")).getConnection();// connecting to ambari db
        connectionAmbaridb.setAutoCommit(false);
        logger.info("loop will continue for " + dbpojoPigSavedscript.size() + "times");

        //for each pig script found in Hue Database

        for (i = 0; i < dbpojoPigSavedscript.size(); i++) {


          float calc = ((float) (i + 1)) / dbpojoPigSavedscript.size() * 100;
          int progressPercentage = Math.round(calc);

          session.setAttribute(ProgressBarStatus.TASK_PROGRESS_VARIABLE, progressPercentage);

          logger.info("Loop No." + (i + 1));
          logger.info("________________");
          logger.info("the title of script:  " + dbpojoPigSavedscript.get(i).getTitle());

          pigInstanceTableName = pigsavedscriptmigration.fetchInstanceTablenamePigScript(view.getProperties().get("ambaridrivername"), connectionAmbaridb, instance);// finding the table name in ambari from the given instance

          maxcountforpigsavedscript = (pigsavedscriptmigration.fetchmaxIdforPigSavedScript(view.getProperties().get("ambaridrivername"), connectionAmbaridb, pigInstanceTableName) + 1);// maximum count of the primary key of Pig Script table

          time = pigsavedscriptmigration.getTime();

          timetobeInorder = pigsavedscriptmigration.getTimeInorder();

          epochTime = pigsavedscriptmigration.getEpochTime();

          dirNameForPigScript = "/user/admin/pig/scripts/";

          pigscriptFilename = dbpojoPigSavedscript.get(i).getTitle() + "-" + time + ".pig";

          completeDirandFilePath = dirNameForPigScript + pigscriptFilename;

          pigsavedscriptmigration.writetPigScripttoLocalFile(dbpojoPigSavedscript.get(i).getScript(), dbpojoPigSavedscript.get(i).getTitle(), dbpojoPigSavedscript.get(i).getDt(), ConfFileReader.getHomeDir(), pigscriptFilename);

          pigsavedscriptmigration.insertRowForPigScript(view.getProperties().get("ambaridrivername"), completeDirandFilePath, maxcountforsavequery, maxcountforpigsavedscript, time, timetobeInorder, epochTime, dbpojoPigSavedscript.get(i).getTitle(), connectionAmbaridb, pigInstanceTableName, instance, i);

          if (view.getProperties().get("KerberoseEnabled").equals("y")) {
            pigsavedscriptmigration.putFileinHdfsSecured(ConfFileReader.getHomeDir() + pigscriptFilename, dirNameForPigScript, view.getProperties().get("namenode_URI_Ambari"));
          } else {
            pigsavedscriptmigration.putFileinHdfs(ConfFileReader.getHomeDir() + pigscriptFilename, dirNameForPigScript, view.getProperties().get("namenode_URI_Ambari"));
          }

          logger.info(dbpojoPigSavedscript.get(i).getTitle() + "Migrated to Ambari");

          pigsavedscriptmigration.deletePigScriptLocalFile(ConfFileReader.getHomeDir(), pigscriptFilename);

        }
        connectionAmbaridb.commit();

      }


    } catch (SQLException e) {
      logger.error("Sql exception in ambari database", e);
      try {
        connectionAmbaridb.rollback();
        logger.info("rollback done");
      } catch (SQLException e1) {
        logger.error("Sql exception while doing roll back", e);
      }
    } catch (ClassNotFoundException e2) {
      logger.error("class not found exception", e2);
    } catch (ParseException e) {
      logger.error("ParseException: " , e);
    } catch (PropertyVetoException e) {
      logger.error("PropertyVetoException: " , e);
    } finally {
      if (null != connectionAmbaridb)
        try {
          connectionAmbaridb.close();
        } catch (SQLException e) {
          logger.error("connection close exception: ", e);
        }
    }

    session.setAttribute(ProgressBarStatus.TASK_PROGRESS_VARIABLE, 0);

    resp.setContentType("text/html");
    PrintWriter out = resp.getWriter();
    out.println("<br>");
    out.println("<h4>" + i + " Pig Script has been migrated to " + instance + "</h4>");

    logger.info("----------------------------------");
    logger.info("Pig saved script Migration ends");
    logger.info("----------------------------------");
  }


}
