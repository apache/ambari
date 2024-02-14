# Runtime

This is a runtime library for [TypeScript](http://www.typescriptlang.org/) that contains all of the TypeScript helper functions.

# Installing

For the latest stable version:

## npm

```sh
# TypeScript 2.3.3 or later
npm install tslib

# TypeScript 2.3.2 or earlier
npm install tslib@1.6.1
```

## bower

```sh
# TypeScript 2.3.3 or later
bower install tslib

# TypeScript 2.3.2 or earlier
bower install tslib@1.6.1
```

## JSPM

```sh
# TypeScript 2.3.3 or later
jspm install npm:tslib

# TypeScript 2.3.2 or earlier
jspm install npm:tslib@1.6.1
```

# Usage

Set the `importHelpers` compiler option on the command line:

```
tsc --importHelpers file.ts
```

or in your tsconfig.json:

```json
{
    "compilerOptions": {
        "importHelpers": true
    }
}
```

#### For bower and JSPM users

You will need to add a `paths` mapping for `tslib`, e.g. For Bower users:

```json
{
    "compilerOptions": {
        "module": "amd",
        "importHelpers": true,
        "baseUrl": "./",
        "paths": {
            "tslib" : ["bower_components/tslib/tslib.d.ts"]
        }
    }
}
```

For JSPM users:

```json
{
    "compilerOptions": {
        "module": "System",
        "importHelpers": true,
        "baseUrl": "./",
        "paths": {
            "tslib" : ["jspm_packages/npm/tslib@1.7.0/tslib.d.ts"]
        }
    }
}
```


# Contribute

There are many ways to [contribute](https://github.com/Microsoft/TypeScript/blob/master/CONTRIBUTING.md) to TypeScript.
* [Submit bugs](https://github.com/Microsoft/TypeScript/issues) and help us verify fixes as they are checked in.
* Review the [source code changes](https://github.com/Microsoft/TypeScript/pulls).
* Engage with other TypeScript users and developers on [StackOverflow](http://stackoverflow.com/questions/tagged/typescript).
* Join the [#typescript](http://twitter.com/#!/search/realtime/%23typescript) discussion on Twitter.
* [Contribute bug fixes](https://github.com/Microsoft/TypeScript/blob/master/CONTRIBUTING.md).
* Read the language specification ([docx](http://go.microsoft.com/fwlink/?LinkId=267121), [pdf](http://go.microsoft.com/fwlink/?LinkId=267238)).

# Documentation

* [Quick tutorial](http://www.typescriptlang.org/Tutorial)
* [Programming handbook](http://www.typescriptlang.org/Handbook)
* [Language specification](https://github.com/Microsoft/TypeScript/blob/master/doc/spec.md)
* [Homepage](http://www.typescriptlang.org/)
