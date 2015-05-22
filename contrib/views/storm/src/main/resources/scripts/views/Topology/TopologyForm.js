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
  'hbs!tmpl/topology/topologyForm',
  'backbone.forms'
  ], function (localization, Globals, tmpl) {
  'use strict';

  // var tmpl = ;
  var TopologyForm = Backbone.Form.extend({

    template: tmpl,

    initialize: function (options) {
      Backbone.Form.prototype.initialize.call(this, options);
      this.bindEvents();
    },

    bindEvents: function () {
      console.log('Binding Events Here !!');
    },

    schema: function () {
      return {
        name: {
          type: 'Text',
          title: localization.tt('lbl.name')+'*',
          editorClass : 'form-control',
          validators: ['required']
        },
        jar: {
          type: 'Fileupload',
          title: localization.tt('lbl.jar')+'*',
          validators: ['required']
        },
        // nimbusHostname: {
        //   type: 'Select',
        //   title: localization.tt('lbl.nimbusHostname')+'*',
        //   options: [{
        //     val: '',
        //     label: '--'
        //   },
        //   {
        //     val: '1',
        //     label: 'Hostname 1'
        //   }, {
        //     val: '2',
        //     label: 'Hostname 2'
        //   }, {
        //     val: '3',
        //     label: 'Hostname 3'
        //   }],
        //   editorClass : 'form-control',
        //   validators: ['required']
        // },
        topologyClass: {
          type: 'Text',
          title: localization.tt('lbl.topologyClass')+'*',
         editorClass : 'form-control',
          validators: ['required']
        },
        arguments: {
          type: 'Text',
          title: localization.tt('lbl.arguments'),
          editorClass : 'form-control',
          // validators: ['required']
        }
      };
    },

    // render: function (options) {
    //   Backbone.Form.prototype.render.call(this, options);
    // },

    getData: function () {
      return this.getValue();
    },

    close: function () {
      console.log('Closing form view');
    }
  });

  return TopologyForm;
});