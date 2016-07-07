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
import {Workflow} from '../domain/workflow';
import {WorkflowXmlMapper} from '../domain/workflow_xml_mapper';
var WorkflowImporter= Ember.Object.extend({
  workflowMapper:null,
  x2js : new X2JS(),
  importWorkflow(workflowXml){
    var workflow=Workflow.create({});
    workflow.initialize();
    this.workflowMapper=WorkflowXmlMapper.create({schemaVersions:workflow.schemaVersions});
    return this.processWorkflowXml(workflowXml,workflow);
  },
  processWorkflowXml(workflowXml,workflow){
    var workflowJson= this.get("x2js").xml_str2json(workflowXml);
    if (!workflowJson["workflow-app"]){
      throw "Invalid workflow";
    }
    var workflowAppJson=workflowJson["workflow-app"];
    var workflowVersion=CommonUtils.extractSchemaVersion(workflowAppJson._xmlns);
    workflow.schemaVersions.setCurrentWorkflowVersion(workflowVersion);
    var nodeMap=this.setupNodeMap(workflowAppJson,workflow);
    this.setupTransitions(workflowAppJson,nodeMap);
    console.log("node map==",nodeMap);
    console.log("imported worfkow=",nodeMap.get("start").node);
    workflow.set("startNode",nodeMap.get("start").node);
    this.populateKillNodes(workflow,nodeMap);
    return workflow;
  },
  processActionNode(nodeMap,action){
    var actionMapper=this.get("workflowMapper").getNodeHandler("action");
    var actionNode=actionMapper.handleImportNode(action);
    nodeMap.set(actionNode.getName(),actionNode);
  },
  setupNodeMap(workflowAppJson,workflow){
    var self=this;
    workflow.set("name",workflowAppJson["_name"]);
    var nodeMap=new Map();
    Object.keys(workflowAppJson).forEach(function (key) {
        var nodeHandler=self.workflowMapper.getNodeHandler(key);
        if (nodeHandler){
          if (Ember.isArray(workflowAppJson[key])){
              workflowAppJson[key].forEach(function(jsonObj){
                var node=nodeHandler.handleImportNode(key,jsonObj,workflow);
                nodeMap.set(jsonObj._name,{json:jsonObj,node:node});
              });
          }else{
              var node=nodeHandler.handleImportNode(key,workflowAppJson[key],workflow);
              if (!workflowAppJson[key]._name){
                nodeMap.set(key,{json:workflowAppJson[key],node:node});
              }else{
                nodeMap.set(workflowAppJson[key]._name,{json:workflowAppJson[key],node:node});
              }
          }
        }
    });
    return nodeMap;
  },
  setupTransitions(workflowAppJson,nodeMap){
    var self=this;
    nodeMap.forEach(function(entry,key){
      var node=entry.node;
      if (!node){
        console.error("could not process:",key);//TODO error handling...
        return;
      }
      var json=entry.json;
      var nodeHandler=self.workflowMapper.getNodeHandler(node.get("type"));
      if (!nodeHandler){
        console.error("could not process:",node.get("type"));//TODO error handling...
      }
      nodeHandler.handleImportTransitions(node,json,nodeMap);
    });
  },
  getNodeIds(nodeMap){
    var ids=[];
    nodeMap.forEach(function(entry,key){
      var node=entry.node;
      ids.push(node.id);
    });
    return ids;
  },
  getNodeNames(nodeMap){
    var names=[];
    nodeMap.forEach(function(entry,key){
      var node=entry.node;
      names.push(node.id);
    });
    return names;
  },
  populateKillNodes(workflow,nodeMap){
    // if (this.containsDefaultKillNode(nodeMap)){
    //   workflow.resetKillNodes();
    // }
    workflow.resetKillNodes();
    nodeMap.forEach(function(entry,key){
      var node=entry.node;
      if (node.isKillNode()){
        workflow.get("killNodes").pushObject(node);
      }
    });
  },
  containsDefaultKillNode(nodeMap){
    var containsDefaultKillNode=false;
    nodeMap.forEach(function(entry,key){
      var node=entry.node;
      if (node.isDefaultKillNode()){
        containsDefaultKillNode=true;
      }
    });
    return containsDefaultKillNode;
  }
});
export {WorkflowImporter};
