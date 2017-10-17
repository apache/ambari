/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var App = require('app');
var arrayUtils = require('utils/array_utils');

App.WizardDownloadProductsController = Em.Controller.extend({

  name: 'wizardDownloadProductsController',

  selectedMpacks:
    [
      {
        "mpackName" : "HDP",
        "displayName": "Hortonworks Data Platform Core",
        "mpackVersion" : "3.0.0",
        "registryId"  : "1"
      },
      {
        "mpackName" : "EDW",
        "displayName": "Data Warehousing",
        "mpackVersion": "1.0.0",
        "registryId": "1"
      },
      {
        "mpackName" : "HDS",
        "displayName": "Data Science and Machine Learning",
        "mpackVersion" : "3.0.0.0",
        "registryId" : "1"
      }
    ],

  mpacks: [],

  addMpacks: function () {
    var self = this;
    this.get('selectedMpacks').forEach( function (mpack) {
      self.get('mpacks').pushObject(Em.Object.create({
        name: mpack.mpackName,
        displayName: mpack.displayName,
        version: mpack.mpackVersion,
        registryId:mpack.registryId,
        inProgress: true,
        failed: false,
        success: false
      }));
    });
  },

  registerMpacks: function () {
    var mpacks = this.get('mpacks');
    var self = this;
    mpacks.forEach(function (mpack) {
      self.downloadMpack(mpack);
    });
  },

  downloadMpack: function (mpack) {
    console.log("downloading mpacks");
    App.ajax.send({
      name:'mpack.download',
      sender: this,
      data: {
        name: mpack.name,
        version: mpack.version,
        registry: mpack.registryId
      },
      success: 'downloadMpackSuccess',
      error: 'downloadMpackError',
    });
  },

  downloadMpackSuccess: function (data, opt, params) {
    console.dir("Mpack " + params.name + " download completed with success code " + data.status);
    this.get('mpacks').findProperty('name', params.name).set('inProgress', false);
    this.get('mpacks').findProperty('name', params.name).set('success', true);
  },

  downloadMpackError: function (request, ajaxOptions, error, opt, params) {
    if(request.status == 409) {
      this.downloadMpackSuccess(request, opt, params);
    } else {
      console.dir("Mpack " + params.name + " download failed with error code " + request.status);
      this.get('mpacks').findProperty('name', params.name).set('inProgress', false);
      this.get('mpacks').findProperty('name', params.name).set('failed', true);
    }
  },

  retryDownload: function (event) {
    var mpack = event.context;
    mpack.set('inProgress', true);
    mpack.set('failed', false);
    this.downloadMpack(mpack);
  }

});