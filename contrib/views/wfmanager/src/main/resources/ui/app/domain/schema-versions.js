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
  clone : {},
  createCopy(){
    this.clone.workflowVersion = this.workflowVersion;
    this.clone.currentActionVersion = this.currentActionVersion.copy();
  },
  rollBack(){
    this.workflowVersion = this.clone.workflowVersion;
    this.currentActionVersion = this.clone.currentActionVersion.copy();
  },
  init(){
    this.workflowVersion = "0.5";
    this.workflowVersions = ["0.5","0.4.5","0.4","0.3","0.2.5","0.2","0.1"];
    this.actionVersions.set("hive",["0.6","0.5","0.4","0.3","0.2","0.1"]);
    this.actionVersions.set("hive2",["0.2","0.1"]);
    this.actionVersions.set("pig",["0.3","0.2","0.1"]);
    this.actionVersions.set("sqoop",["0.3","0.2","0.1"]);
    this.actionVersions.set("shell",["0.3","0.2","0.1"]);
    this.actionVersions.set("spark",["0.2","0.1"]);
    this.actionVersions.set("distcp",["0.2","0.1"]);
    this.actionVersions.set("email",["0.2","0.1"]);

    this.currentActionVersion.set("hive","0.6");
    this.currentActionVersion.set("hive2","0.2");
    this.currentActionVersion.set("pig","0.3");
    this.currentActionVersion.set("sqoop","0.3");
    this.currentActionVersion.set("shell","0.3");
    this.currentActionVersion.set("spark","0.2");
    this.currentActionVersion.set("distcp","0.2");
    this.currentActionVersion.set("email","0.2");
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
  setActionVersion(type, version){
    this.currentActionVersion.set(type, version);
  },
  setCurrentWorkflowVersion(version){
    return this.workflowVersion = version;
  }

});
