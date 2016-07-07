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
    var levelMatrix = [];
    var adjancencyMatrix = {};
    for (var i = 0; i < edges.length; i++) {
      var c = edges[i];
      if(!adjancencyMatrix[c.source.id]){
        adjancencyMatrix[c.source.id] = [];
      }
      adjancencyMatrix[c.source.id].push(c.target.id);
    }
    console.error(adjancencyMatrix);
    var bfsArray = this.doBFS(nodes[0].id, adjancencyMatrix);
    var level = 0;
    levelMatrix[level] = [];
    levelMatrix[level++].push(nodes[0].id);
    bfsArray.forEach((item, index)=>{
      if(!adjancencyMatrix[item]){
        return;
      }
      adjancencyMatrix[item].forEach((value)=>{
        if(!levelMatrix[level]){
          levelMatrix[level] = [];
        }
        levelMatrix[level].push(value);
      });
      level++;
    });
    console.error(levelMatrix);
    var top = 0;
    var left = 400;
    var startNodeWidth = component.$("#"+nodes[0].id).width();
    var center = left+(150-Math.floor(startNodeWidth/2));
    levelMatrix.forEach((nodeArray, level)=>{
      var levelLength = nodeArray.length;
      nodeArray.forEach((node, idx, array)=>{
        Ember.$("#" + node).css("top", top+(level*100)+ "px");
        var nodeWidth=Math.round(component.$("#" + node).width()/10) * 10;
        var avgPositionChange = 0;
        var totalPositions = ((levelLength-1)*(levelLength)/2)*100;
        var displacement = 150-Math.floor(nodeWidth/2);
        var avgPositionChange = (totalPositions/levelLength);
        var eltPosition = idx*100 - avgPositionChange;
        var total = left + eltPosition + displacement;
        // if(total+nodeWidth > center){
        //   if(eltPosition < 0){
        //     total = total - nodeWidth;
        //   }else if(eltPosition > 0){
        //     total = total + nodeWidth;
        //   }else{
        //
        //   }
        // }
        Ember.$("#" + node).css("left", total + "px");
      });
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
