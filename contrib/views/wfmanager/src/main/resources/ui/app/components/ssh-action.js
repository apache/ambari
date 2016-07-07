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
import EmberValidations from 'ember-validations';

export default Ember.Component.extend(EmberValidations, {
  fileBrowser : Ember.inject.service('file-browser'),
  javaOptsObserver : Ember.observer('isSingle',function(){
    if(this.get('isSingle')){
      this.set("actionModel.arg", Ember.A([]));
    }else{
      this.set("actionModel.args", Ember.A([]));
    }
  }),
  setUp : function(){
    if(this.get('actionModel.args') === undefined){
      this.set("actionModel.args", Ember.A([]));
    }
    if(this.get('actionModel.arg') === undefined){
      this.set("actionModel.arg", Ember.A([]));
    }
    if(this.get('actionModel.arg') === undefined && !this.get('actionModel.args')){
      this.set("actionModel.arg", Ember.A([]));
      this.set('isSingle', false);
    }else if(this.get('actionModel.arg') === undefined && this.get('actionModel.args')){
      this.set('isSingle', true);
    }else{
      this.set('isSingle', false);
    }
  }.on('init'),
  initialize : function(){
    this.on('fileSelected',function(fileName){
      this.set(this.get('filePathModel'), fileName);
    }.bind(this));
    this.sendAction('register','javaAction', this);
    //this.set('clonedActionModel',Ember.copy(this.get('actionModel')));
  }.on('didInsertElement'),
  observeError :function(){
    if(this.$('#collapseOne label.text-danger').length > 0 && !this.$('#collapseOne').hasClass("in")){
      this.$('#collapseOne').collapse('show');
    }
  }.on('didUpdate'),
  validations : {
     'actionModel.host': {
       presence: {
         'message' : 'You need to provide a value for host',
       }
      },
     'actionModel.command': {
       presence: {
         'message' : 'You need to provide a value for command',
       }
      }
  },
  actions : {
    openFileBrowser(model, context){
      if(undefined === context){
        context = this;
      }
      this.set('filePathModel', model);
      this.sendAction('openFileBrowser', model, context);
    },
    register (name, context){
      this.sendAction('register',name , context);
    },
    onJavaOptChange(value){
      if(value === "single"){
        this.set('isSingle',true);
      }else{
        this.set('isSingle',false);
      }
    }
  }
});
