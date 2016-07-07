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
import EmberValidations,{ validator } from 'ember-validations';
import {FindNodeMixin} from '../domain/findnode-mixin';

export default Ember.Component.extend(FindNodeMixin, EmberValidations, {
  selectedKillNode : '',
  initialize : function(){
    this.set('descendantNodes',this.getDesendantNodes(this.get('currentNode')));
    this.set('okToNode', this.getOKToNode(this.get('currentNode')));
    this.sendAction('register','transition', this);
    if(Ember.isBlank(this.get('transition.errorNode.name'))){
      this.set('transition.errorNode', this.get('killNodes').objectAt(0));
    }
  }.on('init'),
  //Work-around : Issue in ember-validations framework
  errorNode : Ember.computed.alias('transition.errorNode'),
  validations : {
    'errorNode.name': {
      inline : validator(function() {
        if(!this.get('transition.errorNode.name') || this.get('transition.errorNode.name') === ""){
          return "You need to provide an error-to transition";
        }
      })
    }
  },
  actions : {
    onSelectChange (value){
      this.set('selectedKillNode', value);
      if(this.get('selectedKillNode') === 'createNew'){
        this.set('transition.errorNode.name', "");
        this.set('transition.errorNode.message', "");
        this.set('transition.errorNode.isNew', true);
      }else if(value === ""){
        this.set('transition.errorNode', null);
      }else{
        this.set('transition.errorNode.isNew',false);
        var node = this.get('descendantNodes').findBy('name',value);
        if(node){
          this.set('transition.errorNode', node);
        }else{
          node = this.get('killNodes').findBy('name',value);
          this.set('transition.errorNode', node);
        }
      }
    },
    okNodeHandler (value){

    }
  }
});
