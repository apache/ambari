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
  childComponents : new Map(),
  initialize : function(){
    if(this.get('mode') === 'edit'){
      this.sendAction('register', this, this);
    }
    this.get('childComponents').clear();
    this.set('credentialType',Ember.A([]));
    this.get('credentialType').pushObject({value:'',displayName:'Select'});
    this.get('credentialType').pushObject({value:'hcat',displayName:'HCat'});
    this.get('credentialType').pushObject({value:'hive2',displayName:'Hive2'});
    this.get('credentialType').pushObject({value:'hbase',displayName:'HBase'});

    Ember.addObserver(this, 'credential.type', this, this.credentialTypeObserver);

    this.initializeCredentialDetails();

    if(this.get('mode') === 'create'){
      this.set("credential", {});
      this.set("credential.property", Ember.A([]));
    }
    if(this.get('credential.type') && this.get('credential.property')){
      this.set('staticProps', Ember.copy(this.get('credentialDetails').findBy('name',this.get('credential.type')).staticProps));
      var configProperties = this.get('credential.property');
      configProperties.forEach((property)=>{
        var existingStaticProp = this.get('staticProps').findBy('name',property.name);
        if (existingStaticProp) {
          Ember.set(existingStaticProp,'value', property.value);
          Ember.set(property,'static', true);
        }
      });
    }
  }.on('init'),
  rendered : function(){
    if(this.get('mode') === 'create'){
      this.$('.collapse').collapse('show');
    }else if(this.get('mode') === 'edit'){
      this.$('.collapse').collapse('hide');
    }
  }.on('didInsertElement'),
  initializeCredentialDetails : function(){
    this.set('credentialDetails', Ember.A([]));
    this.get('credentialDetails').pushObject({
      name:'hcat',
      staticProps:
      [{name:'hcat.metastore.principal',displayName:'Hcat Metastore principal', value:'', belongsTo:'credential.property'},
      {name:'hcat.metastore.uri',displayName:'Hcat Metastore uri', value:'', belongsTo:'credential.property'}]
    });
    this.get('credentialDetails').pushObject({
      name:'hive2',
      staticProps:
      [{name:'hive2.jdbc.url',displayName:'Hive2 Jdbc Url', value:'', belongsTo:'credential.property'},
      {name:'hive2.server.principal',displayName:'Hive2 Server principal', value:'', belongsTo:'credential.property'}]
    });
    this.get('credentialDetails').pushObject({
      name:'hbase',
      staticProps:
      [{name:'hadoop.security.authentication',displayName:'Hadoop security auth', value:'', belongsTo:'credential.property'},
      {name:'hbase.security.authentication',displayName:'Hbase security auth', value:'', belongsTo:'credential.property'},
      {name:'hbase.master.kerberos.principal',displayName:'Hbase Master kerberos principal', value:'', belongsTo:'credential.property'},
      {name:'hbase.regionserver.kerberos.principal',displayName:'Hbase regionserver kerberos principal', value:'', belongsTo:'credential.property'},
      {name:'hbase.zookeeper.quorum',displayName:'Hbase zookeeper quorum', value:'', belongsTo:'credential.property'},
      {name:'hadoop.rpc.protection',displayName:'Hadoop Rpc protection', value:'', belongsTo:'credential.property'},
      {name:'hbase.rpc.protection',displayName:'Hbase Rpc protection', value:'', belongsTo:'credential.property'}]
    });
  },
  credentialTypeObserver : function(){
    var credentialType = this.get('credential.type');
    if(!credentialType){
      return;
    }
    this.set('staticProps', Ember.copy(this.get('credentialDetails').findBy('name',credentialType).staticProps));
  },
  processMultivaluedComponents(){
    this.get('childComponents').forEach((childComponent)=>{
      if(childComponent.get('multivalued')){
        childComponent.trigger('bindInputPlaceholder');
      }
    });
  },
  resetForm : function(){
    this.set('credential', {});
    this.set('credential.property',Ember.A([]));
    this.get('staticProps').clear();
    this.initializeCredentialDetails();
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
  validations : {
    'credential.name': {
      presence: {
        'message' : 'Required',
      }
    },
    'credential.type': {
      presence: {
        'message' : 'Required',
      }
    }
  },
  actions : {
    register(component, context){
      if(this.get('mode') === 'edit'){
        this.sendAction('register', component, context);
      }
      this.get('childComponents').set(component, context);
    },
    add(){
      var isFormValid = this.validateChildrenComponents();
      isFormValid.promise.then(function(){
        this.validate().then(function(){
          var staticProps = this.get('staticProps');
          var index = 0;
          staticProps.forEach((property)=>{
            var existingStaticProp = this.get('credential.property').findBy('name',property.name);
            if (existingStaticProp) {
              Ember.set(existingStaticProp,'value',property.value);
              index++;
            } else {
              var propObj = {name : property.name, value:property.value, static:true};
              this.get('credential.property').insertAt(index++, propObj);
            }
          });
          this.processMultivaluedComponents();
          this.sendAction('add',this.get('credential'));
          this.resetForm();
        }.bind(this)).catch(function(e){
        }.bind(this));
      }.bind(this)).catch(function (e) {
      });

    },
    delete(name){
      this.sendAction('delete',name);
    },
    togglePanel (){
      this.$('.collapse').collapse('toggle');
      if(this.$('.collapse').hasClass('in')){
        this.set('isOpen', true);
      }else{
        this.set('isOpen', false);
      }
    }
  }

});
