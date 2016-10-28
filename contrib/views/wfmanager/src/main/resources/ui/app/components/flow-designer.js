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
import {WorkflowJsonImporter} from '../domain/workflow-json-importer';
import {WorkflowContext} from '../domain/workflow-context';
import {JSPlumbRenderer} from '../domain/jsplumb-flow-renderer';
import {CytoscapeRenderer} from '../domain/cytoscape-flow-renderer';
import {FindNodeMixin} from '../domain/findnode-mixin';
import { validator, buildValidations } from 'ember-cp-validations';
import WorkflowPathUtil from '../domain/workflow-path-util';

const Validations = buildValidations({
  'dataNodes': { /* For Cytoscape */
    validators: [
      validator('duplicate-data-node-name', {
        dependentKeys: ['dataNodes.@each.dataNodeName']
      })
    ]
  },
  'workflow.killNodes': {
    validators: [
      validator('duplicate-kill-node-name', {
        dependentKeys: ['workflow.killNodes.@each.name']
      })
    ]
  },
  'flattenedNodes': {
    validators: [
      validator('duplicate-flattened-node-name', {
        dependentKeys: ['flattenedNodes.@each.name']
      })
    ]
  }
});

export default Ember.Component.extend(FindNodeMixin, Validations, {
  workflowContext : WorkflowContext.create({}),
  workflowTitle:"",
  previewXml:"",
  supportedActionTypes:["java", "hive", "pig", "sqoop", "shell", "spark", "map-reduce", "hive2", "sub-workflow", "distcp", "ssh", "FS"],
  workflow:null,
  hoveredWidget:null,/**/
  showingConfirmationNewWorkflow:false,
  showingWorkflowConfigProps:false,
  workflowSubmitConfigs:{},
  showingPreview:false,
  currentTransition:null,
  currentNode:null,
  domain:{},
  showActionEditor : false,
  flattenedNodes: [],
  dataNodes: [], /* For cytoscape */
  hoveredAction: null,
  workflowImporter:WorkflowImporter.create({}),
  propertyExtractor : Ember.inject.service('property-extractor'),
  clipboardService : Ember.inject.service('workflow-clipboard'),
  workspaceManager : Ember.inject.service('workspace-manager'),
  showGlobalConfig : false,
  showParameterSettings : false,
  showNotificationPanel : false,
  globalConfig : {},
  parameters : {},
  clonedDomain : {},
  clonedErrorNode : {},
  validationErrors : [],
  showingFileBrowser : false,
  killNode : {},
  isWorkflowImporting: false,
  isImportingSuccess: true,
  shouldPersist : false,
  useCytoscape: Constants.useCytoscape,
  cyOverflow: {},
  clipboard : Ember.computed.alias('clipboardService.clipboard'),
  isStackTraceVisible: false,
  isStackTraceAvailable: false,
  stackTrace:"",
  initialize : function(){
    var id = 'cy-' + Math.ceil(Math.random() * 1000);
    this.set('cyId', id);
    this.sendAction('register', this.get('tabInfo'), this);
  }.on('init'),
  elementsInserted :function(){
    if (this.useCytoscape){
      this.flowRenderer=CytoscapeRenderer.create({id : this.get('cyId')});
    }else{
      this.flowRenderer=JSPlumbRenderer.create({});
    }

    this.setConentWidth();
    this.set('workflow',Workflow.create({}));
    if(this.get("xmlAppPath")){
      this.showExistingWorkflow();
      return;
    } else {
      this.workflow.initialize();
      this.initAndRenderWorkflow();
      this.$('#wf_title').focus();
      if (Constants.autoRestoreWorkflowEnabled){
        this.restoreWorkflow();
      }
    }
    if(Ember.isBlank(this.get('workflow.name'))){
      this.set('workflow.name', Ember.copy(this.get('tabInfo.name')));
    }

  }.on('didInsertElement'),
  restoreWorkflow(){
    if (!this.get("isNew")){
      var draftWorkflow=this.getDraftWorkflow();
      if (draftWorkflow){
        this.resetDesigner();
        this.set("workflow",draftWorkflow);
        this.rerender();
        this.doValidation();
      }
    }
  },
  observeXmlAppPath : Ember.observer('xmlAppPath', function(){
    if(!this.get('xmlAppPath') || null === this.get('xmlAppPath')){
      return;
    }else{
      this.showExistingWorkflow();
    }
  }),
  observeFilePath : Ember.observer('workflowFilePath', function(){
    if(!this.get('workflowFilePath') || null === this.get('workflowFilePath')){
      return;
    }else{
      this.sendAction('changeFilePath', this.get('tabInfo'), this.get('workflowFilePath'));
    }
  }),
  nameObserver : Ember.observer('workflow.name', function(){
    if(!this.get('workflow')){
      return;
    }else if(this.get('workflow') && Ember.isBlank(this.get('workflow.name'))){
      if(!this.get('clonedTabInfo')){
        this.set('clonedTabInfo', Ember.copy(this.get('tabInfo')));
      }
      this.sendAction('changeTabName', this.get('tabInfo'), this.get('clonedTabInfo.name'));
    }else{
      this.sendAction('changeTabName', this.get('tabInfo'), this.get('workflow.name'));
    }
  }),
  showParentWorkflow(type, path){
    this.sendAction('openTab', type, path);
  },
  showExistingWorkflow(){
    var workflowXmlPath = this.get("xmlAppPath"), relXmlPath = "", tempArr;
    if(workflowXmlPath.indexOf("://") === -1 && workflowXmlPath.indexOf(":") === -1){
      relXmlPath = workflowXmlPath;
    } else{
      tempArr = workflowXmlPath.split("//")[1].split("/");
      tempArr.splice(0, 1);
      relXmlPath = "/" + tempArr.join("/");
      if(relXmlPath.indexOf(".xml") !== relXmlPath.length-4) {
        if(relXmlPath.charAt(relXmlPath.length-1) !== "/"){
          relXmlPath = relXmlPath+ "/" +"workflow.xml";
        } else{
          relXmlPath = relXmlPath+"workflow.xml";
        }
      }
    }
    this.importWorkflow(relXmlPath);
  },
  setConentWidth(){
    var offset = 120;
    if (Ember.ENV.instanceInfo) {
      offset = 0;
    }
    Ember.$(window).resize(function() {
      return;
    });
  },
  workflowXmlDownload(workflowXml){
      var link = document.createElement("a");
      link.download = "workflow.xml";
      link.href = "data:text/xml,"+vkbeautify.xml(workflowXml);
      link.click();
  },
  nodeRendered: function(){
    this.doValidation();
    if(this.get('renderNodeTransitions')){
      this.flowRenderer.onDidUpdate(this,this.get("workflow").startNode,this.get("workflow"));
      this.layout();
      this.set('renderNodeTransitions',false);
    }
    this.resize();
    this.persistWorkInProgress();
  }.on('didUpdate'),
  resize(){
    this.flowRenderer.resize();
  },
  cleanupFlowRenderer:function(){
    this.set('renderNodeTransitions',false);
    this.flowRenderer.cleanup();
  }.on('willDestroyElement'),
  initAndRenderWorkflow(){
    var panelOffset=this.$(".designer-panel").offset();
    var canvasHeight=Ember.$(window).height()-panelOffset.top-25;
    this.flowRenderer.initRenderer(function(){
      this.renderWorkflow();
    }.bind(this),{context:this,flattenedNodes:this.get("flattenedNodes"),dataNodes:this.get("dataNodes"), cyOverflow:this.get("cyOverflow"),canvasHeight:canvasHeight});
  },
  renderWorkflow(){
    this.set('renderNodeTransitions', true);
    this.flowRenderer.renderWorkflow(this.get("workflow"));
    this.doValidation();
  },
  rerender(){
    this.flowRenderer.cleanup();
    this.renderWorkflow(this.get("workflow"));
  },
  setCurrentTransition(transition){
    this.set("currentTransition",transition);
  },
  actionInfo(node){
    this.send("showNotification", node);
  },
  deleteTransition(transition){
    this.createSnapshot();
    this.get("workflow").deleteTransition(transition);
    this.showUndo('transition');
    this.rerender();
  },
  showWorkflowActionSelect(element){
    var self=this;
    this.$('.popover').popover('destroy');
    Ember.$(element).parents(".jsplumb-overlay").css("z-index", "4");
    this.$(element).attr('data-toggle','popover');
    this.$(element).popover({
      html : true,
      title : "Add Node <button type='button' class='close'>&times;</button>",
      placement: 'right',
      trigger : 'focus',
      content : function(){
        return self.$('#workflow-actions').html();
      }
    });
    this.$(element).popover("show");
    this.$('.popover .close').on('click',function(){
      Ember.$(".jsplumb-overlay").css("z-index", "");
      this.$('.popover').popover('destroy');
    }.bind(this));
  },

  layout(){
    this.flowRenderer.refresh();
  },
  doValidation(){
    this.validate();
  },
  getStackTrace(data){
    if(data){
     try{
      var stackTraceMsg = JSON.parse(data).stackTrace;
      if(!stackTraceMsg){
        return "";
      }
     if(stackTraceMsg instanceof Array){
       return stackTraceMsg.join("").replace(/\tat /g, '&nbsp;&nbsp;&nbsp;&nbsp;at&nbsp;');
     } else {
       return stackTraceMsg.replace(/\tat /g, '<br/>&nbsp;&nbsp;&nbsp;&nbsp;at&nbsp;');
     }
     } catch(err){
       return "";
     }
    }
    return "";
  },
  importWorkflow(filePath){
    var self = this;
    this.set("isWorkflowImporting", true);
    this.set("workflowFilePath", filePath);
    this.resetDesigner();
    this.set("isWorkflowImporting", true);
    var workflowXmlDefered=this.getWorkflowFromHdfs(filePath);
    workflowXmlDefered.promise.then(function(data){
      this.importWorkflowFromString(data);
      this.set("isWorkflowImporting", false);
    }.bind(this)).catch(function(data){
    var stackTraceMsg = self.getStackTrace(data.responseText);
    if(stackTraceMsg.length){
      self.set("isStackTraceVisible", true);
      self.set("stackTrace", stackTraceMsg);
      self.set("isStackTraceAvailable", true);
    } else {
      self.set("isStackTraceVisible", false);
      self.set("isStackTraceAvailable", false);
    }
      self.set("isWorkflowImporting", false);
      self.set("isImportingSuccess", false);
    });
  },
  importWorkflowFromString(data){
    var workflow=this.get("workflowImporter").importWorkflow(data);
    if(this.get('workflow')){
      this.resetDesigner();
      this.set("workflow",workflow);
      this.initAndRenderWorkflow();
      this.rerender();
      this.doValidation();
    }else{
      this.workflow.initialize();
      this.set("workflow",workflow);
      this.initAndRenderWorkflow();
      this.$('#wf_title').focus();
    }
  },
  getWorkflowFromHdfs(filePath){
    var url = Ember.ENV.API_URL + "/readWorkflowXml?workflowXmlPath="+filePath;
    var deferred = Ember.RSVP.defer();
    Ember.$.ajax({
      url: url,
      method: 'GET',
      dataType: "text",
      beforeSend: function (xhr) {
        xhr.setRequestHeader("X-XSRF-HEADER", Math.round(Math.random()*100000));
        xhr.setRequestHeader("X-Requested-By", "Ambari");
      }
    }).done(function(data){
      deferred.resolve(data);
    }).fail(function(data){
      deferred.reject(data);
    });
    return deferred;
  },
  resetDesigner(){
    this.set("isImportingSuccess", true);
    this.set("xmlAppPath", null);
    this.set('errors',[]);
    this.set('validationErrors',[]);
    this.set('workflowFilePath',"");
    this.get("workflow").resetWorfklow();
    this.set('globalConfig', {});
    this.set('parameters', {});
    if(this.get('workflow.parameters') !== null){
      this.set('workflow.parameters', {});
    }
    this.set('parameters', {});
    this.flowRenderer.reset();
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
  persistWorkInProgress(){
   var json=JSON.stringify(this.get("workflow"));
   this.get('workspaceManager').saveWorkInProgress(this.get('tabInfo.id'), json);
  },
  getDraftWorkflow(){
    var drafWorkflowJson = this.get('workspaceManager').restoreWorkInProgress(this.get('tabInfo.id'));
    var workflowImporter=WorkflowJsonImporter.create({});
    var workflow=workflowImporter.importWorkflow(drafWorkflowJson);
    return workflow;
  },
  createSnapshot() {
    this.set('undoAvailable', false);
    this.set('workflowSnapshot', JSON.stringify(this.get("workflow")));
  },
  showUndo (type){
    this.set('undoAvailable', true);
    this.set('undoType', type);
  },
  deleteWorkflowNode(node){
    this.createSnapshot();
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
    this.showUndo('node');
  },
  addWorkflowBranch(node){
    this.createSnapshot();
    this.get("workflow").addBranch(node);
    this.rerender();
  },
  openWorkflowEditor(node){
    this.createSnapshot();
    var validOkToNodes = WorkflowPathUtil.findValidTransitionsTo(this.get('workflow'), node);
    this.set('showActionEditor', true);
    this.set('currentAction', node.actionType);
    var domain = node.getNodeDetail();
    this.set('clonedDomain',Ember.copy(domain));
    this.set('clonedErrorNode', node.errorNode);
    this.set('clonedKillMessage',node.get('killMessage'));
    node.set("domain", domain);
    node.set("validOkToNodes", validOkToNodes);
    this.set('currentNode', node);
  },
  openDecisionEditor(node) {
    this.get("addBranchListener").trigger("showBranchOptions", node);
  },

  copyNode(node){
    this.get('clipboardService').setContent(node, 'copy');
  },
  cutNode(node){
    this.get('clipboardService').setContent(node, 'cut');
    this.deleteWorkflowNode(node);
  },
  replaceNode(node){
    var clipboardContent = this.get('clipboardService').getContent();
    Ember.set(node, 'name', clipboardContent.name+'-copy');
    Ember.set(node, 'domain', clipboardContent.domain);
    Ember.set(node, 'actionType', clipboardContent.actionType);
    this.rerender();
    this.doValidation();
  },
  scrollToNewPosition(){
    if (Constants.useCytoscape){
      return;
    }
    var scroll = Ember.$(window).scrollTop();
    Ember.$('html, body')
    .animate({
      scrollTop: scroll+200
    }, 1000);
  },
  openSaveWorkflow (){
    this.get('workflowContext').clearErrors();
    var workflowGenerator=WorkflowGenerator.create({workflow:this.get("workflow"),
    workflowContext:this.get('workflowContext')});
    var workflowXml=workflowGenerator.process();
    if(this.get('workflowContext').hasErrors()){
      this.set('errors',this.get('workflowContext').getErrors());
    }else{
      var dynamicProperties = this.get('propertyExtractor').getDynamicProperties(workflowXml);
      var configForSubmit={props:dynamicProperties,xml:workflowXml,params:this.get('workflow.parameters')};
      this.set("workflowSubmitConfigs",configForSubmit);
      this.set("showingSaveWorkflow",true);
    }
  },  
  openJobConfig (){
    this.get('workflowContext').clearErrors();
    var workflowGenerator=WorkflowGenerator.create({workflow:this.get("workflow"),
    workflowContext:this.get('workflowContext')});
    var workflowXml=workflowGenerator.process();
    if(this.get('workflowContext').hasErrors()){
      this.set('errors',this.get('workflowContext').getErrors());
    }else{
      var dynamicProperties = this.get('propertyExtractor').getDynamicProperties(workflowXml);
      var configForSubmit={props:dynamicProperties,xml:workflowXml,params:this.get('workflow.parameters')};
      this.set("workflowSubmitConfigs",configForSubmit);
      this.set("showingWorkflowConfigProps",true);
    }
  },
  actions:{
    showStackTrace(){
      this.set("isStackTraceVisible", true);
    },
    hideStackTrace(){
      this.set("isStackTraceVisible", false);
    },
    showWorkflowSla (value) {
      this.set('showWorkflowSla', value);
    },
    showCreateKillNode (value){
      this.set('showKillNodeManager', value);
      this.set('addKillNodeMode', true);
      this.set('editMode', false);
    },
    showKillNodeManager (value){
      this.set('showKillNodeManager', value);
      this.set('addKillNodeMode', false);
    },
    closeKillNodeManager(){
      this.set("showKillNodeManager", false);
    },
    showVersionSettings(value){
      this.set('showVersionSettings', value);
    },
    showingParameterSettings(value){ 
      if(this.get('workflow.parameters') !== null){
        this.set('parameters', Ember.copy(this.get('workflow.parameters')));
      }else{
        this.set('parameters', {});
      }
      this.set('showParameterSettings', value);
    },
    showCredentials(value){
      this.set('showCredentials', value);
    },
    createKillNode(killNode){
      this.set("killNode", killNode);
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
      this.get("workflow").createKillNode(this.get('killNode.name'),this.get('killNode.killMessage'));
      this.set('killNode',{});
      this.rerender();
      this.layout();
      this.doValidation();
      this.$("#kill-node-dialog").modal("hide");
      this.set('showCreateKillNode', false);
    },
    addNode(type){
      this.createSnapshot();
      var currentTransition=this.get("currentTransition");
      this.get("workflow").addNode(this.findTransition(this.get("workflow").startNode, currentTransition.sourceNodeId, currentTransition.targetNode.id),type);
      this.rerender();
      this.doValidation();
      this.scrollToNewPosition();
    },

    nameChanged(){
      this.doValidation();
    },
    copyNode(node){
      this.copyNode(node);
    },
    pasteNode(){
      var clipboardContent = this.get('clipboardService').getContent();
      var currentTransition = this.get("currentTransition");
      var node = this.get("workflow").addNode(currentTransition, clipboardContent.actionType);
      if(clipboardContent.operation === 'cut'){
        node.name = clipboardContent.name;
      }else{
        node.name = clipboardContent.name + '-copy';
      }
      node.domain = clipboardContent.domain;
      node.actionType = clipboardContent.actionType;
      this.rerender();
      this.doValidation();
      this.scrollToNewPosition();
    },
    deleteNode(node){
      this.deleteWorkflowNode(node);
    },
    openEditor(node){
      this.openWorkflowEditor(node);
    },
    setFilePath(filePath){
      this.set("workflowFilePath", filePath);
    },
    showNotification(node){
      this.set("showNotificationPanel", true);
      if(node.actionType){
        //this.set("hoveredWidget", node.actionType+"-action-info");
        //this.set("hoveredAction", node.getNodeDetail());
      }
    },
    hideNotification(){
      this.set("showNotificationPanel", false);
    },
    addBranch(node){
      this.addWorkflowBranch(node);
    },
    addDecisionBranch(settings){
      this.createSnapshot();
      this.get("workflow").addDecisionBranch(settings);
      this.rerender();
    },
    setNodeTransitions(transition){
      var currentNode= this.get("currentNode");
      if(transition.errorNode && transition.errorNode.isNew){
        this.get("workflow").addKillNode(currentNode,transition.errorNode);
        this.get("workflow.killNodes").push(transition.errorNode);
      }else {
        this.set('currentNode.errorNode', transition.errorNode);
      }
      currentNode.transitions.forEach((trans)=>{
        if(transition.okToNode){
          if(trans.targetNode.id !== transition.okToNode.id){
            trans.targetNode = transition.okToNode;
            this.showUndo('transition');
          }          
        }
      }, this);
    },
    submitWorkflow(){
      this.set('dryrun', false);
      this.openJobConfig();
    },
    saveWorkflow(){
      this.set('dryrun', false);
      this.openSaveWorkflow();
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
    downloadWorkflowXml(){
      this.get('workflowContext').clearErrors();
      var workflowGenerator=WorkflowGenerator.create({workflow:this.get("workflow"),
      workflowContext:this.get('workflowContext')});
      var workflowXml=workflowGenerator.process();
      if(this.get('workflowContext').hasErrors()){
        this.set('errors',this.get('workflowContext').getErrors());
      }else{
        this.workflowXmlDownload(workflowXml);
      }
    },
    closeWorkflowSubmitConfigs(){
      this.set("showingWorkflowConfigProps",false);
      this.set("showingSaveWorkflow",false);
    },
    closeSaveWorkflow(){
      this.set("showingSaveWorkflow",false);
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
    closeWorkFlowParam(){
      this.set("showParameterSettings", false);
    },
    saveWorkFlowParam(){
      this.set('workflow.parameters', Ember.copy(this.get('parameters')));
      this.set("showParameterSettings", false);
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
    resetLayout() {
      this.flowRenderer.resetLayout();
    },
    closeActionEditor (isSaved){
      this.send("hideNotification");
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
    },
    saveDraft(){
      this.persistWorkInProgress();
    },

    undoDelete () {
      var workflowImporter = WorkflowJsonImporter.create({});
      var workflow = workflowImporter.importWorkflow(this.get('workflowSnapshot'));
      this.resetDesigner();
      this.set("workflow", workflow);
      this.rerender();
      this.doValidation();
      this.set('undoAvailable', false);
    },

    registerAddBranchAction(component){
      this.set("addBranchListener",component);
    },
    dryRunWorkflow(){
      this.set('dryrun', true);
      this.openJobConfig();
    }
  }
});
