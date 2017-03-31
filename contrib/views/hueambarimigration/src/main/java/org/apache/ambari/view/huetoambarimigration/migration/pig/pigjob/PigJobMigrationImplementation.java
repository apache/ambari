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

import java.security.PrivilegedExceptionAction;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URI;
;
import org.apache.ambari.view.huetoambarimigration.datasource.queryset.ambariqueryset.pig.jobqueryset.QuerySetAmbariDB;
import org.apache.ambari.view.huetoambarimigration.datasource.queryset.huequeryset.pig.jobqueryset.QuerySetHueDb;
import org.apache.ambari.view.huetoambarimigration.resources.scripts.models.PigModel;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.log4j.Logger;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import org.apache.ambari.view.huetoambarimigration.migration.configuration.ConfigurationCheckImplementation;

public class PigJobMigrationImplementation {

  static final Logger logger = Logger.getLogger(PigJobMigrationImplementation.class);
  final String USER_DIRECTORY = "/user";

  private static String readAll(Reader rd) throws IOException {
    StringBuilder sb = new StringBuilder();
    int cp;
    while ((cp = rd.read()) != -1) {
      sb.append((char) cp);
    }
    return sb.toString();
  }

  public void wrtitetoalternatesqlfile(String dirname, String content, String instance, int i) throws IOException {
    Date dNow = new Date();
    SimpleDateFormat ft = new SimpleDateFormat("YYYY-MM-dd hh:mm:ss");
    String currentDate = ft.format(dNow);
    XMLOutputter xmlOutput = new XMLOutputter();
    xmlOutput.setFormat(Format.getPrettyFormat());
    File xmlfile = new File(ConfigurationCheckImplementation.getHomeDir() + "RevertChangesService.xml");
    if (xmlfile.exists()) {
      String iteration = Integer.toString(i + 1);
      SAXBuilder builder = new SAXBuilder();
      Document doc;
      try {
        doc = (Document) builder.build(xmlfile);
        Element rootNode = doc.getRootElement();
        Element record = new Element("RevertRecord");
        record.setAttribute(new Attribute("id", iteration));
        record.addContent(new Element("datetime").setText(currentDate.toString()));
        record.addContent(new Element("dirname").setText(dirname));
        record.addContent(new Element("instance").setText(instance));
        record.addContent(new Element("query").setText(content));
        rootNode.addContent(record);
        xmlOutput.output(doc, new FileWriter(ConfigurationCheckImplementation.getHomeDir() + "RevertChangesService.xml"));
      } catch (JDOMException e) {

        logger.error("Jdom Exception: ", e);
      }


    } else {
      // create
      try {
        String iteration = Integer.toString(i + 1);
        Element revertrecord = new Element("RevertChangePage");
        Document doc = new Document(revertrecord);
        doc.setRootElement(revertrecord);
        Element record = new Element("RevertRecord");
        record.setAttribute(new Attribute("id", iteration));
        record.addContent(new Element("datetime").setText(currentDate.toString()));
        record.addContent(new Element("dirname").setText(dirname));
        record.addContent(new Element("instance").setText(instance));
        record.addContent(new Element("query").setText(content));
        doc.getRootElement().addContent(record);
        xmlOutput.output(doc, new FileWriter(ConfigurationCheckImplementation.getHomeDir() + "RevertChangesService.xml"));
      } catch (IOException io) {
        logger.error("Jdom Exception: ", io);
      }

    }

  }

  public int fetchMaxIdforPigJob(Connection c, int id, QuerySetAmbariDB ambaridatabase) throws SQLException {


    String ds_id = null;
    ResultSet rs = null;
    PreparedStatement prSt = null;

    prSt = ambaridatabase.getMaxDsIdFromTableId(c, id);

    rs = prSt.executeQuery();

    while (rs.next()) {
      ds_id = rs.getString("max");
    }

    int num;
    if (ds_id == null) {
      num = 1;
    } else {
      num = Integer.parseInt(ds_id);
    }
    return num;

  }

  public int fetchSequenceno(Connection c, int id, QuerySetAmbariDB ambaridatabase) throws SQLException {

    String ds_id = new String();
    Statement stmt = null;
    PreparedStatement prSt = null;
    int sequencevalue=0;


    ResultSet rs = null;


    prSt = ambaridatabase.getSequenceNoFromAmbariSequence(c, id);

    logger.info("sql statement to fetch is from ambari instance:= =  " + prSt);

    rs = prSt.executeQuery();

    while (rs.next()) {
      sequencevalue = rs.getInt("sequence_value");
    }
    return sequencevalue;
  }

  public int fetchInstanceTablename(Connection c, String instance, QuerySetAmbariDB ambaridatabase) throws SQLException {


    String ds_id = new String();
    int id = 0;
    Statement stmt = null;
    PreparedStatement prSt = null;


    ResultSet rs = null;


    prSt = ambaridatabase.getTableIdFromInstanceName(c, instance);

    logger.info("sql statement to fetch is from ambari instance:= =  " + prSt);

    rs = prSt.executeQuery();

    while (rs.next()) {
      id = rs.getInt("id");
    }
    return id;
  }

  public void insertRowPigJob(String dirname, int maxcountforpigjob, String time, String time2, long epochtime, String title, Connection c, int id, String status, String instance, int i, QuerySetAmbariDB ambaridatabase,String username) throws SQLException, IOException {

    String epochtime1 = Long.toString(epochtime);
    String maxcountforpigjob1 = Integer.toString(maxcountforpigjob);
    String ds_id = new String();
    String revSql;

    PreparedStatement prSt = null;

    prSt = ambaridatabase.insertToPigJob(dirname, maxcountforpigjob1, epochtime, title, c, id, status,username);

    prSt.executeUpdate();

    revSql = ambaridatabase.revertSql(id, maxcountforpigjob1);

    wrtitetoalternatesqlfile(dirname, revSql, instance, i);

  }

  public long getEpochTime() throws ParseException {
    int day, month, year;
    int second, minute, hour;
    int milisecond;
    GregorianCalendar date = new GregorianCalendar();

    day = date.get(Calendar.DAY_OF_MONTH);
    month = date.get(Calendar.MONTH);
    year = date.get(Calendar.YEAR);

    second = date.get(Calendar.SECOND);
    minute = date.get(Calendar.MINUTE);
    hour = date.get(Calendar.HOUR);
    milisecond = date.get(Calendar.MILLISECOND);
    String s1 = year + "-" + (month + 1) + "-" + day + "_" + hour + "-" + minute + "-" + second + "-" + milisecond;
    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS");
    Date date1 = df.parse(s1);
    long epoch = date1.getTime();
    return epoch;

  }

  public String getTime() throws ParseException {
    int day, month, year;
    int second, minute, hour;
    int milisecond;
    GregorianCalendar date = new GregorianCalendar();

    day = date.get(Calendar.DAY_OF_MONTH);
    month = date.get(Calendar.MONTH);
    year = date.get(Calendar.YEAR);

    second = date.get(Calendar.SECOND);
    minute = date.get(Calendar.MINUTE);
    hour = date.get(Calendar.HOUR);
    milisecond = date.get(Calendar.MILLISECOND);
    String s = year + "-" + (month + 1) + "-" + day + "_" + hour + "-" + minute;
    String s1 = year + "-" + (month + 1) + "-" + day + "_" + hour + "-" + minute + "-" + second + "-" + milisecond;
    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS");
    Date date1 = df.parse(s1);
    long epoch = date1.getTime();
    return s;

  }

  public String getTimeInorder() throws ParseException {
    SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.msssss +00:00:00");//dd/MM/yyyy
    Date now = new Date();
    String strDate = sdfDate.format(now);
    return strDate;
  }

  public ArrayList<PigModel> fetchFromHueDB(String username, String startdate, String endtime, Connection connection, QuerySetHueDb huedatabase) throws ClassNotFoundException, SQLException, IOException {
    int id = 0;
    int i = 0;
    String[] query = new String[100];
    ArrayList<PigModel> pigjobarraylist = new ArrayList<PigModel>();
    try {
      connection.setAutoCommit(false);
      PreparedStatement prSt = null;
      Statement statement = connection.createStatement();
      ResultSet rs;
      String ownerName = "";
      int ownerId;

      ResultSet rs1 = null;
      if (username.equals("all")) {
      } else {

        prSt = huedatabase.getUseridfromUserName(connection, username);

        rs = prSt.executeQuery();

        while (rs.next()) {
          id = rs.getInt("id");
        }
      }

      if (startdate.equals("") && endtime.equals("")) {
        if (username.equals("all")) {
          prSt = huedatabase.getQueriesNoStartDateNoEndDateAllUser(connection);
        } else {
          prSt = huedatabase.getQueriesNoStartDateNoEndDate(connection, id);

        }

      } else if ((startdate.equals("")) && !(endtime.equals(""))) {
        if (username.equals("all")) {
          prSt = huedatabase.getQueriesNoStartDateYesEndDateAllUser(connection, endtime);
        } else {
          prSt = huedatabase.getQueriesNoStartDateYesEndDate(connection, id, endtime);

        }
      } else if (!(startdate.equals("")) && (endtime.equals(""))) {
        if (username.equals("all")) {
          prSt = huedatabase.getQueriesYesStartDateNoEndDateAllUser(connection, startdate);
        } else {
          prSt = huedatabase.getQueriesYesStartDateNoEndDate(connection, id, startdate);

        }

      } else if (!(startdate.equals("")) && !(endtime.equals(""))) {
        if (username.equals("all")) {
          prSt = huedatabase.getQueriesYesStartDateYesEndDateAllUser(connection, startdate, endtime);
        } else {
          prSt = huedatabase.getQueriesYesStartDateYesEndDate(connection, id, startdate, endtime);
        }


      }

      rs1 = prSt.executeQuery();


      while (rs1.next()) {
        PigModel pigjjobobject = new PigModel();
        ownerId = rs1.getInt("user_id");
        if(username.equals("all")) {
          prSt = huedatabase.getUserName(connection, ownerId);
          ResultSet resultSet = prSt.executeQuery();
          while(resultSet.next()) {
            ownerName = resultSet.getString("username");
          }
        }

        int runstatus = rs1.getInt("status");

        if (runstatus == 1) {
          pigjjobobject.setStatus("RUNNING");
        } else if (runstatus == 2) {
          pigjjobobject.setStatus("SUCCEEDED");
        } else if (runstatus == 3) {
          pigjjobobject.setStatus("SUBMIT_FAILED");
        } else if (runstatus == 4) {
          pigjjobobject.setStatus("KILLED");
        }
        String title = rs1.getString("script_title");


        pigjjobobject.setUserName(ownerName);
        pigjjobobject.setTitle(title);
        String dir = rs1.getString("statusdir");
        pigjjobobject.setDir(dir);
        Date created_data = rs1.getDate("start_time");
        pigjjobobject.setDt(created_data);

        pigjobarraylist.add(pigjjobobject);

        i++;
      }
      connection.commit();

    } catch (SQLException e) {
      logger.error("Sqlexception: ", e);
      connection.rollback();
    } finally {
      try {
        if (connection != null)
          connection.close();
      } catch (SQLException e) {
        logger.error("Sqlexception in closing the connection: ", e);

      }
    }

    return pigjobarraylist;

  }

  public void updateSequenceno(Connection c, int seqNo, int id, QuerySetAmbariDB ambaridatabase) throws SQLException, IOException {

    PreparedStatement prSt;

    prSt = ambaridatabase.updateSequenceNoInAmbariSequence(c, seqNo, id);

    logger.info("The actual insert statement is " + prSt);

    prSt.executeUpdate();


    logger.info("adding revert sql hive history");



  }
  public void createDirPigJob(final String dir, final String namenodeuri,final String username) throws IOException,
    URISyntaxException {

    try {
      UserGroupInformation ugi = UserGroupInformation
        .createRemoteUser("hdfs");

      ugi.doAs(new PrivilegedExceptionAction<Void>() {

        public Void run() throws Exception {

          Configuration conf = new Configuration();
          conf.set("fs.hdfs.impl",
            org.apache.hadoop.hdfs.DistributedFileSystem.class.getName()
          );
          conf.set("fs.file.impl",
            org.apache.hadoop.fs.LocalFileSystem.class.getName()
          );
          conf.set("fs.defaultFS", namenodeuri);
          conf.set("hadoop.job.ugi", "hdfs");

          URI uri = new URI(dir);
          FileSystem fs = FileSystem.get(conf);
          Path src = new Path(dir);
          fs.mkdirs(src);

          String[] subDirs = dir.split("/");
          String dirPath = USER_DIRECTORY;
          for(int i=2;i<subDirs.length;i++) {
            dirPath += "/"+subDirs[i];
            fs.setOwner(new Path(dirPath), username, username);
          }
          return null;
        }
      });
    } catch (Exception e) {
      logger.error("Webhdfs exception: ", e);
    }
  }

  /**/
  public void createDirPigJobSecured(final String dir, final String namenodeuri,final String username,final String principalName) throws IOException,
    URISyntaxException {
    try {
      final Configuration conf = new Configuration();

      conf.set("fs.hdfs.impl",
        org.apache.hadoop.hdfs.DistributedFileSystem.class.getName()
      );
      conf.set("fs.file.impl",
        org.apache.hadoop.fs.LocalFileSystem.class.getName()
      );
      conf.set("fs.defaultFS", namenodeuri);
      conf.set("hadoop.security.authentication", "Kerberos");
      UserGroupInformation.setConfiguration(conf);
      UserGroupInformation proxyUser ;
      proxyUser = UserGroupInformation.createRemoteUser(principalName);
      UserGroupInformation ugi = UserGroupInformation.createProxyUser("hdfs", proxyUser);
      ugi.doAs(new PrivilegedExceptionAction<Boolean>() {

        public Boolean run() throws Exception {
          URI uri = new URI(dir);
          FileSystem fs = FileSystem.get(conf);
          Path src = new Path(dir);
          Boolean b = fs.mkdirs(src);

          String[] subDirs = dir.split("/");
          String dirPath = USER_DIRECTORY;
          for(int i=2;i<subDirs.length;i++) {
            dirPath += "/"+subDirs[i];
            fs.setOwner(new Path(dirPath), username, username);
          }
          return b;
        }
      });
    } catch (Exception e) {
      logger.error("Exception in Webhdfs", e);
    }
  }

  /**/
  public void copyFileBetweenHdfs(final String source, final String dest, final String nameNodeuriAmbari, final String nameNodeuriHue,final String username)
    throws IOException {

    try {
      UserGroupInformation ugi = UserGroupInformation
        .createRemoteUser("hdfs");

      ugi.doAs(new PrivilegedExceptionAction<Void>() {

        public Void run() throws Exception {

          Configuration confAmbari = new Configuration();
          confAmbari.set("fs.defaultFS", nameNodeuriAmbari);
          confAmbari.set("hadoop.job.ugi", "hdfs");
          FileSystem fileSystemAmbari = FileSystem.get(confAmbari);

          Configuration confHue = new Configuration();
          confHue.set("fs.defaultFS", nameNodeuriAmbari);
          confHue.set("hadoop.job.ugi", "hdfs");
          FileSystem fileSystemHue = FileSystem.get(confHue);

          String filename = source.substring(
            source.lastIndexOf('/') + 1, source.length());
          String dest1;
          if (dest.charAt(dest.length() - 1) != '/') {
            dest1 = dest + "/" + filename;
          } else {
            dest1 = dest + filename;
          }

          Path path1 = new Path(source);
          if(!fileSystemHue.exists(path1)) {
            FSDataOutputStream out = fileSystemHue.create(path1);
            out.close();
          }
          FSDataInputStream in1 = fileSystemHue.open(path1);

          Path path = new Path(dest1);
          if (fileSystemAmbari.exists(path)) {
            fileSystemAmbari.delete(path, true);
          }

          FSDataOutputStream out = fileSystemAmbari.create(path);

          byte[] b = new byte[1024];
          int numBytes = 0;
          while ((numBytes = in1.read(b)) > 0) {
            out.write(b, 0, numBytes);
          }
          in1.close();
          out.close();
          fileSystemAmbari.setOwner(path, username, username);
          fileSystemHue.close();
          fileSystemAmbari.close();
          return null;
        }
      });
    } catch (Exception e) {
      logger.error("Webhdfs exception: ", e);
    }

  }

  /**/
  public void copyFileBetweenHdfsSecured(final String source, final String dest, final String nameNodeuriAmbari, final String nameNodeuriHue,final String username,final String pricipalName)
    throws IOException {

    try {

      final Configuration confAmbari = new Configuration();
      confAmbari.set("fs.defaultFS", nameNodeuriAmbari);
      confAmbari.set("hadoop.job.ugi", "hdfs");

      final Configuration confHue = new Configuration();
      confHue.set("fs.defaultFS", nameNodeuriAmbari);
      confHue.set("hadoop.job.ugi", "hdfs");

      confAmbari.set("hadoop.security.authentication", "Kerberos");
      confHue.set("hadoop.security.authentication", "Kerberos");

      UserGroupInformation.setConfiguration(confAmbari);
      UserGroupInformation proxyUser ;
      proxyUser = UserGroupInformation.createRemoteUser(pricipalName);
      UserGroupInformation ugi = UserGroupInformation.createProxyUser("hdfs", proxyUser);

      ugi.doAs(new PrivilegedExceptionAction<Void>() {

        public Void run() throws Exception {

          FileSystem fileSystemAmbari = FileSystem.get(confAmbari);

          FileSystem fileSystemHue = FileSystem.get(confHue);

          String filename = source.substring(
            source.lastIndexOf('/') + 1, source.length());
          String dest1;
          if (dest.charAt(dest.length() - 1) != '/') {
            dest1 = dest + "/" + filename;
          } else {
            dest1 = dest + filename;
          }

          Path path1 = new Path(source);
          if(!fileSystemHue.exists(path1)) {
            FSDataOutputStream out = fileSystemHue.create(path1);
            out.close();
          }
          FSDataInputStream in1 = fileSystemHue.open(path1);

          Path path = new Path(dest1);
          if (fileSystemAmbari.exists(path)) {
            fileSystemAmbari.delete(path, true);
          }
          FSDataOutputStream out = fileSystemAmbari.create(path);
          byte[] b = new byte[1024];
          int numBytes = 0;
          while ((numBytes = in1.read(b)) > 0) {
            out.write(b, 0, numBytes);
          }
          in1.close();
          out.close();
          fileSystemAmbari.setOwner(path, username, username);
          fileSystemHue.close();
          fileSystemAmbari.close();
          return null;
        }
      });
    } catch (Exception e) {
      logger.error("Webhdfs exception: ", e);
    }

  }

}
