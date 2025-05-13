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
(function() {
    var script = document.createElement('script');
    script.type = 'text/javascript';
    script.text=`setTimeout(()=>{const tooltipTriggerList = document.querySelectorAll('[data-bs-toggle="tooltip"]')
    console.log("List is",tooltipTriggerList)
    const tooltipList = [...tooltipTriggerList].map(tooltipTriggerEl => new bootstrap.Tooltip(tooltipTriggerEl))},3000)`
    document.getElementsByTagName('head')[0].appendChild(script);
    })();
    
    (function() {
    var script = document.createElement('script');
    script.type = 'text/javascript';
    script.text = `setTimeout(()=>{const popoverTriggerList = document.querySelectorAll('[data-bs-toggle="popover"]')
    const popoverList = [...popoverTriggerList].map(popoverTriggerEl => new bootstrap.Popover(popoverTriggerEl))},3000)`;
    document.getElementsByTagName('head')[0].appendChild(script);
    })();