Scenario: Export documents form solr and upload them to s3 using defult configuration

Given 1000 documents in solr
When start export_audit_logs job
Then Check filenames contains the text audit_logs on s3 server after 20 seconds


Scenario: Exporting 10 documents using writeBlockSize=3 produces 4 files

Given 10 documents in solr with logtime from 2010-10-09T05:00:00.000Z to 2010-10-09T20:00:00.000Z
When start export_audit_logs job with parameters writeBlockSize=3,start=2010-10-09T00:00:00.000Z,end=2010-10-11T00:00:00.000Z
Then Check 4 files exists on s3 server with filenames containing the text solr_archive_audit_logs_-_2010-10-09 after 20 seconds


Scenario: Export job fails when part of the data is exported. After resolving the issue and restarting the job exports the rest of the data.

Given 200 documents in solr with logtime from 2011-10-09T05:00:00.000Z to 2011-10-09T20:00:00.000Z
And a file on s3 with key solr_archive_audit_logs_-_2011-10-09T08:00:00.000Z.json.tar.gz
When start export_audit_logs job with parameters writeBlockSize=20,start=2010-11-09T00:00:00.000Z,end=2011-10-11T00:00:00.000Z
Then Check 3 files exists on s3 server with filenames containing the text solr_archive_audit_logs_-_2011-10-09 after 20 seconds
When delete file with key solr_archive_audit_logs_-_2011-10-09T08:00:00.000Z.json.tar.gz from s3
And restart export_audit_logs job with parameters writeBlockSize=20,start=2010-11-09T00:00:00.000Z,end=2011-10-11T00:00:00.000Z
Then Check 10 files exists on s3 server with filenames containing the text solr_archive_audit_logs_-_2011-10-09 after 20 seconds
