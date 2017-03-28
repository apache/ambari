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

    baseUrl: "/scripts",

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
        'bootstrap': {
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
        'backgrid-columnmanager': {
            deps: ['backbone', 'backgrid']
        },
        'backgrid-sizeable': {
            deps: ['backbone', 'backgrid']
        },
        'backgrid-orderable': {
            deps: ['backbone', 'backgrid']
        },
        'Backbone.BootstrapModal': {
            deps: ['jquery', 'underscore', 'backbone']
        },
        'bootstrap-editable': {
            deps: ['bootstrap']
        },
        'jquery-ui': {
            deps: ['jquery']
        },
        'globalize': {
            exports: 'Globalize'
        },
        'bootstrap-daterangepicker': {
            deps: ['bootstrap', 'jquery', 'moment'],
            exports: 'daterangepicker'
        },
        'moment': {
            deps: [],
            exports: 'moment'
        },
        'moment-tz': {
            deps: ['moment']
        },
        'localstorage': {
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
        'noty': {
            deps: ['jquery']
        },
        'WorldMapGenerator': {
            deps: ['jquery']
        },
        'jstimezonedetect': {
            exports: 'jstz'
        },
        'sparkline': {
            deps: ['jquery']
        },
        'gridster': {
            deps: ['jquery']
        },
        'dashboard': {
            deps: ['jquery', 'jquery-ui', 'gridster']
        },
        'tour' : {
           exports : 'tour'
        }
    },

    paths: {
         // Workaround for exports
        'jQuery' : '../libs/bower/jquery/jquery.min',
        'Backbone': '../libs/bower/backbone/backbone',
        '_' : '../libs/bower/underscore/underscore-min',
        'Backgrid': '../libs/bower/backgrid/backgrid.min',


        'jquery': '../libs/bower/jquery/jquery.min',
        'jquery-ui': '../libs/bower/jquery-ui/js/jquery-ui-1.11.4.min',
        'backbone': '../libs/bower/backbone/backbone',
        'underscore': '../libs/bower/underscore/underscore-min',

        /* alias all marionette libs */
        'backbone.marionette': '../libs/bower/backbone.marionette/backbone.marionette.min',
        'backbone.wreqr': '../libs/bower/backbone.wreqr/backbone.wreqr.min',
        'backbone.babysitter': '../libs/bower/backbone.babysitter/backbone.babysitter.min',
        //'backbone.paginator': '../libs/bower/backbone.paginator/js/backbone.paginator.min',

        /* alias the bootstrap js lib */
        'bootstrap': '../libs/bower/bootstrap/bootstrap.min',

        /* BackGrid for Tables */
        'backgrid': '../libs/bower/backgrid/backgrid.min',
        'backgrid-filter': '../libs/bower/backgrid-filter/backgrid-filter.min',
        'backgrid-select-all': '../libs/bower/backgrid-select-all/backgrid-select-all.min',
        'backgrid-paginator': '../libs/bower/backgrid-paginator/backgrid-paginator.min',
        'backgrid-columnmanager': '../libs/custom/backgrid-columnmanager/Backgrid.ColumnManager',
        'backgrid-sizeable': '../libs/bower/backgrid-sizeable-columns/backgrid-sizeable-columns',
        'backgrid-orderable': '../libs/bower/backgrid-orderable-columns/backgrid-orderable-columns',

        'backbone-pageable': '../libs/bower/backbone-pageable/backbone-pageable.min',
        'localstorage': '../libs/bower/backbone.localstorage/backbone.localStorage-min',
        'bootstrap-daterangepicker': '../libs/custom/daterangepicker/js/daterangepicker',
        'select2': '../libs/bower/select2/select2.min',
        'bootbox': '../libs/bower/bootbox/bootbox',
        'moment': '../libs/bower/moment/moment.min',
        'moment-tz': '../libs/bower/moment/moment-timezone-with-data.min',
        'visualsearch': '../libs/custom/visualsearch/visualsearch',
        'globalize': '../libs/bower/globalize/globalize',
        'handlebars': '../libs/bower/require-handlebars-plugin/Handlebars',
        'i18nprecompile': '../libs/bower/require-handlebars-plugin/i18nprecompile',
        'json2': '../libs/bower/require-handlebars-plugin/json2',
        'hbs': '../libs/bower/require-handlebars-plugin/hbs',
        'd3': "../libs/bower/d3/d3.min",
        'd3.tip': "../libs/bower/d3/d3.tip",
        'nv': "../libs/bower/nvd3/nv.d3.min",
        'noty': '../libs/bower/noty/jquery.noty.packaged.min',
        'tmpl': '../templates',
        'WorldMapGenerator': '../libs/custom/timezone/WorldMapGenerator',
        'jstimezonedetect': '../libs/custom/timezone/jstz-1.0.4.min',
        'sparkline': '../libs/bower/sparkline/jquery.sparkline',
        'gridster': '../libs/bower/gridster/jquery.gridster.min',
        'dashboard': '../libs/bower/dashboard/dashboard',
        'tour' : '../libs/bower/bootstrap-tour/bootstrap-tour.min'
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
