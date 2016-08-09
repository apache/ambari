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

export default Ember.Component.extend(Ember.Evented, {
  credentialsList : Ember.A([]),
  credentialsInfo : {},
  childComponents : new Map(),
  initialize : function(){
    this.get('credentialsList').clear();
    this.get('childComponents').clear();
    this.set('credentialsList', Ember.copy(this.get('workflowCredentials')));
  }.on('init'),
  rendered : function(){
    this.$('#workflow_credentials_dialog').modal({
      backdrop: 'static',
      keyboard: false
    });
    this.$('#workflow_credentials_dialog').modal('show');
    this.$('#workflow_credentials_dialog').modal().on('hidden.bs.modal', function() {
      this.sendAction('showCredentials', false);
    }.bind(this));
  }.on('didInsertElement'),
  processMultivaluedComponents(){
    this.get('childComponents').forEach((childComponent)=>{
      if(childComponent.get('multivalued')){
        childComponent.trigger('bindInputPlaceholder');
      }
    });
  },
  validateChildrenComponents(){
    var validationPromises = [];
    var deferred = Ember.RSVP.defer();
    if(this.get('childComponents').size === 0){
      deferred.resolve(true);
    }else{
      this.get('childComponents').forEach((childComponent)=>{
        if(!childComponent.validations){
          return;
        }
        var validationDeferred = Ember.RSVP.defer();
        childComponent.validate().then(()=>{
          validationDeferred.resolve();
        }).catch((e)=>{
          validationDeferred.reject(e);
        });
        validationPromises.push(validationDeferred.promise);
      });
      Ember.RSVP.Promise.all(validationPromises).then(function(){
        deferred.resolve(true);
      }).catch(function(e){
        deferred.reject(e);
      });
    }
    return deferred;
  },
  actions : {
    register(component, context){
      this.get('childComponents').set(component, context);
    },
    addCredentials (credentialsInfo){
      this.get('credentialsList').pushObject(credentialsInfo);
    },
    deleteCredentials(name){
      var credentials = this.get('credentialsList').findBy('name', name);
      this.get('credentialsList').removeObject(credentials);
    },
    saveCredentials (){
      var isFormValid = this.validateChildrenComponents();
      isFormValid.promise.then(function(){
        this.processMultivaluedComponents();
        this.set('workflowCredentials', Ember.copy(this.get('credentialsList')));
        this.$('#workflow_credentials_dialog').modal('hide');
      }.bind(this)).catch(function (e) {
      });
    }
  }
});
