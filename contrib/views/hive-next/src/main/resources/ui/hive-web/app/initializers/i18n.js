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
  initialize: function () {
    Ember.ENV.I18N_COMPILE_WITHOUT_HANDLEBARS = true;
    Ember.FEATURES.I18N_TRANSLATE_HELPER_SPAN = false;
    Ember.I18n.translations = TRANSLATIONS;
    Ember.TextField.reopen(Ember.I18n.TranslateableAttributes);
  }
};

TRANSLATIONS = {
  tooltips: {
    refresh: 'Refresh database',
    loadSample: 'Load sample data',
    query: 'Query',
    settings: 'Settings',
    visualExplain: 'Visual Explain',
    tez: 'Tez',
    visualization: 'Visualization',
    notifications: 'Notifications',
    expand: 'Expand query panel',
    makeSettingGlobal: 'Make setting global',
    overwriteGlobalValue: 'Overwrite global setting value'
  },

  alerts: {
    errors: {
      save: {
        query: "Error when trying to execute the query",
        results: "Error when trying to save the results."
      },
      get: {
        tables: 'Error when trying to retrieve the tables for the selected database',
        columns: 'Error when trying to retrieve the table columns.'
      },
      sessions: {
        delete: 'Error invalidating sessions'
      },
      job: {
        status: "An error occured while processing the job."
      }
    },
    success: {
      sessions: {
        deleted: 'Session invalidated.'
      },
      settings: {
        saved: 'Settings have been saved.'
      },
      query: {
        execution: 'Query has been submitted.',
        rerun: "Password updated, please execute the query again",
        save: 'The query has been saved.',
        update: 'The query has been updated.'
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
      message: 'Enter name:',
      overwrite: 'Saving will overwrite previously saved query'
    },

    download: {
      csv: 'Download results as CSV',
      hdfs: 'Please enter save path and name'
    },

    downloadQuery: {
      heading: 'Download Query'
    },

    changeTitle: {
      heading: 'Rename worksheet'
    },
    authenticationLDAP: {
       heading: 'Enter the LDAP password'
    }
  },

  titles: {
    database: 'Database Explorer',
    explorer: 'Databases',
    results: 'Search Results',
    settings: 'Database Settings',
    query: {
      tab: 'Worksheet',
      editor: 'Query Editor',
      process: 'Query Process Results',
      parameters: 'Parameters',
      visualExplain: 'Visual Explain',
      tez: 'TEZ',
      status: 'Status: ',
      messages: 'Messages',
      visualization: 'Visualization'
    },
    download: 'Save results...',
    tableSample: '{{tableName}} sample'
  },

  placeholders: {
    search: {
      tables: 'Search tables...',
      columns: 'Search columns in result tables...',
      results: 'Filter columns...'
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
    uploadTable: 'Upload Table',
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
    downloadQuery: 'Download',
    newQuery: 'New Worksheet',
    uploadQuery: 'Upload',
    newUdf: 'New UDF',
    history: 'History',
    ok: 'OK',
    stopJob: 'Stop execution',
    stoppingJob: 'Stopping...',
    close: 'Close',
    clearFilters: 'Clear filters',
    refresh: 'Refresh',
    expand: 'Expand message',
    collapse: 'Collapse message',
    previousPage: 'previous',
    uploadTable: 'Upload Table',
    showPreview: 'Preview',
    nextPage: 'next',
    loadMore: 'Load more...',
    saveHdfs: 'Save to HDFS',
    saveCsv: 'Download as CSV',
    runOnTez: 'Run on Tez',
    killSession: 'Kill Session'
  },

  labels: {
    noTablesMatch: 'No tables match',
    noColumnsMatch: 'No columns match',
    table: 'Table ',
    hrsShort: "{{hours}} hrs",
    minsShort: "{{minutes}} mins",
    secsShort: "{{seconds}} secs"
  },

  popover: {
    visualExplain: {
      statistics: "Statistics"
    },
    queryEditorHelp: {
      title: "Did you know?",
      content: {
        line1: "Press CTRL + Space to autocomplete",
        line2: "You can execute queries with multiple SQL statements delimited by a semicolon ';'",
        line3: "You can highlight and run a fragment of a query"
      }
    },
    add: 'Add'
  },

  tez: {
    errors: {
      'not.deployed': "Tez View isn't deployed.",
      'no.instance': "No instance of Tez View found.",
      'no.dag': "No DAG available"
    }
  },

  hive: {
    errors: {
      'no.query': "No query to process.",
      'emptyDatabase' : "Please select {{ database }}.",
      'emptyTableName' : "Please enter {{ tableNameField }}.",
      'illegalTableName': "Illegal {{ tableNameField }} : '{{ tableName }}'",
      'emptyIsFirstRow' : "{{isFirstRowHeaderField}} cannot be null.",
      'emptyHeaders': "Headers (containing column names) cannot be null.",
      'emptyColumnName': "Column name cannot be null.",
      'illegalColumnName': "Illegal column name : '{{columnName}}' in column number {{index}}",
      'emptyHdfsPath': "HdfsPath Name cannot be null or empty.",
      'illegalHdfPath': "Illegal hdfs path : {{hdfsPath}}"
    },
    messages : {
      'generatingPreview':"Generating Preview.",
      'startingToCreateActualTable' : "Starting to create Actual table",
      'waitingToCreateActualTable' : "Waiting for creation of Actual table",
      'successfullyCreatedActualTable' : "Successfully created Actual table.",
      'failedToCreateActualTable' : "Failed to create Actual table.",
      'startingToCreateTemporaryTable' : "Starting to create Temporary table.",
      'waitingToCreateTemporaryTable' : "Waiting for creation of Temporary table.",
      'successfullyCreatedTemporaryTable' : "Successfully created Temporary table.",
      'failedToCreateTemporaryTable' : " Failed to create temporary table.",
      'deletingTable' :  "Deleting {{table}} table.",
      'succesfullyDeletedTable' :  "Successfully deleted {{ table}} table.",
      'failedToDeleteTable' :  "Failed to delete {{table}} table.",
      'startingToUploadFile' :  "Starting to upload the file.",
      'waitingToUploadFile' :  "Waiting for uploading file.",
      'successfullyUploadedFile' :  "Successfully uploaded file.",
      'failedToUploadFile' :  "Failed to upload file.",
      'startingToInsertRows' :  "Starting to insert rows from temporary table to actual table.",
      'waitingToInsertRows' :  "Waiting for insertion of rows from temporary table to actual table.",
      'successfullyInsertedRows' :  "Successfully inserted rows from temporary table to actual table.",
      'failedToInsertRows' :  "Failed to insert rows from temporary table to actual table.",
      'startingToDeleteTemporaryTable' :  "Starting to delete temporary table.",
      'waitingToDeleteTemporaryTable' :  "Waiting for deletion of temporary table.",
      'successfullyDeletedTemporaryTable' :  "Successfully deleted temporary table",
      'manuallyDeleteTable' :  "You will have to manually delete the table {{databaseName}}.{{tableName}}",
      'uploadingFromHdfs' :  "Uploading file from HDFS ",
      'successfullyUploadedTableMessage' : "Table {{tableName}} created in database {{databaseName}}",
      'successfullyUploadedTableHeader' : "Uploaded Successfully"
    },
    words :{
      temporary : "Temporary",
      actual : "Actual",
      database : "Database"
    },
    ui : {
      'uploadProgress' : "Upload Progress",
      'uploading': "Uploading..",
      'uploadFromLocal':"Upload from Local",
      'uploadFromHdfs':"Upload from HDFS",
      'selectFileType':"Select File Type",
      'fileType':"File type",
      'selectFromLocal':"Select from local",
      'hdfsPath':"HDFS Path",
      'selectDatabase':"Select a Database",
      'tableName':"Table name",
      'tableNameErrorMessage':"Only alphanumeric and underscore characters are allowed in table name.",
      'tableNameTooltip':"Enter valid (alphanumeric + underscore) table name.",
      'storedAs':"Stored as",
      'isFirstRowHeader':"Is first row header ?",
      'columnNameTooltip':"Enter valid (alphanumeric + underscore) column name.",
      'columnNameErrorMessage':"Only alphanumeric and underscore characters are allowed in column names.",
      'hdfsFieldTooltip':"Enter full HDFS path",
      'hdfsFieldPlaceholder':"Enter full HDFS path",
      'hdfsFieldErrorMessage':"Please enter complete path of hdfs file to upload.",
      'containsEndlines': "Contains endlines?",
      'columnDelimiterTooltip': "Delimiter for the column values. Default is comman (,).",
      'escapeCharacterTooltip': "Escape character. Default is backslash (\).",
      'quoteCharacterTooltip': 'Quote character. Default is double quote (").',
      'quoteCharacterField': "Quote Character",
      'escapeCharacterField': "Escape Character",
      'columnDelimterField': "Field Delimiter",
      'fieldsTerminatedByField': "Fields Terminated By",
      'escapedByField': "Escape By",
      'escapedByTooltip': "Escaped By character for Hive table.",
      'fieldsTerminatedByTooltip': "Fields Terminated By character for Hive table.",
      'isFirstRowHeaderTooltip': "Check if the first row of CSV is a header."
    }
  },

  emptyList: {
    history: {
      noItems: "No queries were run.",
      noMatches: "No jobs match your filtering criteria",
    },
    savedQueries: {
      noItems: "No queries were saved.",
      noMatches: "No queries match your filtering criteria"
    }
  },

  settings: {
    parsed: "Query settings added"
  },

  generalError: 'Unexpected error'
};
