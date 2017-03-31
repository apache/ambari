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

package org.apache.ambari.view.huetoambarimigration.migration.hive.historyquery;

import org.apache.ambari.view.huetoambarimigration.datasource.queryset.ambariqueryset.hive.historyqueryset.QuerySetAmbariDB;
import org.apache.ambari.view.huetoambarimigration.datasource.queryset.huequeryset.hive.historyqueryset.QuerySetHueDB;
import org.apache.ambari.view.huetoambarimigration.migration.configuration.ConfigurationCheckImplementation;
import org.apache.ambari.view.huetoambarimigration.resources.scripts.models.HiveModel;
import org.apache.hadoop.conf.Configuration;
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

import java.beans.PropertyVetoException;
import java.io.*;
import java.net.URISyntaxException;
import java.security.PrivilegedExceptionAction;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.net.URI;

public class HiveHistoryQueryMigrationImplementation {

  static final Logger logger = Logger.getLogger(HiveHistoryQueryMigrationImplementation.class);
  final String USER_DIRECTORY = "/user";

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
        logger.error("JDOMException", e);

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
        xmlOutput.output(doc, new FileWriter(ConfigurationCheckImplementation.getHomeDir() + "RevertChangesService.xml"));
      } catch (IOException io) {
        logger.error("JDOMException", io);
      }

    }

  }

  public int fetchMaximumIdfromAmbaridb(Connection c, int id, QuerySetAmbariDB ambaridatabase) throws SQLException {

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


  public void insertRowinAmbaridb(String dirname, int maxcount, long epochtime, Connection c, int id, String instance, int i, QuerySetAmbariDB ambaridatabase, String versionName, String username, String jobStatus) throws SQLException, IOException {

    String maxcount1 = Integer.toString(maxcount);
    String epochtime1 = Long.toString(epochtime);
    PreparedStatement prSt = null;
    String revsql = null;
    if (versionName.contains("1.0")) {
      prSt = ambaridatabase.insertToHiveHistoryForHive(c, id, maxcount1, epochtime, dirname, username, jobStatus);
    } else {
      prSt = ambaridatabase.insertToHiveHistoryForHiveNext(c, id, maxcount1, epochtime, dirname, username, jobStatus);
    }

    logger.info("The actual insert statement is " + prSt);
    prSt.executeUpdate();
    revsql = ambaridatabase.revertSql(id, maxcount1);
    logger.info("adding revert sql hive history");
    wrtitetoalternatesqlfile(dirname, revsql, instance, i);
  }

  public void updateSequenceno(Connection c, int seqNo, int id, QuerySetAmbariDB ambaridatabase) throws SQLException, IOException {

    PreparedStatement prSt;
    prSt = ambaridatabase.updateSequenceNoInAmbariSequence(c, seqNo, id);
    logger.info("The actual insert statement is " + prSt);
    prSt.executeUpdate();
    logger.info("adding revert sql hive history");
  }

  public String getAllHiveVersionInstance(Connection c, QuerySetAmbariDB ambaridatabase, String viewName) throws PropertyVetoException, SQLException, IOException {


    PreparedStatement prSt;
    ResultSet rs1 = null;
    String instanceVersion = null;
    int i = 0;
    prSt = ambaridatabase.getHiveVersionInstance(c, viewName);
    rs1 = prSt.executeQuery();

    while (rs1.next()) {

      instanceVersion = rs1.getString(1);

    }
    rs1.close();
    prSt.close();
    return instanceVersion;

  }

  public int fetchSequenceno(Connection c, int id, QuerySetAmbariDB ambaridatabase) throws SQLException {

    String ds_id = new String();
    Statement stmt = null;
    PreparedStatement prSt = null;
    int sequencevalue = 0;


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

  public long getEpochTime() throws ParseException {
    long seconds = System.currentTimeMillis();
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

  public ArrayList<HiveModel> fetchFromHue(String username, String startdate, String endtime, Connection connection, QuerySetHueDB huedatabase) throws ClassNotFoundException, SQLException, IOException {
    int id = 0;
    int i = 0;
    ArrayList<HiveModel> hiveArrayList = new ArrayList<HiveModel>();


    try {
      connection.setAutoCommit(false);
      PreparedStatement prSt = null;
      Statement statement = connection.createStatement();
      String query, ownerName = "";
      ResultSet rs;
      int ownerId, jobState;

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
        HiveModel hivepojo = new HiveModel();
        jobState = rs1.getInt("last_state");
        ownerId = rs1.getInt("owner_id");
        if(username.equals("all")) {
          prSt = huedatabase.getUserName(connection, ownerId);
          ResultSet resultSet = prSt.executeQuery();
          while(resultSet.next()) {
            ownerName = resultSet.getString("username");
          }
        }
        query = rs1.getString("query");
        hivepojo.setJobStatus(jobState);
        hivepojo.setOwnerName(ownerName);
        hivepojo.setQuery(query);
        hiveArrayList.add(hivepojo);
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
    return hiveArrayList;

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
      logger.error("IOException", e);
    }

  }

  public void deleteFileQueryhql(String homedir) {
    try {
      File file = new File(homedir + "query.hql");

      if (file.delete()) {
        logger.info("temporary hql file deleted");
      } else {
        logger.info("temporary hql file delete failed");
      }

    } catch (Exception e) {

      logger.error("File Exception ", e);

    }

  }

  public void deleteFileQueryLogs(String homedir) {
    try {
      File file = new File(homedir + "logs");

      if (file.delete()) {
        logger.info("temporary logs file deleted");
      } else {
        logger.info("temporary logs file delete failed");
      }

    } catch (Exception e) {

      logger.error("File Exception ", e);

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
      logger.error("IOException", e);
    }

  }

  public void createDir(final String dir, final String namenodeuri, final String username) throws IOException,
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

  public void createDirKerberorisedSecured(final String dir, final String namenodeuri, final String username,final String principalName) throws IOException,
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


  public void putFileinHdfs(final String source, final String dest, final String namenodeuri,final String username)
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
          fileSystem.setOwner(path, username, username);
          fileSystem.close();
          return null;
        }
      });
    } catch (Exception e) {
      logger.error("Webhdfs exception", e);
    }

  }

  public void putFileinHdfsKerborizedSecured(final String source, final String dest, final String namenodeuri,final String username,final String principalName)
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
      conf.set("hadoop.security.authentication", "Kerberos");
      UserGroupInformation.setConfiguration(conf);
      UserGroupInformation proxyUser ;
      proxyUser = UserGroupInformation.createRemoteUser(principalName);
      UserGroupInformation ugi = UserGroupInformation.createProxyUser("hdfs", proxyUser);
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
          fileSystem.setOwner(path, username, username);
          fileSystem.close();
          return null;
        }
      });
    } catch (Exception e) {
      logger.error("Webhdfs exception", e);

    }

  }

}
