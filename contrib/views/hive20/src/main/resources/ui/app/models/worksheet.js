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

import DS from 'ember-data';

export default DS.Model.extend({
  title: DS.attr('string'),
  query: DS.attr('string', {defaultValue: ''}),
  selectedDb: DS.attr('string'),
  owner: DS.attr('string'),
  queryResult: DS.attr({defaultValue: {'schema' :[], 'rows' :[]}}),
  currentPage: DS.attr('number', {defaultValue: 0}),
  previousPage: DS.attr('number', {defaultValue: -1}),
  nextPage: DS.attr('number', {defaultValue: 1}),
  selected: DS.attr('boolean', {transient: true, defaultValue: false}),
  jobData: DS.attr({defaultValue: []}),
  currentJobId: DS.attr({defaultValue: null}),
  currentJobData: DS.attr({defaultValue: null}),
  hasNext: DS.attr('boolean', { defaultValue: false}),
  hasPrevious: DS.attr('boolean', { defaultValue: false}),
  selectedTablesModels: DS.attr(),
  selectedMultiDb: DS.attr(),
  queryFile: DS.attr('string', {defaultValue: ""}),
  logFile: DS.attr('string', {defaultValue: ""}),
  logResults: DS.attr('string', {defaultValue: ""}),
  isQueryRunning: DS.attr('boolean', {defaultValue: false}),
  isQueryDirty: DS.attr('boolean', {defaultValue: false}),
  isQueryResultContainer: DS.attr('boolean', {defaultValue: false}),
  visualExplainJson: DS.attr({defaultValue: null}),
  lastResultRoute: DS.attr({defaultValue: ""}),
  tezUrl: DS.attr('string', {defaultValue: null})
});
