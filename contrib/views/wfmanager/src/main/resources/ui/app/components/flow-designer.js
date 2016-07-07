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
import {Workflow} from '../domain/workflow';
import Constants from '../utils/constants';
import {WorkflowGenerator} from '../domain/workflow-xml-generator';
import {WorkflowImporter} from '../domain/workflow-importer';
import {WorkflowContext} from '../domain/workflow-context';
import {DefaultLayoutManager as LayoutManager} from '../domain/default-layout-manager';
//import {LayoutManager1 as LayoutManager} from '../domain/layout-manager1';
import EmberValidations,{ validator } from 'ember-validations';


export default Ember.Component.extend(EmberValidations,{
  workflowContext : WorkflowContext.create({}),
  workflowTitle:"",
  previewXml:"",
  supportedActionTypes:["java", "hive", "pig", "sqoop", "shell", "spark", "map-reduce", "hive2", "sub-workflow", "distcp", "ssh", "FS"],
  workflow:null,
  showingConfirmationNewWorkflow:false,
  showingWorkflowConfigProps:false,
  workflowSubmitConfigs:{},
  showingPreview:false,
  currentTransition:null,
  currentNode:null,
  domain:{},
  showActionEditor : false,
  flattenedNodes: [],

  workflowImporter:WorkflowImporter.create({}),
  designerPlumb:null,
  propertyExtractor : Ember.inject.service('property-extractor'),
  showGlobalConfig : false,
  globalConfig : {},
  clonedDomain : {},
  clonedErrorNode : {},
  validationErrors : [],
  layoutManager:null,
  showingFileBrowser : false,
  killNode : {},
  initialize :function(){
    this.designerPlumb=jsPlumb.getInstance({});
    this.layoutManager=LayoutManager.create({});
    this.setConentWidth();
    this.set('workflow',Workflow.create({}));
    this.workflow.initialize();
    this.initAndRenderWorkflow();
    this.$('#wf_title').focus();
  }.on('didInsertElement'),
  validations: {
    'flattenedNodes': {
      inline : validator(function() {
        var nodeNames = new Map();
        this.get("validationErrors").clear();
        this.get('flattenedNodes').forEach((item)=>{
          Ember.set(item, "errors", false);
          if(nodeNames.get(item.name)){
            Ember.set(item, "errors", true);
            this.get("validationErrors").pushObject({node:item,message:"Node name should be unique"});
          }else{
            nodeNames.set(item.name, item);
            Ember.set(item, "errors", false);
          }
          if(this.get("supportedActionTypes").indexOf(item.actionType) === -1 && item.type === "action"){
            //this.get('validationErrors').pushObject({node : item ,message : item.actionType+" is unsupported"});
            this.get('validationErrors').pushObject({node : item ,message : item.actionType+" is unsupported"});
          }
          var nodeErrors=item.validateCustom();
          if (nodeErrors.length>0){
            Ember.set(item, "errors", true);
            nodeErrors.forEach(function(errMsg){
                this.get("errors").pushObject({node:item,message:errMsg });
            }.bind(this));
          }
        }.bind(this));

        if(this.get('flattenedNodes').length !== nodeNames.size || this.get("errors").length>0){
          return true;
        }
      })
    },
    "workflow.killnodes": {
      inline : validator(function() {
        let killNodes = [], flag;
        if(this.get("workflow") && this.get("workflow").killNodes){
          killNodes = this.get("workflow").killNodes;
          for(let i=0; i<killNodes.length; i++){
            for(let j=0; j<killNodes.length; j++){
              if(killNodes[i].name === killNodes[j].name && i !== j){
                this.get('validationErrors').pushObject({node : killNodes[j] ,message : "Duplicate killnode"});
                flag = true;
                break;
              }
            }
            if(flag){
              break;
            }
          }
        }
        if (flag){
          return true;
        }
      })
    }
  },
  setConentWidth(){
    var offset = 120;
    if (Ember.ENV.instanceInfo) {
        offset = 0;
    }
    // Ember.$("#content .designer-panel")
    //     .css("height", window.innerHeight - offset);
    //
    // Ember.$("#content")
    //     .css("width", window.innerWidth /*- 5*/ );

    Ember.$(window)
        .resize(function() {
          return;
            // OnResise: resize the panel to the screen height
            // Ember.$("#content .designer-panel")
            //     .css("height", window.innerHeight - offset);
            // Ember.$("#content")
            //     .css("width", window.innerWidth /*- 5*/ );
        });
  },
  nodeRendered: function(){
    if(this.get('renderNodeTransitions')){
      this.renderTransitions(this.get("workflow").startNode);
      this.layout();
      this.set('renderNodeTransitions',false);
    }
  }.on('didUpdate'),
  cleanUpJsplumb:function(){
    this.get('flattenedNodes').clear();
    this.set('renderNodeTransitions',false);
    this.designerPlumb.detachEveryConnection();
//    this.designerPlumb.empty("flow-designer");

  }.on('willDestroyElement'),
  initAndRenderWorkflow(){
    this.designerPlumb.ready(function() {
      this.renderWorkflow();
    }.bind(this));
  },
  renderWorkflow(){
    //this.get('flattenedNodes').splice(0,this.get('flattenedNodes').length);
    this.get('flattenedNodes').clear();
    this.set('renderNodeTransitions', true);
    this.renderNodes(this.get("workflow").startNode);
  },
  rerender(){
    this.designerPlumb.detachEveryConnection();
    this.renderWorkflow(this.get("workflow"));
  },
  setCurrentTransition(transition){
    this.set("currentTransition",transition);
  },
  renderNodes(node){
    if (!node || node.isKillNode()){
      return;
    }
    if(!this.get('flattenedNodes').contains(node)){
      this.get('flattenedNodes').pushObject(node);
    }
    if (node.transitions.length > 0){
      node.transitions.forEach(function(transition) {
        var target = transition.targetNode;
        this.renderNodes(target);
      }.bind(this));
    }
  },
  createConnection(sourceNode,target,transition){
    var connectionColor="#777";
    var lineWidth=1;
    if (transition.condition){
      if(transition.condition==="default"){
        //connectionColor=Constants.globalSetting.defaultTransitionColor;
        lineWidth=2;
      }else if (transition.condition==="error"|| transition.errorPath){
        connectionColor=Constants.globalSetting.errorTransitionColor;
      }
    }
    var connection=this.designerPlumb.connect({
        source:sourceNode.id,
        target:target.id,
        //connector:["Flowchart", { stub:[0,0],alwaysRespectStubs :true,cornerRadius: 5 }],
        connector:["Straight"],
        //connector:["StateMachine",{curviness:0}],

        paintStyle:{lineWidth:lineWidth,strokeStyle:connectionColor},
        endpointStyle:{fillStyle:'rgb(243,229,0)'},
        endpoint: ["Dot", {
          radius: 1
        }],
        alwaysRespectStubs:true,
        anchors: [["Bottom"],["Top"]]
      //  anchors: [["Continuous"],["Continuous"]]
    });
    return connection;
  },
  deleteTransition(transition){
    console.log("delete transition called :",transition);
    this.get("workflow").deleteTransition(transition);
    this.rerender();
  },
  renderTransitions(sourceNode){
    var self=this;
    if(!sourceNode){
      return;
    }
    if (sourceNode.hasTransition() ){
      var transitionCount=sourceNode.transitions.length;
      sourceNode.transitions.forEach(function(transition) {
        var target = transition.targetNode;
        if (target.isKillNode() || !Constants.showErrorTransitions && transition.isOnError()){
          return;
        }
        var connection=self.createConnection(sourceNode,target,transition);
        if (transition.condition){
            var conditionHTML = "<div class='decision-condition' title='"+transition.condition+"'>"+ transition.condition+"</div>";
            connection.addOverlay([ "Label", {label:conditionHTML, location:0.75, id:"myLabel" } ]);
        }
        if (!target.isPlaceholder()){
          connection.addOverlay(["PlainArrow",{location:-0.1,width: 7,length: 7}]);
        }
        if (!(sourceNode.isPlaceholder() || target.isKillNode())){
        //if (!sourceNode.isPlaceholder()){
          var location=target.type==="placeholder"?1:0.5;
          var addNodeoverlay=["Custom" , {
                  id: sourceNode.id+"_"+target.id+"_"+"connector",
                  location:location,
                  create:function(component) {
                    var container=Ember.$('<div />');
                    var plus= Ember.$('<div class="fa fa-plus connector_overlay_new"></div>');
                    if ((sourceNode.isDecisionNode() && transitionCount>1 ||sourceNode.isForkNode() && transitionCount>2 )
                      && target.isPlaceholder()
                      && !transition.isDefaultCasePath()){
                      var trash=Ember.$('<div class="node_actions node_left"><i class="fa fa-trash-o"></i></div>');
                      trash.on("click",function(){
                        self.deleteTransition(transition);
                      });
                      plus.append(trash);
                    }
                    container.append(plus);
                    return container;
                  },
          events:{
              click:function(labelOverlay, originalEvent) {
                var element = originalEvent.target;
                self.set('popOverElement', element);
                self.setCurrentTransition(transition);
                self.$('.popover').popover('destroy');
                Ember.$(element).parents(".jsplumb-overlay").css("z-index", "4");
                self.$(element).attr('data-toggle','popover');
                self.$(element).popover({
                  html : true,
                  title : "Click on an Action to Insert <button type='button' class='close'>&times;</button>",
                  placement: 'right',
                  trigger : 'focus',
                  content : function(){
                    return self.$('#workflow-actions').html();
                  }
                });
                self.$(element).popover("show");
                self.$('.popover .close').on('click',function(){
                  //  $(element).parents(".jsplumb-overlay").css("z-index", "");
                    Ember.$(".jsplumb-overlay").css("z-index", "");
                    self.$('.popover').popover('destroy');
                });
              }
            }
          }];
          connection.addOverlay(addNodeoverlay);
        }
        self.renderTransitions(target);
      });
    }
  },
  layout(){
    var nodes = Ember.$(".nodecontainer");
    var edges = this.designerPlumb.getConnections();
    console.log(edges);
    this.layoutManager.doLayout(this,nodes,edges,this.get("workflow"));
    this.designerPlumb.repaintEverything();
    var endNodeTop=this.$("#node-end").offset().top;
    var endNodeLeft=this.$("#node-end").offset().left;
    this.$("#killnodes-container").offset({top:endNodeTop+50,left:endNodeLeft-50});
    var top = this.$("#killnodes-container").offset().top + 40;
    var left = this.$("#killnodes-container").offset().left - 28;
    this.$('.kill').each(function(index,value){
      this.$(value).offset({top:top,left:left});
      top = this.$(value).offset().top+70 ;
      //left = this.$(value).offset().left+200;
    }.bind(this));
  },
  doValidation(){
    this.set('validationErrors',[]);
    this.validate().then(() => {
      this.set('validationErrors',[]);
    }).catch(() => {
        this.get('flattenedNodes').filterBy('errors',true).forEach((node)=>{
        //this.get('validationErrors').pushObject({node : node ,message :"Node name should be unique"});
        this.get('validationErrors').pushObjects(node.errorMsgs);
      }.bind(this));

    }.bind(this));
  },
  importWorkflow(filePath){
    this.set("workflowFilePath", filePath);
    this.resetDesigner();
    var workflowXmlDefered=this.getWorkflowFromHdfs(filePath);
    workflowXmlDefered.promise.then(function(data){
      var workflow=this.get("workflowImporter").importWorkflow(data);
      this.resetDesigner();
      this.set("workflow",workflow);
      this.rerender();
      this.doValidation();
    }.bind(this)).catch(function(e){
      console.error(e);
    });
  },
  getWorkflowFromHdfs(filePath){
    var url = Ember.ENV.API_URL + "/readWorkflowXml?workflowXmlPath="+filePath;
    var deferred = Ember.RSVP.defer();
    Ember.$.ajax({
        url: url,
        method: 'GET',
        dataType: "text",
        beforeSend: function (xhr) {
            xhr.setRequestHeader("X-Requested-By", "Ambari");
        }
    }).done(function(data){
      deferred.resolve(data);
    }).fail(function(){
      deferred.reject();
    });
    return deferred;
  },
  resetDesigner(){
    this.set('errors',{});
    this.set('validationErrors',{});
    this.set('workflowFilePath',"");
    this.get("workflow").resetWorfklow();
    this.set('globalConfig', {});
    this.designerPlumb.reset();
  },
  resetZoomLevel(){
    this.set("zoomLevel", 1);
  },
  incZoomLevel(){
    this.set("zoomLevel", this.get("zoomLevel")+0.1);
  },
  decZoomLevel(){
    this.set("zoomLevel", this.get("zoomLevel")-0.1);
  },
  importSampleWorkflow (){
    var deferred = Ember.RSVP.defer();
    Ember.$.ajax({
        url: "/sampledata/workflow.xml",
        dataType: "text",
        cache:false,
        success: function(data) {
          var workflow=this.get("workflowImporter").importWorkflow(data);
          deferred.resolve(workflow);
        }.bind(this),
        failure : function(data){
          deferred.reject(data);
        }
    });
    return deferred;
  },
  actions:{
    showWorkflowSla (value) {
      this.set('showWorkflowSla', value);
    },
    showCreateKillNode(value){
      this.set('showWorkflowSla', value);
    },
    showVersionSettings(value){
      this.set('showVersionSettings', value);
    },
    createKillNode(){
      this.set("createKillnodeError",null);
      var existingKillNode=this.get('workflow').get("killNodes").findBy("name",this.get('killNode.name'));
      if (existingKillNode){
        this.set("createKillnodeError","The kill node already exists");
        return;
      }
      if (Ember.isBlank(this.get('killNode.name'))){
          this.set("createKillnodeError","The kill node cannot be empty");
          return;
      }
      this.get("workflow").createKillNode(this.get('killNode.name'),this.get('killNode.message'));
      this.set('killNode',{});
      this.rerender();
      this.layout();
      this.doValidation();
      this.$("#kill-node-dialog").modal("hide");
      this.set('showCreateKillNode', false);
    },
    addNode(type){
      var currentTransition=this.get("currentTransition");
      //this.get("workflow").addNode(currentTransition.startNode,currentTransition.endNode,type);
      this.get("workflow").addNode(currentTransition,type);
      this.rerender();
      if(currentTransition.targetNode.isPlaceholder()){
        this.designerPlumb.remove(currentTransition.targetNode.id);
      }
      this.doValidation();
    },
    nameChanged(){
      this.doValidation();
    },
    deleteNode(node){
      if(node.isKillNode()){
        var result=this.get("workflow").deleteKillNode(node);
        if (result && result.status===false){
          this.get('validationErrors').pushObject({node : node ,message :result.message});
        }
      } else {
        this.get("workflow").deleteNode(node);
      }
      this.rerender();
      this.doValidation();
    },
    openEditor(node){
      this.set('showActionEditor', true);
      this.set('currentAction', node.actionType);
      var domain = node.getNodeDetail();
      this.set('clonedDomain',Ember.copy(domain));
      this.set('clonedErrorNode', node.errorNode);
      this.set('clonedKillMessage',node.get('killMessage'));
      node.set("domain", domain);
      this.set('currentNode', node);
    },
    addBranch(node){
      this.get("workflow").addBranch(node);
      this.rerender();
    },
    addDecisionBranch(settings){
      console.log("flowdesigner: add decision branh",settings);
      this.get("workflow").addDecisionBranch(settings);
      this.rerender();
    },
    addKillNode(errorNode){
      var currentNode= this.get("currentNode");
      if(errorNode && errorNode.isNew){
        //this.get("workflow").addNode(currentNode,null,"kill",errorNode);
        this.get("workflow").addKillNode(currentNode,errorNode);
        this.get("workflow.killNodes").push(errorNode);
      }else {
        this.set('currentNode.errorNode', errorNode);
      }
    //  this.rerender();
    },
    submitWorkflow(){
      //var workflowConverter=WorkflowConverter.create({});
      this.get('workflowContext').clearErrors();
      var workflowGenerator=WorkflowGenerator.create({workflow:this.get("workflow"),
        workflowContext:this.get('workflowContext')});
      var workflowXml=workflowGenerator.process();
      if(this.get('workflowContext').hasErrors()){
        this.set('errors',this.get('workflowContext').getErrors());
      }else{
        var dynamicProperties = this.get('propertyExtractor').getDynamicProperties(workflowXml);
        var configForSubmit={props:dynamicProperties,xml:workflowXml};
        this.set("workflowSubmitConfigs",configForSubmit);
        this.set("showingWorkflowConfigProps",true);
      }

    },
    previewWorkflow(){
        this.set("showingPreview",false);
      this.get('workflowContext').clearErrors();
      var workflowGenerator=WorkflowGenerator.create({workflow:this.get("workflow"),
        workflowContext:this.get('workflowContext')});
      var workflowXml=workflowGenerator.process();
      if(this.get('workflowContext').hasErrors()){
        this.set('errors',this.get('workflowContext').getErrors());
      }else{
        this.set("previewXml",vkbeautify.xml(workflowXml));
        this.set("showingPreview",true);
      }
    },
    closeWorkflowSubmitConfigs(){
      this.set("showingWorkflowConfigProps",false);
    },
    importWorkflowTest(){
      var deferred = this.importSampleWorkflow();
      deferred.promise.then(function(data){
        this.resetDesigner();
        this.set("workflow",data);
        this.rerender();
        this.doValidation();
      }.bind(this)).catch(function(e){
        console.error(e);
      });
    },
    closeFileBrowser(){
       this.set("showingFileBrowser",false);
       if(this.get('workflowFilePath')){
         this.importWorkflow(this.get('workflowFilePath'));
       }
    },
    showFileBrowser(){
      this.set('showingFileBrowser', true);
    },
    createNewWorkflow(){
      this.resetDesigner();
      this.rerender();
      this.set("workflowFilePath", "");
      this.$('#wf_title').focus();
    },
    conirmCreatingNewWorkflow(){
      this.set('showingConfirmationNewWorkflow', true);
    },
    showWorkflowGlobalProps(){
      if(this.get('workflow.globalSetting') !== null){
        this.set('globalConfig', Ember.copy(this.get('workflow.globalSetting')));
      }else{
        this.set('globalConfig', {});
      }
      this.set("showGlobalConfig", true);
    },
    closeWorkflowGlobalProps(){
      this.set("showGlobalConfig", false);
    },
    saveGlobalConfig(){
      this.set('workflow.globalSetting', Ember.copy(this.get('globalConfig')));
      this.set("showGlobalConfig", false);
    },
    zoomIn(){
      if(!this.get("zoomLevel")){
         this.resetZoomLevel();
      }
      this.decZoomLevel();
      var lev = this.get("zoomLevel") <= 0 ? 0.1 : this.get("zoomLevel");
      this.$("#flow-designer").css("transform", "scale(" + lev + ")");
    },
    zoomOut(){
      if(!this.get("zoomLevel")){
         this.resetZoomLevel();
      }
      this.incZoomLevel();
      var lev = this.get("zoomLevel") >= 1 ? 1 : this.get("zoomLevel");
      this.$("#flow-designer").css("transform", "scale(" + lev + ")");
    },
    zoomReset(){
      this.resetZoomLevel();
      this.$("#flow-designer").css("transform", "scale(" + 1 + ")");
    },
    closeActionEditor (isSaved){
      if(isSaved){
        this.currentNode.onSave();
        this.doValidation();
      }	else {
        this.set('currentNode.domain',Ember.copy(this.get('clonedDomain')));
        this.set('currentNode.errorNode', this.get('clonedErrorNode'));
        if(this.currentNode.type === 'kill'){
            this.set('currentNode.killMessage', this.get('clonedKillMessage'));
        }
      }
      this.set('showActionEditor', false);
      this.rerender();
    }
  }
});
