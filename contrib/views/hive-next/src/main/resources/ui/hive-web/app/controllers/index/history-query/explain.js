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
import utils from 'hive/utils/functions';

export default Ember.ObjectController.extend({
  cachedExplains: [],

  clearCachedExplainSet: function (jobId) {
    var existingJob = this.get('cachedExplains').findBy('id', jobId);

    if (existingJob) {
      this.set('cachedExplains', this.get('cachedExplains').without(existingJob));
    }
  },

  initExplain: function () {
    var cachedExplain;

    cachedExplain = this.get('cachedExplains').findBy('id', this.get('content.id'));

    if (cachedExplain) {
      this.formatExplainResults(cachedExplain);
    } else {
      this.getExplain(true);
    }
  }.observes('content'),

  getExplain: function (firstPage, rows) {
    var self = this;
    var url = this.container.lookup('adapter:application').buildURL();
    url += '/' + constants.namingConventions.jobs + '/' + this.get('content.id') + '/results';

    if (firstPage) {
      url += '?first=true';
    }

    this.get('content').reload().then(function () {
      Ember.$.getJSON(url).then(function (data) {
        var explainSet;

        //if rows from a previous page read exist, prepend them
        if (rows) {
          data.rows.unshiftObjects(rows);
        }

        if (!data.hasNext) {
          explainSet = self.get('cachedExplains').pushObject(Ember.Object.create({
            id: self.get('content.id'),
            explain: data
          }));

          self.set('content.explain', explainSet);

          self.formatExplainResults(explainSet);
        } else {
          self.getExplain(false, data.rows);
        }
      });
    })
  },

  formatExplainResults: function (explainSet) {
    var formatted = [],
        currentNode,
        currentNodeWhitespace,
        previousNode,
        getLeadingWhitespacesCount = function (str) {
          return str.replace(utils.regexes.whitespaces, '$1').length;
        };

    explainSet = explainSet
                 .get('explain.rows')
                 .map(function (row) {
                    return row[0];
                  })
                 .filter(Boolean)
                 .map(function (str) {
                    return {
                      text: str,
                      parentNode: null,
                      contents: []
                    };
                  });

    for (var i = 0; i < explainSet.length; i++) {
      currentNode = explainSet[i];
      previousNode = explainSet[i-1];

      if (i > 0) {
        currentNodeWhitespace = getLeadingWhitespacesCount(currentNode.text);

        if (currentNodeWhitespace > getLeadingWhitespacesCount(previousNode.text)) {
          currentNode.parentNode = previousNode;
          previousNode.contents.pushObject(currentNode);
        } else {
          for (var j = i - 1; j >= 0; j--) {
            if (currentNodeWhitespace === getLeadingWhitespacesCount(explainSet[j].text)) {
              if (currentNodeWhitespace > 0) {
                currentNode.parentNode = explainSet[j].parentNode;
                currentNode.parentNode.contents.pushObject(currentNode);
              } else {
                formatted.pushObject(currentNode);
              }

              break;
            }
          }
        }
      } else {
        formatted.pushObject(currentNode);
      }
    }

    this.set('formattedExplain', formatted);
  }
});