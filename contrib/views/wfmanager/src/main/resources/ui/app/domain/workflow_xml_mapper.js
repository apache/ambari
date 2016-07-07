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
import * as nodeHandler from '../domain/node-handler';

import {MappingMixin,ConfigurationMapper} from "../domain/mapping-utils";
var WorkflowXmlMapper= Ember.Object.extend({
  nodeHandlerMap:null,
  globalConfigHandler:null,
  actionTypeResolver:null,
  schemaVersions:null,
  init: function() {
    this.actionTypeResolver=nodeHandler.ActionTypeResolver.create({schemaVersions:this.schemaVersions});
    this.set("globalConfigHandler",GlobalConfigHandler.create({}));
    this.nodeHandlerMap=new Map();
    this.nodeHandlerMap.set("start",nodeHandler.StartNodeHandler.create({}));
    this.nodeHandlerMap.set("end",nodeHandler.EndNodeHandler.create({}));
    this.nodeHandlerMap.set("action",nodeHandler.ActionNodeHandler.create({actionTypeResolver:this.actionTypeResolver}));
    this.nodeHandlerMap.set("decision",nodeHandler.DecisionNodeHandler.create({}));
    this.nodeHandlerMap.set("fork",nodeHandler.ForkNodeHandler.create({}));
    this.nodeHandlerMap.set("join",nodeHandler.JoinNodeHandler.create({}));
    this.nodeHandlerMap.set("kill",nodeHandler.KillNodeHandler.create({}));
  },
  getNodeHandler(type){
    return this.nodeHandlerMap.get(type);
  },
  getGlobalConfigHandler(){
    return this.globalConfigHandler;
  },
  getActionJobHandler(jobType){
    return this.actionTypeResolver.getActionJobHandler(jobType);
  },
});
var GlobalConfigHandler=Ember.Object.extend(MappingMixin,{
  mapping:null,
  configurationMapper:ConfigurationMapper.create({}),
  init(){
    this.mapping=[
      {xml:"job-tracker",domain:"jobTracker"},
      {xml:"name-node",domain:"nameNode"},
      {xml:"configuration",customHandler:this.configurationMapper}
    ];
  },

  handle(domainObject,nodeObj){
    if (!domainObject){
      console.log("no domain object set");
      return;
    }
    var globalObj={};
    nodeObj["global"]=globalObj;
    this.handleMapping(domainObject,globalObj,this.mapping);
  }
});
export {WorkflowXmlMapper};
