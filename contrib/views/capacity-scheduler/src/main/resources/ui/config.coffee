#  Licensed to the Apache Software Foundation (ASF) under one
#  or more contributor license agreements.  See the NOTICE file
#  distributed with this work for additional information
#  regarding copyright ownership.  The ASF licenses this file
#  to you under the Apache License, Version 2.0 (the
#  "License"); you may not use this file except in compliance
#  with the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.


exports.config = 

  files: 
    
    javascripts: 
      defaultExtension: 'js'
      joinTo: 
        'javascripts/app.js': /^app/
        'javascripts/vendor.js': /^bower_components|vendor/
      order:
        before: [
          'bower_components/jquery/jquery.js',
          'bower_components/handlebars/handlebars.js',
          'bower_components/ember/ember.js',
          'bower_components/ember-data/ember-data.js'
          'bower_components/ember-i18n/lib/i18n.js'
          ]

    stylesheets:
      defaultExtension: 'css'
      joinTo:
        'stylesheets/app.css': /^app/
        'stylesheets/vendor.css': /^bower_components|vendor/
      order:
        before: [
          'bower_components/bootstrap/dist/css/bootstrap.css',
          'bower_components/bootstrap/dist/css/bootstrap-theme.css',
          'bower_components/font-awesome/font-awesome.css'
        ]

    templates:
      precompile: true
      root: 'templates'
      defaultExtension: 'hbs'
      joinTo: 'javascripts/app.js' : /^app/
      paths:
        jquery: 'bower_components/jquery/jquery.js'
        handlebars: 'bower_components/handlebars/handlebars.js'
        ember: 'bower_components/ember/ember.js'

  modules:
    addSourceURLs: true

  paths:
    public: '/usr/lib/ambari-server/web/views-debug/CAPACITY-SCHEDULER/0.1.0/CS_1/'

  overrides:
    production:
        paths:
          public: 'public'

  server:
    port: 3333
    base: '/'
    run: no
