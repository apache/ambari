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


var App = require('app');

App.messages = {
  'app_name': 'Ambari',
  'page_title': 'Ambari',
  'installer_welcome': 'Welcome to Ambari installation wizard',
  'login_error': 'Invalid username/password combination.',
  'login_header': 'Sign in',
  'username_label': 'Username',
  'password_label': 'Password',
  'login_button': 'Sign in',
  'step1_header': 'Ambari Cluster Install Wizard',
  'step1_body': 'Welcome to Apache Ambari<br>' +
    'Ambari makes it really easy to install,manage, and monitor Hadoop clusters.<br\>' +
    'We will walk you through the cluster installation process with this ' +
    'step-by-step wizard.',
  'step2_header': 'Install Options',
  'step3_header': 'Confirm Hosts',
  'step3_body': 'Here are the results of the host discovery process.<br/> ' +
    'Please verify and remove the ones that you do not want to be the part of the cluster.',
  'step4_header': 'Choose Services',
  'step5_header': 'Assign Masters',
  'step6_header': 'Assign Slaves',
  'step7_header': 'Customize Services',
  'step8_header': 'Review',
  'step9_header': 'Install, Start and Test',
  'step10_header': 'Summary',
  'step1_clusterPop': 'Enter a unique cluster name. Cluster name cannot be changed later.',
  'step1_clusterName_error_required': 'Cluster Name is required',
  'step1_clusterName_error_whitespaces': 'Cluster Name cannot contain white spaces',
  'step1_clusterName_error_specialChar': 'Cluster Name cannot contain special character',
  'topnav_help_link': 'http://incubator.apache.org/ambari/install.html',
  'welcome_header': 'Welcome to Ambari!',
  'step2_targetHosts': 'Enter a list of host names, one per line. Or use',
  'step2_targetHosts_label': 'Specify Hosts to Manage',
  'step2_hostNameEmptyError': 'host names cannot be left empty',
  'step2_sshKeyNullErr': 'ssh key cannot be empty',
  'step2_passphraseMatchErr': 'Passphrases do not match',
  'step2_hostNameNotRequireErr': 'Host names not required for ssh-less install of ambari agents',
  'step2_hostNameErr': 'Invalid host name: host name cannot start or end with hyphen',
  'step2_manualInstallOpt': 'Do not use SSH to automatically configure the hosts ',
  'step2_localRepoOpt': '',
  'step2_softRepo_default_localPath': '/etc/yum/repos.d/hdp',
  'step2_softRepo_remotePath': '',
  'step2_advancedOption_label': 'Advanced Options',
  'step2_repoConf_label': 'Software Repository Configuration File Path',
  'step2_localRepoOpt': 'Use a local software repository',
  'step2_localRepoErr': 'Local repository file path is required',
  'step2_localRepoExplan': '<p class=\"text-info\">The repository configuration file should be installed on each host in your cluster. ' +
    'This file instructs package manager to use your local software repository to retrieve software packages,instead of ' +
    'using internet.</p>',
  'step2_hostPatternPop': 'Space, tab or new line can be used as deliminiter',
  'step2_localRepoPop': 'The repository configuration file should be installed on each host' +
    'in your cluster. This file instructs package manager to use your local' +
    'software repository to retrieve software packages, instead of using the internet.',
  'step2_manualInstallExplain': 'Ambari agents will not be installed ' +
    'automatically. You will require to install it manually on each host of ' +
    'your cluster. Agents should be started before you move to next stage.',
  'step2_popupBody': 'Ambari agents should be manually installed before you proceed ahead.',
  'welcome_body': '<p>Ambari makes it easy for you to install, configure, and manage your Hadoop cluster.<br>First, ' +
    'we will walk you through setting up your cluster with a step-by-step wizard.</p>',
  'welcome_note': 'Before you proceed, make sure you have performed all the pre-installation steps.',
  'welcome_submit_label': 'Get started',
  'installFailed_header': 'Cluster installation failed',
  'installFailed_body': 'Cluster installation failed.  To continue, you will need to uninstall the cluster first and ' +
    're-install the cluster.',
  'installFailed_submit_label': 'Start the uninstall process',
  'uninstallFailed_header': 'Cluster uninstallation failed',
  'uninstallFailed_body': 'Failed to uninstall the cluster'
};

