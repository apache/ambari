# Blocking Proxy [![Build Status](https://circleci.com/gh/angular/blocking-proxy.svg?style=shield)](https://circleci.com/gh/angular/blocking-proxy)

# TODO

 - API commands
  - Set stability function and args
 - simplify running e2e tests 
 - set up travis
 - Timeout behavior
 - proper propagation of errors
 - Logging level or log to file

Protractor needs to be able to start this in a separate process - it should
work with all the existing methods of starting up webdriver.

Should pass up errors in a reasonable way.

# Running e2e tests

Start webdriver

    webdriver-manager update
    webdriver-manager start

in another terminal, start the testapp

    npm run testapp 

Start the proxy with 
  
    npm start

in yet another terminal, run the tests

    npm run test:e2e
