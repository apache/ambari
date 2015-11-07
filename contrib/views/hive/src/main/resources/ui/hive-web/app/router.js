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
import constants from 'hive/utils/constants';

var Router = Ember.Router.extend({
  location: config.locationType
});

Router.map(function () {
  var savedQueryPath = constants.namingConventions.routes.queries + '/:' + constants.namingConventions.savedQuery + '_id';
  var historyQueryPath = constants.namingConventions.routes.history + '/:' + constants.namingConventions.job + '_id';

  this.route(constants.namingConventions.routes.queries);
  this.route(constants.namingConventions.routes.history);
  this.route(constants.namingConventions.routes.udfs);
  this.route(constants.namingConventions.routes.uploadTable);

  this.resource(constants.namingConventions.routes.index, { path: '/' }, function () {
    this.route(constants.namingConventions.routes.savedQuery, { path: savedQueryPath});
    this.route(constants.namingConventions.routes.historyQuery, { path: historyQueryPath}, function () {
      this.route(constants.namingConventions.routes.logs);
      this.route(constants.namingConventions.routes.results);
      this.route(constants.namingConventions.routes.explain);
    });
  });

  this.route('loading');
});

export default Router;
