Meta:

Narrative:
As a user
I want to start LogSearch services and login to the UI
So that I can validate the proper user

Scenario: login with admin/admin

Given logsearch docker container
And open logsearch home page
When login with admin / admin
Then page contains text: 'Refresh'

Scenario: login with admin and wrong password

Given logsearch docker container
And open logsearch home page
When login with admin / wrongpassword
Then page does not contain text: 'Refresh'