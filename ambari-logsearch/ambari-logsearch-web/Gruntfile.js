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
module.exports = function(grunt) {
  'use strict';

  var bowerLibsDir = 'libs/';
  var vendorDir = "../libs/";
  var sourceBowerLibsDir = '../../src/main/webapp/libs/bower';
  var sourceCustomLibsDir = '../../src/main/webapp/libs/custom';

  grunt.initConfig({
    clean: {
      vendors: [ vendorDir ]
    },
    copy: {
      main: {
        files: [
          {
            expand: true, cwd: bowerLibsDir, src:
            [
              'backbone/backbone.js', 'backbone/LICENSE',
              'backbone.localstorage/backbone.localStorage-min.js',
              'backgrid-filter/backgrid-filter.min.js', 'backgrid-filter/backgrid-filter.min.css',
              'backgrid-paginator/backgrid-paginator.min.js', 'backgrid-paginator/backgrid-paginator.min.css',
              'backgrid-select-all/backgrid-select-all.min.js', 'backgrid-select-all/backgrid-select-all.min.css',
              'backgrid-sizeable-columns/backgrid-sizeable-columns.js', 'backgrid-sizeable-columns/backgrid-sizeable-columns.css',
              'backgrid-orderable-columns/backgrid-orderable-columns.js', 'backgrid-orderable-columns/backgrid-orderable-columns.css',
              'bootbox/bootbox.js',
              'bootstrap-notify/js/bootstrap-notify.js', 'bootstrap-notify/css/bootstrap-notify.css',
              'jquery/jquery.min.js',
              'jquery-toggles/toggles.min.js', 'jquery-toggles/toggles.css',
              'requirejs/require.js',
              'require-handlebars-plugin/hbs.js',
              'select2/select2.css', 'select2/select2.min.js', 'select2/select2.png', 'select2/select2-spinner.gif',
              'd3/d3.min.js',
              'underscore/underscore-min.js'
            ], dest: vendorDir, filter: 'isFile'
          },
          // Merge source files to vendor
          {
            expand: true, cwd: sourceBowerLibsDir, src: ['**'], dest: vendorDir, filter: 'isFile'
          },
          // Merge custom source files to vendor
          {
            expand: true, cwd: sourceCustomLibsDir, src: ['**'], dest: vendorDir, filter: 'isFile'
          },
          // Custom mappings
          {
            expand: true, cwd: bowerLibsDir + 'backbone.babysitter/lib/', src: ['backbone.babysitter.min.js', 'backbone.babysitter.min.js.map'], dest: vendorDir + 'backbone.babysitter', filter: 'isFile'
          },
          {
            expand: true, cwd: bowerLibsDir + 'backbone.marionette/lib/', src: ['backbone.marionette.min.js', 'backbone.marionette.map'], dest: vendorDir + 'backbone.marionette', filter: 'isFile'
          },
          {
            expand: true, cwd: bowerLibsDir + 'backbone.wreqr/lib/', src: ['backbone.wreqr.min.js', 'backbone.wreqr.min.js.map'], dest: vendorDir + 'backbone.wreqr', filter: 'isFile'
          },
          {
            expand: true, cwd: bowerLibsDir + 'backbone-forms/distribution/', src: ['**'], dest: vendorDir + 'backbone-forms', filter: 'isFile'
          },
          {
            expand: true, cwd: bowerLibsDir + 'backgrid/lib/', src: ['backgrid.min.js', 'backgrid.min.css'], dest: vendorDir + 'backgrid', filter: 'isFile'
          },
          {
            expand: true, cwd: bowerLibsDir, src: ['backgrid-sizeable-columns/backgrid-sizeable-columns.js', 'backgrid-sizeable-columns/backgrid-sizeable-columns.css'], dest: vendorDir, filter: 'isFile'
          },
          {
            expand: true, cwd: bowerLibsDir, src: ['backgrid-orderable-columns/backgrid-orderable-columns.js', 'backgrid-orderable-columns/backgrid-orderable-columns.css'], dest: vendorDir, filter: 'isFile'
          },
          {
            expand: true, cwd: bowerLibsDir + 'bootstrap/dist/js', src: ['bootstrap.min.js'], dest: vendorDir + 'bootstrap', filter: 'isFile'
          },
          {
            expand: true, cwd: bowerLibsDir + 'bootstrap/dist/css', src: ['bootstrap.min.css', 'bootstrap-theme.min.css', 'bootstrap-theme.css.map', 'bootstrap.css.map'], dest: vendorDir + 'bootstrap', filter: 'isFile'
          },
          {
            expand: true, cwd: bowerLibsDir + 'globalize/lib', src: ['**'], dest: vendorDir + 'globalize', filter: 'isFile'
          },
          {
            expand: true, cwd: bowerLibsDir + 'moment/min/', src: ['moment.min.js'], dest: vendorDir + 'moment', filter: 'isFile'
          },
          {
            expand: true, cwd: bowerLibsDir + 'moment-timezone/builds/', src: ['moment-timezone-with-data.min.js'], dest: vendorDir + 'moment', filter: 'isFile'
          },
          {
            expand: true, cwd: bowerLibsDir + 'require-handlebars-plugin/hbs', src: ['i18nprecompile.js', 'json2.js'], dest: vendorDir + 'require-handlebars-plugin', filter: 'isFile'
          },
          {
            expand: true, cwd: bowerLibsDir, src: ['i18nprecompile.js', 'json2.js'], dest: vendorDir + 'require-handlebars-plugin', filter: 'isFile'
          },
          {
            expand: true, cwd: bowerLibsDir + 'nvd3/build/', src: ['nv.d3.min.js', 'nv.d3.min.css'], dest: vendorDir + 'nvd3', filter: 'isFile'
          },
          {
            expand: true, cwd: bowerLibsDir + 'noty/js/noty/packaged', src: ['jquery.noty.packaged.min.js'], dest: vendorDir + 'noty', filter: 'isFile'
          },
          {
            expand: true, cwd: bowerLibsDir + 'gridster.js/dist/', src: ['jquery.gridster.min.js', 'jquery.gridster.min.css'], dest: vendorDir + 'gridster', filter: 'isFile'
          },
          {
            expand: true, cwd: bowerLibsDir + 'bootstrap-tour/build/js/', src: ['bootstrap-tour.min.js'], dest: vendorDir + 'bootstrap-tour', filter: 'isFile'
          },
          {
            expand: true, cwd: bowerLibsDir + 'bootstrap-tour/build/css/', src: ['bootstrap-tour.min.css'], dest: vendorDir + 'bootstrap-tour', filter: 'isFile'
          }
        ]
      }
    }
  });

  grunt.loadNpmTasks('grunt-contrib-copy');
  grunt.registerTask('default', ['copy']);
  grunt.registerTask('build', 'Copy necessary files (js/css) from third party libraries.', function() {
    if (!grunt.file.isDir(vendorDir)) {
      grunt.file.mkdir(vendorDir);

      grunt.log.oklns(grunt.template.process('Directory "<%= directory %>" was created successfully.',
        {
          data: { directory: vendorDir }
        }));
    }
    grunt.task.run('copy');
  });

};