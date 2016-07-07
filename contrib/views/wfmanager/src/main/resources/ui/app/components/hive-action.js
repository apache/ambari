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

export default Ember.Component.extend(EmberValidations,{
  hiveOptionObserver : Ember.observer('isScript',function(){
    if(this.get('isScript')){
      this.set("actionModel.query", undefined);
    }else{
      this.set("actionModel.script",  undefined);
    }
  }),
  setUp : function(){
    if(this.get('actionModel.script')){
      this.set('isScript', true);
    }else if(this.get('actionModel.query')){
      this.set('isScript', false);
    }else{
      this.set('isScript', true);
    }
    if(this.get('actionModel.jobXml') === undefined){
      this.set("actionModel.jobXml", Ember.A([]));
    }
    if(this.get('actionModel.args') === undefined){
      this.set("actionModel.args", Ember.A([]));
    }
    if(this.get('actionModel.params') === undefined){
      this.set("actionModel.params", Ember.A([]));
    }
    if(this.get('actionModel.files') === undefined){
      this.set("actionModel.files", Ember.A([]));
    }
    if(this.get('actionModel.archives') === undefined){
      this.set("actionModel.archives", Ember.A([]));
    }
    if(this.get('actionModel.prepare') === undefined){
      this.set("actionModel.prepare", Ember.A([]));
    }
    if(this.get('actionModel.configuration') === undefined){
      this.set("actionModel.configuration",{});
      this.set("actionModel.configuration.property", Ember.A([]));
    }
  }.on('init'),
  initialize : function(){
    this.sendAction('register','hiveAction', this);
    this.on('fileSelected',function(fileName){
      this.set(this.get('filePathModel'), fileName);
    }.bind(this));
  }.on('didInsertElement'),
  observeError :function(){
    if(this.$('#collapseOne label.text-danger').length > 0 && !this.$('#collapseOne').hasClass("in")){
      this.$('#collapseOne').collapse('show');
    }
  }.on('didUpdate'),
  validations : {
      'actionModel.script': {
        presence: {
          'if':'isScript',
          'message' : 'You need to provide a value for Script'
        }
     },
     'actionModel.query': {
       presence: {
         unless :'isScript',
         'message' : 'You need to provide a value for Query'
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
    onHiveOptionChange(value){
      if(value === "script"){
        this.set('isScript',true);
      }else{
        this.set('isScript',false);
      }
    }
  }
});
