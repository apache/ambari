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



-- Removing current foreign key constraints.
DECLARE
  l_statement VARCHAR2(32676);
BEGIN
  SELECT 'ALTER TABLE WORKFLOW DROP CONSTRAINT ' || constraint_name
  INTO l_statement
  FROM user_cons_columns
  WHERE table_name = 'WORKFLOW' AND column_name = 'PARENTWORKFLOWID';
  EXECUTE IMMEDIATE l_statement;

  SELECT 'ALTER TABLE JOB DROP CONSTRAINT ' || constraint_name
  INTO l_statement
  FROM user_cons_columns
  WHERE table_name = 'JOB' AND column_name = 'WORKFLOWID';
  EXECUTE IMMEDIATE l_statement;

  SELECT 'ALTER TABLE TASK DROP CONSTRAINT ' || constraint_name
  INTO l_statement
  FROM user_cons_columns
  WHERE table_name = 'TASK' AND column_name = 'JOBID';
  EXECUTE IMMEDIATE l_statement;

  SELECT 'ALTER TABLE TASKATTEMPT DROP CONSTRAINT ' || constraint_name
  INTO l_statement
  FROM user_cons_columns
  WHERE table_name = 'TASKATTEMPT' AND column_name = 'TASKID';
  EXECUTE IMMEDIATE l_statement;

  SELECT 'ALTER TABLE TASKATTEMPT DROP CONSTRAINT ' || constraint_name
  INTO l_statement
  FROM user_cons_columns
  WHERE table_name = 'TASKATTEMPT' AND column_name = 'JOBID';
  EXECUTE IMMEDIATE l_statement;

END;
/

COMMIT;

--Adding ON DELETE CASCADE foreign key constraints for convenient RCA cleanup
ALTER TABLE workflow
ADD FOREIGN KEY(parentworkflowid) REFERENCES workflow(workflowid) ON DELETE CASCADE ;

ALTER TABLE job
ADD FOREIGN KEY (workflowId) REFERENCES workflow (workflowId) ON DELETE CASCADE;

ALTER TABLE task
ADD FOREIGN KEY ( jobid ) REFERENCES job ( jobid ) ON DELETE CASCADE;

ALTER TABLE taskAttempt
ADD FOREIGN KEY (taskid) REFERENCES task(taskid) ON DELETE CASCADE;

ALTER TABLE taskAttempt
ADD FOREIGN KEY ( jobid ) REFERENCES job ( jobid ) ON DELETE CASCADE;


COMMIT;
