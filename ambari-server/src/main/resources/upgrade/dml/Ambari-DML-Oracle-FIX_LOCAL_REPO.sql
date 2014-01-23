--
-- Licensed to the Apache Software Foundation (ASF) under one
-- or more contributor license agreements.  See the NOTICE file
-- distributed with this work for additional information
-- regarding copyright ownership.  The ASF licenses this file
-- to you under the Apache License, Version 2.0 (the
-- 'License'); you may not use this file except in compliance
-- with the License.  You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an 'AS IS' BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

-- Remove HDPLocal in the stack

UPDATE ambari.clusters
  SET desired_stack_version = replace(desired_stack_version, 'HDPLocal', 'HDP')
  WHERE INSTR(desired_stack_version, 'HDPLocal') > 0;

UPDATE ambari.clusterstate
  SET current_stack_version = replace(current_stack_version, 'HDPLocal', 'HDP')
  WHERE INSTR(current_stack_version, 'HDPLocal') > 0;

UPDATE ambari.hostcomponentdesiredstate
  SET desired_stack_version = replace(desired_stack_version, 'HDPLocal', 'HDP')
  WHERE INSTR(desired_stack_version, 'HDPLocal') > 0;

UPDATE ambari.hostcomponentstate
  SET current_stack_version = replace(current_stack_version, 'HDPLocal', 'HDP')
  WHERE INSTR(current_stack_version, 'HDPLocal') > 0;

UPDATE ambari.servicecomponentdesiredstate
  SET desired_stack_version = replace(desired_stack_version, 'HDPLocal', 'HDP')
  WHERE INSTR(desired_stack_version, 'HDPLocal') > 0;

UPDATE ambari.servicedesiredstate
  SET desired_stack_version = replace(desired_stack_version, 'HDPLocal', 'HDP')
  WHERE INSTR(desired_stack_version, 'HDPLocal') > 0;

commit;
