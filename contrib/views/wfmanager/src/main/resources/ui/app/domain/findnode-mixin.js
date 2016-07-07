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
var FindNodeMixin= Ember.Mixin.create({
  findNodeById(startNode,id){

    return this.findNodeByIdInternal(startNode,id);
  },
  findNodeByIdInternal(node,id){
    var self=this;
    if (node.get("id")===id){
      return node;
    }else{
      if (node.transitions){
        for (var i = 0; i < node.transitions.length; i++) {
          var transition=node.transitions[i];
          var result=self.findNodeByIdInternal(transition.getTargetNode(true),id);
          if (result){
            return result;
          }
        }
      }else{
        return null;
      }
    }
  },
  getOKToNode(node){
    var okToNode;
    if (!node){
      okToNode = null;
    }else{
      var transitions = node.transitions;
      transitions.forEach(function(trans){
        if (!trans.condition){
          okToNode = trans.targetNode;
          return;
        }
      });
    }
    return okToNode;
  },
  getDesendantNodes(node, ignoreEndNode){
    if (!node){
      return null;
    }
    var currNode=null;
    var nodes = [], nxtPath = node.getTargets();
    for(var i =0; i< nxtPath.length; i++){
      currNode = nxtPath[i];
      do {
         if(this.insertUniqueNodes(currNode, nodes) && currNode){
          nodes.push(currNode);
         }
         var nodesList = currNode.getTargets();
         if(nodesList.length > 1){
           for(var j=0; j<nodesList.length; j++) {
            if(nodesList[j].getTargets().length>1){
              var tmp = this.getDesendantNodes(nodesList[j]);
              if(tmp.length){
                nodes = nodes.concat(tmp);
              }
            } else if(this.insertUniqueNodes(nodesList[j], nodes) && nodesList[j]){
                nodes.push(nodesList[j]);
                currNode = nodesList[j];
             } else {
                currNode = nodesList[j];
             }
           }
         } else {
             currNode = nodesList[0];
         }
      } while(currNode && currNode.get("id") && currNode.get("id") !== "node-end");
    }
    if(!ignoreEndNode && currNode){
      nodes.push(currNode);
    }
    return nodes;
  },
  insertUniqueNodes(currNode, nodes){
         if(nodes.indexOf(currNode) > -1){
         } else {
           if (!( currNode.isKillNode() || currNode.isPlaceholder() || currNode.isJoinNode() || currNode.isDecisionEnd())){
              return true;
           }
         }
  },
});
export{FindNodeMixin};
