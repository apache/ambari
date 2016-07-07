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
import Constants from '../utils/constants';
import {FindNodeMixin} from '../domain/findnode-mixin';
import {NodeFactory} from '../domain/node-factory';
import SchemaVersions from '../domain/schema-versions';
import {NodeVisitor} from '../domain/node-visitor';
import {idGen} from '../domain/id-gen';
import {SlaInfo} from '../domain/sla-info'
var Workflow= Ember.Object.extend(FindNodeMixin,{
  name:"",
  startNode:null,
  globalSetting:null,
  usePlaceholders: true,
  killNodes : null,
  nodeVisitor : null,
  nodeFactory:NodeFactory.create({}),
  schemaVersions:SchemaVersions.create({}),
  sla : SlaInfo.create({}),
  initialize(){
      this.nodeVisitor=NodeVisitor.create({});
      var src =this.nodeFactory.createStartNode();
      var dest =this.nodeFactory.createEndNode("end");
      this.set("startNode", src);
      this.set("killNodes",Ember.A([]));
      this.set("globalSetting",null);
      this.set("name","");
      this.appendDefaultKillNode();
      src.addTransitionTo(dest);
  },
  appendDefaultKillNode(){
    this.createKillNode(Constants.defaultKillNodeName,"${wf:errorMessage(wf:lastErrorNode())}");
  },
  createKillNode(name, message){
    var killNode=this.nodeFactory.createKillNode(name,message);
    this.get("killNodes").pushObject(killNode);
  },
  resetKillNodes(){//TODO refactor
      this.set("killNodes",Ember.A([]));
  },
  resetWorfklow(){
    //idGen.reset();
    this.initialize();
  },
  findCommonTargetNodeId(node){
    var nodeIds = {}, targ, decPath = node.getTargets(), tempId = 0;
    for(var i =0; i< decPath.length; i++){
      var currNode = decPath[i];
      do {
         if(nodeIds.hasOwnProperty(currNode.get("id"))){
          nodeIds[currNode.get("id")] = nodeIds[currNode.get("id")] + 1;
         } else {
           nodeIds[currNode.get("id")] = 1;
         }
         if(currNode.get("id") === "node-end"){
           break;
         }
         currNode = currNode.getTargets()[0];
      } while(currNode && currNode.get("id"));
    }
    for(var j in nodeIds){
       if(tempId < nodeIds[j]){
         targ = j;
         tempId = nodeIds[j];
       }
    }
    return targ;
  },
  findJoinNode(node){
    var commonTargetId=null;
    var commonTarget=null;
    if (node.isDecisionNode()){
      if (Constants.globalSetting.useJoinNodeForDecision){
        var target=this.findNodeById(node,"decision_end_"+node.get("id"));
        if (!target){
          commonTargetId=this.findCommonTargetNodeId(node);
          commonTarget=this.findNodeById(this.startNode,commonTargetId);
          return commonTarget;
        }else{
          return target;
        }
      }else{
        commonTargetId=this.findCommonTargetNodeId(node);
        commonTarget=this.findNodeById(this.startNode,commonTargetId);
        return commonTarget;
      }
    }else if (node.isForkNode()) {
      commonTargetId=this.findCommonTargetNodeId(node);
      commonTarget=this.findNodeById(this.startNode,commonTargetId);
      return commonTarget;
    }else{
      return null;
    }
  },
  addBranch(sourceNode){
    var target=this.findJoinNode(sourceNode);
    if (this.get("usePlaceholders")){
      var placeholderNode=this.nodeFactory.createPlaceholderNode(target) ;
      sourceNode.addTransitionTo(placeholderNode);
    }else{
      sourceNode.addTransitionTo(target);
    }
  },
  addDecisionBranch(settings){
    if (!settings.targetNode){
        console.error("target node cant be empty");
        return;
    }
    var sourceNode=settings.sourceNode;
    var insertNodeOnPath=settings.newNodeType?true:false;
    var target=settings.targetNode;
    if (!insertNodeOnPath){
      if (this.get("usePlaceholders")){
        var placeholderNode=this.nodeFactory.createPlaceholderNode(target) ;
        sourceNode.addTransitionTo(placeholderNode,settings.condition);
      }else{
         sourceNode.addTransitionTo(target,settings.condition);
      }
    }else{
    }
  },
  generatedNode(target,type){
    var generatedNode=null;
    if ("decision" === type){
      generatedNode=this.nodeFactory.generateDecisionNode(target);
    }else  if ("fork" === type){
      generatedNode=this.nodeFactory.generateForkNode(target);
    }else  if ("kill" === type){
      generatedNode = this.nodeFactory.createKillNode(settings.name);
      source.deleteCurrentKillNode();
    }else{
      //todo show proper killnode name...
      generatedNode = this.nodeFactory.createActionNode(type);
      generatedNode.addTransitionTo(target);
    }
    return generatedNode;
  },
  addKillNode(node,settings){
    var generatedNode=this.generatedNode(null,"kill");
    return source.addTransitionTo(generatedNode,"error");
  },
  addNode(transition,type,settings) {
    var source=transition.sourceNode;
    var target=transition.targetNode;
    var computedTarget=target;
    if (target && target.isPlaceholder()){
      computedTarget=target.getTargets()[0];
    }
    var generatedNode=this.generatedNode(computedTarget,type);
    transition.targetNode=generatedNode;
  },
  deleteKillNode(node){
    let killNodes = this.get("killNodes");
    var killNodeReferenced=false;
    this.nodeVisitor.process(this.startNode,function(n,ctx){
      if (n.errorNode && n.errorNode.name===node.name){
        killNodeReferenced=true;
      }
    });
    if (killNodeReferenced){
      return{
        status: false,
        message: "Kill node is being referenced by other nodes."
      };
    }
    for(var i=0; i<killNodes.length; i++){
      if(node.id === killNodes[i].id){
        this.get("killNodes").removeObject(killNodes[i]);
        break;
      }
    }
    return {
      status:true
    };
  },
  deleteNode(node){
    var self=this;
    var target=node.getDefaultTransitionTarget();
    if (node.isForkNode()|| node.isDecisionNode()){
      target=this.findJoinNode(node);
      if (target.isJoinNode()){
        target=target.getDefaultTransitionTarget();
      }
    }
    var transitionslist=this.findTransistionsToNode(node);
    transitionslist.forEach(function(tran){
      if (tran.getSourceNode().isDecisionNode()){
        var joinNode=self.findJoinNode(tran.getSourceNode());
        if (joinNode===target){
          if (tran.isDefaultCasePath()){
            var placeholderNode=self.nodeFactory.createPlaceholderNode(target);
            tran.targetNode=placeholderNode;
          }else   if (tran.getSourceNode().getOkTransitionCount()>2){
            tran.getSourceNode().removeTransition(tran);
          }else{
            var placeholderNode=self.nodeFactory.createPlaceholderNode(target);
            tran.targetNode=placeholderNode;
          }
        }else{
          tran.targetNode=target;
        }
      }else if (tran.getSourceNode().isForkNode()){
        var joinNode=self.findJoinNode(tran.getSourceNode());
        if (joinNode===target){
          if (tran.getSourceNode().getOkTransitionCount()>2){
             tran.getSourceNode().removeTransition(tran);
          }else{
             var placeholderNode=self.nodeFactory.createPlaceholderNode(target);
             tran.targetNode=placeholderNode;
          }
        }else{
          tran.targetNode=target;
        }
      }else{
        tran.targetNode=target;
      }
    });
  },
  deleteTransition(transition){
    var src=transition.getSourceNode();
    src.removeTransition(transition);
  },
  deleteEmptyTransitions(transitionslist){
    transitionslist.forEach(function(tran){
      if (tran.getSourceNode().isForkNode()&& tran.getTargetNode().isJoinNode()){
        tran.getSourceNode().removeTransition(tran);
      }
    });
  },
  findTransistionsToNode(matchingNode){
    var transitionslist=[];
    this.findTransistionsToNodeInternal(this.startNode,matchingNode,transitionslist);
    return transitionslist;
  },
  findTransistionsToNodeInternal(node,matchingNode,transitionslist){
    var self=this;
    if (node.transitions){
      node.transitions.forEach(function(tran){
        if (tran.getTargetNode()===matchingNode){
          transitionslist.push(tran);
        }
        self.findTransistionsToNodeInternal(tran.getTargetNode(),matchingNode,transitionslist);
      });
    }
  }
});


export {Workflow};
