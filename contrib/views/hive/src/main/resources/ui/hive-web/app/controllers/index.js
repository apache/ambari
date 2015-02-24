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
import constants from 'hive/utils/constants';

export default Ember.Controller.extend({
  needs: [ constants.namingConventions.openQueries,
           constants.namingConventions.databases,
           constants.namingConventions.udfs,
           constants.namingConventions.jobLogs,
           constants.namingConventions.jobResults,
           constants.namingConventions.jobExplain,
           constants.namingConventions.settings
  ],

  openQueries: Ember.computed.alias('controllers.' + constants.namingConventions.openQueries),
  databases: Ember.computed.alias('controllers.' + constants.namingConventions.databases),
  udfs: Ember.computed.alias('controllers.' + constants.namingConventions.udfs + '.udfs'),
  logs: Ember.computed.alias('controllers.' + constants.namingConventions.jobLogs),
  results: Ember.computed.alias('controllers.' + constants.namingConventions.jobResults),
  explain: Ember.computed.alias('controllers.' + constants.namingConventions.jobExplain),
  settings: Ember.computed.alias('controllers.' + constants.namingConventions.settings),

  canExecute: function () {
    var isModelRunning = this.get('model.isRunning');
    var hasParams = this.get('queryParams.length');

    if (isModelRunning) {
      return false;
    }

    if (hasParams) {
      // all param have values?
      return this.get('queryParams').every(function (param) { return param.value; });
    }

    return true;
  }.property('model.isRunning', 'queryParams.@each.value'),

  parseQueryParams: function () {
    var query = this.get('openQueries.currentQuery.fileContent'),
        param,
        updatedParams = [],
        currentParams = this.get('queryParams'),
        paramRegExp = /\$\w+/ig,
        paramNames = query.match(paramRegExp) || [];

    paramNames = paramNames.uniq();

    paramNames.forEach(function (name) {
      param = currentParams.findBy('name', name);
      if (param) {
        updatedParams.push(param);
      } else {
        updatedParams.push({ name: name, value: "" });
      }
    });

    currentParams.setObjects(updatedParams);
  }.observes('openQueries.currentQuery.fileContent'),

  _executeQuery: function (shouldExplain) {
    var queryId,
        self = this,
        query,
        finalQuery,
        job,
        originalModel = this.get('model');

    job = this.store.createRecord(constants.namingConventions.job, {
      title: originalModel.get('title'),
      dataBase: this.get('databases.selectedDatabase.name')
    });

    originalModel.set('isRunning', true);

     //if it's a saved query / history entry set the queryId
    if (!originalModel.get('isNew')) {
      queryId = originalModel.get('constructor.typeKey') === constants.namingConventions.job ?
                originalModel.get('queryId') :
                originalModel.get('id');

      job.set('queryId', queryId);
    }

    query = this.get('openQueries').getQueryForModel(originalModel);

    finalQuery = this.buildQuery(query, shouldExplain);
    finalQuery = this.bindQueryParams(finalQuery);
    finalQuery = this.prependQuerySettings(finalQuery);

    job.set('forcedContent', finalQuery);

    return this.saveQuery(job, originalModel);
  },

  saveQuery: function (job, originalModel) {
    var defer = Ember.RSVP.defer(),
        self = this,
        openQueries = this.get('openQueries');

    var handleError = function (err) {
      originalModel.set('isRunning', undefined);
      defer.reject(err);
    };

    job.save().then(function () {
      //convert tab for current model since the execution will create a new job, and navigate to the new job route.
      openQueries.convertTabToJob(originalModel, job).then(function () {
        //reset flag on the original model
        originalModel.set('isRunning', undefined);

        defer.resolve(job);
      }, function (err) {
        handleError(err);
      });

      self.get('settings').updateSettingsId(originalModel.get('id'), job.get('id'));
    }, function (err) {
      handleError(err);
    });

    return defer.promise;
  },

  prependQuerySettings: function (query) {
    var settings = this.get('settings').getSettingsString();

    if (settings.length) {
      return settings + "\n\n" + query;
    }

    return query;
  },

  buildQuery: function (query, shouldExplain) {
    var selections = this.get('openQueries.highlightedText'),
        isQuerySelected = selections && selections[0] !== "",
        queryComponents = this.extractComponents(query.get('fileContent')),
        finalQuery = '',
        queries;

    if (isQuerySelected) {
      queries = selections.map(function (s) {
        return s.replace(";", "");
      });
    } else {
      queries = queryComponents.queryString.split(';');
      queries = queries.filter(Boolean);
    }

    queries = queries.map(function (query) {
      var explainIndex = query.indexOf(constants.namingConventions.explainPrefix);

      if (shouldExplain) {
        if (query.indexOf(constants.namingConventions.explainPrefix) === -1) {
          return constants.namingConventions.explainPrefix + query;
        }

        return query;
      } else {
        if (query.indexOf(constants.namingConventions.explainPrefix) > -1) {
          return query.replace(constants.namingConventions.explainPrefix, '');
        }

        return query;
      }
    });

    if (queryComponents.files.length) {
      finalQuery += queryComponents.files.join("\n") + "\n\n";
    }

    if (queryComponents.udfs.length) {
      finalQuery += queryComponents.udfs.join("\n") + "\n\n";
    }

    finalQuery += queries.join(";");
    return finalQuery;
  },

  bindQueryParams: function (query) {
    var params = this.get('queryParams');

    if (!params.get('length')) {
      return query;
    }

    params.forEach(function (param) {
      query = query.split(param.name).join(param.value);
    });

    return query;
  },

  init: function () {
    this._super();

    // initialize queryParams with an empty array
    this.set('queryParams', Ember.ArrayProxy.create({ content: Ember.A([]) }))

    this.set('queryProcessTabs', Ember.ArrayProxy.create({ content: Ember.A([
      Ember.Object.create({
        name: Ember.I18n.t('menus.logs'),
        path: constants.namingConventions.subroutes.jobLogs
      }),
      Ember.Object.create({
        name: Ember.I18n.t('menus.results'),
        path: constants.namingConventions.subroutes.jobResults
      }),
      Ember.Object.create({
        name: Ember.I18n.t('menus.explain'),
        path: constants.namingConventions.subroutes.jobExplain
      })
    ])}));
  },

  displayJobTabs: function () {
    return this.get('content.constructor.typeKey') === constants.namingConventions.job;
  }.property('content'),

  modelChanged: function () {
    var self = this;
    var content = this.get('content');
    var openQueries = this.get('openQueries');

    //update open queries list when current query model changes
    openQueries.update(content).then(function (isExplainedQuery) {
      var newId = content.get('id');
      var tab = openQueries.getTabForModel(content);

      if (content.get('constructor.typeKey') === constants.namingConventions.job) {
        self.get('queryProcessTabs').forEach(function (queryTab) {
          queryTab.set('id', newId);
        });

        if (isExplainedQuery) {
          self.set('explain.content', content);
        } else {
          self.set('logs.content', content);
          self.set('results.content', content);
        }

        self.setExplainVisibility(isExplainedQuery);

        self.transitionToRoute(tab.get('subroute'));
      }
    });
  }.observes('content'),

  csvUrl: function () {
    if (this.get('content.constructor.typeKey') !== constants.namingConventions.job) {
      return;
    }

    if (this.get('content.status') !== constants.statuses.finished) {
      return;
    }

    var url = this.container.lookup('adapter:application').buildURL();
    url += '/' + constants.namingConventions.jobs + '/' + this.get('content.id');
    url += '/results/csv';

    return url;
  }.property('content'),

  downloadMenu: function () {
    var items = [];
    var tabs = this.get('queryProcessTabs');
    var isResultsTabVisible = tabs.findBy('path', constants.namingConventions.subroutes.jobResults).get('visible');

    if (this.get('content.status') === constants.statuses.finished && isResultsTabVisible) {
      items.push({
        title: Ember.I18n.t('buttons.saveHdfs'),
        action: 'saveToHDFS'
      });

      if (this.get('csvUrl')) {
        items.push(
          Ember.Object.create({
            title: Ember.I18n.t('buttons.saveCsv'),
            href: this.get('csvUrl')
          })
        );
      }
    }

    return items.length ? items : null;
  }.property('content.status', 'queryProcessTabs.@each.visible'),

  extractComponents: function (queryString) {
    var components = {};

    var udfRegEx = new RegExp("(" + constants.namingConventions.udfInsertPrefix + ").+", "ig");
    var fileRegEx = new RegExp("(" + constants.namingConventions.fileInsertPrefix + ").+", "ig");

    components.udfs         = queryString.match(udfRegEx) || [];
    components.files        = queryString.match(fileRegEx) || [];
    components.queryString  = queryString.replace(udfRegEx, "").replace(fileRegEx, "").trim();

    return components;
  },

  saveToHDFS: function () {
    var job = this.get('content');

    if (job.get('status') !== constants.statuses.finished) {
      return;
    }

    var self = this;

    var file = "/tmp/" + job.get('id') + ".csv";
    var url = this.container.lookup('adapter:application').buildURL();
    url +=  "/jobs/" + job.get('id') + "/results/csv/saveToHDFS";

    Ember.$.getJSON(url, {
        commence: true,
        file: file
    }).then(function (response) {
      self.pollSaveToHDFS(response);
    }, function (response) {
      self.send('addAlert', constants.alerts.error, response.message, "alerts.errors.save.results");
    });
  },

  pollSaveToHDFS: function (data) {
    var self = this;
    var url = this.container.lookup('adapter:application').buildURL();
    url += "/jobs/" + data.jobId + "/results/csv/saveToHDFS";

    Ember.run.later(function () {
      Ember.$.getJSON(url).then(function (response) {
        if (response.status !== constants.results.statuses.terminated) {
          self.pollSaveToHDFS(response);
        } else {
          self.set('content.isRunning', false);
        }
      }, function (response) {
        self.send('addAlert', constants.alerts.error, response.message, "alerts.errors.save.results");
      });
    }, 2000);
  },

  setExplainVisibility: function (show) {
    var tabs = this.get('queryProcessTabs');

    tabs.findBy('path', constants.namingConventions.subroutes.jobExplain).set('visible', show);
    tabs.findBy('path', constants.namingConventions.subroutes.jobLogs).set('visible', !show);
    tabs.findBy('path', constants.namingConventions.subroutes.jobResults).set('visible', !show);
  },

  actions: {
    saveToHDFS: function () {
      this.set('content.isRunning', true);
      this.saveToHDFS();
    },

    insertUdf: function (item) {
      var query = this.get('openQueries').getQueryForModel(this.get('model'));

      var queryString = query.get('fileContent');

      var newUdf = constants.namingConventions.udfInsertPrefix + item.get('name') + " as '" + item.get('classname') + "';";
      var newFileResource = item.get('fileResource.path');

      if (item.get('fileResource.path')) {
        newFileResource = constants.namingConventions.fileInsertPrefix + item.get('fileResource.path') + ";";
      }

      var components = this.extractComponents(queryString);

      if (!components.files.contains(newFileResource) && newFileResource) {
        components.files.push(newFileResource);
      }

      if (!components.udfs.contains(newUdf)) {
        components.udfs.push(newUdf);
      }

      var updatedContent = components.files.join("\n") + "\n\n";
      updatedContent += components.udfs.join("\n") + "\n\n";
      updatedContent += components.queryString;

      query.set('fileContent', updatedContent);
    },

    addQuery: (function () {
      var idCounter = 0;

      return function () {
        var model = this.store.createRecord(constants.namingConventions.savedQuery, {
          dataBase: this.get('databases.selectedDatabase.name'),
          title: 'New Query',
          type: constants.namingConventions.savedQuery,
          queryFile: '',
          id: 'fixture_' + idCounter
        });

        idCounter++;

        this.transitionToRoute(constants.namingConventions.subroutes.savedQuery, model);
      };
    }()),

    saveQuery: function () {
      var self = this,
          wasNew = this.get('model.isNew'),
          defer = Ember.RSVP.defer();

      this.send('openModal', 'modal-save', {
        heading: "modals.save.heading",
        text: this.get('content.title'),
        defer: defer
      });

      defer.promise.then(function (text) {
        self.get('content').set('title', text);

        self.get('openQueries').save(self.get('content')).then(function () {
          if (wasNew) {
            self.transitionToRoute(constants.namingConventions.subroutes.savedQuery, self.get('model.id'));
          }
        });
      });
    },

    executeQuery: function () {
      var self = this;
      var subroute;

      this._executeQuery().then(function (job) {
        if (job.get('status') !== constants.statuses.finished) {
          subroute = constants.namingConventions.subroutes.jobLogs;
        } else {
          subroute = constants.namingConventions.subroutes.jobResults;
        }

        self.get('openQueries').updateTabSubroute(job, subroute);

        self.transitionToRoute(constants.namingConventions.subroutes.historyQuery, job.get('id'));
      }, function (err) {
        self.send('addAlert', constants.alerts.error, err.responseText, "alerts.errors.save.query");
      });
    },

    explainQuery: function () {
      var self = this;

      this._executeQuery(true).then(function (job) {
        self.get('openQueries').updateTabSubroute(job, constants.namingConventions.subroutes.jobExplain);

        self.transitionToRoute(constants.namingConventions.subroutes.historyQuery, job.get('id'));
      }, function (err) {
        self.send('addAlert', constants.alerts.error, err.responseText, "alerts.errors.save.query");
      });
    }
  }
});
