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
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

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
import org.apache.ambari.view.huetoambarimigration.service.*;
import org.apache.ambari.view.huetoambarimigration.model.*;
import org.apache.ambari.view.huetoambarimigration.service.configurationcheck.ConfFileReader;
import org.apache.ambari.view.huetoambarimigration.service.pig.PigJobImpl;

public class PigJobMigration extends HttpServlet {

  private static final long serialVersionUID = 1031422249396784970L;
  ViewContext view;
  int i = 0;
  private String userName;
  private String startDate;
  private String endDate;
  private String instance;

  @Override
  public void init(ServletConfig config) throws ServletException {

    super.init(config);
    ServletContext context = config.getServletContext();
    view = (ViewContext) context.getAttribute(ViewContext.CONTEXT_ATTRIBUTE);

  }

  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

    HttpSession session = req.getSession(true);
    final Logger logger = Logger.getLogger(PigJobMigration.class);
    Connection connectionHuedb = null;
    Connection connectionAmbaridb = null;

    // fetchinf data from the clients
    userName = req.getParameter("username");
    startDate = req.getParameter("startdate");
    endDate = req.getParameter("enddate");
    instance = req.getParameter("instance");

    logger.info("------------------------------");
    logger.info("Pig Jobs Migration started");
    logger.info("------------------------------");
    logger.info("start date: " + startDate);
    logger.info("enddate date: " + endDate);
    logger.info("instance is: " + userName);
    logger.info("hue username is : " + instance);

    PigJobImpl pigjobimpl = new PigJobImpl();// creating the implementation object
    int maxCountforPigScript = 0;

    String time = null, timeIndorder = null;
    Long epochtime = null;
    String pigJobDirName;
    ArrayList<PojoPig> pigJobDbPojo = new ArrayList<PojoPig>();

    try {

      connectionHuedb = DataSourceHueDatabase.getInstance(view.getProperties().get("huedrivername"), view.getProperties().get("huejdbcurl"), view.getProperties().get("huedbusername"), view.getProperties().get("huedbpassword")).getConnection();//connecting to hue database
      pigJobDbPojo = pigjobimpl.fetchFromHueDB(userName, startDate, endDate, connectionHuedb);// fetching the PigJobs details from hue

			/*No Pig Job details has been fetched accordring to search criteria*/
      if (pigJobDbPojo.size() == 0) {

        logger.info("no Pig Job has been selected from hue according to your criteria of searching");
        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();
        out.println("<br>");
        out.println("<h4>No Pig Job  selected according to your criteria</h4>");

      } else {

        connectionAmbaridb = DataSourceAmbariDatabase.getInstance(view.getProperties().get("ambaridrivername"), view.getProperties().get("ambarijdbcurl"), view.getProperties().get("ambaridbusername"), view.getProperties().get("ambaridbpassword")).getConnection();
        connectionAmbaridb.setAutoCommit(false);

        for (i = 0; i < pigJobDbPojo.size(); i++) {

          float calc = ((float) (i + 1)) / pigJobDbPojo.size() * 100;
          int progressPercentage = Math.round(calc);

          session.setAttribute(ProgressBarStatus.TASK_PROGRESS_VARIABLE, progressPercentage);

          logger.info("Loop No." + (i + 1));
          logger.info("________________");
          logger.info("the title of script " + pigJobDbPojo.get(i).getTitle());

          int fetchPigTablenameInstance = pigjobimpl.fetchInstanceTablename(view.getProperties().get("ambaridrivername"), connectionAmbaridb, instance);
          maxCountforPigScript = (pigjobimpl.fetchMaxIdforPigJob(view.getProperties().get("ambaridrivername"), connectionAmbaridb, fetchPigTablenameInstance) + 1);

          time = pigjobimpl.getTime();
          timeIndorder = pigjobimpl.getTimeInorder();
          epochtime = pigjobimpl.getEpochTime();

          pigJobDirName = "/user/admin/pig/jobs/" + pigJobDbPojo.get(i).getTitle() + "_" + time + "/";

          pigjobimpl.insertRowPigJob(view.getProperties().get("ambaridrivername"), pigJobDirName, maxCountforPigScript, time, timeIndorder, epochtime, pigJobDbPojo.get(i).getTitle(), connectionAmbaridb, fetchPigTablenameInstance, pigJobDbPojo.get(i).getStatus(), instance, i);

          if (view.getProperties().get("KerberoseEnabled").equals("y")) {

            pigjobimpl.createDirPigJobSecured(pigJobDirName, view.getProperties().get("namenode_URI_Ambari"));
            pigjobimpl.copyFileBetweenHdfsSecured(pigJobDbPojo.get(i).getDir() + "/script.pig", pigJobDirName, view.getProperties().get("namenode_URI_Ambari"), view.getProperties().get("namenode_URI_Hue"));
            pigjobimpl.copyFileBetweenHdfsSecured(pigJobDbPojo.get(i).getDir() + "/stderr", pigJobDirName, view.getProperties().get("namenode_URI_Ambari"), view.getProperties().get("namenode_URI_Hue"));
            pigjobimpl.copyFileBetweenHdfsSecured(pigJobDbPojo.get(i).getDir() + "/stdout", pigJobDirName, view.getProperties().get("namenode_URI_Ambari"), view.getProperties().get("namenode_URI_Hue"));

          } else {

            pigjobimpl.createDirPigJob(pigJobDirName, view.getProperties().get("namenode_URI_Ambari"));
            pigjobimpl.copyFileBetweenHdfs(pigJobDbPojo.get(i).getDir() + "/script.pig", pigJobDirName, view.getProperties().get("namenode_URI_Ambari"), view.getProperties().get("namenode_URI_Hue"));
            pigjobimpl.copyFileBetweenHdfs(pigJobDbPojo.get(i).getDir() + "/stderr", pigJobDirName, view.getProperties().get("namenode_URI_Ambari"), view.getProperties().get("namenode_URI_Hue"));
            pigjobimpl.copyFileBetweenHdfs(pigJobDbPojo.get(i).getDir() + "/stdout", pigJobDirName, view.getProperties().get("namenode_URI_Ambari"), view.getProperties().get("namenode_URI_Hue"));

          }

          logger.info(pigJobDbPojo.get(i).getTitle() + "has been migrated to Ambari");

        }
        connectionAmbaridb.commit();
      }

    } catch (SQLException e) {
      logger.error("sql exception in ambari database:", e);
      try {
        connectionAmbaridb.rollback();
        logger.info("roll back done");
      } catch (SQLException e1) {
        logger.error("roll back  exception:",e1);
      }
    } catch (ClassNotFoundException e2) {
      logger.error("class not found exception:",e2);
    } catch (ParseException e) {
      logger.error("ParseException: " ,e);
    } catch (URISyntaxException e) {
      logger.error("URISyntaxException" ,e);
    } catch (PropertyVetoException e) {
      logger.error("PropertyVetoException" ,e);
    } finally {
      if (null != connectionAmbaridb)
        try {
          connectionAmbaridb.close();
        } catch (SQLException e) {
          logger.error("connection closing exception ", e);
        }
    }

    logger.info("------------------------------");
    logger.info("Pig Job Migration End");
    logger.info("------------------------------");

    session.setAttribute(ProgressBarStatus.TASK_PROGRESS_VARIABLE, 0);
    resp.setContentType("text/html");
    PrintWriter out = resp.getWriter();
    out.println("<br>");
    out.println("<h4>" + i + " Pig jobs has been migrated to  "
      + instance + "</h4>");
  }

}

