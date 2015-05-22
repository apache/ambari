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
  'backbone.forms'
  ], function (localization, Globals) {
  'use strict';

  var RebalanceForm = Backbone.Form.extend({

    initialize: function (options) {
      this.spoutCollection = options.spoutCollection;
      this.boltCollection = options.boltCollection;
      Backbone.Form.prototype.initialize.call(this, options);
    },

    schema: function () {
      var that = this;
      var obj = {
        workers: {
          type: 'Number',
          title: localization.tt('lbl.workers')
        }
      };

      if(that.spoutCollection.length){
        _.each(that.spoutCollection.models, function(model){
          if(model.has('spoutId')) {
            obj[model.get('spoutId')] = {
              type: 'Number',
              title: model.get('spoutId')
            };
          }
        });
      }
      if(that.boltCollection.length){
        _.each(that.boltCollection.models, function(model){
          if(model.has('boltId')) {
            obj[model.get('boltId')] = {
              type: 'Number',
              title: model.get('boltId')
            };
          }
        });
      }
      obj.waitTime = {
        type: 'Number',
        title: localization.tt('lbl.waitTime')+'*',
        validators: ['required']
      }
      return obj
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