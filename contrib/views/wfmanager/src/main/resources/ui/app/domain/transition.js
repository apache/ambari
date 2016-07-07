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
var Transition = Ember.Object.extend({
  id:null,
  sourceNode:null,
  targetNode:null,
  type:null,
  condition:null,
  errorPath:false,
  init(){

  },
  copyAttribs(transition){
    this.condition=transition.condition;
  },
  isOnError(){
    return this.condition==="error";
  },
  isDefaultCasePath(){
    return this.condition==="default";
  },
  getSourceNode(){
    return this.get("sourceNode");
  },
  getTargetNode(skipPlaceholder){
    var currNode=this.targetNode;
    if (skipPlaceholder===false){
      return currNode;
    }
    while(currNode.isPlaceholder()){
      var targets=currNode.getTargets();
      currNode=targets[0];
    }
    return currNode;
    /*if (this.targetNode.isPlaceholder()){
      return this.targetNode.getTargets()[0];
    }else{
      return this.targetNode;
    }*/
  }
});
export {Transition};
