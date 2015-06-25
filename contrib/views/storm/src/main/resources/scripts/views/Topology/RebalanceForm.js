/**
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

define(['utils/LangSupport',
  'utils/Globals',
  'hbs!tmpl/topology/rebalanceForm',
  'models/Cluster',
  'backbone.forms'
  ], function (localization, Globals, tmpl, clusterModel) {
  'use strict';

  var RebalanceForm = Backbone.Form.extend({

    template: tmpl,

    initialize: function (options) {
      this.showSpout = false,
      this.showBolt = false,
      this.spoutData = [],
      this.boltData = [];
      this.spoutCollection = options.spoutCollection;
      this.boltCollection = options.boltCollection;
      this.model = options.model;
      this.schemaObj = this.generateSchema();
      this.templateData = {
        "spoutFlag": this.showSpout,
        "boltFlag": this.showBolt,
        "spoutData": this.spoutData,
        "boltData": this.boltData,
        "showParallelism": (this.showSpout && this.showBolt) ? true : false
      },
      Backbone.Form.prototype.initialize.call(this, options);
    },

    generateSchema: function(){
      var that = this;
      var freeSlots = this.getClusterSummary(new clusterModel());
      freeSlots += this.model.get('workers');
      var obj = {
        workers: {
          type: 'RangeSlider',
          title: localization.tt('lbl.workers'),
          options: {"min":0,"max":freeSlots}
        }
      };

      if(that.spoutCollection.length){
        _.each(that.spoutCollection.models, function(model){
          if(model.has('spoutId')) {
            that.showSpout = true;
            var name = model.get('spoutId');
            obj[name] = {
              type: 'Number',
              title: name
            };
            that.spoutData.push(name);
            that.model.set(name,model.get('executors'));
          }
        });
      }
      if(that.boltCollection.length){
        _.each(that.boltCollection.models, function(model){
          if(model.has('boltId')) {
            var name = model.get('boltId');
            that.showBolt = true;
            obj[name] = {
              type: 'Number',
              title: name
            };
            that.boltData.push(name);
            that.model.set(name,model.get('executors'));
          }
        });
      }
      obj.waitTime = {
        type: 'Number',
        title: localization.tt('lbl.waitTime')+'*',
        validators: ['required']
      }
      return obj;
    },

    getClusterSummary: function(model){
      var freeSlots = 0;
      model.fetch({
        async: false,
        success: function(model, response, options) {
          if (model) {
            freeSlots = model.get('slotsFree');
          }
        }
      });
      return freeSlots;
    },

    schema: function () {
      return this.schemaObj;
    },

    render: function(options){
      Backbone.Form.prototype.render.call(this,options);
    },

    getData: function () {
      return this.getValue();
    },

    close: function () {
      console.log('Closing form view');
    }
  });

  return RebalanceForm;
});