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
  //optionsForFs={"mkdir":{"value":true}, "delete":{"value":false}, "chmod":{"value":false}, "move":{"value":false}, "touch":{"value":false}, "chgrp":{"value":false}},
  mkdirORdeleteORtouchz: true,
  mkdir:1,
  delete:0,
  touchz:0,
  chmod:0,
  move:0,
  chgrp:0,

  multivalued: true,
  prepareType : 'mkdir',
  fileBrowser : Ember.inject.service('file-browser'),
  initialize : function(){
    this.on('fileSelected',function(fileName){
      if(!Ember.isBlank(this.get('filePathModel'))){
        var prepareObj = this.get('fsOps').objectAt(this.get('filePathModel'));
        Ember.set(prepareObj,"path", fileName);
        this.get('fsOps').replace(this.get('filePathModel'), 1, prepareObj);
      }else{
        this.set('path', fileName);
      }
    }.bind(this));
    this.on('bindInputPlaceholder',function () {
      this.set('addUnboundValue', true);
    }.bind(this));
    this.sendAction('register', 'fsOps', this);
  }.on('init'),
  bindInputPlaceholder : function(){
    if(this.get('addUnboundValue')){
	let value = this.get("prepareType");
	  if(value === "chgrp" && this.get('path') && this.get('group')){
	  this.addPrepare();
	  } else if(value === "move" && this.get('source') && this.get('target')){
	  this.addPrepare();
	  } else if(value === "chmod" && this.get('path') && this.get('permissions')){
	  this.addPrepare();
	  } else if(value === "mkdir" || value === "delete" || value === "touchz"  && this.get('path')){
	  this.addPrepare();
      }

    }
  }.on('willDestroyElement'),
  addPrepare : function (){
	let value = this.get("prepareType");
	  if(value === "chgrp"){
	      this.get('fsOps').pushObject({settings:{path:this.get('path'),group:this.get('group')}, type:value});
	  } else if(value === "move"){
	      this.get('fsOps').pushObject({settings:{source:this.get('source'),target:this.get('target')}, type:value});
	  } else if(value === "chmod"){
	      this.get('fsOps').pushObject({settings:{path:this.get('path'),permissions:this.get('permissions')}, type:value});
	  } else if(value === "mkdir" || value === "delete" || value === "touchz"){
	      this.get('fsOps').pushObject({settings:{path:this.get('path')}, type:value});
      }
      console.log("---------prepare-----------");
      console.log(this.get('fsOps'));
      this.set('prepareType', "mkdir");
      this.set('preparePath', "");
  },
  toggleAllFields : function(){
      this.set("mkdir", 0);
      this.set("delete", 0);
      this.set("chmod", 0);
      this.set("touchz", 0);
      this.set("chmod", 0);
      this.set("move", 0);
      this.set("chgrp", 0);
  },
  actions : {
    onPrepareTypeChange (value) {
      this.set('prepareType', value);
      this.toggleAllFields();
      this.set(value, 1);
      if(value === "mkdir" || value === "delete" || value === "touchz"){
	this.set("mkdirORdeleteORtouchz", true);
      } else {
	this.set("mkdirORdeleteORtouchz", false);
      }
    },
    addPrepare () {
      this.addPrepare();
    },
    deletePrepare(index){
      this.get('fsOps').removeAt(index);
    },
    openFileBrowser(model){
      this.set('filePathModel', model);
      this.sendAction("openFileBrowser", model, this);
    }
  }
});
