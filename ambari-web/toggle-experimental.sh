#!/bin/sh

if [ "$1" == "true" ]
then
  echo 'Enabling experimental features...'
  sed 's/App.enableExperimental.*=.*;/App.enableExperimental = true;/' public/javascripts/app.js > public/javascripts/tmp.js; mv public/javascripts/tmp.js public/javascripts/app.js
fi
