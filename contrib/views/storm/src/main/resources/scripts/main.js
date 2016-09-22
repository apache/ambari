/**
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at
*
     http://www.apache.org/licenses/LICENSE-2.0
*
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/

require.config({
  deps: [],
  waitSeconds: 30,
  shim: {
    backbone: {
      deps: ['underscore', 'jquery'],
      exports: 'Backbone'
    },
    react: {
      exports: 'React'
    },
    bootstrap: {
      deps: ['jquery'],
      exports: 'jquery'
    },
    'bootstrap-switch': {
      deps: ['bootstrap']
    },
    'bootstrap-slider': {
      deps: ['bootstrap']
    },
    'bootstrap-notify': {
      deps: ['bootstrap']
    },
    underscore: {
      exports: '_'
    },
    JSXTransformer: {
        exports: "JSXTransformer"
    },
    'd3.tip': {
      deps: ['d3']
    },
    'dagreD3':{
      deps: ['d3'],
      exports: 'dagreD3'
    },
    'x-editable': {
      deps: ['bootstrap']
    }
  },
  paths: {
    'jquery': '../libs/jQuery/js/jquery-2.2.3.min',
    'underscore': '../libs/Underscore/js/Underscore',
    'backbone': '../libs/Backbone/js/Backbone',
    'backbone.paginator': '../libs/Backbone-Paginator/js/backbone-paginator.min',
    'bootstrap': '../libs/Bootstrap/js/bootstrap.min',
    'bootstrap-switch': '../libs/Bootstrap/js/bootstrap-switch.min',
    'bootstrap-slider': '../libs/Bootstrap/js/bootstrap-slider.min',
    'bootstrap-notify': '../libs/Bootstrap/js/bootstrap-notify.min',
    'bootbox': '../libs/bootbox/js/bootbox.min',
    'd3': '../libs/d3/js/d3.min',
    'd3.tip': '../libs/d3/js/d3-tip.min',
    'text': '../libs/require-text/js/text',
    'react':'../libs/react/js/react-with-addons',
    'react-dom': '../libs/react/js/react-dom',
    'JSXTransformer': '../libs/jsx/JSXTransformer',
    'jsx': "../libs/jsx/jsx",
    'x-editable':'../libs/Bootstrap/js/bootstrap-editable.min',
    'dagreD3': '../libs/dagre-d3/dagre-d3.min'
  },
  jsx: {
    fileExtension: '.jsx',
  }
});

require([
  "jquery",
  "backbone",
  "utils/Overrides",
  "router/Router"
  ], function($, Backbone, Overrides, Router) {
      window.App = {};

      App.Container = document.getElementById('container');
      App.Footer = document.getElementById('footer');

      App.appRouter = new Router();
      Backbone.history.start();
});