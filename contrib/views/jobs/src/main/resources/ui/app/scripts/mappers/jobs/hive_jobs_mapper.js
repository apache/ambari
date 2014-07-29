/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

App.hiveJobsMapper = App.QuickDataMapper.create({

  json_map: {
    id: 'entity',
    name: 'entity',
    user: 'primaryfilters.user',
    hasTezDag: {
      custom: function(source) {
        var query = Ember.get(source, 'otherinfo.query');
        return Ember.isNone(query) ? false : query.match("\"Tez\".*\"DagName:\"");
      }
    },
    queryText: {
      custom: function(source) {
        var query = Ember.get(source, 'otherinfo.query');
        return Ember.isNone(query) ? '' : $.parseJSON(query).queryText;
      }
    },
    failed: {
      custom: function(source) {
        return Ember.get(source ,'otherinfo.status') === false;
      }
    },
    startTime: {
      custom: function(source) {
        return source.starttime > 0 ? source.starttime : null
      }
    },
    endTime: {
      custom: function(source) {
        return source.endtime > 0 ? source.endtime : null
      }
    }
  },

  map: function (json) {

    var model = this.get('model'),
      jobsToDelete = App.HiveJob.store.all('hiveJob').get('content').mapProperty('id'),
      map = this.get('json_map'),
      hiveJobs = [];

    if (json) {
      if (!json.entities) {
        json.entities = [];
        if (json.entity) {
          json.entities = [json];
        }
      }

      json.entities.forEach(function (entity) {
        var hiveJob = Ember.JsonMapper.map(entity, map);

        if (entity.events != null) {
          entity.events.forEach(function (event) {
            switch (event.eventtype) {
              case "QUERY_SUBMITTED":
                hiveJob.startTime = event.timestamp;
                break;
              case "QUERY_COMPLETED":
                hiveJob.endTime = event.timestamp;
                break;
              default:
                break;
            }
          });
        }
        hiveJobs.push(hiveJob);
        var tezDag = App.HiveJob.store.all('tezDag').findBy('hiveJob.id', hiveJob.id);
        if (!Em.isNone(tezDag)) {
          hiveJob.tezDag = tezDag.id;
        }
        jobsToDelete = jobsToDelete.without(hiveJob.id);
      });

      jobsToDelete.forEach(function (id) {
        var r = App.HiveJob.store.getById('hiveJob', id);
        if(r) r.destroyRecord();
      });

    }
    App.HiveJob.store.pushMany('hiveJob', hiveJobs);
  }

});
