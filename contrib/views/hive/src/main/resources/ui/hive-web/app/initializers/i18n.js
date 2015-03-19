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

var TRANSLATIONS;

export default {
  name: 'i18n',
  initialize: function() {
    Ember.ENV.I18N_COMPILE_WITHOUT_HANDLEBARS = true;
    Ember.FEATURES.I18N_TRANSLATE_HELPER_SPAN = false;
    Ember.I18n.translations = TRANSLATIONS;
    Ember.TextField.reopen(Ember.I18n.TranslateableAttributes);
  }
};

TRANSLATIONS = {
  alerts: {
    errors: {
      save: {
        query: "Error when trying to execute the query",
        results: "Error when trying to save the results."
      },
      get: {
        tables: 'Error when trying to retrieve the tables for the selected database',
        columns: 'Error when trying to retrieve the table columns.'
      }
    }
  },
  modals: {
    delete: {
      heading: 'Confirm deletion',
      message: 'Are you sure you want to delete this item?',
      emptyQueryMessage: "Your query is empty. Do you want to delete this item?"
    },

    save: {
      heading: 'Saving item',
      saveBeforeCloseHeading: "Save item before closing?",
      message: 'Enter name:'
    }
  },
  titles: {
    database: 'Database Explorer',
    explorer: 'Databases',
    results: 'Search Results',
    settings: 'Database Settings',
    query: {
      editor: 'Query Editor',
      process: 'Query Process Results',
      parameters: 'Parameters',
      visualExplain: 'Visual Explain',
      tez: 'TEZ'
    },
    download: 'Save results...'
  },
  placeholders: {
    search: {
      tables: 'Search tables...',
      columns: 'Search columns in result tables...'
    },
    select: {
      database: 'Select Database...',
      udfs: 'Insert udfs',
      file: 'Select File Resource...',
      noFileResource: '(no file)',
      value: "Select value..."
    },
    fileResource: {
      name: "resource name",
      path: "resource path"
    },
    udfs: {
      name: 'udf name',
      className: 'udf class name',
      path: "resource path",
      database: 'Select Database...'
    },
    settings: {
      key: 'mapred.reduce.tasks',
      value: '1'
    }
  },
  menus: {
    query: 'Query',
    savedQueries: 'Saved Queries',
    history: 'History',
    udfs: 'UDFs',
    logs: 'Logs',
    results: 'Results',
    explain: 'Explain'
  },
  columns: {
    id: 'id',
    shortQuery: 'preview',
    fileResource: 'file resource',
    title: 'title',
    database: 'database',
    owner: 'owner',
    user: 'user',
    date: 'date submitted',
    duration: 'duration',
    status: 'status',
    expand: '',
    actions: ''
  },
  buttons: {
    addItem: 'Add new item...',
    insert: 'Insert',
    delete: 'Delete',
    cancel: 'Cancel',
    edit: 'Edit',
    execute: 'Execute',
    explain: 'Explain',
    saveAs: 'Save as...',
    save: 'Save',
    newQuery: 'New Query',
    newUdf: 'New UDF',
    history: 'History',
    ok: 'OK',
    stopJob: 'Stop execution',
    close: 'Close',
    clearFilters: 'Clear filters',
    expand: 'Expand message',
    collapse: 'Collapse message',
    previousPage: 'previous',
    nextPage: 'next',
    loadMore: 'Load more...',
    saveHdfs: 'Save to HDFS',
    saveCsv: 'Download as CSV',
    runOnTez: 'Run on Tez'
  },
  labels: {
    noTablesMatches: 'No tables matches for'
  },
  popover: {
    queryEditorHelp: {
      title: "Did you know?",
      content: {
        line1: "Press CTRL + Space to autocomplete",
        line2: "You can execute queries with multiple SQL statements delimited by a semicolon ';'",
        line3: "You can highlight and run a fragment of a query"
      }
    },
    add: 'Add'
  }
};
