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

import java.nio.charset.Charset;
import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Scanner;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;

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
import org.json.JSONArray;
import org.json.JSONObject;
import org.apache.hadoop.security.authentication.client.AuthenticatedURL;
import org.apache.hadoop.security.UserGroupInformation;

import org.apache.ambari.view.huetoambarimigration.model.*;

public class HiveSavedQueryImpl {

  static final Logger logger = Logger.getLogger(HiveSavedQueryImpl.class);

  private static String readAll(Reader rd) throws IOException {
    StringBuilder sb = new StringBuilder();
    int cp;
    while ((cp = rd.read()) != -1) {
      sb.append((char) cp);
    }
    return sb.toString();
  }

  public void wrtitetoalternatesqlfile(String dirname, String content,
                                       String instance, int i) throws IOException {

    Date dNow = new Date();
    SimpleDateFormat ft = new SimpleDateFormat("YYYY-MM-dd hh:mm:ss");
    String currentDate = ft.format(dNow);

    XMLOutputter xmlOutput = new XMLOutputter();

    xmlOutput.setFormat(Format.getPrettyFormat());

    File xmlfile = new File(ConfFileReader.getHomeDir() + "RevertChange.xml");

    if (xmlfile.exists()) {
      String iteration = Integer.toString(i + 1);
      SAXBuilder builder = new SAXBuilder();
      Document doc;
      try {
        doc = (Document) builder.build(xmlfile);

        Element rootNode = doc.getRootElement();

        Element record = new Element("RevertRecord");
        record.setAttribute(new Attribute("id", iteration));
        record.addContent(new Element("datetime").setText(currentDate
          .toString()));
        record.addContent(new Element("dirname").setText(dirname));
        record.addContent(new Element("instance").setText(instance));
        record.addContent(new Element("query").setText(content));

        rootNode.addContent(record);
        xmlOutput.output(doc, new FileWriter(ConfFileReader.getHomeDir() + "RevertChange.xml"));

      } catch (JDOMException e) {
        // TODO Auto-generated catch block
        logger.error("JDOMException: " , e);
      }

    } else {

      try {
        String iteration = Integer.toString(i + 1);
        Element revertrecord = new Element("RevertChangePage");
        Document doc = new Document(revertrecord);
        doc.setRootElement(revertrecord);

        Element record = new Element("RevertRecord");
        record.setAttribute(new Attribute("id", iteration));
        record.addContent(new Element("datetime").setText(currentDate
          .toString()));
        record.addContent(new Element("dirname").setText(dirname));
        record.addContent(new Element("instance").setText(instance));
        record.addContent(new Element("query").setText(content));

        doc.getRootElement().addContent(record);

        xmlOutput.output(doc, new FileWriter(ConfFileReader.getHomeDir() + "RevertChange.xml"));

      } catch (IOException io) {

      }

    }

  }

  public int fetchMaxidforSavedQueryHive(String driverName, Connection c, int id)
    throws SQLException {

    String ds_id = null;
    Statement stmt = null;
    stmt = c.createStatement();
    ResultSet rs = null;

    if (driverName.contains("postgresql")) {
      rs = stmt.executeQuery("select MAX(cast(ds_id as integer)) as max from ds_savedquery_" + id + ";");
    } else if (driverName.contains("mysql")) {
      rs = stmt.executeQuery("select max(cast(ds_id as unsigned) ) as max from DS_SAVEDQUERY_" + id + ";");
    } else if (driverName.contains("oracle")) {
      rs = stmt.executeQuery("select MAX(cast(ds_id as integer)) as max from ds_savedquery_" + id + ";");
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

  public int fetchInstancetablenameForSavedqueryHive(String driverName, Connection c,
                                                     String instance) throws SQLException {

    String ds_id = new String();
    int id = 0;
    Statement stmt = null;

    stmt = c.createStatement();
    ResultSet rs = null;

    if (driverName.contains("oracle")) {
      rs = stmt
        .executeQuery("select * from viewentity where class_name LIKE 'org.apache.ambari.view.hive.resources.savedQueries.SavedQuery' and view_instance_name='"
          + instance + "'");
    } else {
      rs = stmt
        .executeQuery("select * from viewentity where class_name LIKE 'org.apache.ambari.view.hive.resources.savedQueries.SavedQuery' and view_instance_name='"
          + instance + "';");
    }


    while (rs.next()) {
      id = rs.getInt("id");

    }

    return id;
  }

  public int fetchInstanceTablenameHiveHistory(String driverName, Connection c,
                                               String instance) throws SQLException {
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
      System.out.println("id is " + id);

    }

    return id;

  }

  public int fetchMaxdsidFromHiveHistory(String driverName, Connection c, int id)
    throws SQLException {

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


  /**/
  public void insertRowHiveHistory(String driverName, String dirname, int maxcount,
                                   long epochtime, Connection c, int id, String instance, int i)
    throws SQLException, IOException {
    String maxcount1 = Integer.toString(maxcount);

    String epochtime1 = Long.toString(epochtime);

    String ds_id = new String();
    Statement stmt = null;

    stmt = c.createStatement();
    String sql = "";
    String revsql = "";

    if (driverName.contains("mysql")) {
      sql = "INSERT INTO DS_JOBIMPL_" + id + " values ('" + maxcount1
        + "','','','','','default'," + epochtime1 + ",0,'','','"
        + dirname + "logs','admin','" + dirname
        + "query.hql','','job','','','Unknown','" + dirname
        + "','','Worksheet');";

      revsql = "delete from  DS_JOBIMPL_" + id + " where ds_id='"
        + maxcount1 + "';";

    } else if (driverName.contains("postgresql")) {
      sql = "INSERT INTO ds_jobimpl_" + id + " values ('" + maxcount1
        + "','','','','','default'," + epochtime1 + ",0,'','','"
        + dirname + "logs','admin','" + dirname
        + "query.hql','','job','','','Unknown','" + dirname
        + "','','Worksheet');";

      revsql = "delete from  ds_jobimpl_" + id + " where ds_id='"
        + maxcount1 + "';";

    } else if (driverName.contains("oracle")) {
      sql = "INSERT INTO ds_jobimpl_" + id + " values ('" + maxcount1
        + "','','','','','default'," + epochtime1 + ",0,'','','"
        + dirname + "logs','admin','" + dirname
        + "query.hql','','job','','','Unknown','" + dirname
        + "','','Worksheet')";
      revsql = "delete from  ds_jobimpl_" + id + " where ds_id='"
        + maxcount1 + "'";

    }
    stmt.executeUpdate(sql);
    wrtitetoalternatesqlfile(dirname, revsql, instance, i);
  }

  public void insertRowinSavedQuery(String driverName, int maxcount, String database,
                                    String dirname, String query, String name, Connection c, int id,
                                    String instance, int i) throws SQLException, IOException {
    String maxcount1 = Integer.toString(maxcount);

    String ds_id = new String();
    Statement stmt = null;
    String sql = "";
    String revsql = "";
    stmt = c.createStatement();

    if (driverName.contains("mysql")) {
      sql = "INSERT INTO DS_SAVEDQUERY_" + id + " values ('"
        + maxcount1 + "','" + database + "','" + "admin" + "','"
        + dirname + "query.hql','" + query + "','" + name + "');";

      revsql = "delete from  DS_SAVEDQUERY_" + id + " where ds_id='"
        + maxcount1 + "';";

    } else if (driverName.contains("postgresql")) {
      sql = "INSERT INTO ds_savedquery_" + id + " values ('"
        + maxcount1 + "','" + database + "','" + "admin" + "','"
        + dirname + "query.hql','" + query + "','" + name + "');";

      revsql = "delete from  ds_savedquery_" + id + " where ds_id='"
        + maxcount1 + "';";

    } else if (driverName.contains("oracle")) {
      sql = "INSERT INTO ds_savedquery_" + id + " values ('"
        + maxcount1 + "','" + database + "','" + "admin" + "','"
        + dirname + "query.hql','" + query + "','" + name + "')";

      revsql = "delete from  ds_savedquery_" + id + " where ds_id='"
        + maxcount1 + "'";

    }
    wrtitetoalternatesqlfile(dirname, revsql, instance, i);
    stmt.executeUpdate(sql);
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

  public ArrayList<PojoHive> fetchFromHuedb(String username,
                                            String startdate, String endtime, Connection connection)
    throws ClassNotFoundException, IOException {
    int id = 0;
    int i = 0;
    String[] query = new String[100];
    ArrayList<PojoHive> hiveArrayList = new ArrayList<PojoHive>();
    ResultSet rs1 = null;

    try {
      Statement statement = connection.createStatement();
      if (username.equals("all")) {
      } else {
        ResultSet rs = statement
          .executeQuery("select id from auth_user where username='"
            + username + "';");
        while (rs.next()) {

          id = rs.getInt("id");

        }

      }
      if (startdate.equals("") && endtime.equals("")) {
        if (username.equals("all")) {
          rs1 = statement
            .executeQuery("select data,name,owner_id from beeswax_savedquery;");

        } else {
          rs1 = statement
            .executeQuery("select data,name,owner_id from beeswax_savedquery where name!='My saved query'and owner_id ="
              + id + ";");
        }

      } else if (!(startdate.equals("")) && !(endtime.equals(""))) {
        if (username.equals("all")) {
          rs1 = statement
            .executeQuery("select data,name,owner_id from beeswax_savedquery where name!='My saved query' AND mtime >= date('"
              + startdate
              + "') AND mtime <= date('"
              + endtime + "');");
        } else {
          rs1 = statement
            .executeQuery("select data,name,owner_id from beeswax_savedquery where name!='My saved query'and owner_id ="
              + id
              + " AND mtime >= date('"
              + startdate
              + "') AND mtime <= date('"
              + endtime
              + "');");
        }

      } else if (!(startdate.equals("")) && (endtime.equals(""))) {
        if (username.equals("all")) {
          rs1 = statement
            .executeQuery("select data,name,owner_id from beeswax_savedquery where name!='My saved query'and  mtime >= date('"
              + startdate + "');");
        } else {
          rs1 = statement
            .executeQuery("select data,name,owner_id from beeswax_savedquery where name!='My saved query'and owner_id ="
              + id
              + " AND mtime >= date('"
              + startdate
              + "');");
        }

      } else if ((startdate.equals("")) && !(endtime.equals(""))) {
        if (username.equals("all")) {
          rs1 = statement
            .executeQuery("select data,name,owner_id from beeswax_savedquery where name!='My saved query' AND mtime <= date('"
              + endtime + "');");
        } else {
          rs1 = statement
            .executeQuery("select data,name,owner_id from beeswax_savedquery where name!='My saved query'and owner_id ="
              + id
              + " AND mtime <= date('"
              + endtime
              + "');");
        }

      }
      while (rs1.next()) {
        PojoHive hivepojo = new PojoHive();
        String name = rs1.getString("name");
        String temp = rs1.getString("data");
        InputStream is = new ByteArrayInputStream(temp.getBytes());
        BufferedReader rd = new BufferedReader(new InputStreamReader(
          is, Charset.forName("UTF-8")));
        String jsonText = readAll(rd);
        JSONObject json = new JSONObject(jsonText);
        String resources = json.get("query").toString();
        json = new JSONObject(resources);

        String resarr = (json.get("query")).toString();

        json = new JSONObject(resources);
        String database = (json.get("database")).toString();
        hivepojo.setQuery(resarr);
        hivepojo.setDatabase(database);
        hivepojo.setOwner(name);
        hiveArrayList.add(hivepojo);
        i++;
      }

    } catch (SQLException e) {
      // if the error message is "out of memory",
      // it probably means no database file is found
      System.err.println(e.getMessage());
    } finally {
      try {
        if (connection != null)
          connection.close();
      } catch (SQLException e) {
        logger.error("sql connection exception" , e);
      }
    }

    return hiveArrayList;

  }


  public void writetoFilequeryHql(String content, String homedir) {
    try {
      File file = new File(homedir + "query.hql");
      if (!file.exists()) {
        file.createNewFile();
      }

      FileWriter fw = new FileWriter(file.getAbsoluteFile());
      BufferedWriter bw = new BufferedWriter(fw);
      bw.write(content);
      bw.close();

    } catch (IOException e) {
      logger.error("IOException: " , e);
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
      logger.error("IOException: " , e);
    }

  }

  public void createDirHive(final String dir, final String namenodeuri)
    throws IOException, URISyntaxException {

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

          FileSystem fs = FileSystem.get(conf);
          Path src = new Path(dir);
          fs.mkdirs(src);
          return null;
        }
      });
    } catch (Exception e) {
      logger.error("Webhdfs: " , e);
    }
  }

  public void createDirHiveSecured(final String dir, final String namenodeuri)
    throws IOException, URISyntaxException {

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

          FileSystem fs = FileSystem.get(conf);
          Path src = new Path(dir);
          fs.mkdirs(src);
          return null;
        }
      });
    } catch (Exception e) {
      logger.error("Webhdfs: " , e);
    }
  }

  public void putFileinHdfs(final String source, final String dest,
                            final String namenodeuri) throws IOException {

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


  public void putFileinHdfsSecured(final String source, final String dest,
                                   final String namenodeuri) throws IOException {

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
          // Path pathsource = new Path(source);
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
