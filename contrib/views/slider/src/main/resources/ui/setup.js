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

'use strict';

var command = process.argv[2],
    exec = require('child_process').exec;


var skeletonURL = 'https://codeload.github.com/gcollazo/brunch-with-ember-reloaded/zip/master';
var fileSources = [
  {
    src: 'http://builds.emberjs.com/release/ember.js',
    dest: 'vendor/scripts/development/ember.js'
  },
  {
    src: 'http://builds.emberjs.com.s3.amazonaws.com/ember-data-latest.js',
    dest: 'vendor/scripts/development/ember-data.js'
  },
  {
    src: 'http://builds.emberjs.com/release/ember.prod.js',
    dest: 'vendor/scripts/production/ember.js'
  },
  {
    src: 'http://builds.emberjs.com.s3.amazonaws.com/ember-data-latest.prod.js',
    dest: 'vendor/scripts/production/ember-data.js'
  }
];


switch (command) {
  case 'update:ember':
    console.log('-> Downloading files...');
    fileSources.forEach(function(file) {
      exec('curl ' + file.src + ' > ' + file.dest);
    });
    break;

  case 'update:skeleton':
    exec('curl ' + skeletonURL + '> master.zip', function() {
      exec('unzip master.zip', function() {
        exec('cat brunch-with-ember-reloaded-master/config.js > config.js');
        exec('cat brunch-with-ember-reloaded-master/karma.conf.js > karma.conf.js');
        exec('cat brunch-with-ember-reloaded-master/package.json > package.json');
        exec('cat brunch-with-ember-reloaded-master/README.md > README.md');
        exec('cat brunch-with-ember-reloaded-master/setup.js > setup.js');
        exec('rm -rf generators', function() {
          exec('mv brunch-with-ember-reloaded-master/generators/ generators/', function() {
            exec('rm -rf brunch-with-ember-reloaded-master');
            exec('rm -r master.zip');
          });
        });
      });
    });
    break;

  default:
    console.log();
    console.log('Usage:');
    console.log('\tnpm run update:ember     Updates ember.js an ember-data.js');
    console.log('\tnpm run update:skeleton  Updates all skeleton files');
    console.log();
}
