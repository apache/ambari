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
App.db = {};

if (typeof Storage !== 'undefined') {
  Storage.prototype.setObject = function(key,value) {
    this.setItem(key, JSON.stringify(value));
  }

  Storage.prototype.getObject = function(key) {
    var value = this.getItem(key);
    return value && JSON.parse(value);
  }
} else {
  // stub for unit testing purposes
  window.localStorage = {};
  localStorage.setItem = function (key, val) {
    this[key] = val;
  }
  localStorage.getItem = function (key) {
    return this[key];
  }
  window.localStorage.setObject = function(key, value) {
    this[key] = value;
  };
  window.localStorage.getObject = function(key, value) {
    return this[key];
  };
}

App.db.cleanUp = function() {
  console.log('TRACE: Entering db:cleanup function');
  App.db.data = {
    'app' : {
      'loginName' : '',
      'authenticated' : false
    }
  }
  localStorage.setObject('ambari',App.db.data);
}

// called whenever user logs in
if(localStorage.getObject('ambari') == null) {
  console.log('doing a cleanup');
  App.db.cleanUp();
}



/*
 * setter methods
 */



App.db.setLoginName = function(name) {
  console.log('TRACE: Entering db:setLoginName function');
  App.db.data = localStorage.getObject('ambari');
  App.db.data.app.loginName = name;
  localStorage.setObject('ambari',App.db.data);
}

App.db.setAuthenticated = function(authenticated) {
  console.log('TRACE: Entering db:setAuthenticated function');

  App.db.data = localStorage.getObject('ambari');
  console.log('present value of authentication is: ' + App.db.data.app.authenticated);
  console.log('desired value of authentication is: ' + authenticated) ;
  App.db.data.app.authenticated = authenticated;
  localStorage.setObject('ambari',App.db.data);
  App.db.data = localStorage.getObject('ambari');
  console.log('Now present value of authentication is: ' + App.db.data.app.authenticated);
}

App.db.setSection = function(section) {
  console.log('TRACE: Entering db:setSection function');
  App.db.data = localStorage.getObject('ambari');
  var user = App.db.data.app.loginName;
  if(App.db.data[user] == undefined) {
    App.db.data[user] = {'name':user};
  }
  if (App.db.data[user].ClusterName == undefined) {
    App.db.data[user].ClusterName = {};
  }
  App.db.data[user].section = section;
  localStorage.setObject('ambari',App.db.data);
}

App.db.setInstallerCurrentStep = function (currentStep, completed) {
  console.log('TRACE: Entering db:setInstallerCurrentStep function');
  App.db.data = localStorage.getObject('ambari');
  var user = App.db.data.app.loginName;
  if(App.db.data[user] == undefined) {
    console.log('In data[user] condition');
    App.db.data[user] = {'name':user};
    console.log('value of data[user].name: '+ App.db.data[user].name);
  }
  if (App.db.data[user].Installer == undefined) {
    App.db.data[user].Installer = {};
    console.log('');
  }
  App.db.data[user].Installer.currentStep = currentStep;
  App.db.data[user].Installer.completed = completed;
  localStorage.setObject('ambari',App.db.data);
}

App.db.setClusterName = function(name) {
  console.log('TRACE: Entering db:setClusterName function');
  App.db.data = localStorage.getObject('ambari');
  var user = App.db.data.app.loginName;
  // all information from Installer.ClusterName will be transferred to clusters[ClusterName] when app migrates from installer to main
  if(App.db.data[user] == undefined) {
    App.db.data[user] = {'name':user};
  }
  if (App.db.data[user].clusters == undefined) {
    App.db.data[user].clusters = {};
  }

  if (App.db.data[user].Installer == undefined) {
    App.db.data[user].Installer = {};
  }
  App.db.data[user].Installer.ClusterName = name;
  localStorage.setObject('ambari',App.db.data);
}

App.db.setHosts = function(hostInfo) {
	console.log('TRACE: Entering db:setHosts function');
  App.db.data = localStorage.getObject('ambari');
  var user = App.db.data.app.loginName;
  if (App.db.data[user] == undefined) {
    App.db.data[user] = {'name':user};
  }
  App.db.data[user].Installer.hostInfo = hostInfo;
  localStorage.setObject('ambari',App.db.data);
}

App.db.setSoftRepo = function(softRepo) {
	console.log('TRACE: Entering db:setSoftRepo function');
	App.db.data = localStorage.getObject('ambari');
	var user = App.db.data.app.loginName;
	if(App.db.data[user] == undefined) {
		App.db.data[user] = {'name':user};
	}
  App.db.data[user].Installer.softRepo = softRepo;
  localStorage.setObject('ambari',App.db.data);
}

App.db.removeHosts = function(hostInfo) {
  console.log('TRACE: Entering db:setSoftRepo function');
  var hostList = App.db.getHosts();
  hostInfo.forEach(function(_hostInfo) {
    var host = _hostInfo.hostName;
    delete hostList[host];
  });
  App.db.setHosts(hostList);
}

App.db.setSelectedServiceNames = function(serviceNames) {
  App.db.data = localStorage.getObject('ambari');
  var user = App.db.data.app.loginName;
  App.db.data[user].Installer.selectedServiceNames = serviceNames;
  localStorage.setObject('ambari', App.db.data);
}

App.db.setMasterComponentHosts = function(masterComponentHosts) {
  App.db.data = localStorage.getObject('ambari');
  var user = App.db.data.app.loginName;
  App.db.data[user].Installer.masterComponentHosts = masterComponentHosts;
  localStorage.setObject('ambari', App.db.data);
}


App.db.setHostSlaveComponents = function(hostSlaveComponents) {
  App.db.data = localStorage.getObject('ambari');
  var user = App.db.data.app.loginName;
  App.db.data[user].Installer.hostSlaveComponents = hostSlaveComponents;
  localStorage.setObject('ambari', App.db.data);
}

App.db.setSlaveComponentHosts = function(slaveComponentHosts) {
  App.db.data = localStorage.getObject('ambari');
  var user = App.db.data.app.loginName;
  App.db.data[user].Installer.slaveComponentHosts = slaveComponentHosts;
  localStorage.setObject('ambari', App.db.data);
}

App.db.setServiceConfigProperties = function(configProperties) {
  App.db.data = localStorage.getObject('ambari');
  var user = App.db.data.app.loginName;
  App.db.data[user].Installer.configProperties = configProperties;
  localStorage.setObject('ambari', App.db.data);
}



/*
 *  getter methods
 */

App.db.getLoginName = function() {
  console.log('Trace: Entering db:getLoginName function');
  App.db.data = localStorage.getObject('ambari');
  return App.db.data.app.loginName;
}

App.db.getAuthenticated = function() {
  console.log('Trace: Entering db:getAuthenticated function');
  App.db.data = localStorage.getObject('ambari');
  return App.db.data.app.authenticated;
}

App.db.getSection = function() {
  console.log('Trace: Entering db:getSection function');
  App.db.data = localStorage.getObject('ambari');
  var user = App.db.data.app.loginName
  if(App.db.data[user] == undefined || App.db.data[user] == '') {
    return 0;
  }
  return App.db.data[user].section;
}

App.db.getClusterName = function() {
  console.log('Trace: Entering db:getClusterName function');
  App.db.data = localStorage.getObject('ambari');
  var user = App.db.data.app.loginName;
  if(user) {
    return App.db.data[user].Installer.ClusterName;
  }
}

App.db.getInstallerCurrentStep = function() {
  console.log('Trace: Entering db:getInstallerCurrentStep function');
  App.db.data = localStorage.getObject('ambari');
  var user = App.db.data.app.loginName;
  if(App.db.data[user] == undefined || App.db.data[user] == '') {
     return 0;
  }
  return App.db.data[user].Installer.currentStep;
}

App.db.isCompleted = function() {
  App.db.data = localStorage.getObject('ambari');
  var user = App.db.data.app.loginName;
  return App.db.data[user].Installer.completed;
}

App.db.getHosts = function() {
	console.log('TRACE: Entering db:getHosts function');
  App.db.data = localStorage.getObject('ambari');
  var user = App.db.data.app.loginName;
	if(App.db.data[user] == undefined || App.db.data[user] == '') {
		console.log('ERROR: loginName required for storing host info');
		return 0;
	}
  return App.db.data[user].Installer.hostInfo;
}

App.db.getSelectedServiceNames = function() {
  App.db.data = localStorage.getObject('ambari');
  var user = App.db.data.app.loginName;
  return App.db.data[user].Installer.selectedServiceNames;
}

App.db.getMasterComponentHosts = function() {
  App.db.data = localStorage.getObject('ambari');
  var user = App.db.data.app.loginName;
  return App.db.data[user].Installer.masterComponentHosts;
}

App.db.getHostSlaveComponents = function() {
  App.db.data = localStorage.getObject('ambari');
  var user = App.db.data.app.loginName;
  return App.db.data[user].Installer.hostSlaveComponents;
}

App.db.getSlaveComponentHosts = function() {
  App.db.data = localStorage.getObject('ambari');
  var user = App.db.data.app.loginName;
  return App.db.data[user].Installer.slaveComponentHosts;
}

App.db.setServiceConfigProperties = function() {
  App.db.data = localStorage.getObject('ambari');
  var user = App.db.data.app.loginName;
  return App.db.data[user].Installer.configProperties;
}

module.exports = App.db;
