#!/bin/sh

if [ "$1" != '${defaultStackVersion}' ]
then
  STACK=${1%-*}
  VERSION=${1##*-}
  echo "Setting default stack to '$STACK' and version to '$VERSION'"
  sed "s/App.defaultStackVersion.*=.*;/App.defaultStackVersion = '${STACK}-${VERSION}';/" public/javascripts/app.js > public/javascripts/tmp.js; mv public/javascripts/tmp.js public/javascripts/app.js
  sed "s/App.defaultLocalStackVersion.*=.*;/App.defaultLocalStackVersion = '${STACK}Local-${VERSION}';/" public/javascripts/app.js > public/javascripts/tmp.js; mv public/javascripts/tmp.js public/javascripts/app.js
fi
