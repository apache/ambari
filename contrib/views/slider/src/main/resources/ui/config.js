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

exports.config = {
  paths: {
    watched: ['app', 'envs', 'vendor', 'test']
  },
  fileListInterval: 500,
  files: {
    javascripts: {
      joinTo: {
        'javascripts/app.js': /^(app|envs\/development)/,
        'javascripts/vendor.js': /^(vendor\/scripts\/(common|development)|vendor\\scripts\\(common|development))/,
        'javascripts/test.js': /^test(\/|\\)(?!vendor)/
      },
      order: {
        before: [
          'vendor/scripts/common/d3.v2.js',
          'vendor/scripts/common/tv4.js',
          'vendor/scripts/common/cubism.v1.js',
          'vendor/scripts/common/rickshaw.js',
          'vendor/scripts/common/console-polyfill.js',
          'vendor/scripts/common/jquery.js',
          'vendor/scripts/common/jquery.ui.core.js',
          'vendor/scripts/common/jquery.ui.widget.js',
          'vendor/scripts/common/jquery.ui.mouse.js',
          'vendor/scripts/common/jquery.ui.sortable.js',
          'vendor/scripts/common/jquery.timeago.js',
          'vendor/scripts/common/handlebars.js',
          'vendor/scripts/development/ember.js',
          'vendor/scripts/production/ember-data.js',
          'vendor/scripts/common/bs-core.min.js',
          'vendor/scripts/common/bs-nav.min.js',
          'vendor/scripts/common/bs-basic.min.js',
          'vendor/scripts/common/bs-button.min.js',
          'vendor/scripts/common/bs-modal.min.js',
          'vendor/scripts/common/bs-popover.min.js',
          'vendor/scripts/common/ember-i18n-1.4.1.js',
          'vendor/scripts/common/bootstrap.js',
          'vendor/scripts/common/moment.js'
        ]
      }
    },
    stylesheets: {
      joinTo: {
        'stylesheets/app.css': /^(app|vendor)/
      },
      order: {
        before: [
          'vendor/styles/cubism.css',
          'vendor/styles/rickshaw.css',
          'vendor/styles/bootstrap.css',
          'vendor/styles/font-awesome.css',
          'vendor/styles/font-awesome-ie7.css'
        ]
      }
    },
    templates: {
      precompile: true,
      root: 'templates',
      joinTo: {
        'javascripts/app.js': /^app/
      }
    }
  },
  overrides: {

    // Production Settings
    production: {
      files: {
        javascripts: {
          joinTo: {
            'javascripts/app.js': /^(app|envs\/production)/,
            'javascripts/vendor.js': /^(vendor\/scripts\/(common|production)|vendor\\scripts\\(common|production))/
          },
          order: {
            before: [
              'vendor/scripts/common/d3.v2.js',
              'vendor/scripts/common/cubism.v1.js',
              'vendor/scripts/common/rickshaw.js',
              'vendor/scripts/common/console-polyfill.js',
              'vendor/scripts/common/jquery.js',
              'vendor/scripts/common/handlebars.js',
              'vendor/scripts/production/ember.js',
              'vendor/scripts/production/ember-data.js',
              'vendor/scripts/common/bs-core.min.js',
              'vendor/scripts/common/bs-nav.min.js',
              'vendor/scripts/common/bs-basic.min.js',
              'vendor/scripts/common/bs-button.min.js',
              'vendor/scripts/common/bs-modal.min.js',
              'vendor/scripts/common/bs-popover.min.js',
              'vendor/scripts/common/ember-i18n-1.4.1.js',
              'vendor/scripts/common/bootstrap.js',
              'vendor/scripts/common/moment.js'
            ]
          }
        }
      },
      optimize: true,
      sourceMaps: false,
      plugins: {
        autoReload: {
          enabled: false
        }
      }
    }
  }
};
