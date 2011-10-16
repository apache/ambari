/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hms.controller;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.prefs.Preferences;

import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hms.common.conf.CommonConfigurationKeys;
import org.apache.hms.common.util.DaemonWatcher;
import org.apache.hms.common.util.ExceptionUtil;
import org.apache.hms.common.util.MulticastDNS;
import org.apache.hms.common.util.ServiceDiscoveryUtil;
import org.apache.hms.controller.ClientHandler;
import org.apache.hms.controller.CommandHandler;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooDefs.Ids;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.DefaultServlet;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.resource.Resource;
import org.mortbay.resource.ResourceCollection;


import com.sun.jersey.spi.container.servlet.ServletContainer;

public class Controller implements Watcher {
  private static Log LOG = LogFactory.getLog(Controller.class);
  public static String CONTROLLER_PREFIX = "v1";
  public static int CONTROLLER_PORT = 4080;
  private static Controller instance = new Controller();
  private Server server = null;
  private String credential = null;
  
  private ZooKeeper zk;
  private ClientHandler clientHandler;
  private CommandHandler commandHandler;
  public volatile boolean running = true; // true while controller runs
  private String zookeeperAddress = CommonConfigurationKeys.ZOOKEEPER_ADDRESS_DEFAULT;

  public static Controller getInstance() {
    return instance;
  }

  public ZooKeeper getZKInstance() {
    return this.zk;
  }
  
  public ClientHandler getClientHandler() {
    return clientHandler;
  }
  
  public CommandHandler getCommandHandler() {
    return commandHandler;
  }
  
  public void process(WatchedEvent event) {
    if (event.getType() == Event.EventType.None) {
        // We are are being told that the state of the
        // connection has changed
        switch (event.getState()) {
        case SyncConnected:
            // In this particular example we don't need to do anything
            // here - watches are automatically re-registered with 
            // server and any watches triggered while the client was 
            // disconnected will be delivered (in order of course)
            break;
        case Expired:
            // It's all over
            running = false;
            commandHandler.stop();
            break;
        }
    }
  }
  
  public void parseConfig() {
    StringBuilder confPath = new StringBuilder();
    String confDir = System.getProperty("HMS_CONF_DIR");
    if(confDir==null) {
      confDir = "/etc/hms";
    }
    confPath.append(confDir);
    confPath.append("/hms.ini");
    try {
      HierarchicalINIConfiguration ini = new HierarchicalINIConfiguration(confPath.toString());
      zookeeperAddress = ini.getSection("zookeeper").getString("quorum", null);
      String user = ini.getSection("zookeeper").getString("user", null);
      String password = ini.getSection("zookeeper").getString("password", null);
      if(user!=null && password!=null) {
        credential = new StringBuilder().append(user).append(":").append(password).toString();
      }
    } catch (Exception e) {
      LOG.warn("Invalid HMS configuration file: " + confPath);
      zookeeperAddress = null;
    }
    LOG.info("ZooKeeper Quorum in "+confPath.toString()+": "+zookeeperAddress);
  }
  
  // Resolve the list of zookeeper hosts from HMS beacons
  public void initmDNS() {
    try {
      ServiceDiscoveryUtil sdu = new ServiceDiscoveryUtil(CommonConfigurationKeys.ZEROCONF_ZOOKEEPER_TYPE);
      sdu.start();
      Thread.sleep(5000);
      Collection<String> list = sdu.resolve();
      if(list.size()>0) {
        StringBuffer buf = new StringBuffer();
        String delimiter = "";
        for(String addr : list) {
          buf.append(delimiter);
          buf.append(addr);
          delimiter = ",";
        }
        zookeeperAddress = buf.toString();
      }
      sdu.close();
      if(zookeeperAddress.equals("")) {
        throw new RuntimeException("Unknown ZooKeeper location.");
      }
      LOG.info("Discovered zookeeper location: "+zookeeperAddress);
    } catch(Exception e) {
      zookeeperAddress = CommonConfigurationKeys.ZOOKEEPER_ADDRESS_DEFAULT;
      LOG.info("Use default zookeeper location: "+zookeeperAddress);
    }
  }
  
  public void start() {
    try {
      //System.out.close();
      //System.err.close();
      parseConfig();
      if(zookeeperAddress == null) {
        initmDNS();
      }
      run();
    } catch(Exception e) {
      LOG.error(ExceptionUtil.getStackTrace(e));
      System.exit(-1);
    }
  }

  private void createDirectory(String path) throws KeeperException, InterruptedException {
    try {
      zk.create(path, new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
      if(credential!=null) {
        zk.setACL(path, Ids.CREATOR_ALL_ACL, -1);
      }
      LOG.info("Created HMS cluster root at " + CommonConfigurationKeys.ZOOKEEPER_CLUSTER_ROOT_DEFAULT);        
    } catch (KeeperException.NodeExistsException e) {
    } catch (KeeperException.AuthFailedException e) {
      LOG.warn("Failed to authenticate for "+path);
    }
  }
  
  private void initializeZooKeeper() throws KeeperException, InterruptedException, IOException {
    zk = new ZooKeeper(zookeeperAddress, 600000, this);
    if(credential!=null) {
      zk.addAuthInfo("digest", credential.getBytes());
    }
    String[] list = {
        CommonConfigurationKeys.ZOOKEEPER_CLUSTER_ROOT_DEFAULT,
        CommonConfigurationKeys.ZOOKEEPER_COMMAND_QUEUE_PATH_DEFAULT,
        CommonConfigurationKeys.ZOOKEEPER_LOCK_QUEUE_PATH_DEFAULT,
        CommonConfigurationKeys.ZOOKEEPER_LIVE_CONTROLLER_PATH_DEFAULT,
        CommonConfigurationKeys.ZOOKEEPER_NODES_MANIFEST_PATH_DEFAULT,
        CommonConfigurationKeys.ZOOKEEPER_STATUS_QUEUE_PATH_DEFAULT,
        CommonConfigurationKeys.ZOOKEEPER_SOFTWARE_MANIFEST_PATH_DEFAULT,
        CommonConfigurationKeys.ZOOKEEPER_CONFIG_BLUEPRINT_PATH_DEFAULT
    };
    for(String path : list) {
      createDirectory(path);
    }
  }
  
  public void run() {
    try {
      initializeZooKeeper();
      LOG.info("Connected to ZooKeeper");
      clientHandler = new ClientHandler(zk);
      commandHandler = new CommandHandler(zk, 200);
      commandHandler.start();
    } catch (Exception e) {
      LOG.error(ExceptionUtil.getStackTrace(e));
    }
    server = new Server(CONTROLLER_PORT);

    try {
      Context root = new Context(server, "/", Context.SESSIONS);
      String HMS_HOME = System.getenv("HMS_HOME");
      root.setBaseResource(new ResourceCollection(new Resource[]
        {
          Resource.newResource(HMS_HOME+"/webapps/")
        }));
      ServletHolder rootServlet = root.addServlet(DefaultServlet.class, "/");
      rootServlet.setInitOrder(1);
      
      ServletHolder sh = new ServletHolder(ServletContainer.class);
      sh.setInitParameter("com.sun.jersey.config.property.resourceConfigClass", "com.sun.jersey.api.core.PackagesResourceConfig");
      sh.setInitParameter("com.sun.jersey.config.property.packages", "org.apache.hms.controller.rest");      
      root.addServlet(sh, "/"+CONTROLLER_PREFIX+"/*");
      sh.setInitOrder(2);
      server.setStopAtShutdown(true);
      server.start();
    } catch (Exception e) {
      LOG.error(ExceptionUtil.getStackTrace(e));
    }
  }
  
  public void stop() throws Exception {
    try {
      commandHandler.stop();
      server.stop();
    } catch (Exception e) {
      LOG.error(ExceptionUtil.getStackTrace(e));
    }
  }
  
  /**
   * Wait for service to finish.
   * (Normally, it runs forever.)
   */
  public void join() {
    try {
      this.commandHandler.join();
    } catch (InterruptedException ie) {
    }
  }

  public static void main(String[] args) {
    DaemonWatcher.createInstance(System.getProperty("PID"), 9100);
    try {
      Controller controller = Controller.getInstance();
      if (controller != null) {
        controller.start();
        controller.join();
      }
    } catch(Throwable t) {
      DaemonWatcher.bailout(1);
    }
  }

}
