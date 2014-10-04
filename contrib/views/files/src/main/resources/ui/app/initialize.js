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

//////////////////////////////////
// Adapter
//////////////////////////////////

require('adapter');

//////////////////////////////////
// Templates
//////////////////////////////////

require('templates/application');
require('templates/index');
require('templates/files');
require('templates/error');
require('templates/util/errorRow');
require('templates/util/fileRow');

require('templates/components/uploader');
require('templates/components/renameInput');
require('templates/components/deletePopover');
require('templates/components/mkdirInput');
require('templates/components/contextMenu');
require('templates/components/deleteBulk');
require('templates/components/chmodInput');

//////////////////////////////////
// Models
//////////////////////////////////

require('models/file');

/////////////////////////////////
// Controllers
/////////////////////////////////

require('controllers/files');
require('controllers/file');
require('controllers/error');
require('controllers/filesAlert');

/////////////////////////////////
// Components
/////////////////////////////////

require('components/uploader');
require('components/contextMenu');
require('components/renameInput');
require('components/bsPopover');
require('components/confirmDelete');
require('components/sortArrow');
require('components/breadCrumbs');
require('components/popoverDelete');
require('components/bulkCheckbox');
require('components/mkdirInput');
require('components/toggleContext');
require('components/chmodInput');

/////////////////////////////////
// Views
/////////////////////////////////

require('views/file');
require('views/filesAlert');

/////////////////////////////////
// Routes
/////////////////////////////////

require('routes/file');
require('routes/error');

/////////////////////////////////
// Router
/////////////////////////////////

require('router');
