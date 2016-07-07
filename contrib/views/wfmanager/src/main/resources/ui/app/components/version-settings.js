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

export default Ember.Component.extend({
  initialize : function(){
    this.set('currentWorkflowVersion', this.get('schemaVersions').getCurrentWorkflowVersion());
    this.set('workflowSchemaVersions', this.get('schemaVersions').getWorkflowVersions());
    this.get('schemaVersions').createCopy();
    this.set('actionSchemaVersions', Constants.actions);
    var actionVersions = Ember.A([]);
    Object.keys(this.get('actionSchemaVersions')).forEach((key)=>{
      var action = this.get('actionSchemaVersions')[key];
      if(action.supportsSchema){
        actionVersions.push({name:action.name, supporedVersions :this.get('schemaVersions').getActionVersions(action.name),selectedVersion:this.get('schemaVersions').getActionVersion(action.name)});
      }
    });
    this.set('actionVersions',actionVersions);
  }.on('init'),
  WorkflowVersionObserver : Ember.observer('currentWorkflowVersion',function(){
    this.get('schemaVersions').setCurrentWorkflowVersion(this.get('currentWorkflowVersion'));
  }),
  rendered : function(){
    this.$('#version-settings-dialog').modal({
      backdrop: 'static',
      keyboard: false
    });
    this.$('#version-settings-dialog').modal('show');
    this.$('#version-settings-dialog').modal().on('hidden.bs.modal', function() {
      this.sendAction('showVersionSettings', false);
    }.bind(this));
  }.on('didInsertElement'),
  actions : {
    versionChanged : function(actionName, version){
      this.get('schemaVersions').setActionVersion(actionName, version);
    },
    save (){
      this.$('#version-settings-dialog').modal('hide');
    },
    cancel (){
      this.get('schemaVersions').rollBack();
      this.$('#version-settings-dialog').modal('hide');
    }
  }
});
