Meta:

Narrative:
As a user
I want to perform queries against Log Search api
So that I can validate the json outputs

Scenario: Log Search API JSON responses

Given logsearch docker container
When LogSearch api query sent: <apiQuery>
Then The api query result is <jsonResult>

Examples:
|apiQuery|jsonResult|
|/api/v1/service/logs/schema/fields|service-log-schema.json|
|/api/v1/service/logs/levels/counts?page=0&pageSize=25&startIndex=0&q=*%3A*|service-log-level-counts-values.json|