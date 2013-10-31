--
-- Licensed to the Apache Software Foundation (ASF) under one
-- or more contributor license agreements.  See the NOTICE file
-- distributed with this work for additional information
-- regarding copyright ownership.  The ASF licenses this file
-- to you under the Apache License, Version 2.0 (the
-- "License"); you may not use this file except in compliance
-- with the License.  You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--


--Adding ON DELETE CASCADE constrain for convenient RCA cleanup
ALTER TABLE workflow
DROP CONSTRAINT workflow_parentworkflowid_fkey,
ADD FOREIGN KEY(parentworkflowid) REFERENCES workflow(workflowid) ON DELETE CASCADE ;

ALTER TABLE job
DROP CONSTRAINT job_workflowid_fkey,
ADD FOREIGN KEY (workflowId) REFERENCES workflow (workflowId) ON DELETE CASCADE;

ALTER TABLE task
DROP CONSTRAINT task_jobid_fkey,
ADD FOREIGN KEY ( jobid ) REFERENCES job ( jobid ) ON DELETE CASCADE;

ALTER TABLE taskAttempt
DROP CONSTRAINT taskattempt_taskid_fkey,
DROP CONSTRAINT taskattempt_jobid_fkey,
ADD FOREIGN KEY (taskid) REFERENCES task(taskid) ON DELETE CASCADE,
ADD FOREIGN KEY ( jobid ) REFERENCES job ( jobid ) ON DELETE CASCADE;
