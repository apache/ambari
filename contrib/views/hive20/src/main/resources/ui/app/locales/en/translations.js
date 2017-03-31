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

export default {
  "hive": {
    "ui": {
      "fileSource": {
        'uploadFromLocal': "Upload from Local",
        'uploadFromHdfs': "Upload from HDFS",
        'selectFileType': "Select File Type",
        'fileType': "File type",
        "selectHdfsLocation": "Select HDFS Directory",
        "enterHdfsPathLabel": "Enter Hdfs Path",
        "selectLocalFileLabel": "Select Local File",
      },
      "csvFormatParams": {
        'columnDelimterField': "Field Delimiter",
        'columnDelimiterTooltip': "Delimiter for the column values. Default is comman (,).",
        'escapeCharacterField': "Escape Character",
        'escapeCharacterTooltip': "Escape character. Default is backslash (\).",
        'quoteCharacterTooltip': 'Quote character. Default is double quote (").',
        'quoteCharacterField': "Quote Character",
        'isFirstRowHeader': "Is first row header?",
        'fieldsTerminatedByTooltip': "Fields Terminated By character for Hive table.",
        'isFirstRowHeaderTooltip': "Check if the first row of CSV is a header.",
        'containsEndlines': "Contains endlines?",
      },
      "uploadTable": {
        'uploadProgress': "Upload Progress",
        'uploading': "Uploading..",
        'selectFromLocal': "Select from local",
        'hdfsPath': "HDFS Path",
        'tableName': "Table name",
        'tableNameErrorMessage': "Only alphanumeric and underscore characters are allowed in table name.",
        'tableNameTooltip': "Enter valid (alphanumeric + underscore) table name.",
        'columnNameErrorMessage': "Only alphanumeric and underscore characters are allowed in column names.",
        'hdfsFieldTooltip': "Enter full HDFS path",
        'hdfsFieldPlaceholder': "Enter full HDFS path",
        'hdfsFieldErrorMessage': "Please enter complete path of hdfs file to upload.",
        'showPreview': "Preview"
      }
    },
    words :{
      temporary : "Temporary",
      actual : "Actual",
      database : "Database"
    },
    errors: {
      'no.query': "No query to process.",
      'emptyDatabase': "Please select {{ database }}.",
      'emptyTableName': "Please enter {{ tableNameField }}.",
      'illegalTableName': "Illegal {{ tableNameField }} : '{{ tableName }}'",
      'emptyIsFirstRow': "{{isFirstRowHeaderField}} cannot be null.",
      'emptyHeaders': "Headers (containing column names) cannot be null.",
      'emptyColumnName': "Column name cannot be null.",
      'illegalColumnName': "Illegal column name : '{{columnName}}' in column number {{index}}",
      'emptyHdfsPath': "HdfsPath Name cannot be null or empty.",
      'illegalHdfPath': "Illegal hdfs path : {{hdfsPath}}"
    },
    messages: {
      'generatingPreview': "Generating Preview.",
      'startingToCreateActualTable': "Creating Actual table",
      'waitingToCreateActualTable': "Waiting for creation of Actual table",
      'successfullyCreatedActualTable': "Successfully created Actual table.",
      'failedToCreateActualTable': "Failed to create Actual table.",
      'startingToCreateTemporaryTable': "Creating Temporary table.",
      'waitingToCreateTemporaryTable': "Waiting for creation of Temporary table.",
      'successfullyCreatedTemporaryTable': "Successfully created Temporary table.",
      'failedToCreateTemporaryTable': " Failed to create temporary table.",
      'deletingTable': "Deleting {{table}} table.",
      'succesfullyDeletedTable': "Successfully deleted {{ table}} table.",
      'failedToDeleteTable': "Failed to delete {{table}} table.",
      'startingToUploadFile': "Uploading file.",
      'waitingToUploadFile': "Waiting for uploading file.",
      'successfullyUploadedFile': "Successfully uploaded file.",
      'failedToUploadFile': "Failed to upload file.",
      'startingToInsertRows': "Inserting rows from temporary table to actual table.",
      'waitingToInsertRows': "Waiting for insertion of rows from temporary table to actual table.",
      'successfullyInsertedRows': "Successfully inserted rows from temporary table to actual table.",
      'failedToInsertRows': "Failed to insert rows from temporary table to actual table.",
      'startingToDeleteTemporaryTable': "Deleting temporary table.",
      'waitingToDeleteTemporaryTable': "Waiting for deletion of temporary table.",
      'successfullyDeletedTemporaryTable': "Successfully deleted temporary table",
      'manuallyDeleteTable': "You will have to manually delete the table {{databaseName}}.{{tableName}}",
      'uploadingFromHdfs': "Uploading file from HDFS ",
      'successfullyUploadedTableMessage': "Table {{tableName}} created in database {{databaseName}}",
      'successfullyUploadedTableHeader': "Uploaded Successfully"
    },
  }
};
