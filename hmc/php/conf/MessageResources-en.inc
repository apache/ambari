<?php
/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
*/

$RES['app.name']="Ambari";
$RES['page.title']="Ambari";
$RES['page.footer.body']="<a href='http://www.apache.org/licenses/LICENSE-2.0' target='_blank'>Licensed under the Apache License, Version 2.0</a>.<br><a href='/hmc/licenses/NOTICE.txt' target='_blank'>See third-party tools/resources that Ambari uses and their respective authors</a>";
$RES['topnav.help.link']="http://incubator.apache.org/ambari/install.html";
$RES['welcome.header']="Welcome to Ambari!";
$RES['welcome.body']="";
$RES['welcome.note']="Before you proceed, make sure you have performed all the pre-installation steps.";
$RES['welcome.submit.label']="Get started";
$RES['installFailed.header']="Cluster installation failed";
$RES['installFailed.body']="Cluster installation failed.  To continue, you will need to uninstall the cluster first and re-install the cluster.";
$RES['installFailed.submit.label']="Start the uninstall process";
$RES['reinstall.header']="You have not set up a cluster yet.";
$RES['reinstall.body']="We'll walk you through setting up your cluster with a 7-step wizard.";
$RES['reinstall.submit.label']="Let's get started!";
$RES['uninstallFailed.header']="Cluster uninstallation failed";
$RES['uninstallFailed.body']="Failed to uninstall the cluster";
$RES['common.sshUsername.label']="SSH Username";
$RES['common.sshPrivateKeyFile.label']="SSH Private Key File for <i>root</i>";
$RES['common.hostsFile.label']="Hosts File (newline-delimited list of hostnames)";
$RES['initWizard.createCluster.pageSummary.header']="Name your Hadoop cluster";
$RES['initWizard.createCluster.pageSummary.body']="";
$RES['initWizard.createCluster.clusterName.label']="Name of your new cluster";
$RES['initWizard.createCluster.submit.label']="Next";
$RES['initWizard.addNodes.pageSummary.header']="Which nodes do you want to install Hadoop on?";
$RES['initWizard.addNodes.pageSummary.body']="We will use the SSH private key for the <i>root</i> user and a file containing a list of hostnames to perform installation on your nodes.  The corresponding public key must already be in <i>authorized_keys</i> on all the nodes.";
$RES['initWizard.addNodes.useLocalYum.label']="Use local yum mirror instead of downloading packages from the internet";
$RES['initWizard.addNodes.yumRepoFilePath.label']="YUM Repo File Path";
$RES['initWizard.addNodes.apacheArtifactsDownloadUrl.label']="Apache Artifacts Download URL";
$RES['initWizard.addNodes.gplArtifactsDownloadUrl.label']="GPL Artifacts Download URL";
$RES['initWizard.addNodes.submit.label']="Add Nodes";
$RES['initWizard.selectServices.pageSummary.header']="Which services do you want to install?";
$RES['initWizard.selectServices.pageSummary.body']="We will automatically take care of dependencies (e.g., HBase requires ZooKeeper, etc.)";
$RES['initWizard.selectServices.submit.label']="Select Services";
$RES['initWizard.assignMasters.pageSummary.header']="Assign Master Services to Hosts";
$RES['initWizard.assignMasters.pageSummary.body']="We have come up with recommendations on where the master services should be hosted.  Modify as you see fit.";
$RES['initWizard.assignMasters.submit.label']="Next";
$RES['initWizard.configureCluster.pageSummary.header']="Specify Disk Mount Points";
$RES['initWizard.configureCluster.pageSummary.body']="We found the following disk mount points on your nodes. Please confirm/modify the mount points to use for your nodes.";
$RES['initWizard.configureCluster.customMountPoints.label']="Custom mount points";
$RES['initWizard.configureCluster.preview.label']="Preview directories to be used";
$RES['initWizard.configureCluster.submit.label']="Next";
$RES['initWizard.configureClusterAdvanced.pageSummary.header']="Customize Settings";
$RES['initWizard.configureClusterAdvanced.pageSummary.body']="We have come up with reasonable default settings.  Customize as you see fit.";
$RES['initWizard.configureClusterAdvanced.back.label']="&lt; Back";
$RES['initWizard.configureClusterAdvanced.next.label']="Next &gt;";
$RES['initWizard.configureClusterAdvanced.submit.label']="Finished customizing all components";
$RES['initWizard.reviewAndDeploy.pageSummary.header']="Review and Deploy";
$RES['initWizard.reviewAndDeploy.pageSummary.body']="<p>Please review your settings below.</p><p>If you wish to make any changes, you can click on any of the installation stages above.  Note that if you do go back to a stage, you will have to go through all the subsequent stages again.</p>";
$RES['initWizard.reviewAndDeploy.submit.label']='Deploy';
$RES['addNodesWizard.addNodes.pageSummary.header']="Which nodes are you adding to the cluster?";
$RES['addNodesWizard.addNodes.pageSummary.body']="We will use the SSH private key for the <i>root</i> user and a file containing a list of hostnames to perform installation on your nodes.  The corresponding public key must already be in <i>authorized_keys</i> on all the nodes.";
$RES['addNodesWizard.addNodes.submit.label']="Add Nodes";
$RES['addNodesWizard.selectServices.pageSummary.header']='Install Services';
$RES['addNodesWizard.selectServices.pageSummary.body']="Select the services that you wish to install on your nodes.  Note that some services are selected by default and cannot be deselected.";
$RES['addNodesWizard.selectServices.submit.label']="Proceed";
$RES['uninstallWizard.pageSummary.header']="Uninstall Cluster";
$RES['uninstallWizard.pageSummary.body']="<span class='label label-important'>Warning</span>  Proceed with caution!  This will delete all services from all of your nodes in the cluster (and all of your data, if you choose the <i>wipe out</i> option).";
$RES['uninstallWizard.wipeout.label']="Perform wipe out (delete all data)";
$RES['uninstallWizard.submit.label']="Uninstall Cluster";
$RES['initWizard.configureClusterAdvanced.validation.error']="We've encountered invalid settings.";
?>
