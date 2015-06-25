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

require.config({
  /* starting point for application */
  hbs: {
    disableI18n: true, // This disables the i18n helper and doesn't require the json i18n files (e.g. en_us.json)
    helperPathCallback: // Callback to determine the path to look for helpers
      function(name) { // ('/template/helpers/'+name by default)
        return "modules/Helpers";
      },
    templateExtension: "html", // Set the extension automatically appended to templates
    compileOptions: {} // options object which is passed to Handlebars compiler
  },

  /**
   * Requested as soon as the loader has processed the configuration. It does
   * not block any other require() calls from starting their requests for
   * modules, it is just a way to specify some modules to load asynchronously
   * as part of a config block.
   * @type {Array} An array of dependencies to load.
   */
  deps: ['marionette', 'globalize', 'utils/LangSupport'],

  /**
   * The number of seconds to wait before giving up on loading a script.
   * @default 7 seconds
   * @type {Number}
   */
  waitSeconds: 30,


  shim: {
    backbone: {
      deps: ['underscore', 'jquery'],
      exports: 'Backbone'
    },
    bootstrap: {
      deps: ['jquery'],
    },
    underscore: {
      exports: '_'
    },
    marionette: {
      deps: ['backbone']
    },
    backgrid: {
      deps: ['backbone']
    },
    'backbone.forms': {
      deps: ['backbone']
    },
    'backbone-forms.templates': {
      deps: ['backbone-forms.list', 'backbone.forms']
    },
    'bootstrap.filestyle': {
      deps: ['jquery', 'bootstrap']
    },
    'bootstrap.notify': {
      deps: ['jquery']
    },
    'jquery-ui': {
      deps: ['jquery'],
    },
    'jquery.ui.widget': {
      deps: ['jquery']
    },
    'jquery-ui-slider': {
      deps: ['jquery', 'jquery-ui', 'jquery.ui.widget']
    },
    globalize: {
      exports: 'Globalize'
    },
    bootbox: {
      deps: ['jquery']
    },
    arbor: {
      deps: ['jquery']
    },
    'arbor-tween':{
      deps: ['jquery', 'arbor']
    },
    'arbor-graphics':{
      deps: ['jquery', 'arbor']
    },
    hbs: {
      deps: ['underscore', 'handlebars']
    }
  },

  paths: {
    'jquery': '../libs/bower/jquery/js/jquery',
    'backbone': '../libs/bower/backbone/js/backbone',
    'underscore': '../libs/bower/underscore/js/underscore',
    'marionette': '../libs/bower/backbone.marionette/js/backbone.marionette',
    'backbone.wreqr': '../libs/bower/backbone.wreqr/js/backbone.wreqr',
    'backbone.babysitter': '../libs/bower/backbone.babysitter/js/backbone.babysitter',
    'backbone.forms': '../libs/bower/backbone-forms/js/backbone-forms',
    'backbone-forms.list': '../libs/bower/backbone-forms/js/list',
    'bootstrap': '../libs/bower/bootstrap/js/bootstrap',
    'bootstrap.filestyle': '../libs/bower/bootstrap/js/bootstrap-filestyle.min',
    'bootstrap.notify': '../libs/bower/bootstrap/js/bootstrap-notify',
    'backgrid': '../libs/bower/backgrid/js/backgrid',
    'jquery-ui': '../libs/bower/jquery-ui/js/jquery-ui-1.10.3.custom',
    'jquery.ui.widget': '../libs/bower/jquery-ui/js/jquery.ui.widget.min',
    'jquery-ui-slider' : '../libs/bower/jquery-ui/js/jquery-ui-slider',
    'globalize': '../libs/bower/globalize/js/globalize',
    'gblMessages' : '../scripts/globalize',
    'bootbox': '../libs/bower/bootbox/js/bootbox',
    'arbor': '../libs/other/arbor',
    'arbor-tween': '../libs/other/arbor-tween',
    'arbor-graphics': '../libs/other/arbor-graphics',
    'handlebars': '../libs/bower/require-handlebars-plugin/js/handlebars',
    'i18nprecompile': '../libs/bower/require-handlebars-plugin/js/i18nprecompile',
    'json2': '../libs/bower/require-handlebars-plugin/js/json2',
    'hbs': '../libs/bower/require-handlebars-plugin/js/hbs',
    'tmpl': '../templates'
  },

  /**
   * If set to true, an error will be thrown if a script loads that does not
   * call define() or have a shim exports string value that can be checked.
   * To get timely, correct error triggers in IE, force a define/shim export.
   * @type {Boolean}
   */
  enforceDefine: false
});

define(["App",
  "router/Router",
  'jquery-ui',
  'jquery.ui.widget',
  'jquery-ui-slider',
  "utils/Overrides",
  "arbor",
  "arbor-tween",
  "arbor-graphics"
  ], function(App, Router) {
    'use strict';

  App.appRouter = new Router();
  App.start();
});
