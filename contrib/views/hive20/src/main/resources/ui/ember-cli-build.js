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

/*jshint node:true*/
/* global require, module */
var EmberApp = require('ember-cli/lib/broccoli/ember-app');

module.exports = function(defaults) {
  var app = new EmberApp(defaults, {
    // Add options here
    bootstrap: {
      // List of Bootstrap plugins to use
      plugins: ['dropdown']
    },
    fingerprint: {
      enabled: false
    },
    codemirror: {
      modes: ['sql'],
      themes: ['solarized']
    }
  });

  // Use `app.import` to add additional libraries to the generated
  // output files.
  //
  // If you need to use different assets in different
  // environments, specify an object as the first parameter. That
  // object's keys should be the environment name and the values
  // should be the asset to use in that environment.
  //
  // If the library that you are including contains AMD or ES6
  // modules that you would like to import into your application
  // please specify an object with the list of modules as keys
  // along with the exports of each module as its value.


   app.import('bower_components/codemirror/lib/codemirror.js');
   app.import('bower_components/codemirror/addon/hint/sql-hint.js');
   app.import('bower_components/codemirror/mode/sql/sql.js');
   app.import('bower_components/codemirror/addon/hint/show-hint.js');
   app.import('bower_components/d3/d3.js');
   app.import('bower_components/webcola/WebCola/cola.min.js');
   app.import('bower_components/codemirror/lib/codemirror.css');
   app.import('bower_components/jquery-ui/jquery-ui.js');
   app.import('bower_components/jquery-ui/themes/base/jquery-ui.css');
   app.import('bower_components/codemirror/addon/hint/show-hint.css');
   app.import('vendor/browser-pollyfills.js');

  /*
  app.import('vendor/codemirror/codemirror-min.js');
  app.import('vendor/codemirror/sql-hint.js');
  app.import('vendor/codemirror/show-hint.js');
  app.import('vendor/codemirror/codemirror.css');
  app.import('vendor/codemirror/show-hint.css');
  */

  return app.toTree();
};
