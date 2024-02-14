# Tsickle - TypeScript to Closure Translator [![Build Status](https://travis-ci.org/angular/tsickle.svg?branch=master)](https://travis-ci.org/angular/tsickle)

Tsickle converts TypeScript code into a form acceptable to the [Closure
Compiler].  This allows using TypeScript to transpile your sources, and then
using Closure Compiler to bundle and optimize them, while taking advantage of
type information in Closure Compiler.

[Closure Compiler]: https://github.com/google/closure-compiler/

## What conversion means

A (non-exhaustive) list of the sorts of transformations Tsickle applies:

- inserts Closure-compatible JSDoc annotations on functions/classes/etc
- converts ES6 modules into `goog.module` modules
- generates externs.js from TypeScript d.ts (and `declare`, see below)
- declares types for class member variables
- translates `export * from ...` into a form Closure accepts
- converts TypeScript enums into a form Closure accepts
- reprocesses all jsdoc to strip Closure-invalid tags

In general the goal is that you write valid TypeScript and Tsickle handles
making it valid Closure Compiler code.

## Installation

- Execute `npm i` to install the dependencies.

## Usage

### Project Setup

Tsickle works by wrapping `tsc`.  To use it, you must set up your project such
that it builds correctly when you run `tsc` from the command line, by
configuring the settings in `tsconfig.json`.

If you have complicated tsc command lines and flags in a build file (like a
gulpfile etc.) Tsickle won't know about it.  Another reason it's nice to put
everything in `tsconfig.json` is so your editor inherits all these settings as
well.

### Invocation

Run `tsickle --help` for the full syntax, but basically you provide any tsickle
specific options and use it as a TypeScript compiler.

### Differences from TypeScript

Closure and TypeScript are not identical.  Tsickle hides most of the
differences, but users must still be aware of some differences.

#### `declare`

Any declaration in a `.d.ts` file, as well as any declaration tagged with
`declare ...`, is intepreted by Tsickle as a name that should be preserved
through Closure compilation (i.e. not renamed into something shorter).  Use it
any time the specific string names of your fields are significant.  That would
most often happen when the object either coming from outside your program, or
being passed out of the program.

Example:

    declare interface JSONResult {
        username: string;
    }
    let r = JSON.parse(input) as JSONResult;
    console.log(r.username);

By adding `declare` to the interface (or if it were in a `.d.ts` file), Tsickle
will inform Closure that it must use exactly the field name `.username` (and not
e.g. `.a`) in the output JS.  This matters for this example because the input
JSON probably uses the string `'username'` and not whatever name Closure would
invent for it.  (Note: `declare` on an interface has no additional meaning in
pure TypeScript.)

#### Exporting decorators

An exporting decorator is a decorator that has `@ExportDecoratedItems` in its
JSDoc.

The name of the element that have an exporting decorator are preserved through
the Closure compilation process.

Example:

    /** @ExportDecoratedItems */
    function myDecorator() {
      // ...
    }

    @myDecorator()
    class DoNotRenameThisClass { ... }

## Development

### Gulp tasks

- `gulp watch` executes the unit tests in watch mode (use `gulp test.unit` for a
  single run),
- `gulp test.e2e` executes the e2e tests,
- `gulp test.check-format` checks the source code formatting using
  `clang-format`,
- `gulp test` runs unit tests, e2e tests and checks the source code formatting.

### Environment variables

Export the environment variable `UPDATE_GOLDENS=1` to have the test suite
rewrite the golden files when you run it.

Export the environment variable `TEST_FILTER`, a regex, to limit the end-to-end
tests (found in `test_files/...`) run tests with a name matching the regex.
