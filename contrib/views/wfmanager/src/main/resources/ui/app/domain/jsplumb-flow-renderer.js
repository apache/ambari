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
import {DefaultLayoutManager as LayoutManager} from '../domain/default-layout-manager';
var JSPlumbRenderer= Ember.Object.extend({
  designerPlumb:null,
  flattenedNodes:null,
  _createConnection(sourceNode,target,transition){
    var connectionColor="#777";
    var lineWidth=1;
    if (transition.condition){
      if(transition.condition==="default"){
        lineWidth=2;
      }else if (transition.condition==="error"|| transition.errorPath){
        connectionColor=Constants.globalSetting.errorTransitionColor;
      }
    }
    var connectionObj={
      source:sourceNode.id,
      target:target.id,
      connector:["Straight"],
      paintStyle:{lineWidth:lineWidth,strokeStyle:connectionColor},
      endpointStyle:{fillStyle:'rgb(243,229,0)'},
      endpoint: ["Dot", {
        radius: 1
      }],
      alwaysRespectStubs:true,
      anchors: [["Bottom"],["Top"]],
      overlays:[]
    };
    return connectionObj;
  },
  _getAddNodeOverlay(context,sourceNode,target,transition){
    var location=target.type==="placeholder"?1:0.5;
    var transitionCount=sourceNode.transitions.length;
    return {
      id: sourceNode.id+"_"+target.id+"_"+"connector",
      location:location,
      /* jshint unused:vars */
      create:function(component) {
        var container=Ember.$('<div />');
        var plus= Ember.$('<div class="fa fa-plus connector_overlay_new"></div>');
        if ((sourceNode.isDecisionNode() && transitionCount>1 ||sourceNode.isForkNode() && transitionCount>2 ) &&
        target.isPlaceholder() &&
        !transition.isDefaultCasePath()){
          var trash=Ember.$('<div class="node_actions node_left"><i class="fa fa-trash-o"></i></div>');
          trash.on("click",function(){
            context.deleteTransition(transition);
          });
          plus.append(trash);
        }
        container.append(plus);
        return container;
      },
      events:{
        click:function(labelOverlay, originalEvent) {
          var element = originalEvent.target;
          context.set('popOverElement', element);
          context.setCurrentTransition(transition);
          context.showWorkflowActionSelect(element);
        }
      }
    };
  },

  _renderNodes(node,visitedNodes){
    if (!node || node.isKillNode()){
      return;
    }
    if (visitedNodes.contains(node)){
      return;
    }
    visitedNodes.push(node);
    if(!this.get("flattenedNodes").contains(node)){
      this.get("flattenedNodes").pushObject(node);
    }
    if (node.transitions.length > 0){
      node.transitions.forEach(function(transition) {
        var target = transition.targetNode;
        this._renderNodes(target,visitedNodes);
      }.bind(this));
    }
  },
  _connectNodes(context,sourceNode){
    var connections=[];
    var visitedNodes=[];
    this._renderTransitions(sourceNode,connections,visitedNodes,context);
    this._layout(connections);
    this.designerPlumb.setSuspendDrawing(true);
    this.designerPlumb.batch(function(){
      connections.forEach(function(conn){
        this.designerPlumb.connect(conn);
      }.bind(this));
    }.bind(this));
    this.designerPlumb.setSuspendDrawing(false,true);

  },
  _renderTransitions(sourceNode,connections,visitedNodes,context){
    var self=this;
    if(!sourceNode){
      return;
    }
    if (visitedNodes.contains(sourceNode)){
      return;
    }
    if (sourceNode.hasTransition() ){
      sourceNode.transitions.forEach(function(transition) {
        var target = transition.targetNode;
        if (target.isKillNode() || !Constants.showErrorTransitions && transition.isOnError()){
          return;
        }
        var connectionObj=self._createConnection(sourceNode,target,transition);

        if (transition.condition){
          var conditionHTML = "<div class='decision-condition' title='"+transition.condition+"'>"+ transition.condition+"</div>";
          connectionObj.overlays.push([ "Label", {label:conditionHTML, location:0.75, id:"myLabel" } ]);
        }
        if (!target.isPlaceholder()){
          connectionObj.overlays.push(["PlainArrow",{location:-0.1,width: 7,length: 7}]);
        }
        if (!(sourceNode.isPlaceholder() || target.isKillNode())){
          var addNodeoverlay=["Custom" , self._getAddNodeOverlay(context,sourceNode,target,transition)];
          connectionObj.overlays.push(addNodeoverlay);
        }
        connections.push(connectionObj);
        self._renderTransitions(target,connections,visitedNodes,context);
      });
    }
  },
  _layout(edges){
    var nodes = Ember.$(".nodecontainer");
    this.layoutManager.doLayout(this.get("context"),nodes,edges,this.get("workflow"));
  },
  initRenderer(callback,settings){
    this.designerPlumb=jsPlumb.getInstance({});
    this.layoutManager=LayoutManager.create({});
    this.context=settings.context;
    this.flattenedNodes=settings.flattenedNodes;
    this.designerPlumb.ready(function() {
      callback();
    }.bind(this));
    return this.designerPlumb;
  },
  refresh(){
    this.designerPlumb.repaintEverything();
  },
  reset(){
    if(!this.get('flattenedNodes')){
      return;
    }
    this.get("flattenedNodes").clear();
    this.designerPlumb.reset();
  },
  cleanup(){
    if(!this.get('flattenedNodes')){
      return;
    }
    this.get('flattenedNodes').clear();
    this.designerPlumb.detachEveryConnection();
  },
  onDidUpdate(){
    this._connectNodes(this.get("context"),this.get("workflow").startNode,this.get("workflow"));
  },
  renderWorkflow(workflow){
    var visitedNodes=[];
    this.set("workflow",workflow);
    this._renderNodes(this.get("workflow").startNode,visitedNodes);
  },

  getBottomPosition(){
    return {
      top : this.get("context").$(".nodeEnd").offset().top,
      left : this.get("context").$(".nodeEnd").offset().left
    };
  }

});
export {JSPlumbRenderer};
