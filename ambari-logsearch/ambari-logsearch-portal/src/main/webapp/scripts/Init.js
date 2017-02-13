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

    //    baseUrl: "/scripts",

    urlArgs: 'ver=build.version',

    /* starting point for application */
    deps: ['backbone.marionette', 'bootstrap', 'Main', 'globalize', 'utils/LangSupport'],

    waitSeconds: 30,

    shim: {
        'backbone': {
            deps: ['underscore', 'jquery'],
            exports: 'Backbone'
        },
        'underscore': {
            exports: '_'
        },
        'backbone.marionette': {
            deps: ['backbone']
        },
        bootstrap: {
            deps: ['jquery'],
            exports: 'jquery'
        },
        'backgrid': {
            deps: ['backbone', 'jquery'],
            exports: 'Backgrid'
        },
        'backgrid-paginator': {
            deps: ['backbone', 'backgrid']
        },
        'backgrid-filter': {
            deps: ['backbone', 'backgrid']
        },
        'backgrid-select-all': {
            deps: ['backbone', 'backgrid']
        },
        'backgrid-filter': {
            deps: ['backbone', 'backgrid']
        },
        'backgrid-columnmanager': {
            deps: ['backbone', 'backgrid'],
        },
        'backgrid-sizeable': {
            deps: ['backbone', 'backgrid'],
        },
        'backgrid-orderable': {
            deps: ['backbone', 'backgrid'],
        },
        /* 'backbone-forms.templates': {
            deps: ['backbone-forms.list', 'backbone-forms']
        },
        'backbone-forms.XAOverrides': {
            deps: ['backbone-forms']
        },*/
        'Backbone.BootstrapModal': {
            deps: ['jquery', 'underscore', 'backbone']
        },
        'bootstrap-editable': {
            deps: ['bootstrap']
        },
        /* 'jquery-toggles': {
            deps: ['jquery']
        },
        'jquery.cookie': {
            deps: ['jquery']
        },
        'tag-it': {
            deps: ['jquery', 'jquery-ui']
        },*/
        'jquery-ui': {
            deps: ['jquery']
        },
        'globalize': {
            exports: 'Globalize'
        },
        /*  'bootstrap-datepicker': {
             deps: ['bootstrap']
         },*/
        'bootstrap-daterangepicker': {
            deps: ['bootstrap', 'jquery', 'moment'],
            exports: 'daterangepicker'
        },
        /* 'bootstrap-notify': {
            deps: ['jquery', 'bootstrap'],
        },*/
        moment: {
            deps: [],
            exports: 'moment'
        },
        'moment-tz': {
            deps: ['moment']
        },
        'localstorage': {
            deps: ['backbone', 'underscore', 'jquery']
        },
        'backbone-fetch-cache': {
            deps: ['backbone', 'underscore', 'jquery']
        },
        'visualsearch': {
            deps: ['jquery', 'jquery-ui', 'backbone', 'underscore']
        },
        'select2': {
            deps: ['jquery', 'bootstrap']
        },
        'bootbox': {
            deps: ['jquery']
        },
        'd3.tip': {
            deps: ['d3']
        },
        'nv': {
            deps: ['d3'],
            exports: 'nv'
        },
        noty: {
            deps: ['jquery'],
        },
        WorldMapGenerator: {
            deps: ['jquery']
        },
        jstimezonedetect: {
            exports: 'jstz'
        },
        sparkline: {
            deps: ['jquery']
        },
        gridster: {
            deps: ['jquery']
        },
        dashboard: {
            deps: ['jquery', 'jquery-ui', 'gridster']
        },
        tour : {
           exports : 'tour'
        }

    },

    paths: {

        jquery: '../libs/bower/jquery/js/jquery.min',
        'jquery-ui': '../libs/other/jquery-ui/js/jquery-ui-1.11.4.min',
        backbone: '../libs/bower/backbone-amd/js/backbone',
        underscore: '../libs/bower/underscore-amd/js/underscore',

        /* alias all marionette libs */
        'backbone.marionette': '../libs/bower/backbone.marionette/js/backbone.marionette',
        'backbone.wreqr': '../libs/bower/backbone.wreqr/js/backbone.wreqr',
        'backbone.babysitter': '../libs/bower/backbone.babysitter/js/backbone.babysitter',

        /* alias the bootstrap js lib */
        bootstrap: '../libs/bower/bootstrap/bootstrap',

        /* BackGrid for Tables */
        'backgrid': '../libs/bower/backgrid/js/backgrid.min',
        'backgrid-filter': '../libs/bower/backgrid-filter/js/backgrid-filter.min',
        'backgrid-select-all': '../libs/bower/backgrid-select-all/js/backgrid-select-all.min',
        'backbone.paginator': '../libs/bower/backbone.paginator/js/backbone.paginator.min',
        'backgrid-paginator': '../libs/bower/backgrid-paginator/js/backgrid-paginator.min',
        'backgrid-columnmanager': '../libs/bower/backgrid-columnmanager/Backgrid.ColumnManager',
        'backgrid-sizeable': '../libs/bower/backgrid-sizeable/js/backgrid-sizeable-columns',
        'backgrid-orderable': '../libs/bower/backgrid-sizeable/backgrid-orderable-columns',

        'backbone-pageable': '../libs/bower/backbone-pageable/js/backbone-pageable',
        'localstorage': '../libs/bower/backbone.localstorage/backbone.localStorage',
        /*'backbone-forms': '../libs/bower/backbone-forms/js/backbone-forms',
        'backbone-forms.list': '../libs/bower/backbone-forms/js/list',
        'backbone-forms.templates': '../libs/bower/backbone-forms/js/bootstrap',
        'backbone-forms.XAOverrides': '../libs/fsOverrides/BBFOverrides',*/

        /*'Backbone.BootstrapModal': '../libs/bower/backbone.bootstrap-modal/js/backbone.bootstrap-modal',
        'bootstrap-editable': '../libs/bower/x-editable/js/bootstrap-editable',
        'bootstrap-datepicker': '../libs/other/datepicker/js/bootstrap-datepicker',
        'bootstrap-notify': '../libs/bower/bootstrap-notify/js/bootstrap-notify',*/
        'bootstrap-daterangepicker': '../libs/custom/daterangepicker/js/daterangepicker',


        /*'jquery.cookie': '../libs/other/jquery-cookie/js/jquery.cookie',
        'jquery-toggles': '../libs/bower/jquery-toggles/js/toggles.min',
        'tag-it': '../libs/bower/tag-it/js/tag-it',*/
        'select2': '../libs/bower/select2/select2',
        'bootbox': '../libs/bower/bootbox/js/bootbox.min',
        'moment': '../libs/bower/moment/js/moment.min',
        'moment-tz': '../libs/bower/moment/js/moment-timezone-with-data.min',
        'visualsearch': '../libs/other/custom/visualsearch/visualsearch',
        'globalize': '../libs/bower/globalize/lib/globalize',
        /*'handlebars from the require handlerbars plugin below */
        'handlebars': '../libs/bower/require-handlebars-plugin/js/Handlebars',
        /* require handlebars plugin - Alex Sexton */
        'i18nprecompile': '../libs/bower/require-handlebars-plugin/js/i18nprecompile',
        'json2': '../libs/bower/require-handlebars-plugin/js/json2',
        'hbs': '../libs/bower/require-handlebars-plugin/js/hbs',
        'd3': "../libs/other/d3/d3.min",
        'd3.tip': "../libs/other/d3/d3.tip",
        'nv': "../libs/other/nvd3/js/nv.d3",
        'noty': '../libs/other/noty/jquery.noty.packaged',
        'tmpl': '../templates',
        'WorldMapGenerator': '../libs/custom/timezone/WorldMapGenerator',
        'jstimezonedetect': '../libs/custom/timezone/jstz-1.0.4.min',
        'sparkline': '../libs/other/sparkline/jquery.sparkline',
        'gridster': '../libs/other/gridster/js/jquery.gridster',
        'dashboard': '../libs/other/dashboard/dashboard',
        'tour' : '../libs/other/bootstrap-tour-0.10.3/js/bootstrap-tour.min'


    },

    hbs: {
        disableI18n: true,
        helperPathCallback: // Callback to determine the path to look for helpers
            function(name) { // ('/template/helpers/'+name by default)
            return "../helpers/Helpers";
        },
        templateExtension: "html",
        compileOptions: {}
    }
});
