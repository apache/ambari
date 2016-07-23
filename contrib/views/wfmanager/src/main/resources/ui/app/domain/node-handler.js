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
import {NodeFactory} from '../domain/node-factory';
import * as actionJobHandler from '../domain/actionjob_hanlder';
import {SlaInfo} from '../domain/sla-info';
import {SLAMapper} from "../domain/mapping-utils";
var ActionTypeResolver=Ember.Object.extend({
  actionJobHandlerMap:null,
  validStandardActionProps:["ok","error","info"],
  init(){
    var settings={schemaVersions:this.schemaVersions};
    this.actionJobHandlerMap=new Map();
    this.actionJobHandlerMap.set("java",actionJobHandler.JavaActionJobHandler.create(settings));
    this.actionJobHandlerMap.set("pig",actionJobHandler.PigActionJobHandler.create(settings));
    this.actionJobHandlerMap.set("hive",actionJobHandler.HiveActionJobHandler.create(settings));
    this.actionJobHandlerMap.set("hive2",actionJobHandler.Hive2ActionJobHandler.create(settings));
    this.actionJobHandlerMap.set("sqoop",actionJobHandler.SqoopActionJobHandler.create(settings));
    this.actionJobHandlerMap.set("shell",actionJobHandler.ShellActionJobHandler.create(settings));
    this.actionJobHandlerMap.set("spark",actionJobHandler.SparkActionJobHandler.create(settings));
    this.actionJobHandlerMap.set("map-reduce",actionJobHandler.MapRedActionJobHandler.create(settings));
    this.actionJobHandlerMap.set("sub-workflow",actionJobHandler.SubWFActionJobHandler.create(settings));
    this.actionJobHandlerMap.set("distcp",actionJobHandler.DistCpJobHandler.create(settings));
    this.actionJobHandlerMap.set("ssh",actionJobHandler.SshActionJobHandler.create(settings));
    this.actionJobHandlerMap.set("email",actionJobHandler.EmailActionJobHandler.create(settings));
    this.actionJobHandlerMap.set("fs",actionJobHandler.FSActionJobHandler.create(settings));
  },
  getActionType(json){
    var self=this;
    var resolvedType=null;
    var problaleActionsTypes=[];
    Object.keys(json).forEach(function functionName(key) {
      if (!self.validStandardActionProps.contains(key) && !key.startsWith("_")){
        problaleActionsTypes.push(key);
      }
    });
    if (problaleActionsTypes.length===1){
      return problaleActionsTypes[0];
    }else{
      console.error("Invalid Action spec..",json);
    }
    return resolvedType;
  },
  getActionJobHandler(jobType){
    return this.actionJobHandlerMap.get(jobType);
  }
});
var NodeHandler=Ember.Object.extend({
  nodeFactory:NodeFactory.create({}),
  context : {},
  setContext(context){
    this.context = context;
  },
  getContext(){
    return this.context;
  },
  hasMany(){
    return true;
  },
  handleNode(node){
    return {"_name":node.get("name")};
  },

  handleTransitions(transitions,nodeObj){

  },
  handleImportNode(type,node){
  },
  handleImportTransitions(node,json,nodeMap){
  }
});
var StartNodeHandler= NodeHandler.extend({
  hasMany(){
    return false;
  },
  handleNode(node){
    return {};
  },
  handleTransitions(transitions,nodeObj){
    if (transitions.length!==1){
      this.context.addError({node:nodeObj, message:"Invalid Start Node"});
    }
    nodeObj["_to"]=transitions[0].targetNode.getName();
  },
  handleImportNode(type,node,workflow){
    return this.nodeFactory.createStartNode();
  },
  handleImportTransitions(node,json,nodeMap){
    node.addTransitionTo(nodeMap.get(json._to).node);
  }
});
var EndNodeHandler= NodeHandler.extend({
  hasMany(){
    return false;
  },
  handleImportNode(type,node,workflow){
    return this.nodeFactory.createEndNode("end");
  },

});
var KillNodeHandler= NodeHandler.extend({
  type:"kill",
  handleImportNode(type,node,workflow){
    return this.nodeFactory.createKillNode(node._name,node.message);
  },
  handleNode(node){
    var obj= {"_name":node.get("name")};
    if (!Ember.isBlank(node.get("killMessage"))){
      obj["message"]=node.get("killMessage");
    }
    return obj;
  }
});
var ActionNodeHandler= NodeHandler.extend({
  type:"action",
  actionTypeResolver:null,
  schemaVersions: null,
  slaMapper: SLAMapper.create({}),
  init(){
  },
  handleNode(node){
    var nodeObj=this._super(node);
    if (node.domain && !Ember.isBlank(node.domain.credentials)){
      nodeObj._cred=node.domain.credentials;
    }
    return nodeObj;
  },
  handleSla(domain,nodeObj){
    if (domain && domain.slaEnabled){
      return this.slaMapper.hanldeGeneration(domain.slaInfo,nodeObj);
    }
  },

  handleTransitions(transitions,nodeObj){
    transitions.forEach(function(tran){
      if (!tran.condition){
        nodeObj["ok"]={"_to":tran.getTargetNode().getName()};
      }else if (tran.condition="error"){
        nodeObj["error"]={"_to":tran.getTargetNode().getName()};
      }
    });
  },
  handleImportNode(type,nodeJson,workflow){
    var actionType=this.get("actionTypeResolver").getActionType(nodeJson);

    var actionNode = this.nodeFactory.createActionNode(actionType,nodeJson._name);
    if (actionType===null){
      console.error("cannot handle unsupported node:"+nodeJson);//TODO error handling...
      return actionNode;
    }
    var actionJobHandler=this.get("actionTypeResolver").getActionJobHandler(actionType);
    if (!actionJobHandler){
      console.error("cannot handle unsupported action type:"+actionType+" for "+nodeJson._name);//TODO error handling...
      return actionNode;
    }
    actionJobHandler.handleImport(actionNode,nodeJson[actionType]);
    if (nodeJson.info && nodeJson.info.__prefix==="sla") {
      actionNode.domain.slaEnabled=true;
      this.slaMapper.handleImport(actionNode.domain,nodeJson.info,"slaInfo");
    }
    actionNode.domain.credentials=nodeJson._cred;
    return actionNode;
  },
  handleImportTransitions(node,json,nodeMap){
    node.addTransitionTo(nodeMap.get(json.ok._to).node);
    if (json.error && json.error._to){
      node.addTransitionTo(nodeMap.get(json.error._to).node,"error");
    }
  }
});
var DecisionNodeHandler= NodeHandler.extend({
  type:"decision",
  handleTransitions(transitions,nodeObj){
    var swithCaseObj={"case":[]};
    nodeObj["switch"]=swithCaseObj;
    var caseObjects=swithCaseObj["case"];
    transitions.forEach(function(tran){
      if (tran.condition!=="default"){
        caseObjects.push({"_to":tran.getTargetNode().getName(),"__text":tran.condition});
      }else{
        swithCaseObj['default']={};
        swithCaseObj['default']["_to"]=tran.getTargetNode().getName();
      }

    });
  },
  handleImportNode(type,node,workflow){
    return this.nodeFactory.createEmptyDecisionNode(node._name);
  },
  handleImportTransitions(node,json,nodeMap){
    var defaultPath=json.switch.default._to;
    node.addTransitionTo(nodeMap.get(defaultPath).node,"default");
    var cases=[];
    if (Ember.isArray(json.switch.case)){
      cases=json.switch.case;
    }else{
      cases.push(json.switch.case);
    }
    cases.forEach(function(caseExpr){
      node.addTransitionTo(nodeMap.get(caseExpr._to).node,caseExpr.__text);
    });
  }
});
var ForkNodeHandler= NodeHandler.extend({
  type:"fork",
  handleTransitions(transitions,nodeObj){
    var pathObjects=[];
    nodeObj["path"]=pathObjects;
    transitions.forEach(function(tran){
      pathObjects.push({"_start":tran.getTargetNode().getName()});
    });
  },
  handleImportNode(type,node,workflow){
    return this.nodeFactory.createEmptyForkNode(node._name);
  },
  handleImportTransitions(node,json,nodeMap){
    json.path.forEach(function(path){
      node.addTransitionTo(nodeMap.get(path._start).node);
    });
  }
});
var JoinNodeHandler= NodeHandler.extend({
  type:"join",
  handleTransitions(transitions,nodeObj){
    transitions.forEach(function(tran){
      nodeObj["_to"]=tran.getTargetNode().getName();
    });
  },
  handleImportNode(type,node,workflow){
    return this.nodeFactory.createEmptyJoinNode(node._name);
  },
  handleImportTransitions(node,json,nodeMap){
    node.addTransitionTo(nodeMap.get(json._to).node);
  }
});
export{ActionTypeResolver,NodeHandler,StartNodeHandler,EndNodeHandler,KillNodeHandler,ActionNodeHandler,DecisionNodeHandler,ForkNodeHandler,JoinNodeHandler};
