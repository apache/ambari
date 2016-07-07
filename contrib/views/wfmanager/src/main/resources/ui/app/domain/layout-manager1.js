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
var LayoutManager1= Ember.Object.extend({
  doLayout(component,nodes,edges,workflow){
    // var nodeMap={};
    // nodes.each(function(idx,node){
    //   nodeMap[node.id]=node;
    // });
    // console.log("NodeMap==",nodeMap);
    // console.log("in layout workflow==",workflow);
    var levelMatrix = [];
    var adjancencyMatrix = {};
    for (var i = 0; i < edges.length; i++) {
      var c = edges[i];
      if(!adjancencyMatrix[c.source.id]){
        adjancencyMatrix[c.source.id] = [];
      }
      adjancencyMatrix[c.source.id].push(c.target.id);
    }
    //console.error(adjancencyMatrix);
    var bfsArray = this.doBFS(nodes[0].id, adjancencyMatrix);
    var level = 0;
    bfsArray.forEach((item, index)=>{
      if(!adjancencyMatrix[item]){
        return;
      }
      adjancencyMatrix[item].forEach((value)=>{
        // if(!levelMatrix[value]){
        //   levelMatrix[value] = level;
        // }
        if(!levelMatrix[level]){
          levelMatrix[level] = [];
        }
        levelMatrix[level].push(value);
      });
      level++;
    });
    console.error(levelMatrix);
    var startNodeOffset = component.$("#node-start").offset();
    var top = Math.floor(startNodeOffset.top);
    var left = Math.floor(startNodeOffset.left);
    levelMatrix.forEach((nodeArray, level)=>{
      var levelLength = nodeArray.length;
      var levelSplit = left/levelLength;
      nodeArray.forEach((node, idx, array)=>{
        if(levelLength == 1){
          Ember.$("#" + node).css("top", top+(level*100)+ "px");
        }else{
          Ember.$("#" + node).css("top", top+ "px");
          if(idx < levelLength/2){
            Ember.$("#" + node).css("left", left-(idx*100) + "px");
          }else if(idx === levelLength/2){
            Ember.$("#" + node).css("left", left + "px");
          }else{
            Ember.$("#" + node).css("left", left+(idx*100) + "px");
          }
        }
      });
    //  Ember.$("#" + v).css("left", g.node(v).x+displacement + "px");

    });
  },
  doBFS (root, adjancencyMatrix){
    var bfsResult = [];
    var level = 0;
    var visited = {};
    visited[root] = true;
    var queue = [];
    queue.push(root);
    while(queue.length !== 0){
      root = queue.shift();
      console.log(root+"--->"+level);
      bfsResult.push(root);
      if(!adjancencyMatrix[root]){
        continue;
      }
      adjancencyMatrix[root].forEach(function(node){
        if(!visited[node]){
          visited[node] = true;
          queue.push(node);
        }
      });
    }
    return bfsResult;
  },
});
export {LayoutManager1};
