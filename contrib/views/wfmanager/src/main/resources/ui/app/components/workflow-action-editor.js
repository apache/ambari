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
import {SlaInfo} from '../domain/sla-info';

export default Ember.Component.extend( Ember.Evented,{
  actionIcons : {
    "hive": "server",
    "hive2": "server",
    "pig": "product-hunt",
    "sqoop": "database",
    "hdfs": "copy",
    "java": "code",
    "shell": "terminal",
    "distcp": "clone",
    "map-reduce": "cubes",
    "spark": "star",
    "ssh": "terminal",
    "sub-workflow":"share-alt-square",
    "stream": "exchange",
    "email": "envelope",
    "fs":"folder-o"
  },
  clonedActionModel : {},
  showingFileBrowser : false,
  childComponents : new Map(),
  isActionNode : Ember.computed('nodeType',function(){
    if(this.get('nodeType') === 'action'){
      return true;
    }else{
      return false;
    }
  }),
  type : Ember.computed('nodeType','actionType',function(){
    if(this.get('nodeType') === 'action'){
      return this.get('actionType');
    }else if(this.get('nodeType') === 'decision' || this.get('nodeType') === 'kill'){
      return  this.get('nodeType');
    }
  }),
  icon : Ember.computed('actionIcons', 'actionType',function(){
    return this.get('actionIcons')[this.get('actionType')];
  }),
  saveClicked : false,
  containsUnsupportedProperties : Ember.computed('actionModel.unsupportedProperties', function(){
    return this.get('actionModel.unsupportedProperties') ? !Ember.isEmpty(Object.keys(this.get('actionModel.unsupportedProperties'))) : false;
  }),
  unsupportedPropertiesXml : Ember.computed('actionModel.unsupportedProperties', function(){
    if(this.get('containsUnsupportedProperties')){
      var x2js = new X2JS();
      return vkbeautify.xml(x2js.json2xml_str(this.get('actionModel.unsupportedProperties')));
    }
  }),
  fileBrowser : Ember.inject.service('file-browser'),
  onDestroy : function(){
    this.set('transition',{});
    this.get('childComponents').clear();
  }.on('willDestroyElement'),
  setUp : function () {
    var errorNode = Ember.Object.extend(Ember.Copyable).create({
      name : "",
      isNew : false,
      message : ""
    });
    var errorNodeOfCurrentNode = this.get('currentNode').get('errorNode');
    if(errorNodeOfCurrentNode){
      errorNode.set('name', errorNodeOfCurrentNode.get('name'));
      errorNode.set('message', errorNodeOfCurrentNode.get('killMessage'));
    }
    var transition = Ember.Object.extend(Ember.Copyable).create({
      errorNode : errorNode
    });
    this.set('transition',transition);
    if (Ember.isBlank(this.get("actionModel.jobTracker"))){
      this.set('actionModel.jobTracker',Constants.rmDefaultValue);
    }
    if (Ember.isBlank(this.get("actionModel.nameNode"))){
      this.set('actionModel.nameNode','${nameNode}');
    }
    if(this.get('nodeType') === 'action' && this.get('actionModel.slaInfo') === undefined){
      this.set('actionModel.slaInfo', SlaInfo.create({}));
    }
  }.on('init'),
  initialize : function(){
    this.$('#action_properties_dialog').modal({
      backdrop: 'static',
      keyboard: false
    });
    this.$('#action_properties_dialog').modal('show');
    this.$('#action_properties_dialog').modal().on('hidden.bs.modal', function() {
      this.sendAction('closeActionEditor', this.get('saveClicked'));
    }.bind(this));
    this.get('fileBrowser').on('fileBrowserOpened',function(context){
      this.get('fileBrowser').setContext(context);
    }.bind(this));
    this.on('fileSelected',function(fileName){
      this.set(this.get('filePathModel'), fileName);
    }.bind(this));
  }.on('didInsertElement'),
  observeError :function(){
    if(this.$('#collapseOne label.text-danger').length > 0 && !this.$('#collapseOne').hasClass("in")){
      this.$('#collapseOne').collapse('show');
    }
  }.on('didUpdate'),
  validateChildrenComponents(){
    var isChildComponentsValid = true;
    this.get('childComponents').forEach((context)=>{
      if(context.get('validations') && context.get('validations.isInvalid')){
        isChildComponentsValid =  false;
        context.set('showErrorMessage', true);
      }
    }.bind(this));
    return isChildComponentsValid;
  },
  processMultivaluedComponents(){
    this.get('childComponents').forEach((childComponent)=>{
      if(childComponent.get('multivalued')){
        childComponent.trigger('bindInputPlaceholder');
      }
    });
  },
  processStaticProps(){
    this.get('childComponents').forEach((childComponent)=>{
      if(childComponent.get('hasStaticProps')){
        childComponent.get('staticProps').forEach((property)=>{
          this.get(property.belongsTo).push({name:property.name,value:property.value});
        });
      }
    });
  },
  actions : {
    closeEditor (){
      this.sendAction('close');
    },
    save () {
      var isChildComponentsValid = this.validateChildrenComponents();
      if(this.get('validations.isInvalid') || !isChildComponentsValid) {
        this.set('showErrorMessage', true);
        return;
      }
      this.processMultivaluedComponents();
      this.processStaticProps();
      this.$('#action_properties_dialog').modal('hide');
      this.sendAction('setNodeTransitions', this.get('transition'));
      this.set('saveClicked', true);
    },
    openFileBrowser(model, context){
      if(!context){
        context = this;
      }
      this.get('fileBrowser').trigger('fileBrowserOpened',context);
      this.set('filePathModel', model);
      this.set('showingFileBrowser',true);
    },
    closeFileBrowser(){
      this.get('fileBrowser').getContext().trigger('fileSelected', this.get('filePath'));
      this.set("showingFileBrowser",false);
    },
    registerChild (name, context){
      this.get('childComponents').set(name, context);
    }
  }
});
