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

/*
  Common values that can be used throughout the application.
*/
var App = require('app');

//These can be used, for example, to bind the Boolean values of true and false to element attributes in templates,
//which will enable comparisons against Boolean values from code to be performed correctly against the atttributes.
//(Otherwise, you would end up setting attributes to the string values "true" and "false", which both evaluate to Boolean true.)
App.False = false;
App.True = true;