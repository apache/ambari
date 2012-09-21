/**
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

Em.I18n.translations = {

  'app.name': 'Ambari',

  'login.header': 'Sign in',
  'login.username': 'Username',
  'login.password': 'Password',
  'login.loginButton': 'Sign in',
  'login.error': 'Invalid username/password combination.',

  'services.nagios.description': 'Nagios desc',
  'services.ganglia.description': 'Ganglia desc',
  'services.hdfs.description': 'HDFS desc',
  'services.mapreduce.description': 'MapReduce desc',
  'services.sqoop.description': 'Sqoop desc',
  'services.pig.description': 'Pig desc',
  'services.hive.description': 'Hive/HCat desc',
  'services.oozie.description': 'Oozie desc',
  'services.zookeeper.description': 'ZooKeeper desc',
  'services.hbase.description': 'HBase desc',

  'topnav.help.href': 'http://incubator.apache.org/ambari/install.html',

  'installer.step1.header': 'Ambari Cluster Install Wizard',
  'installer.step1.body': 'Welcome to Apache Ambari!<br>' +
    'Ambari makes it easy to install, manage, and monitor Hadoop clusters.<br>' +
    'We will walk you through the cluster installation process with this step-by-step wizard.',
  'installer.step1.clusterName': 'Name your cluster',
  'installer.step1.clusterName.tooltip.title': 'Cluster Name',
  'installer.step1.clusterName.tooltip.content': 'Enter a unique cluster name. Cluster name cannot be changed later.',
  'installer.step1.clusterName.error.required': 'Cluster Name is required',
  'installer.step1.clusterName.error.whitespaces': 'Cluster Name cannot contain white spaces',
  'installer.step1.clusterName.error.specialChar': 'Cluster Name cannot contain special characters',

  'installer.step2.header': 'Install Options',
  'installer.step2.targetHosts': 'Specify Hosts to Manage',
  'installer.step2.targetHosts.info': 'Enter a list of host names, one per line',
  'installer.step2.hostPattern.tooltip.title': 'Pattern Expressions',
  'installer.step2.hostPattern.tooltip.content': 'You can use pattern expressions to specify a number of target hosts.  Explain brackets.',
  'installer.step2.hostName.error.required': 'Host Names cannot be left empty',
  'installer.step2.hostName.error.notRequired': 'Host Names will be ignored if not using SSH to automatically configure hosts',
  'installer.step2.hostName.error.invalid': 'Invalid Host Name(s) - cannot start or end with a hyphen',
  'installer.step2.sshKey.error.required': 'SSH Private Key is required',
  'installer.step2.passphrase.error.match': 'Passphrases do not match',
  'installer.step2.manualInstallOption': 'Do not use SSH to automatically configure hosts ',
  'installer.step2.advancedOption': 'Advanced Options',
  'installer.step2.repoConf': 'Software Repository Configuration File Path',
  'installer.step2.localRepoOption': 'Use a local software repository',
  'installer.step2.localRepo.error.required': 'Local repository file path is required',
  'installer.step2.localRepo.info': '<p class=\"text-info\">The repository configuration file should be installed on each host in your cluster. ' +
    'This file instructs package manager to use your local software repository to retrieve software packages, instead of ' +
    'downloading from the internet.</p>',
  'installer.step2.localRepo.tooltip.title': 'Local Software Repository',
  'installer.step2.localRepo.tooltip.content': 'The repository configuration file should be installed on each host' +
    'in your cluster. This file instructs package manager to use your local' +
    'software repository to retrieve software packages, instead of using the internet.',
  'installer.step2.manualInstall.tooltip.title': 'Not Using SSH (Manual Install)',
  'installer.step2.manualInstall.tooltip.content': 'Ambari agents will not be installed ' +
    'automatically. You need to install it manually on each host that you want to manage as part of ' +
    'your cluster. Agents should be started before you move to the next stage.',
  'installer.step2.manualInstall.popup.header': 'Before You Proceed',
  'installer.step2.manualInstall.popup.body': 'You must install Ambari Agents on each host you want to manage before you proceed.  <a href="#" target="_blank">Learn more</a>',

  'installer.step3.header': 'Confirm Hosts',
  'installer.step3.body': 'Here are the results of the host discovery process.<br>' +
    'Please verify and remove the ones that you do not want to be the part of the cluster.',
  'installer.step3.hostLog.popup.header': 'Log file for the host',
  'installer.step3.hostLog.popup.body': 'Placeholder for the log file',

  'installer.step4.header': 'Choose Services',
  'installer.step4.body': 'Choose which services you want to install on your cluster.<br>Note that some services have dependencies (e.g., HBase requires ZooKeeper.)',

  'installer.step5.header': 'Assign Masters',
  'installer.step5.body': 'Assign master components to hosts you want to run them on.',

  'installer.step6.header': 'Assign Slaves',
  'installer.step6.body': 'Assign slave components to hosts you want to run them on.',
  'installer.step6.error.mustSelectOne': 'You must assign at least one host to each.',

  'installer.step7.header': 'Customize Services',
  'installer.step7.body': 'We have come up with recommended configurations for the services you selected.  Customize them as you see fit.',
  'installer.step7.attentionNeeded': '<strong>Attention:</strong> Some configurations need your attention before you can proceed.',

  'installer.step8.header': 'Review',

  'installer.step9.header': 'Install, Start and Test',

  'installer.step10.header': 'Summary'
};
