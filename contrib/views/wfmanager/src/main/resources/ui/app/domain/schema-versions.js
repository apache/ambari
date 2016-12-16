/*
*    Licensed to the Apache Software Foundation (ASF) under one or more
*    contributor license agreements.  See the NOTICE file distributed with
*    this work for additional information regarding copyright ownership.
*    The ASF licenses this file to You under the Apache License, Version 2.0
*    (the "License"); you may not use this file except in compliance with
*    the License.  You may obtain a copy of the License at
*
*        http://www.apache.org/licenses/LICENSE-2.0
*
*    Unless required by applicable law or agreed to in writing, software
*    distributed under the License is distributed on an "AS IS" BASIS,
*    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*    See the License for the specific language governing permissions and
*    limitations under the License.
*/
import Ember from 'ember';
export default Ember.Object.extend({
  actionVersions: Ember.Map.create(),
  currentActionVersion:Ember.Map.create(),
  workflowVersions: [],
  workflowVersion: null,
  bundleVersions: [],
  bundleVersion: null,
  coordinatorVersions: [],
  coordinatorVersion: null,
  clone : {},
  actionSchemaMap:{
    "hive":["0.6","0.5","0.4","0.3","0.2","0.1"],
    "hive2":["0.2","0.1"],
    "sqoop":["0.3","0.2","0.1"],
    "shell":["0.3","0.2","0.1"],
    "spark":["0.2","0.1"],
    "distcp":["0.2","0.1"],
    "email":["0.2","0.1"]
  },
  createCopy(){
    this.clone.workflowVersion = this.workflowVersion;
    this.clone.currentActionVersion = this.currentActionVersion.copy();
    this.clone.coordinatorVersion = this.coordinatorVersion;
    this.clone.bundleVersion = this.bundleVersion;
  },
  rollBack(){
    this.workflowVersion = this.clone.workflowVersion;
    this.currentActionVersion = this.clone.currentActionVersion.copy();
  },
  importAdminConfigs(){
    var url = Ember.ENV.API_URL + "/v1/admin/configuration";
    var deferred = Ember.RSVP.defer();
    Ember.$.ajax({
      url: url,
      method: "GET",
      dataType: "text",
      contentType: "text/plain;charset=utf-8",
      beforeSend: function(request) {
        request.setRequestHeader("X-Requested-By", "workflow-designer");
      },
      success: function(response) {
        deferred.resolve(response);
      }.bind(this),
      error: function(response) {
        deferred.reject(response);
      }.bind(this)
    });
    return deferred;
  },
  setWfSchemaVersions(configSettings) {
    this.workflowVersions = [];
    var wfSchemaVersions = configSettings["oozie.service.SchemaService.wf.schemas"].trim().split(",");
    wfSchemaVersions = wfSchemaVersions.map(Function.prototype.call, String.prototype.trim).sort();
    wfSchemaVersions.forEach(function(wfSchemaVersion) {
      var wfSchema = wfSchemaVersion.split("-");
      var wfSchemaName = wfSchema[0];
      var wfSchemaType = wfSchema[1];
      var wfSchemaVersionNumber = wfSchema[2].replace(".xsd", "");
      if (wfSchemaType === "action") {
        if (this.actionVersions.get(wfSchemaName)) {
          this.actionVersions.get(wfSchemaName).push(wfSchemaVersionNumber);
          this.currentActionVersion.set(wfSchemaName, wfSchemaVersionNumber);
        } else {
          this.actionVersions.set(wfSchemaName, [wfSchemaVersionNumber]);
          this.currentActionVersion.set(wfSchemaName, wfSchemaVersionNumber);
        }
      } else if (wfSchemaType === "workflow") {
        this.workflowVersions.push(wfSchemaVersionNumber);
        this.workflowVersion = wfSchemaVersionNumber;
      }
    }.bind(this));
  },
  setCoordSchemaVersions(configSettings) {
    this.coordinatorVersions = [];
    var coordSchemaVersions = configSettings["oozie.service.SchemaService.coord.schemas"].trim().split(",");
    coordSchemaVersions = coordSchemaVersions.map(Function.prototype.call, String.prototype.trim).sort();
    coordSchemaVersions.forEach(function(coordSchemaVersion) {
      var coordSchema = coordSchemaVersion.split("-");
      var coordSchemaType = coordSchema[1];
      var coordSchemaVersionNumber = coordSchema[2].replace(".xsd", "");
      if (coordSchemaType === "coordinator") {
        this.coordinatorVersions.push(coordSchemaVersionNumber);
        this.coordinatorVersion = coordSchemaVersionNumber;
      }
    }.bind(this));
  },
  setBundleSchemaVersions(configSettings) {
    this.bundleVersions = [];
    var bundleSchemaVersions = configSettings["oozie.service.SchemaService.bundle.schemas"].trim().split(",");
    bundleSchemaVersions = bundleSchemaVersions.map(Function.prototype.call, String.prototype.trim).sort();
    bundleSchemaVersions.forEach(function(bundleSchemaVersion) {
      var bundleSchema = bundleSchemaVersion.split("-");
      var bundleSchemaType = bundleSchema[1];
      var bundleSchemaVersionNumber = bundleSchema[2].replace(".xsd", "");
      if (bundleSchemaType === "bundle") {
        this.bundleVersions.push(bundleSchemaVersionNumber);
        this.bundleVersion = bundleSchemaVersionNumber;
      }
    }.bind(this));
  },
  init(){
    var importAdminConfigsDefered=this.importAdminConfigs();
    importAdminConfigsDefered.promise.then(function(data){
      var configSettings = JSON.parse(data);
      if (!(configSettings instanceof Object)) {
        configSettings = JSON.parse(configSettings);
      }
      this.setWfSchemaVersions(configSettings);
      this.setCoordSchemaVersions(configSettings);
      this.setBundleSchemaVersions(configSettings);
      this.setDefaultActionVersions();
    }.bind(this)).catch(function(){
      this.setWorkflowVersions(["0.5","0.4.5","0.4","0.3","0.2.5","0.2","0.1"]);
      this.setCurrentWorkflowVersion("0.5");
      this.setCoordinatorVersions(["0.4","0.3", "0.2","0.1"]);
      this.setCurrentCoordinatorVersion("0.4");
      this.setBundleVersions(["0.2","0.1"]);
      this.setCurrentBundleVersion("0.2");
      this.setDefaultActionVersions();
      console.error("There is some problem while importing schema versions. defaulting to known versions.");
    }.bind(this));
  },
  setDefaultActionVersions(){
    var self=this;
    Object.keys(this.actionSchemaMap).forEach(function(key) {
      if (!self.actionVersions.get(key)){
        self.actionVersions.set(key,self.actionSchemaMap[key]);
        self.currentActionVersion.set(key,self.actionSchemaMap[key][0]);
      }
    });
  },
  getActionVersions(type){
    return this.actionVersions.get(type);
  },
  getActionVersion(type){
    return this.currentActionVersion.get(type);
  },
  getCurrentWorkflowVersion(){
    return this.workflowVersion;
  },
  getWorkflowVersions(){
    return this.workflowVersions;
  },
  getCurrentCoordinatorVersion(){
    return this.coordinatorVersion;
  },
  getCoordinatorVersions(){
    return this.coordinatorVersions;
  },
  getCurrentBundleVersion(){
    return this.bundleVersion;
  },
  getBundleVersions(){
    return this.bundleVersions;
  },
  setActionVersion(type, version){
    this.currentActionVersion.set(type, version);
  },
  setCurrentWorkflowVersion(version){
    this.workflowVersion = version;
  },
  setCurrentCoordinatorVersion(version){
    this.coordinatorVersion = version;
  },
  setCurrentBundleVersion(version){
    this.bundleVersion = version;
  },
  setWorkflowVersions(versions){
    this.workflowVersions = versions;
  },
  setCoordinatorVersions(versions){
    this.coordinatorVersions = versions;
  },
  setBundleVersions(versions){
    this.bundleVersions = versions;
  }

});
