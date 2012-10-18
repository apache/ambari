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

  'installer.header': 'Cluster Install Wizard',
  'installer.step1.header': 'Welcome',
  'installer.step1.body.header': 'Welcome to Apache Ambari!',
  'installer.step1.body': 'Ambari makes it easy to install, manage, and monitor Hadoop clusters.<br>' +
    'We will walk you through the cluster installation process with this step-by-step wizard.',
  'installer.step1.clusterName': 'Name your cluster',
  'installer.step1.clusterName.tooltip.title': 'Cluster Name',
  'installer.step1.clusterName.tooltip.content': 'Enter a unique cluster name. Cluster name cannot be changed later.',
  'installer.step1.clusterName.error.required': 'Cluster Name is required',
  'installer.step1.clusterName.error.whitespaces': 'Cluster Name cannot contain white spaces',
  'installer.step1.clusterName.error.specialChar': 'Cluster Name cannot contain special characters',

  'installer.step2.header': 'Install Options',
  'installer.step2.body': 'Enter the list of hosts to be included in the cluster, provide your SSH key, and optionally specify a local repository.',
  'installer.step2.targetHosts': 'Target Hosts',
  'installer.step2.targetHosts.info': 'Enter a list of host names, one per line',
  'installer.step2.hostPattern.tooltip.title': 'Pattern Expressions',
  'installer.step2.hostPattern.tooltip.content': 'You can use pattern expressions to specify a number of target hosts.  Explain brackets.',
  'installer.step2.hostName.error.required': 'Host Names cannot be left empty',
  'installer.step2.hostName.error.notRequired': 'Host Names will be ignored if not using SSH to automatically configure hosts',
  'installer.step2.hostName.error.invalid': 'Invalid Host Name(s) - cannot start or end with a hyphen',
  'installer.step2.sshKey': 'Host Connectivity Information',
  'installer.step2.sshKey.info': 'Provide your SSH Private Key (<b>id_rsa</b> for <b>root</b>)',
  'installer.step2.sshKey.error.required': 'SSH Private Key is required',
  'installer.step2.passphrase.error.match': 'Passphrases do not match',
  'installer.step2.manualInstall.label': 'Do not use SSH to automatically configure hosts ',
  'installer.step2.manualInstall.info': 'By not using SSH to connect to the target hosts, you must manually install and start the ' +
    'Ambari Agent on each host in order for the wizard to perform the necessary configurations and software installs.',
  'installer.step2.advancedOption': 'Advanced Options',
  'installer.step2.repoConf': 'Software Repository Configuration File Path',
  'installer.step2.localRepo.header': 'Software repository',
  'installer.step2.localRepo.label': 'Use a local software repository',
  'installer.step2.localRepo.error.required': 'Local repository file path is required',
  'installer.step2.localRepo.info': 'The repository configuration file should be installed on each host in your cluster. ' +
    'This file instructs the package manager to use your local software repository to retrieve software packages, instead of ' +
    'downloading them from the internet.',
  'installer.step2.localRepo.tooltip.title': 'Local Software Repository',
  'installer.step2.localRepo.tooltip.content': 'The repository configuration file should be installed on each host ' +
    'in your cluster. This file instructs package manager to use your local' +
    'software repository to retrieve software packages, instead of using the internet.',
  'installer.step2.manualInstall.tooltip.title': 'Not Using SSH (Manual Install)',
  'installer.step2.manualInstall.tooltip.content': 'If you do not wish Ambari to automatically configure the target hosts via SSH,' +
    ' you have the option of configuring them yourself.  This involves installing and starting Ambari Agent on each of your target hosts.',
  'installer.step2.manualInstall.popup.header': 'Before You Proceed',
  'installer.step2.manualInstall.popup.body': 'You must install Ambari Agents on each host you want to manage before you proceed.  <a href="#" target="_blank">Learn more</a>',

  'installer.step3.header': 'Confirm Hosts',
  'installer.step3.body': 'Here are the results of the host discovery process.<br>' +
    'Please verify and remove the ones that you do not want to be part of the cluster.',
  'installer.step3.hostLog.popup.header': 'Log file for the host',
  'installer.step3.hostLog.popup.body': 'Placeholder for the log file',
  'installer.step3.hosts.remove.popup.header': 'Remove Hosts',
  'installer.step3.hosts.remove.popup.body': 'Are you sure you want to remove the selected host(s)?',
  'installer.step3.hosts.retry.popup.header': 'Retry Host Discovery',
  'installer.step3.hosts.retry.popup.body': 'Are you sure you want to retry discovery of the selected host(s)?',

  'installer.step4.header': 'Choose Services',
  'installer.step4.body': 'Choose which services you want to install on your cluster.<br>Note that some services have dependencies (e.g., HBase requires ZooKeeper.)',
  'installer.step4.mapreduceCheck.popup.header': 'MapReduce Needed',
  'installer.step4.mapreduceCheck.popup.body': 'You did not select MapReduce, but it is needed by other services you selected.  We will automatically add MapReduce.  Is this OK?',
  'installer.step4.monitoringCheck.popup.header': 'Limited Functionality Warning',
  'installer.step4.monitoringCheck.popup.body': 'You did not select Nagios and/or Ganglia.  If both are not selected, monitoring and alerts will not function properly.  Is this OK?',

  'installer.step5.header': 'Assign Masters',
  'installer.step5.attention': ' hosts not running master services',
  'installer.step5.body': 'Assign master components to hosts you want to run them on.',

  'installer.step6.header': 'Assign Slaves',
  'installer.step6.body': 'Assign slave components to hosts you want to run them on.',
  'installer.step6.error.mustSelectOne': 'You must assign at least one host to each.',

  'installer.step7.header': 'Customize Services',
  'installer.step7.body': 'We have come up with recommended configurations for the services you selected.  Customize them as you see fit.',
  'installer.step7.attentionNeeded': '<strong>Attention:</strong> Some configurations need your attention before you can proceed.',

  'installer.step8.header': 'Review',
  'installer.step8.body': 'Please review the cluster configuration before installation',

  'installer.step9.header': 'Install, Start and Test',
  'installer.step9.body': 'Wait to complete the cluster installation. Installing, Starting and Testing selected services',
  'installer.step9.status.success': 'Successfully installed the cluster',
  'installer.step9.status.failed': 'Failure in installation',
  'installer.step9.host.status.success': 'success',
  'installer.step9.host.status.warning': 'tolerable failures encountered',
  'installer.step9.host.status.failed': 'failures encountered',

  'installer.step10.header': 'Summary',

  'form.create': 'Create',
  'form.save': 'Save',
  'form.cancel': 'Cancel',
  'form.password': 'Password',
  'form.passwordRetype': 'Retype Password',
  'form.saveSuccess': 'Successfully saved.',
  'form.saveError': 'Sorry, errors occured.',

  'form.validator.invalidIp': 'Please enter valid ip address',

  'admin.advanced.title': 'Advanced',
  'admin.advanced.caution': 'This section is for advanced user only.<br/>Proceed with caution.',
  'admin.advanced.button.uninstallIncludingData': 'Uninstall cluster including all data.',
  'admin.advanced.button.uninstallKeepData': 'Uninstall cluster but keep data.',

  'admin.advanced.popup.header': 'Uninstall Cluster',
  /*'admin.advanced.popup.text':'Uninstall Cluster',*/

  'admin.audit.grid.date': "Date/Time",
  'admin.audit.grid.category': "Category",
  'admin.audit.grid.operationName': "Operation",
  'admin.audit.grid.performedBy': "Performed By",
  'admin.audit.grid.service': "Category",

  'admin.authentication.form.method.database': 'Use Ambari Database to authenticate users',
  'admin.authentication.form.method.ldap': 'Use LDAP/Active Directory to authenticate',
  'admin.authentication.form.primaryServer': 'Primary Server',
  'admin.authentication.form.secondaryServer': 'Secondary Server',
  'admin.authentication.form.useSsl': 'Use SSL',
  'admin.authentication.form.bind.anonymously': "Bind Anonymously",
  'admin.authentication.form.bind.useCrenedtials': "Use Credentials To Bind",
  'admin.authentication.form.bindUserDN': 'Bind User DN',
  'admin.authentication.form.searchBaseDN': 'Search Base DN',
  'admin.authentication.form.usernameAttribute': 'Username Attribute',

  'admin.authentication.form.userDN': 'User DN',
  'admin.authentication.form.password': 'Password',
  'admin.authentication.form.configurationTest': 'Configuration Test',
  'admin.authentication.form.testConfiguration': 'Test Configuration',

  'admin.authentication.form.test.success': 'The configuration passes the test',
  'admin.authentication.form.test.fail': 'The configuration fails the test',

  'admin.security.title': 'Kerberos Security has not been enabled on this cluster.',
  'admin.security.button.enable': 'Enable Kerberos Security on this cluster',

  'admin.users.ldapAuthentionUsed': 'LDAP Authentication is being used to authenticate users',
  'admin.users.deleteYourselfMessage': 'You can\'t delete yourself',
  'admin.users.addButton': 'Add User',
  'admin.users.delete': 'delete',
  'admin.users.edit': 'edit',
  'admin.users.privileges': 'Admin',
  'admin.users.password': 'Password',
  'admin.users.passwordRetype': 'Retype Password',
  'admin.users.username': 'Username',

  'question.sure': 'Are you sure?',

  'services.service.start': 'Start',
  'services.service.stop': 'Stop',
  'services.service.start.popup.header': 'Confirmation',
  'services.service.stop.popup.header': 'Confirmation',
  'services.service.start.popup.body': 'Are you sure?',
  'services.service.stop.popup.body': 'Are you sure?',
  'services.service.summary.version': 'Version',
  'services.service.summary.nameNode': 'NameNode',
  'services.service.summary.nameNodeUptime': 'NameNode Uptime',
  'services.service.summary.nameNodeHeap': 'NameNode Heap',
  'services.service.summary.pendingUpgradeStatus': 'HDFS Pending Upgrade Status',
  'services.service.summary.safeModeStatus': 'HDFS Safe Mode Status',
  'services.service.summary.dataNodes': 'DataNodes (live/dead/decom)',
  'services.service.summary.diskCapacity': 'HDFS Disk Capacity',
  'services.service.summary.blocksTotal': 'Blocks (total)',
  'services.service.summary.blockErrors': 'Block Errors (corr./miss./underrep.)',
  'services.service.summary.totalFiles': 'Total Files + Directory Count',

  'hosts.host.start.popup.header': 'Confirmation',
  'hosts.host.stop.popup.header': 'Confirmation',
  'hosts.host.start.popup.body': 'Are you sure?',
  'hosts.host.stop.popup.body': 'Are you sure?',
  'hosts.assignedToRack.popup.body': 'Are you sure?',
  'hosts.assignedToRack.popup.header': 'Confirmation',
  'hosts.decommission.popup.body': 'Are you sure?',
  'hosts.decommission.popup.header': 'Confirmation',
  'hosts.delete.popup.body': 'Are you sure?',
  'hosts.delete.popup.header': 'Confirmation',

  'charts.horizon.chart.showText': 'show',
  'charts.horizon.chart.hideText': 'hide',
  'charts.horizon.chart.attributes.cpu': 'CPU',
  'charts.horizon.chart.attributes.memory': 'Memory',
  'charts.horizon.chart.attributes.network': 'Network',
  'charts.horizon.chart.attributes.io': 'I/O',

  'metric.default': 'default',
  'metric.cpu': 'cpu',
  'metric.memory': 'disk used',
  'metric.network': 'network',
  'metric.io': 'io',
  'hosts.add.header' : 'Add Host Wizard',
  'hosts.add.step2.warning' : 'Hosts are already part of the cluster and will be ignored'
};