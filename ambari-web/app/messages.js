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
	'login_header': 'Ambari Login',
	'username_label': 'Username',
	'password_label': 'Password',
	'login_button': 'Login',

	'step1_header': 'Welcome',
	'step2_header': 'Install Options',
	'step3_header': 'Confirm Hosts',
	'step4_header': 'Choose Services',
	'step5_header': 'Assign Services',
	'step6_header': 'Customize Services',
	'step7_header': 'Review',
	'step8_header': 'Install, Start and Test',
	'step9_header': 'Summary',
	'page_footer_body': '<a href = \"http://www.apache.org/licenses/LICENSE-2.0\" target = \"_blank\">Licensed under the ' +
		'Apache License, Version 2.0</a>.<br><a href = \"/licenses/NOTICE.txt\" target = \"_blank\">See third-party ' +
		'tools/resources that Ambari uses and their respective authors</a>',
	'step1_clusterName_error_null': 'Cluster Name cannot be null value',
	'step1_clusterName_error_Whitespaces': 'Cluster Name cannot contain white spaces',
	'step1_clusterName_error_specialChar': 'Cluster Name cannot have hyphen as first or last alphabet',
	'topnav_help_link': 'http://incubator.apache.org/ambari/install.html',
	'welcome_header': 'Welcome to Ambari!',
	'step2_targetHosts': '<p>Enter a list of host names, one per line. Or use <a href=\"javascript:void 0\"> ' +
		'Pattern expression</a> </p>',
	'step2_targetHosts_label': 'Specify Hosts to Manage',
	'step2_hostNameEmptyError': 'host names cannot be left empty',
	'step2_sshKeyNullErr': 'ssh key cannot be empty' ,
	'step2_passphraseMatchErr': '\"Confirm passphrase\" doesn\'t matches \"passphrase\" value',
	'step2_hostNameNotRequireErr' : 'Host names not required for manual install of ambari agents',
	'step2_softRepo_default_localPath': '/etc/yum/repos.d/hdp',
	'step2_softRepo_remotePath': '',
	'step2_advancedOption_label': 'Advanced Options',
	'step2_repoConf_label': 'Yum Repository Configuration File Path',
	'step2_localRepoExplan': '<p>The repository configuration file should be installed on each host in your cluster. ' +
		'This file instructs package manager to use your local software repository to retrieve software packages,instead of ' +
		'using internet.</p>',
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

