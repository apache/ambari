Scenario: Exporting documents form solr and upload them to s3 using defult configuration

Given 1000 documents in solr
When start archive_audit_logs job
Then Check filenames contains the text audit_logs on s3 server after 20 seconds


Scenario: Exporting 10 documents using writeBlockSize=3 produces 4 files

Given 10 documents in solr with logtime from 2010-10-09T05:00:00.000Z to 2010-10-09T20:00:00.000Z
When start archive_audit_logs job with parameters writeBlockSize=3,start=2010-10-09T00:00:00.000Z,end=2010-10-11T00:00:00.000Z after 2 seconds
Then Check 4 files exists on s3 server with filenames containing the text solr_archive_audit_logs_-_2010-10-09 after 20 seconds
And solr does not contain documents between 2010-10-09T05:00:00.000Z and 2010-10-09T20:00:00.000Z after 5 seconds


Scenario: Running archiving job with a bigger start value than end value exports and deletes 0 documents

Given 10 documents in solr with logtime from 2010-01-01T05:00:00.000Z to 2010-01-04T05:00:00.000Z
When start archive_audit_logs job with parameters writeBlockSize=3,start=2010-01-03T05:00:00.000Z,end=2010-01-02T05:00:00.000Z after 2 seconds
Then No file exists on s3 server with filenames containing the text solr_archive_audit_logs_-_2010-01-0
And solr contains 10 documents between 2010-01-01T05:00:00.000Z and 2010-01-04T05:00:00.000Z


Scenario: Archiving job fails when part of the data is exported. After resolving the issue and restarting the job exports the rest of the data.

Given 200 documents in solr with logtime from 2011-10-09T05:00:00.000Z to 2011-10-09T20:00:00.000Z
And a file on s3 with key solr_archive_audit_logs_-_2011-10-09T08-00-00.000Z.json.tar.gz
When start archive_audit_logs job with parameters writeBlockSize=20,start=2010-11-09T00:00:00.000Z,end=2011-10-11T00:00:00.000Z after 2 seconds
Then Check 3 files exists on s3 server with filenames containing the text solr_archive_audit_logs_-_2011-10-09 after 20 seconds
And solr does not contain documents between 2011-10-09T05:00:00.000Z and 2011-10-09T07:59:59.999Z after 5 seconds
When delete file with key solr_archive_audit_logs_-_2011-10-09T08-00-00.000Z.json.tar.gz from s3
And restart archive_audit_logs job within 2 seconds
Then Check 10 files exists on s3 server with filenames containing the text solr_archive_audit_logs_-_2011-10-09 after 20 seconds
And solr does not contain documents between 2011-10-09T05:00:00.000Z and 2011-10-09T20:00:00.000Z after 5 seconds


Scenario: After Deleting job deletes documents from solr no document found in the specified interval

Given 10 documents in solr with logtime from 2012-10-09T05:00:00.000Z to 2012-10-09T20:00:00.000Z
When start delete_audit_logs job with parameters start=2012-10-09T05:00:00.000Z,end=2012-10-09T20:00:00.000Z after 2 seconds
Then solr does not contain documents between 2012-10-09T05:00:00.000Z and 2012-10-09T20:00:00.000Z after 5 seconds


Scenario: Archiving documents to hdfs

Given 1000 documents in solr with logtime from 2014-01-04T05:00:00.000Z to 2014-01-06T20:00:00.000Z
When start archive_audit_logs job with parameters start=2014-01-04T05:00:00.000Z,end=2014-01-06T20:00:00.000Z,destination=HDFS after 2 seconds
Then Check 7 files exists on hdfs with filenames containing the text audit_logs_-_2014-01-0 in the folder /test_audit_logs after 10 seconds
And solr does not contain documents between 2014-01-04T05:00:00.000Z and 2014-01-06T20:00:00.000Z after 10 seconds


Scenario: Archiving documents to local filesystem

Given 200 documents in solr with logtime from 2014-02-04T05:00:00.000Z to 2014-02-06T20:00:00.000Z
When start archive_audit_logs job with parameters start=2014-02-04T05:00:00.000Z,end=2014-02-06T20:00:00.000Z,destination=LOCAL,localDestinationDirectory=/root/archive after 2 seconds
Then Check 2 files exists on local filesystem with filenames containing the text audit_logs_-_2014-02-0 in the folder audit_logs_${jobId}_2014-02-06T20-00-00.000Z for job archive_audit_logs
And solr does not contain documents between 2014-02-04T05:00:00.000Z and 2014-02-06T20:00:00.000Z after 10 seconds


Scenario: Launch Archiving job. Initiate stop and check that part of the data is archived. After restart all data must be extracted.

Given 200 documents in solr with logtime from 2014-03-09T05:00:00.000Z to 2014-03-09T20:00:00.000Z
When start archive_audit_logs job with parameters writeBlockSize=20,start=2014-03-09T05:00:00.000Z,end=2014-03-09T20:00:00.000Z after 2 seconds
And stop job archive_audit_logs after at least 1 file exists in s3 with filename containing text solr_archive_audit_logs_-_2014-03-09 within 10 seconds
Then Less than 10 files exists on s3 server with filenames containing the text solr_archive_audit_logs_-_2014-03-09 after 20 seconds
When restart archive_audit_logs job within 10 seconds
Then Check 10 files exists on s3 server with filenames containing the text solr_archive_audit_logs_-_2014-03-09 after 20 seconds
