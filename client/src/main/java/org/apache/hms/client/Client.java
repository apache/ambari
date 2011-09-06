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

package org.apache.hms.client;

import java.io.IOException;
import java.net.URL;

import javax.activity.InvalidActivityException;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hms.common.entity.Response;
import org.apache.hms.common.entity.cluster.MachineState;
import org.apache.hms.common.entity.command.CommandStatus;
import org.apache.hms.common.entity.command.CreateClusterCommand;
import org.apache.hms.common.entity.command.DeleteClusterCommand;
import org.apache.hms.common.entity.command.UpgradeClusterCommand;
import org.apache.hms.common.entity.manifest.ClusterManifest;
import org.apache.hms.common.entity.manifest.ConfigManifest;
import org.apache.hms.common.entity.manifest.NodesManifest;
import org.apache.hms.common.entity.manifest.SoftwareManifest;
import org.apache.hms.common.util.ExceptionUtil;
import org.apache.hms.common.util.JAXBUtil;

import com.sun.jersey.api.Responses;
import com.sun.jersey.api.client.UniformInterfaceException;

public class Client {
  private static Log log = LogFactory.getLog(Client.class);
  private static Executor clientRunner = Executor.getInstance();
 
  @SuppressWarnings("static-access")
  private final static Option help = OptionBuilder.withLongOpt("help").withDescription("Output usage menu and quit").create("h");

  @SuppressWarnings("static-access")
  private final static Option createCluster = OptionBuilder.withLongOpt("create-cluster").withArgName("cluster-name")
    .hasArg().withDescription("Create a cluster")
    .create();

  @SuppressWarnings("static-access")
  private final static Option deleteCluster = OptionBuilder.withLongOpt("delete-cluster").withArgName("cluster-name")
    .hasArg().withDescription("Deletea cluster")
    .create();
  
  @SuppressWarnings("static-access")
  private final static Option clusterStatus = OptionBuilder.withLongOpt("cluster-status").withArgName("cluster-name")
    .hasArg().withDescription("Check cluster status")
    .create("cs");

  @SuppressWarnings("static-access")
  private final static Option upgradeCluster = OptionBuilder.withLongOpt("upgrade-cluster").withArgName("cluster-name")
    .hasArg().withDescription("Upgrade a cluster")
    .create();

  @SuppressWarnings("static-access")
  private final static Option nodeStatus = OptionBuilder.withLongOpt( "node-status" ).withArgName( "nodepath" )
    .hasArg().withDescription("check node status")
    .create("ns");

  @SuppressWarnings("static-access")
  private final static Option cmdStatus = OptionBuilder.withArgName("command-id")
    .hasArg().withDescription("Check command status")
    .create("q");
  
  @SuppressWarnings("static-access")
  private final static Option softwareManifest = OptionBuilder.withLongOpt("software").withArgName("software-url")
    .hasArg().withDescription("Location of software manifest")
    .create();
  
  @SuppressWarnings("static-access")
  private final static Option nodesManifest = OptionBuilder.withLongOpt("nodes").withArgName("nodes-url")
    .hasArg().withDescription("Location of nodes manifest")
    .create();
  
  @SuppressWarnings("static-access")
  private final static Option configManifest = OptionBuilder.withLongOpt("config").withArgName("config-url")
    .hasArg().withDescription("Location of config manifest")
    .create();

  @SuppressWarnings("static-access")
  private final static Option dryRun = OptionBuilder.withLongOpt( "dryrun" )
    .withDescription( "Test command only" ).create();

  @SuppressWarnings("static-access")
  private final static Option verbose = OptionBuilder.withLongOpt( "verbose" )
    .withDescription( "Print verbose information" ).create("v");

  private static Options opt = setupOptions();

  public static Options setupOptions() {
    if(opt==null) {
      opt = new Options();
    }
    opt.addOption(help);
    opt.addOption(nodeStatus);
    opt.addOption(cmdStatus);
    opt.addOption(createCluster);
    opt.addOption(deleteCluster);
    opt.addOption(upgradeCluster);
    opt.addOption(clusterStatus);
    opt.addOption(nodesManifest);
    opt.addOption(configManifest);
    opt.addOption(softwareManifest);
    opt.addOption(verbose);

    opt.addOption(dryRun);
    return opt;
  }

  /**
   * Construct a create cluster command
   * @param clusterName - Cluster name
   * @param nodes - Nodes manifest is a url to a XML file which describes the server compositions of the cluster
   * @param software - Software manifest is a url to a XML file which describes the software compositions of the cluster
   * @param config - Configuration manifest is a url to a XML file which describes the configuration steps for the cluster
   * @return
   * @throws IOException
   */
  public Response createCluster(String clusterName, URL nodes, URL software, URL config) throws IOException {
    ClusterManifest cluster = new ClusterManifest();
    cluster.setClusterName(clusterName);
    NodesManifest nodesM = new NodesManifest();
    nodesM.setUrl(nodes);
    cluster.setNodes(nodesM);
    SoftwareManifest softwareM = new SoftwareManifest();
    softwareM.setUrl(software);
    cluster.setSoftware(softwareM);
    ConfigManifest configM = new ConfigManifest();
    configM.setUrl(config);
    cluster.setConfig(configM);
    return clientRunner.sendToController(new CreateClusterCommand(cluster));
  }

  /**
   * Construct a upgrade cluster command
   * @param clusterName - Cluster name
   * @param nodes - Nodes manifest is a url to a XML file which describes the server compositions of the cluster
   * @param software - Software manifest is a url to a XML file which describes the software compositions of the cluster
   * @param config - Configuration manifest is a url to a XML file which describes the configuration steps for the cluster
   * @return
   * @throws IOException
   */
  public Response upgradeCluster(String clusterName, URL nodes, URL software, URL config) throws IOException {
    ClusterManifest cluster = new ClusterManifest();
    cluster.setClusterName(clusterName);
    NodesManifest nodesM = new NodesManifest();
    nodesM.setUrl(nodes);
    cluster.setNodes(nodesM);
    SoftwareManifest softwareM = new SoftwareManifest();
    softwareM.setUrl(software);
    cluster.setSoftware(softwareM);
    ConfigManifest configM = new ConfigManifest();
    configM.setUrl(config);
    cluster.setConfig(configM);
    return clientRunner.sendToController(new UpgradeClusterCommand(cluster));
  }

  /**
   * Construct a delete cluster command
   * @param clusterName - Cluster name
   * @param config - Configuration manifest is a url to a XML file which describes the decommission steps for the cluster
   * @return
   * @throws IOException
   */
  public Response deleteCluster(String clusterName, URL config) throws IOException {
    ClusterManifest cluster = new ClusterManifest();
    ConfigManifest configM = new ConfigManifest();
    configM.setUrl(config);
    cluster.setConfig(configM);
    return clientRunner.sendToController(new DeleteClusterCommand(clusterName, cluster));
  }
  
  /**
   * Query command status
   * @param id - Command ID
   * @return
   * @throws IOException
   */
  public CommandStatus queryCommandStatus(String id) throws IOException {
    return clientRunner.queryController(id);
  }
  
  /**
   * Parse command line arguments and construct HMS command for HMS Client Executor class
   * @param args
   */
  public void run(String[] args) {
    BasicParser parser = new BasicParser();
    try {
      CommandLine cl = parser.parse(opt, args);
      /* Dry run */
      boolean dryRun = false;
      if ( cl.hasOption("t") ) {
        dryRun = true;
      }
      
    if ( cl.hasOption("q")) {
        String cmdid = cl.getOptionValue("q");
        try {
          CommandStatus cs = queryCommandStatus(cmdid);
          if( cl.hasOption("v")) {
            System.out.println(JAXBUtil.print(cs));
          } else {
            System.out.println("Command ID: "+cmdid);
            System.out.println("Status: "+cs.getStatus());
            System.out.println("Total actions: "+cs.getTotalActions());
            System.out.println("Completed actions: "+cs.getCompletedActions());
          }
        } catch(UniformInterfaceException e) {
          if(e.getResponse().getStatus()==Responses.NOT_FOUND) {
            System.out.println("Command ID:"+cmdid+" does not exist.");
          } else {
            System.out.println("Unknown error occurred, check stack trace.");
            System.out.println(ExceptionUtil.getStackTrace(e));            
          }
        }
      } else if ( cl.hasOption("delete-command")) {
        // TODO: Remove command from the system
        String cmdId = cl.getOptionValue("delete-command");
        if (cmdId == null) {
          throw new RuntimeException("Command ID must be specified for Delete operation");
        }
        // System.out.println(clientRunner.sendToController(new DeleteCommand(cmdId)));        
      } else if ( cl.hasOption("delete-cluster") ) {
        /* delete a cluster */
        String clusterName = cl.getOptionValue("delete-cluster");
        if (clusterName == null) {
          throw new RuntimeException("cluster name must be specified for DELETE operation");
        }
        URL config = new URL(cl.getOptionValue("config-manifest"));
        if (config == null) {
          throw new RuntimeException("config manifest must be specified for DELETE operation");
        }
        try {
          Response response = deleteCluster(clusterName, config);
          showResponse(response, cl.hasOption("v"));
        } catch(Throwable e) {
          showErrors(e);
        }
      } else if ( cl.hasOption("create-cluster") ) {
        /* create a cluster */
        String clusterName = cl.getOptionValue("create-cluster");
        if (clusterName == null) {
          throw new RuntimeException("cluster name must be specified for CREATE operation");
        }
        URL nodes = new URL(cl.getOptionValue("nodes"));
        if (nodes == null) {
          throw new RuntimeException("nodes manifest must be specified for CREATE operation");
        }
        URL software = new URL(cl.getOptionValue("software"));
        if (software == null) {
          throw new RuntimeException("software manifest must be specified for CREATE operation");
        }
        URL config = new URL(cl.getOptionValue("config"));
        if (config == null) {
          throw new RuntimeException("config manifest must be specified for CREATE operation");
        }
        Response response = createCluster(clusterName, nodes, software, config);
        showResponse(response, cl.hasOption("v"));        
      } else if ( cl.hasOption("upgrade-cluster") ) {
        /* upgrade a cluster */
        String clusterName = cl.getOptionValue("upgrade-cluster");
        if (clusterName == null) {
          throw new RuntimeException("cluster name must be specified for CREATE operation");
        }
        URL nodes = new URL(cl.getOptionValue("nodes"));
        if (nodes == null) {
          throw new RuntimeException("nodes manifest must be specified for CREATE operation");
        }
        URL software = new URL(cl.getOptionValue("software"));
        if (software == null) {
          throw new RuntimeException("software manifest must be specified for CREATE operation");
        }
        URL config = new URL(cl.getOptionValue("config"));
        if (config == null) {
          throw new RuntimeException("config manifest must be specified for CREATE operation");
        }
        Response response = upgradeCluster(clusterName, nodes, software, config);
        showResponse(response, cl.hasOption("v"));
      } else if ( cl.hasOption("cluster-status") ) {
        /* check cluster status */
        String clusterId = cl.getOptionValue("cluster-status");
        if (clusterId == null) {
          throw new RuntimeException("Cluster path must be specified for cluster-status operation");
        }
        ClusterManifest cm = clientRunner.checkClusterStatus(clusterId);
        System.out.println(JAXBUtil.print(cm));
      } else if ( cl.hasOption("node-status") ) {
        /* check node status */
        String nodepath = cl.getOptionValue("node-status");
        if (nodepath == null) {
          throw new RuntimeException("nodePath must be specified for nodestatus operation");
        }
        MachineState ms = clientRunner.checkNodeStatus(nodepath);
        System.out.println(JAXBUtil.print(ms));
      } else if ( cl.hasOption("help")) {
        usage();
      } else {
        throw new InvalidActivityException("Invalid arguement.");
      }
    } catch (InvalidActivityException e) {
      usage();
      System.out.println("Argument Error: " + e.getMessage());
    } catch (Throwable e) {
      showErrors(e);
    }
  }
  
  /**
   * Generic utility to handle error feedback for HMS command line client.
   * @param e
   */
  private void showErrors(Throwable e) {
    log.error(ExceptionUtil.getStackTrace(e));
    System.out.println("Error in issuing command.");
    System.out.println(ExceptionUtil.getStackTrace(e));    
  }

  /**
   * Generic utility method to display the response of HMS Controller Rest API.
   * @param response - Response object from HMS Controller Rest API.
   * @param verbose - Display response verbosely.
   * @throws IOException
   */
  private void showResponse(Response response, boolean verbose) throws IOException {
    if(response.getCode()==0) {
      System.out.println("Command has been queued.  Command ID: "+response.getOutput());
    }
    if(verbose) {
      System.out.println("Verbose Output:");
      System.out.println(JAXBUtil.print(response));
    }    
  }

  /**
   * Display usage of HMS command line client
   */
  public static void usage() {
    HelpFormatter f = new HelpFormatter();
    f.printHelp("hms client", opt);
  }
  
  public static void main(String[] args) {
    Client c = new Client();
    c.run(args);
  }

}
