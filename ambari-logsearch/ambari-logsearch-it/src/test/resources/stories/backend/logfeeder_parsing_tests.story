Story Service logs are parsed and stored into Solr

Narrative:
As a user
I want to start logsearch/logfeeder/solr components in a docker container with test logs
So that I can parse and store the logs into Solr

Scenario: Number of logs for components

Given logsearch docker container
When logfeeder started (parse logs & send data to solr)
Then the number of <component> docs is: <docSize>

Examples:
|component|docSize|
|logsearch_app|1|
|zookeeper|3|
|hst_agent|4|
|secure_log|8|
|system_message|17|
