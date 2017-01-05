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
    this.handleMapping(nodeDomain,actionObj,this.mapping,nodeName);
  },
  /* jshint unused:vars */
  validate(nodeDomain){
    //overwrite in implmentations and return array of errors object.
  },
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
  handle(nodeDomain,nodeObj,nodeName){
    this._super(nodeDomain,nodeObj,nodeName);
    if (!nodeDomain.fsOps){
      return;
    }
    nodeDomain.fsOps.forEach(function(fsop){
      if (!nodeObj.fs[fsop.type]){
        nodeObj.fs[fsop.type]=[];
      }
      switch (fsop.type) {
        case "delete":
        nodeObj.fs["delete"].push({"_path":fsop.path});
        break;
        case "mkdir":
        nodeObj.fs["mkdir"].push({"_path":fsop.path});
        break;
        case "move":
        nodeObj.fs["move"].push({"_source":fsop.source,"_target":fsop.target});
        break;
        case "touchz":
        nodeObj.fs["touchz"].push({"_path":fsop.path});
        break;
        case "chmod":
        var permissions, ownerPerm = 0, groupPerm = 0, othersPerm = 0, dirFiles = fsop.dirfiles;

        if(fsop){
          if(fsop.oread){
            ownerPerm = 1;
          }
          if(fsop.owrite){
            ownerPerm = ownerPerm + 2;
          }
          if(fsop.oexecute){
            ownerPerm = ownerPerm + 4;
          }
          if(fsop.gread){
            groupPerm = 1;
          }
          if(fsop.gwrite){
            groupPerm = groupPerm + 2;
          }
          if(fsop.gexecute){
            groupPerm = groupPerm + 4;
          }
          if(fsop.rread){
            othersPerm = 1;
          }
          if(fsop.rwrite){
            othersPerm = othersPerm + 2;
          }
          if(fsop.rexecute){
            othersPerm = othersPerm + 4;
          }
        }
        permissions = ownerPerm+""+groupPerm+""+othersPerm;
        if(dirFiles === undefined){
          dirFiles = false;
        }
        var conf={"_path":fsop.path,"_permissions":permissions,"_dir-files":dirFiles};
        if (fsop.recursive){
          conf["recursive"]="";
        }
        nodeObj.fs["chmod"].push(conf);
        break;
        case "chgrp":
        var dirFiles = fsop.dirfiles;
        if(dirFiles === undefined){
          dirFiles = false;
        }
        var conf={"_path":fsop.path,"_group":fsop.group,"_dir-files":dirFiles};
        if (fsop.recursive){
          conf["recursive"]="";
        }
        nodeObj.fs["chgrp"].push(conf);
        break;
        default:
      }
    });
  },
  handleImport(actionNode,json){
    this._super(actionNode,json);
    var commandKeys=["delete","mkdir","move","chmod","touchz","chgrp"];
    var fsOps=actionNode.domain.fsOps=[];
    Object.keys(json).forEach(function(key){
      if (commandKeys.contains(key)){
        var fileOpsJson=null;
        if (!Ember.isArray(json[key])){
          fileOpsJson=[json[key]];
        }else{
          fileOpsJson=json[key];
        }
        fileOpsJson.forEach(function (fileOpJson) {
          var fsConf={};
          fsOps.push(fsConf);
          fsConf.type=key;
          var settings=fsConf;
          switch (key) {
            case "delete":
            settings.path=fileOpJson._path;
            break;
            case "mkdir":
            settings.path=fileOpJson._path;
            break;
            case "touchz":
            settings.path=fileOpJson._path;
            break;
            case "move":
            settings.source=fileOpJson._source;
            settings.target=fileOpJson._target;
            break;
            case "chmod":
            settings.path=fileOpJson._path;
            settings.permissions=fileOpJson._permissions;
            var perm = settings.permissions.toString();
            if(isNaN(perm)){
              var permList = {"-":0,"r":1,"w":2,"x":4}, permissionNumFormat = "", permTokenNum = 0, tempArr = [1,4,7];
              for(let p=0; p<tempArr.length; p++){
                  var permToken = perm.slice(tempArr[p],tempArr[p]+3);
                  for(let q=0; q<permToken.length; q++){
                    var tok = permList[permToken.slice(q,q+1)]
                    permTokenNum = permTokenNum + tok;
                  }
                  permissionNumFormat = permissionNumFormat +""+ permTokenNum;
                  permTokenNum = 0;
              }
              perm = permissionNumFormat;
            }
            for(var i=0; i< perm.length; i++){
              var keyField;
              if(i===0){
                keyField = "o";
              }else if(i===1){
                keyField = "g";
              }else if(i===2){
                keyField = "r";
              }
              if(perm.slice(i,i+1) === "0"){
                settings[keyField+"read"] = 0;
                settings[keyField+"write"] = 0;
                settings[keyField+"execute"] = 0;
              }else if(perm.slice(i,i+1) === "1"){
                settings[keyField+"read"] = 1;
                settings[keyField+"write"] = 0;
                settings[keyField+"execute"] = 0;
              }else if(perm.slice(i,i+1) === "2"){
                settings[keyField+"read"] = 0;
                settings[keyField+"write"] = 2;
                settings[keyField+"execute"] = 0;
              }else if (perm.slice(i,i+1) === "3"){
                settings[keyField+"read"] = 1;
                settings[keyField+"write"] = 2;
                settings[keyField+"execute"] = 0;
              }else if (perm.slice(i,i+1) === "4"){
                settings[keyField+"read"] = 0;
                settings[keyField+"write"] = 0;
                settings[keyField+"execute"] = 4;
              }else if (perm.slice(i,i+1) === "5"){
                settings[keyField+"read"] = 1;
                settings[keyField+"write"] = 0;
                settings[keyField+"execute"] = 4;
              }else if (perm.slice(i,i+1) === "6"){
                settings[keyField+"read"] = 0;
                settings[keyField+"write"] = 2;
                settings[keyField+"execute"] = 4;
              }else if (perm.slice(i,i+1) === "7"){
                settings[keyField+"read"] = 1;
                settings[keyField+"write"] = 2;
                settings[keyField+"execute"] = 4;
              }
            }
            settings.dirfiles=fileOpJson["_dir-files"];
            if(fileOpJson.hasOwnProperty("recursive")){
              settings.recursive = true;
            }else{
              settings.recursive = false;
            }
            break;
            case "chgrp":
            settings.path=fileOpJson._path;
            settings.group=fileOpJson._group;
            settings.dirfiles=fileOpJson["_dir-files"];
            if(fileOpJson.hasOwnProperty("recursive")){
              settings.recursive = true;
            }else{
              settings.recursive = false;
            }
            break;
          }
        });
      }
    });
  }
});
export{ActionJobHandler,JavaActionJobHandler,PigActionJobHandler,HiveActionJobHandler,SqoopActionJobHandler,ShellActionJobHandler, EmailActionJobHandler,SparkActionJobHandler,MapRedActionJobHandler, Hive2ActionJobHandler, SubWFActionJobHandler, DistCpJobHandler, SshActionJobHandler, FSActionJobHandler};
