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
import CommonUtils from "../utils/common-utils";
import {MappingMixin,ConfigurationMapper,PrepareMapper} from "../domain/mapping-utils";
var ActionJobHandler=Ember.Object.extend(MappingMixin,{
  type:"actionJob",
  context : {},
  configurationMapper:ConfigurationMapper.create({}),
  prepareMapper:PrepareMapper.create({}),
  setContext(context){
    this.context = context;
  },
  getContext(){
    return this.context;
  },
  handle(nodeDomain,nodeObj,nodeName){
    var actionObj={};
    nodeObj[this.get("actionType")]=actionObj;
    if (this.get("nameSpace")){
      var schemaVersion=this.schemaVersions.getActionVersion(this.get("actionType"));
      if (this.get("nameSpace")){
        var schema=this.get("nameSpace");
          if (schemaVersion){
            schema=CommonUtils.extractSchema(schema)+":"+schemaVersion;
          }
          nodeObj[this.get("actionType")]["_xmlns"]=schema;
      }
    }
    // var errors=this.validate(nodeDomain);
    // if (errors){
    //   errors.forEach(function(error){
    //       this.workflowContext.addError(error);
    //   });
    // }
    this.handleMapping(nodeDomain,actionObj,this.mapping,nodeName);
  },
  validate(nodeDomain){
    //overwrite in implmentations and return array of errors object.
  },
  // handlePrepare(node,nodeObj){
  //   console.log("handle prep called");
  //   if (node.prepare && node.prepare.length>0){
  //     var prepareObjs={};
  //     nodeObj["prepare"]=prepareObjs;
  //     node.prepare.forEach(function(prep){
  //       if (!prepareObjs[prep.type]){
  //         prepareObjs[prep.type]=[];
  //       }
  //       prepareObjs[prep.type].push({"_path":prep.path});
  //     });
  //   }
  // },
  handleImport(actionNode,json){
    this.handleImportMapping(actionNode,json,this.mapping);
  }
});
var JavaActionJobHandler=ActionJobHandler.extend({
  actionType:"java",
  mapping:null,
  init(){
    this.mapping=[
      {xml:"job-tracker",domain:"jobTracker"},
      {xml:"name-node",domain:"nameNode"},
      {xml:"prepare",customHandler:this.prepareMapper},
      {xml:"job-xml",domain:"jobXml",occurs:"many",domainProperty:"value"},
      {xml:"configuration",customHandler:this.configurationMapper},
      {xml:"main-class",domain:"mainClass",mandatory:true},
      {xml:"java-opts",domain:"javaOpts"},
      {xml:"java-opt",domain:"javaOpt",occurs:"many", domainProperty:"value"},
      {xml:"arg",domain:"args",occurs:"many",domainProperty:"value"},
      {xml:"file",domain:"files",occurs:"many",domainProperty:"value"},
      {xml:"archive",domain:"archives",occurs:"many",domainProperty:"value"},
      {xml:"capture-output",domain:"captureOutput",ignoreValue:true}
    ];
  },

  handleImport(actionNode,json){
    this._super(actionNode,json);
  }
});
var PigActionJobHandler=ActionJobHandler.extend({
  actionType:"pig",
  mapping:null,
  init(){
      this.mapping=[
        {xml:"job-tracker",domain:"jobTracker"},
        {xml:"name-node",domain:"nameNode"},
        {xml:"prepare",customHandler:this.prepareMapper},
        {xml:"job-xml",domain:"jobXml",occurs:"many",domainProperty:"value"},
        {xml:"configuration",customHandler:this.configurationMapper},
        {xml:"script",domain:"script",mandatory:true},
        {xml:"param",domain:"param",domainProperty:"value",occurs:"many"},
        {xml:"argument",domain:"args",occurs:"many",domainProperty:"value"},
        {xml:"file",domain:"files",occurs:"many",domainProperty:"value"},
        {xml:"archive",domain:"archives",occurs:"many",domainProperty:"value"}
      ];
  }
});
var HiveActionJobHandler=ActionJobHandler.extend({
  actionType:"hive",
  nameSpace:"uri:oozie:hive-action:0.6",
  mapping:null,
  init(){
      this.mapping=[
        {xml:"job-tracker",domain:"jobTracker"},
        {xml:"name-node",domain:"nameNode"},
        {xml:"prepare",customHandler:this.prepareMapper},
        {xml:"job-xml",domain:"jobXml",occurs:"many",domainProperty:"value"},
        {xml:"configuration",customHandler:this.configurationMapper},
        {xml:"script",domain:"script"},
        {xml:"query",domain:"query"},
        {xml:"param",domain:"params",domainProperty:"value",occurs:"many"},
        {xml:"argument",domain:"args",occurs:"many",domainProperty:"value"},
        {xml:"file",domain:"files",occurs:"many",domainProperty:"value"},
        {xml:"archive",domain:"archives",occurs:"many",domainProperty:"value"}
      ];
  },
  validate(nodeDomain){
    if (Ember.isBlank(nodeDomain.script) && Ember.isBlank(nodeDomain.query)){
      return [{message : "Either script or query to be set."}];
    }
  }
});
var Hive2ActionJobHandler=ActionJobHandler.extend({
  actionType:"hive2",
  nameSpace:"uri:oozie:hive2-action:0.2",
  mapping:null,
  init(){
      this.mapping=[
        {xml:"job-tracker",domain:"jobTracker"},
        {xml:"name-node",domain:"nameNode"},
        {xml:"prepare",customHandler:this.prepareMapper},
        {xml:"job-xml",domain:"jobXml",occurs:"many",domainProperty:"value"},
        {xml:"configuration",customHandler:this.configurationMapper},
        {xml:"jdbc-url",domain:"jdbc-url",mandatory:true},
        {xml:"password",domain:"password"},
        {xml:"script",domain:"script"},
        {xml:"query",domain:"query"},
        {xml:"param",domain:"params",domainProperty:"value",occurs:"many"},
        {xml:"argument",domain:"args",occurs:"many",domainProperty:"value"},
        {xml:"file",domain:"files",occurs:"many",domainProperty:"value"},
        {xml:"archive",domain:"archives",occurs:"many",domainProperty:"value"}
      ];
  },
  validate(nodeDomain){
    if (Ember.isBlank(nodeDomain.script) && Ember.isBlank(nodeDomain.query)){
      return [{message : "Either script or query to be set."}];
    }
  }
});

var SqoopActionJobHandler=ActionJobHandler.extend({
  actionType:"sqoop",
  nameSpace:"uri:oozie:sqoop-action:0.4",
  mapping:null,
  init(){
      this.mapping=[
        {xml:"job-tracker",domain:"jobTracker"},
        {xml:"name-node",domain:"nameNode"},
        {xml:"prepare",customHandler:this.prepareMapper},
        {xml:"job-xml",domain:"jobXml",occurs:"many",domainProperty:"value"},
        {xml:"configuration",customHandler:this.configurationMapper},
        {xml:"command",domain:"command"},
        {xml:"argument",domain:"args",occurs:"many",domainProperty:"value"},
        {xml:"file",domain:"files",occurs:"many",domainProperty:"value"},
        {xml:"archive",domain:"archives",occurs:"many",domainProperty:"value"}
      ];
  },
  validate(nodeDomain){
    if (Ember.isBlank(nodeDomain.command) && nodeDomain.args.length<1){
      return [{message : "Either command or arguments have to be set."}];
    }
  }
});
var ShellActionJobHandler=ActionJobHandler.extend({
  actionType:"shell",
  nameSpace:"uri:oozie:shell-action:0.3",
  mapping:null,
  init(){
    this.mapping=[
      {xml:"job-tracker",domain:"jobTracker"},
      {xml:"name-node",domain:"nameNode"},
      {xml:"prepare",customHandler:this.prepareMapper},
      {xml:"job-xml",domain:"jobXml",occurs:"many",domainProperty:"value"},
      {xml:"configuration",customHandler:this.configurationMapper},
      {xml:"exec",domain:"exec",mandatory:true},
      {xml:"argument",domain:"args",occurs:"many",domainProperty:"value"},
      {xml:"env-var",domain:"envVar",occurs:"many",domainProperty:"value"},
      {xml:"file",domain:"files",occurs:"many",domainProperty:"value"},
      {xml:"archive",domain:"archives",occurs:"many",domainProperty:"value"},
      {xml:"capture-output",domain:"captureOutput",ignoreValue:true}
    ];
  },

  handleImport(actionNode,json){
    this._super(actionNode,json);
  }
});
var SparkActionJobHandler=ActionJobHandler.extend({
  actionType:"spark",
  nameSpace:"uri:oozie:spark-action:0.2",
  mapping:null,
  init(){
    this.mapping=[
      {xml:"job-tracker",domain:"jobTracker"},
      {xml:"name-node",domain:"nameNode"},
      {xml:"prepare",customHandler:this.prepareMapper},
      {xml:"job-xml",domain:"jobXml",occurs:"many",domainProperty:"value"},
      {xml:"configuration",customHandler:this.configurationMapper},
      {xml:"master",domain:"master",mandatory:true,displayName:"Runs On"},
      {xml:"mode",domain:"mode"},
      {xml:"name",domain:"sparkName",mandatory:true},
      {xml:"class",domain:"class"},
      {xml:"jar",domain:"jar",mandatory:true,displayName:"Application"},
      {xml:"spark-opts",domain:"sparkOpts"},
      {xml:"arg",domain:"args",occurs:"many",domainProperty:"value"},
      {xml:"file",domain:"files",occurs:"many",domainProperty:"value"},
      {xml:"archive",domain:"archives",occurs:"many",domainProperty:"value"}
    ];
  },
  handleImport(actionNode,json){
    this._super(actionNode,json);
  }
});
var SubWFActionJobHandler=ActionJobHandler.extend({
  actionType:"sub-workflow",
  mapping:null,
  init(){
    this.mapping=[
      {xml:"app-path",domain:"appPath",mandatory:true},
      {xml:"propagate-configuration",domain:"propagate-configuration", ignoreValue:true},
      {xml:"configuration",customHandler:this.configurationMapper}
       ];
  }
});
var DistCpJobHandler=ActionJobHandler.extend({
  actionType:"distcp",
  nameSpace:"uri:oozie:distcp-action:0.2",
  mapping:null,
  init(){
    this.mapping=[
      {xml:"job-tracker",domain:"jobTracker"},
      {xml:"name-node",domain:"nameNode"},
      {xml:"prepare",customHandler:this.handlePrepare},
      {xml:"configuration",customHandler:this.configurationMapper},
      {xml:"java-opts",domain:"javaOpts"},
      {xml:"arg",domain:"args",occurs:"many",domainProperty:"value"},
       ];
  },

});

var SshActionJobHandler=ActionJobHandler.extend({
  actionType:"ssh",
  nameSpace:"uri:oozie:ssh-action:0.2",
  mapping:null,
  init(){
    this.mapping=[
      {xml:"host",domain:"host"},
      {xml:"command",domain:"command"},
      {xml:"args",domain:"args",occurs:"many",domainProperty:"value"},
      {xml:"arg",domain:"arg",occurs:"many",domainProperty:"value"},
      {xml:"capture-output",domain:"captureOutput",ignoreValue:true}
    ];
  },

  handleImport(actionNode,json){
    this._super(actionNode,json);
  }
});

var EmailActionJobHandler=ActionJobHandler.extend({
  actionType:"email",
  nameSpace:"uri:oozie:email-action:0.2",
  mapping:null,
  init(){
    this.mapping=[
      {xml:"to",domain:"to",mandatory:true},
      {xml:"cc",domain:"cc"},
      {xml:"bcc",domain:"bcc"},
      {xml:"subject",domain:"subject",mandatory:true},
      {xml:"body",domain:"body",mandatory:true},
      {xml:"content_type",domain:"content_type"},
      {xml:"attachment",domain:"attachment"}

    ];
  },

  handleImport(actionNode,json){
    this._super(actionNode,json);
  }
});


var MapRedActionJobHandler=ActionJobHandler.extend({
  actionType:"map-reduce",
  mapping:null,
  init(){
    this.mapping=[
      {xml:"job-tracker",domain:"jobTracker"},
      {xml:"name-node",domain:"nameNode"},
      {xml:"prepare",customHandler:this.prepareMapper},
      {xml:"job-xml",domain:"jobXml",occurs:"many",domainProperty:"value"},
      {xml:"config-class", domain:"config-class"},
      {xml:"configuration",customHandler:this.configurationMapper},
      {xml:"file",domain:"files",occurs:"many",domainProperty:"value"},
      {xml:"archive",domain:"archives",occurs:"many",domainProperty:"value"}
    ];
  },

  handleImport(actionNode,json){
    this._super(actionNode,json);
  }
});

var FSActionJobHandler=ActionJobHandler.extend({
  actionType:"fs",
  mapping:null,
  init(){
    this.mapping=[
      {xml:"name-node",domain:"nameNode"},
      {xml:"configuration",customHandler:this.configurationMapper}
    ];
  },

  handleImport(actionNode,json){
    this._super(actionNode,json);
  }
});
export{ActionJobHandler,JavaActionJobHandler,PigActionJobHandler,HiveActionJobHandler,SqoopActionJobHandler,ShellActionJobHandler, EmailActionJobHandler,SparkActionJobHandler,MapRedActionJobHandler, Hive2ActionJobHandler, SubWFActionJobHandler, DistCpJobHandler, SshActionJobHandler, FSActionJobHandler};
