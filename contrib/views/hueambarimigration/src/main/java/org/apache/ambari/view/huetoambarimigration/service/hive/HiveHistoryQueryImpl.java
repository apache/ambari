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

package org.apache.ambari.view.huetoambarimigration.service.hive;

import java.net.URISyntaxException;
import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Scanner;

import org.apache.ambari.view.huetoambarimigration.service.configurationcheck.ConfFileReader;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.UserGroupInformation.AuthenticationMethod;
import org.apache.log4j.Logger;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

public class HiveHistoryQueryImpl {

  static final Logger logger = Logger.getLogger(HiveHistoryQueryImpl.class);

  public void wrtitetoalternatesqlfile(String dirname, String content, String instance, int i) throws IOException {

    Date dNow = new Date();
    SimpleDateFormat ft = new SimpleDateFormat("YYYY-MM-dd hh:mm:ss");
    String currentDate = ft.format(dNow);

    XMLOutputter xmlOutput = new XMLOutputter();
    xmlOutput.setFormat(Format.getPrettyFormat());

    File xmlfile = new File("/var/lib/huetoambari/RevertChange.xml");

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
        xmlOutput.output(doc, new FileWriter(ConfFileReader.getHomeDir() + "RevertChange.xml"));

      } catch (JDOMException e) {
        logger.error("JDOMException" ,e);

      }

    } else {

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
        xmlOutput.output(doc, new FileWriter(ConfFileReader.getHomeDir() + "RevertChange.xml"));
      } catch (IOException io) {
        logger.error("JDOMException" , io);
      }

    }

  }

  public int fetchMaximumIdfromAmbaridb(String driverName, Connection c, int id) throws SQLException {

    String ds_id = null;
    Statement stmt = null;
    stmt = c.createStatement();
    ResultSet rs = null;

    if (driverName.contains("postgresql")) {
      rs = stmt.executeQuery("select MAX(cast(ds_id as integer)) as max from ds_jobimpl_" + id + ";");
    } else if (driverName.contains("mysql")) {
      rs = stmt.executeQuery("select max( cast(ds_id as unsigned) ) as max from DS_JOBIMPL_" + id + ";");
    } else if (driverName.contains("oracle")) {
      rs = stmt.executeQuery("select MAX(cast(ds_id as integer)) as max from ds_jobimpl_" + id);
    }

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

  public void insertRowinAmbaridb(String driverName, String dirname, int maxcount, long epochtime, Connection c, int id, String instance, int i) throws SQLException, IOException {

    String maxcount1 = Integer.toString(maxcount);
    String epochtime1 = Long.toString(epochtime);
    String ds_id = new String();
    Statement stmt = null;
    String sql = "";
    String revsql = "";
    stmt = c.createStatement();

    if (driverName.contains("mysql")) {
      sql = "INSERT INTO DS_JOBIMPL_" + id + " values ('" + maxcount1
        + "','','','','','default'," + epochtime1 + ",0,'','','"
        + dirname + "logs','admin','" + dirname
        + "query.hql','','job','','','Unknown','" + dirname
        + "','','Worksheet');";
      revsql = "delete from  DS_JOBIMPL_" + id + " where ds_id='" + maxcount1 + "';";

    } else if (driverName.contains("postgresql")) {
      sql = "INSERT INTO ds_jobimpl_" + id + " values ('" + maxcount1
        + "','','','','','default'," + epochtime1 + ",0,'','','"
        + dirname + "logs','admin','" + dirname
        + "query.hql','','job','','','Unknown','" + dirname
        + "','','Worksheet');";
      revsql = "delete from  ds_jobimpl_" + id + " where ds_id='" + maxcount1 + "';";

    } else if (driverName.contains("oracle")) {
      sql = "INSERT INTO ds_jobimpl_" + id + " values ('" + maxcount1
        + "','','','','','default'," + epochtime1 + ",0,'','','"
        + dirname + "logs','admin','" + dirname
        + "query.hql','','job','','','Unknown','" + dirname
        + "','','Worksheet')";
      revsql = "delete from  ds_jobimpl_" + id + " where ds_id='" + maxcount1 + "'";

    }
    wrtitetoalternatesqlfile(dirname, revsql, instance, i);

    stmt.executeUpdate(sql);

  }

  public int fetchInstanceTablename(String driverName, Connection c, String instance) throws SQLException {

    String ds_id = new String();
    int id = 0;
    Statement stmt = null;
    stmt = c.createStatement();
    ResultSet rs = null;

    if (driverName.contains("oracle")) {
      rs = stmt.executeQuery("select id from viewentity where class_name LIKE 'org.apache.ambari.view.hive.resources.jobs.viewJobs.JobImpl' and view_instance_name='" + instance + "'");
    } else {
      rs = stmt.executeQuery("select id from viewentity where class_name LIKE 'org.apache.ambari.view.hive.resources.jobs.viewJobs.JobImpl' and view_instance_name='" + instance + "';");
    }

    while (rs.next()) {
      id = rs.getInt("id");
    }
    return id;
  }

  public long getEpochTime() throws ParseException {
    long seconds = System.currentTimeMillis() / 1000l;
    return seconds;

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

    String s = year + "-" + (month + 1) + "-" + day + "_" + hour + "-"
      + minute;
    String s1 = year + "-" + (month + 1) + "-" + day + "_" + hour + "-"
      + minute + "-" + second + "-" + milisecond;
    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS");
    Date date1 = df.parse(s1);
    long epoch = date1.getTime();
    return s;

  }

  public String[] fetchFromHue(String username, String startdate, String endtime, Connection connection) throws ClassNotFoundException, SQLException {
    int id = 0;
    int i = 0;
    String[] query = new String[100];

    try {
      connection.setAutoCommit(false);
      Statement statement = connection.createStatement();

      ResultSet rs1 = null;
      if (username.equals("all")) {
      } else {
        ResultSet rs = statement.executeQuery("select id from auth_user where username='" + username + "';");
        while (rs.next()) {
          id = rs.getInt("id");
        }
      }
      if (startdate.equals("") && endtime.equals("")) {
        if (username.equals("all")) {
          rs1 = statement.executeQuery("select query from beeswax_queryhistory;");
        } else {
          rs1 = statement.executeQuery("select query from beeswax_queryhistory where owner_id =" + id + ";");
        }

      } else if (!(startdate.equals("")) && !(endtime.equals(""))) {
        if (username.equals("all")) {
          rs1 = statement.executeQuery("select query from beeswax_queryhistory where submission_date >= date('" + startdate + "') AND submission_date < date('" + endtime + "');");
        } else {
          rs1 = statement.executeQuery("select query from beeswax_queryhistory where owner_id =" + id + " AND submission_date >= date('" + startdate + "') AND submission_date <= date('" + endtime + "');");
        }
      } else if (!(startdate.equals("")) && (endtime.equals(""))) {
        if (username.equals("all")) {
          rs1 = statement.executeQuery("select query from beeswax_queryhistory where submission_date >= date('" + startdate + "');");
        } else {
          rs1 = statement.executeQuery("select query from beeswax_queryhistory where owner_id =" + id + " AND submission_date >= date('" + startdate + "');");
        }

      } else if ((startdate.equals("")) && !(endtime.equals(""))) {
        if (username.equals("all")) {
          rs1 = statement.executeQuery("select query from beeswax_queryhistory where submission_date < date('" + endtime + "');");
        } else {
          rs1 = statement.executeQuery("select query from beeswax_queryhistory where owner_id =" + id + " AND submission_date < date('" + endtime + "');");
        }
      }


      while (rs1.next()) {
        query[i] = rs1.getString("query");
        i++;
      }

      connection.commit();

    } catch (SQLException e) {
      connection.rollback();

    } finally {
      try {
        if (connection != null)
          connection.close();
      } catch (SQLException e) {
        logger.error("Sql exception error: " + e);
      }
    }
    return query;

  }

  public void writetoFileQueryhql(String content, String homedir) {
    try {
      File file = new File(homedir + "query.hql");
      // if file doesnt exists, then create it
      if (!file.exists()) {
        file.createNewFile();
      }
      FileWriter fw = new FileWriter(file.getAbsoluteFile());
      BufferedWriter bw = new BufferedWriter(fw);
      bw.write(content);
      bw.close();
    } catch (IOException e) {
      logger.error("IOException" , e);
    }

  }

  public void deleteFileQueryhql(String homedir) {
    try{
      File file = new File(homedir + "query.hql");

      if(file.delete()){
        logger.info("temporary hql file deleted");
      }else{
        logger.info("temporary hql file delete failed");
      }

    }catch(Exception e){

     logger.error("File Exception ",e);

    }

  }

  public void deleteFileQueryLogs(String homedir) {
    try{
      File file = new File(homedir + "logs");

      if(file.delete()){
        logger.info("temporary logs file deleted");
      }else{
        logger.info("temporary logs file delete failed");
      }

    }catch(Exception e){

      logger.error("File Exception ",e);

    }

  }

  public void writetoFileLogs(String homedir) {
    try {
      String content = "";
      File file = new File(homedir + "logs");
      // if file doesnt exists, then create it
      if (!file.exists()) {
        file.createNewFile();
      }
      FileWriter fw = new FileWriter(file.getAbsoluteFile());
      BufferedWriter bw = new BufferedWriter(fw);
      bw.write(content);
      bw.close();
    } catch (IOException e) {
      logger.error("IOException" , e);
    }

  }

  public void createDir(final String dir, final String namenodeuri) throws IOException,
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
      conf.set("hadoop.job.ugi", "hdfs");
      UserGroupInformation.setConfiguration(conf);

      UserGroupInformation ugi = UserGroupInformation.createRemoteUser("hdfs");

      ugi.doAs(new PrivilegedExceptionAction<Boolean>() {

        public Boolean run() throws Exception {

          FileSystem fs = FileSystem.get(conf);
          Path src = new Path(dir);
          Boolean b = fs.mkdirs(src);
          return b;
        }
      });
    } catch (Exception e) {
      logger.error("Exception in Webhdfs" , e);
    }
  }

  public void createDirKerberorisedSecured(final String dir, final String namenodeuri) throws IOException,
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
      conf.set("hadoop.job.ugi", "hdfs");
      conf.set("hadoop.security.authentication", "Kerberos");
      UserGroupInformation.setConfiguration(conf);
      UserGroupInformation ugi = UserGroupInformation.createRemoteUser("hdfs");
      ugi.doAs(new PrivilegedExceptionAction<Boolean>() {

        public Boolean run() throws Exception {
          FileSystem fs = FileSystem.get(conf);
          Path src = new Path(dir);
          Boolean b = fs.mkdirs(src);
          return b;
        }
      });
    } catch (Exception e) {
      logger.error("Exception in Webhdfs" , e);
    }
  }


  public void putFileinHdfs(final String source, final String dest, final String namenodeuri)
    throws IOException {

    try {
      UserGroupInformation ugi = UserGroupInformation.createRemoteUser("hdfs");

      ugi.doAs(new PrivilegedExceptionAction<Void>() {

        public Void run() throws Exception {

          Configuration conf = new Configuration();
          conf.set("fs.defaultFS", namenodeuri);
          conf.set("hadoop.job.ugi", "hdfs");
          conf.set("fs.hdfs.impl",
            org.apache.hadoop.hdfs.DistributedFileSystem.class.getName()
          );
          conf.set("fs.file.impl",
            org.apache.hadoop.fs.LocalFileSystem.class.getName()
          );
          FileSystem fileSystem = FileSystem.get(conf);

          String filename = source.substring(
            source.lastIndexOf('/') + 1, source.length());
          String dest1;
          if (dest.charAt(dest.length() - 1) != '/') {
            dest1 = dest + "/" + filename;
          } else {
            dest1 = dest + filename;
          }

          Path path = new Path(dest1);
          if (fileSystem.exists(path)) {

          }
          //	Path pathsource = new Path(source);
          FSDataOutputStream out = fileSystem.create(path);

          InputStream in = new BufferedInputStream(
            new FileInputStream(new File(source)));

          byte[] b = new byte[1024];
          int numBytes = 0;
          while ((numBytes = in.read(b)) > 0) {
            out.write(b, 0, numBytes);
          }
          in.close();
          out.close();
          fileSystem.close();
          return null;
        }
      });
    } catch (Exception e) {
      logger.error("Webhdfs exception" , e);
    }

  }

  public void putFileinHdfsKerborizedSecured(final String source, final String dest, final String namenodeuri)
    throws IOException {

    try {

      final Configuration conf = new Configuration();

      conf.set("fs.hdfs.impl",
        org.apache.hadoop.hdfs.DistributedFileSystem.class.getName()
      );
      conf.set("fs.file.impl",
        org.apache.hadoop.fs.LocalFileSystem.class.getName()
      );
      conf.set("fs.defaultFS", namenodeuri);
      conf.set("hadoop.job.ugi", "hdfs");
      conf.set("hadoop.security.authentication", "Kerberos");
      UserGroupInformation.setConfiguration(conf);
      UserGroupInformation ugi = UserGroupInformation.createRemoteUser("hdfs");

      ugi.doAs(new PrivilegedExceptionAction<Void>() {

        public Void run() throws Exception {

          FileSystem fileSystem = FileSystem.get(conf);

          String filename = source.substring(
            source.lastIndexOf('/') + 1, source.length());
          String dest1;
          if (dest.charAt(dest.length() - 1) != '/') {
            dest1 = dest + "/" + filename;
          } else {
            dest1 = dest + filename;
          }

          Path path = new Path(dest1);
          if (fileSystem.exists(path)) {

          }

          FSDataOutputStream out = fileSystem.create(path);

          InputStream in = new BufferedInputStream(
            new FileInputStream(new File(source)));

          byte[] b = new byte[1024];
          int numBytes = 0;
          while ((numBytes = in.read(b)) > 0) {
            out.write(b, 0, numBytes);
          }
          in.close();
          out.close();
          fileSystem.close();
          return null;
        }
      });
    } catch (Exception e) {
      logger.error("Webhdfs exception" , e);

    }

  }

}
