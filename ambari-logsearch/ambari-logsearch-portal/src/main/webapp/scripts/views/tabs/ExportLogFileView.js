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

define(['require',
    'backbone',
    'utils/Utils',
    'hbs!tmpl/tabs/ExportLogFileView_tmpl'
], function(require, Backbone, Utils, ExportLogFileViewTmpl) {
    'use strict';

    return Backbone.Marionette.Layout.extend(
        /** @lends ComparisonLayoutView */
        {
            _viewName: 'ExportLogFileView',
            template: ExportLogFileViewTmpl,
            /**
             * intialize a new LogFileView Layout
             * @constructs
             */
            ui: {
                rowView: "[data-id='rowsData']"
            },
            initialize: function(options) {
                _.extend(this, options);
            },
            onRender: function() {
                if (this.viewType === "aggregatView") {
                    this.ui.rowView.hide();
                }
            },

        });


});
