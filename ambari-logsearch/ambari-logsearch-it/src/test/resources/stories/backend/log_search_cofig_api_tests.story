Scenario: scenario description

Given logsearch docker container
When LogSearch api request sent: /api/v1/shipper/input/cl1/services/ambari
Then Result is an input.config of ambari_audit with log file path /root/test-logs/ambari-server/ambari-audit.log

Given logsearch docker container
When Update input config of ambari_audit path to /root/test-logs/ambari-server/ambari-audit.log.1 at /api/v1/shipper/input/cl1/services/ambari
Then Result is an input.config of ambari_audit with log file path /root/test-logs/ambari-server/ambari-audit.log.1

Given logsearch docker container
When Update input config with data {"unknownField":[]} at /api/v1/shipper/input/cl1/services/ambari
Then Result is status code 400

Given logsearch docker container
When Update input config with data not_a_json at /api/v1/shipper/input/cl1/services/ambari
Then Result is status code 400
