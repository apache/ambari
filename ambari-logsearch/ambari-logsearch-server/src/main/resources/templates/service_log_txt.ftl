<#--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
**********************Summary**********************
Number of Logs : ${numberOfLogs}
From           : ${from}
To             : ${to}
Host           : ${hosts}
Component      : ${components}
Levels         : ${levels}
Format         : ${format}

Included String: [${iString}]

Excluded String: [${eString}]

************************Logs***********************
2016-09-26 11:49:19,723 WARN MainThread lock.py:60 - Releasing the lock.
<#if logs??>
  <#list logs as log>
${log.data}
  </#list>
</#if>
