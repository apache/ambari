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
import {WorkflowXmlMapper} from '../domain/workflow_xml_mapper';
import {NodeVisitor} from '../domain/node-visitor';
import Constants from '../utils/constants';
var WorkflowGenerator= Ember.Object.extend({
  workflowMapper:null,
  x2js : new X2JS({useDoubleQuotes:true}),
  workflow:null,
  workflowContext : {},
  nodeVisitor:null,
  init(){
    this.workflowMapper=WorkflowXmlMapper.create({schemaVersions:this.workflow.schemaVersions});
    this.nodeVisitor=NodeVisitor.create({});
  },
  process(){
    console.log("About to process workflow ...",this.workflow);
    if (!this.workflow.get("name") || this.workflow.get("name").trim()===""){
      this.workflowContext.addError({message : "Workflow name is mandatory"});
      return;
    }
    //var nameSpace={"_xmlns":"uri:oozie:workflow:0.5"};
    var workflowObj={"workflow-app":{}};
    this.get("workflowMapper").getGlobalConfigHandler().handle(this.workflow.get("globalSetting"),workflowObj["workflow-app"]);
    this.visitNode(workflowObj,this.workflow.startNode);
  //  this.nodeVisitor.process(this.workflow.startNode,this.handleNode,{workflowObj:workflowObj});
    console.log("workflowObj==",workflowObj);
    if (!workflowObj["workflow-app"].action || workflowObj["workflow-app"].action.length<1){
      this.workflowContext.addError({message : "Miniumum of one action node must exist"});
      return;
    }
    var reordered={"workflow-app":{}};
    var srcWorkflowApp=workflowObj["workflow-app"];
    var targetWorkflowApp=reordered["workflow-app"];
    targetWorkflowApp["_name"]=this.workflow.get("name");
    this.copyProp(srcWorkflowApp,targetWorkflowApp,["global","start","decision","fork","join","action","kill","end"]);
    targetWorkflowApp["_xmlns"]="uri:oozie:workflow:"+this.workflow.get("schemaVersions").getCurrentWorkflowVersion();
   // targetWorkflowApp["__cdata"]=Constants.generatedByCdata;
    var xmlAsStr = this.get("x2js").json2xml_str(reordered);
    console.log("Generated Workflow XML==",xmlAsStr);
    return xmlAsStr;
  },
  copyProp(src,dest,props){
    props.forEach(function(prop){
      if (src[prop]){
        dest[prop]=src[prop];
      }
    });
  },
  handleNode(node,context){
    var nodeHandler=this.get("workflowMapper").getNodeHandler(node.type);
    nodeHandler.setContext(this.workflowContext);
    var nodeObj=nodeHandler.handleNode(node);
    if (node.isActionNode()){
      var jobHandler=this.get("workflowMapper").getActionJobHandler(node.actionType);
      if (jobHandler){

        jobHandler.setContext(this.workflowContext);
        if (!node.get("domain")){
          console.error("action details are not present");//todo error handling
          this.workflowContext.addError({node : node, message : "Action Properties are empty"});
        }else{
          //var globalSetting=workflowObj["workflow-app"].global;
          // if (!globalSetting){
          //   if (!Ember.isBlank(globalSetting["job-tracker"])){
          //     if (!Ember.isBlank(node.get("domain").get("jobTracker"))){
          //       node.get("domain").set("jobTracker","${job-tracker}");
          //     }
          //   }
          //   if (!Ember.isBlank(globalSetting["name-node"])){
          //     if (!Ember.isBlank(node.get("domain").get("nameNode"))){
          //       node.get("domain").set("nameNode","${name-node}");
          //     }
          //   }
          // }
          jobHandler.handle(node.get("domain"),nodeObj,node.get("name"));

        }
      }else{
        console.error("Unknown action "+node.actionType);
        this.workflowContext.addError({node : node, message : "Unknown action:"+node.actionType});
      }
    }
    if (nodeHandler.hasMany()){
        if (!workflowApp[node.type]){
            workflowApp[node.type]=[];
        }
      workflowApp[node.type].push(nodeObj);
    }else{
      workflowApp[node.type]=nodeObj;
    }
    nodeHandler.handleTransitions(node.transitions,nodeObj);

  },
  visitNode(workflowObj,node,visitedNodes){
    if (!visitedNodes){
      visitedNodes=[];
    }
    if (visitedNodes.contains(node.get("id"))){
      return;
    }
    visitedNodes.push(node.get("id"));
    var self=this;
    var workflowApp=workflowObj["workflow-app"];
    if (node.isPlaceholder()){
      return self.visitNode(workflowObj,node.transitions[0].targetNode,visitedNodes);
    }
    // if (node.isDecisionEnd()){
    //   return self.visitNode(workflowObj,node.transitions[0].targetNode,visitedNodes);
    // }
    var nodeHandler=this.get("workflowMapper").getNodeHandler(node.type);
    nodeHandler.setContext(this.workflowContext);
    var nodeObj=nodeHandler.handleNode(node);
    if (node.type==='action'){
      var jobHandler=this.get("workflowMapper").getActionJobHandler(node.actionType);
      if (jobHandler){

        jobHandler.setContext(this.workflowContext);
        if (!node.get("domain")){
          console.error("action details are not present");//todo error handling
          this.workflowContext.addError({node : node, message : "Action Properties are empty"});
        }else{
          //var globalSetting=workflowObj["workflow-app"].global;
          // if (!globalSetting){
          //   if (!Ember.isBlank(globalSetting["job-tracker"])){
          //     if (!Ember.isBlank(node.get("domain").get("jobTracker"))){
          //       node.get("domain").set("jobTracker","${job-tracker}");
          //     }
          //   }
          //   if (!Ember.isBlank(globalSetting["name-node"])){
          //     if (!Ember.isBlank(node.get("domain").get("nameNode"))){
          //       node.get("domain").set("nameNode","${name-node}");
          //     }
          //   }
          // }

          jobHandler.handle(node.get("domain"),nodeObj,node.get("name"));
          var errors=jobHandler.validate(node.get("domain"));
          if (errors && errors.length>0){
            errors.forEach(function(err){
              this.workflowContext.addError({node : node, message : err.message});
            }.bind(this));

          }
        }
      }else{
        console.error("Unknown action "+node.actionType);
        this.workflowContext.addError({node : node, message : "Unknown action:"+node.actionType});
      }
    }
    if (nodeHandler.hasMany()){
        if (!workflowApp[node.type]){
            workflowApp[node.type]=[];
        }
      workflowApp[node.type].push(nodeObj);
    }else{
      workflowApp[node.type]=nodeObj;
    }
    nodeHandler.handleTransitions(node.transitions,nodeObj);
    if (node.transitions){
      node.transitions.forEach(function(tran){
        self.visitNode(workflowObj,tran.targetNode,visitedNodes);
      });
    }
  }
});
export {WorkflowGenerator};
