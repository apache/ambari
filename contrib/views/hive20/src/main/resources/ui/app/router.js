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

import Ember from 'ember';
import config from './config/environment';

const Router = Ember.Router.extend({
  location: config.locationType,
  rootURL: config.rootURL
});

Router.map(function() {
  this.route('service-check');
  this.route('password');

  this.route('jobs');
  this.route('udfs', function() {
    this.route('new');
    this.route('udf', {path: '/:udfId'}, function() {
    });
  });

  this.route('settings');
  this.route('savedqueries');

  this.route('databases', function() {
    this.route('newtable');
    this.route('database', {path: '/:databaseId'}, function() {
      this.route('tables', {path: '/tables'}, function() {
        this.route('new-database');
        this.route('new');
        this.route('upload-table');
        this.route('table', {path: '/:name'}, function() {
          this.route('edit');
          this.route('rename');
          this.route('columns');
          this.route('partitions');
          this.route('storage');
          this.route('details');
          this.route('view');
          this.route('ddl');
          this.route('stats');
          this.route('auth');
        });
      });
    });
  });
  this.route('messages', function() {
    this.route('message', {path: '/:message_id'});
  });

  this.route('queries', function() {
    this.route('new');
    this.route('query', {path: '/:worksheetId'}, function() {
      this.route('results');
      this.route('log');
      this.route('visual-explain');
      this.route('tez-ui');
      this.route('loading');
    });
  });
});

export default Router;
