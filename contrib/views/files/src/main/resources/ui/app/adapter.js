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

App = require('app');

function promiseArray(promise, label) {
  return Ember.ArrayProxy.extend(Ember.PromiseProxyMixin).create({
    promise: Ember.RSVP.Promise.cast(promise, label)
  });
}


function serializerForAdapter(adapter, type) {
  var serializer = adapter.serializer,
      defaultSerializer = adapter.defaultSerializer,
      container = adapter.container;

  if (container && serializer === undefined) {
    serializer = serializerFor(container, type.typeKey, defaultSerializer);
  }

  if (serializer === null || serializer === undefined) {
    serializer = {
      extract: function(store, type, payload) { return payload; }
    };
  }

  return serializer;
}

function serializerFor(container, type, defaultSerializer) {
  return container.lookup('serializer:'+type) ||
                 container.lookup('serializer:application') ||
                 container.lookup('serializer:' + defaultSerializer) ||
                 container.lookup('serializer:-default');
}

function _listdir(adapter, store, type, query, recordArray) {
  var promise = adapter.listdir(store, type, query, recordArray),
      serializer = serializerForAdapter(adapter, type),
      label = "";

  return Ember.RSVP.Promise.cast(promise, label).then(function(adapterPayload) {
    var payload = serializer.extractArray(store, type, adapterPayload);

    Ember.assert("The response from a findQuery must be an Array, not " + Ember.inspect(payload), Ember.typeOf(payload) === 'array');

    recordArray.load(payload);
    return recordArray;
  }, null, "DS: Extract payload of findQuery " + type);
}

function _move(adapter, store, record, query) {
  var type = store.modelFor('file'),
      promise = adapter.move(store, type, record, query),
      serializer = serializerForAdapter(adapter, type),
      label = "";

  return promise.then(function(adapterPayload) {
    var payload;

    if (adapterPayload) {
      payload = serializer.extractSingle(store, type, adapterPayload);
    } else {
      payload = adapterPayload;
    }

    //TODO very shady activity :/
    if (typeof record == 'object') {
      store.unloadRecord(record);
    }

    return store.push('file', payload);
  }, function(reason) {

    throw reason;
  }, label);
}

function _mkdir(adapter, store, type, query) {
  var promise = adapter.mkdir(store, type, query),
      serializer = serializerForAdapter(adapter, type),
      label = "";

  return promise.then(function(adapterPayload) {
    var payload;

    if (adapterPayload) {
      payload = serializer.extractSingle(store, type, adapterPayload);
    } else {
      payload = adapterPayload;
    }

    return store.push('file', payload);
  }, function(reason) {
    throw reason;
  }, label);
}

function _remove(adapter, store, record, query, toTrash) {
  var type = record.constructor;
  var method = (toTrash)?'moveToTrash':'remove';
  var promise = adapter[method](store, type, query),
      serializer = serializerForAdapter(adapter, type),
      label = "";

  return promise.then(function(adapterPayload) {
    store.unloadRecord(record);
    return record;
  }, function(reason) {
    if (reason instanceof DS.InvalidError) {
      store.recordWasInvalid(record, reason.errors);
    } else {
      record.rollback();
      //store.recordWasError(record, reason);
    }

    throw reason;
  }, label);
}

Ember.Inflector.inflector.uncountable('fileops');
Ember.Inflector.inflector.uncountable('download');
Ember.Inflector.inflector.uncountable('upload');

function getNamespaceUrl() {
  var parts = window.location.pathname.match(/\/[^\/]*/g);
  var view = parts[1];
  var version = '/versions' + parts[2];
  var instance = parts[3];
  if (parts.length == 4) { // version is not present
    instance = parts[2];
    version = '';
  }
  var namespaceUrl = 'api/v1/views' + view + version + '/instances' + instance + '/';
  return namespaceUrl;
}

App.Store = DS.Store.extend({
  adapter: DS.RESTAdapter.extend({
    namespace: getNamespaceUrl() + 'resources/files',
    headers: {
      'X-Requested-By': 'ambari'
    },
    listdir: function(store, type, query) {
      return this.ajax(this.buildURL('fileops','listdir'), 'GET', { data: query });
    },
    move:function (store, type, record, query) {
      return this.ajax(this.buildURL('fileops','rename'), 'POST', { data: query });
    },
    updateRecord:function (store, type, record) {
      var query = {
        "path":record.get('path'),
        "mode":record.get('permission')
      };
      return this.ajax(this.buildURL('fileops','chmod'), 'POST', { data: query });
    },
    mkdir:function (store, type, query) {
      return this.ajax(this.buildURL('fileops','mkdir'), 'PUT', { data: query });
    },
    remove:function (store, type, query) {
      return this.ajax(this.buildURL('fileops','remove'), 'DELETE', { data: query });
    },
    moveToTrash:function (store, type, query) {
      return this.ajax(this.buildURL('fileops','moveToTrash'), 'DELETE', { data: query });
    },
    downloadUrl:function (option, query) {
      return [this.buildURL('download',option),Em.$.param(query)].join('?');
    },
    linkFor:function (option, query) {
      return this.ajax(this.buildURL('download',[option,'generate-link'].join('/')), 'POST', { data: query });
    }
  }),
  listdir:function (path) {
    var query = {path: path};
    var type = this.modelFor('file');
    var array = this.recordArrayManager
      .createAdapterPopulatedRecordArray(type, query);
    this.recordArrayManager.registerFilteredRecordArray(array, type);

    var adapter = this.adapterFor(type);

    Ember.assert("You tried to load a query but you have no adapter (for " + type + ")", adapter);
    Ember.assert("You tried to load a query but your adapter does not implement `listdir`", adapter.listdir);

    return promiseArray(_listdir(adapter, this, type, query, array));
  },
  move:function (record, path) {
    var oldpath;
    if (typeof record === 'string') {
      oldpath = record;
    } else {
      oldpath = record.get('id');
    }
    var query = {
      "src":oldpath,
      "dst":path
    };
    var promiseLabel = "DS: Model#move " + this;
    var resolver = Ember.RSVP.defer(promiseLabel);
    var adapter = this.adapterFor(record.constructor);

    resolver.resolve(_move(adapter, this, record, query));

    return DS.PromiseObject.create({ promise: resolver.promise });
  },
  chmod:function (record, path) {
    return record.save();
  },
  mkdir:function (path) {
    var query = {
      "path":path
    };
    var type = this.modelFor('file');
    var promiseLabel = "DS: Model#mkdir " + this;
    var resolver = Ember.RSVP.defer(promiseLabel);
    var adapter = this.adapterFor(type);

    resolver.resolve(_mkdir(adapter, this, type, query));

    return DS.PromiseObject.create({ promise: resolver.promise });
  },
  remove:function (record, toTrash) {
    var query = {
      "path":record.get('path'),
      "recursive":true
    };
    var type = this.modelFor('file');
    var promiseLabel = "DS: Model#remove " + this;
    var resolver = Ember.RSVP.defer(promiseLabel);
    var adapter = this.adapterFor(type);
    
    record.deleteRecord();
    resolver.resolve(_remove(adapter, this, record, query, toTrash));

    return DS.PromiseObject.create({ promise: resolver.promise });
  },
  /**
   * get dowload link
   * @param  {Array} file     records for download
   * @param  {String} option            browse, zip or concat
   * @param  {Boolean} download
   * @return {Promise}
   */
  linkFor:function (files, option, download) {
    var resolver = Ember.RSVP.defer('promiseLabel');
    var adapter = this.adapterFor(this.modelFor('file')),
        download = download || true;
        option = option || "browse";

    if (option == 'browse') {
      var query = { "path": files.get('firstObject.path'), "download": download };
      resolver.resolve(adapter.downloadUrl('browse',query))
      return resolver.promise;
    };

    var query = {
      "entries": [],
      "download": download
    };

    files.forEach(function (item) {
      query.entries.push(item.get('path'));
    });

    resolver.resolve(adapter.linkFor(option, query))

    return resolver.promise.then(function(response) {
      return adapter.downloadUrl(option,response);
    }, function(reason) {
      throw reason;
    });
  }
})

App.FileSerializer = DS.RESTSerializer.extend({
  primaryKey:'path',
  extractArray: function(store, type, payload, id, requestType) {
    payload = {'files': payload};
    return this._super(store, type, payload, id, requestType);
  },
  extractSingle: function(store, type, payload, id, requestType) {
    payload = {'files': payload};
    return this._super(store, type, payload, id, requestType);
  },
  extractChmod:function(store, type, payload, id, requestType) {
    return this.extractSingle(store, type, payload, id, requestType);
  },
});

App.Uploader = Ember.Uploader.create({
  url: '',
  type:'PUT',
  upload: function(file,extraData) {
    var data = this.setupFormData(file,extraData);
    var url  = this.get('url');
    var type = this.get('type');
    var self = this;

    this.set('isUploading', true);
    
    return this.ajax(url, data, type)
      .then(Em.run.bind(this,this.uploadSuccess),Em.run.bind(this,this.uploadFailed));
  },
  uploadSuccess:function(respData) {
    this.didUpload(respData);
    return respData;
  },
  uploadFailed:function (error) {
    this.set('isUploading', false);
    this.sendAlert(error);
    return error;
  },
  sendAlert: Em.K,
  ajax: function(url, params, method) {
    var self = this;
    var settings = {
      url: url,
      type: method || 'POST',
      contentType: false,
      processData: false,
      xhr: function() {
        var xhr = Ember.$.ajaxSettings.xhr();
        xhr.upload.onprogress = function(e) {
          self.didProgress(e);
        };
        return xhr;
      },
      beforeSend:function (xhr) {
        xhr.setRequestHeader('X-Requested-By', 'ambari');
      },
      data: params
    };

    return this._ajax(settings);
  }
});

App.IsodateTransform = DS.Transform.extend({  
  deserialize: function (serialized) {
    if (serialized) {
      return moment.utc(serialized).toDate();
    }
    return serialized;
  },
  serialize: function (deserialized) {
    if (deserialized) {
      return moment(deserialized).format('X');
    }
    return deserialized;
  }
});

Ember.Handlebars.registerBoundHelper('showDate', function(date,format) {
  return moment(date).format(format)
});

Ember.Handlebars.registerBoundHelper('showDateUnix', function(date,format) {
  return moment.unix(date).format(format)
});

Ember.Handlebars.registerBoundHelper('capitalize', function(string) {
  return string.capitalize();
});

Ember.Handlebars.registerBoundHelper('humanSize', function(fileSizeInBytes) {
  var i = -1;
  var byteUnits = [' kB', ' MB', ' GB', ' TB', 'PB', 'EB', 'ZB', 'YB'];
  do {
      fileSizeInBytes = fileSizeInBytes / 1024;
      i++;
  } while (fileSizeInBytes > 1024);

  return Math.max(fileSizeInBytes, 0.1).toFixed(1) + byteUnits[i];
});
