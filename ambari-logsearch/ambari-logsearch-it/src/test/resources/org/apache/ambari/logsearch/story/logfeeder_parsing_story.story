Story Service logs are parsed and stored into Solr

Narrative:
As a user
I want to start logsearch/logfeeder/solr components in a docker container with test logs
So that I can parse and store the logs into Solr

Scenario: Logsearch logs are stored into Solr.

Given logsearch docker container
When logfeeder started (parse logs & send data to solr)
Then the number of logsearch_app docs is: 1

Scenario: Zookeeper logs are stored into Solr.

Given logsearch docker container
When logfeeder started (parse logs & send data to solr)
Then the number of zookeeper docs is: 3
