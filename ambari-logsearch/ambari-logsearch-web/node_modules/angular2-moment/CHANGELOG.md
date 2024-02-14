# Changelog

## 1.7.0 - 2017-08-19
- Add `amFromUtc` pipe ([#163](https://github.com/urish/angular2-moment/pull/163), contributed by [connormlewis](https://github.com/connormlewis))

## 1.6.0 - 2017-07-18
- Add `amLocal` pipe ([#153](https://github.com/urish/angular2-moment/pull/153), contributed by [benwilkins](https://github.com/benwilkins))

## 1.5.0 - 2017-07-14
- Add `amLocale` pipe ([#155](https://github.com/urish/angular2-moment/pull/155), contributed by [FallenRiteMonk](https://github.com/FallenRiteMonk))
- Migrate testing framework to jest

## 1.4.0 - 2017-06-18
- Add `amParse` pipe to enable parsing of custom-formatted date string ([#148](https://github.com/urish/angular2-moment/pull/148), contributed by [vin-car](https://github.com/vin-car))

## 1.3.3 - 2017-03-18
- Fix: `amCalendar` causes protractor to timeout on waiting async Angular ([#135](https://github.com/urish/angular2-moment/pull/135), contributed by [romanovma](https://github.com/romanovma))

## 1.3.2 - 2017-03-17
- Fix: Add missing `amAdd` and `amSubtract` pipes to the NgModule ([#134](https://github.com/urish/angular2-moment/pull/134), contributed by [datencia](https://github.com/datencia))

## 1.3.1 - 2017-03-16
- Add missing `amAdd` and `amSubtract` pipes (fixes [#130](https://github.com/urish/angular2-moment/issues/130))

## 1.3.0 - 2017-03-10
- Enable Angular 4 as peer dependency

## 1.2.0 - 2017-02-09
- Add `amUtc` pipe ([#121](https://github.com/urish/angular2-moment/pull/121), contributed by [bodnarbm](https://github.com/bodnarbm))

## 1.1.0 - 2017-01-09
Happy new year!

- Add `referenceTime` and `format` args to `amCalendar` ([#64](https://github.com/urish/angular2-moment/pull/64), contributed by [irsick](https://github.com/irsick))
- Add `amAdd` and `amSubtract` pipes ([#113](https://github.com/urish/angular2-moment/pull/113), contributed by [dustin486](https://github.com/dustin486))
- Fix: Do not import whole Rx.js library ([#117](https://github.com/urish/angular2-moment/pull/117), contributed by [FabienDehopre](https://github.com/FabienDehopre))

## 1.0.0 - 2016-12-01
Promoted 1.0.0-rc.1 to final release

## 1.0.0-rc.1 - 2016-11-11
*** Breaking change: Requires moment 2.16.0 or newer

- Fix “Expression has changed after it was checked” ([#111](https://github.com/urish/angular2-moment/pull/111), contributed by [nithril](https://github.com/nithril))
- Fix "Module 'moment' has no exported member 'UnitOfTime'" ([#112](https://github.com/urish/angular2-moment/issues/112))

## 1.0.0-beta.6 - 2016-10-24
*** Breaking change: typescript sources are no longer published in the npm package

- Inline sources in the source map file, should fix [#96](https://github.com/urish/angular2-moment/issues/96).
- Handle undefined dates in `amDateFormat` pipe ([#105](https://github.com/urish/angular2-moment/pull/105/files), contributed by [amcdnl](https://github.com/amcdnl))

## 1.0.0-beta.5 - 2016-10-13

*** Breaking change: source files renamed, which could affect your imports:

    import { TimeAgoPipe } from 'angular-moment/TimeAgoPipe';

now becomes:

    import { TimeAgoPipe } from 'angular-moment/time-ago.pipe';

All changes:

- Rename source files to follow [Angular 2 Style Guide conventions](https://angular.io/styleguide#!#02-02)
- Require `moment` >= 2.13.0, and remove `@types/moment` from our dependencies (as it is already included in `moment`)

## 1.0.0-beta.4 - 2016-10-06
- Add support for server side pre-rendering ([#89](https://github.com/urish/angular2-moment/pull/89), contributed by [https://github.com/jmezach](https://github.com/jmezach))
- Fix a bug caused TimeAgo and Calendar pipes not to update automatically ([#94](https://github.com/urish/angular2-moment/pull/94))
- Add `@types/moment` to package dependencies (see [#91](https://github.com/urish/angular2-moment/issues/91))

## 1.0.0-beta.3 - 2016-10-04
- Fix exports for Rollup / Ionic 2 users ([#86](https://github.com/urish/angular2-moment/pull/86), contributed by [TheMadBug](https://github.com/TheMadBug))
- Protractor fix: run long standing timeouts outside of angular zones ([#74](https://github.com/urish/angular2-moment/pull/74), contributed by [tiagoroldao](https://github.com/tiagoroldao))

## 1.0.0-beta.2 - 2016-10-01
- Switch to Typescript 2.0
- Angular 2 AoT (Ahead of Time) template compilation support ([#68](https://github.com/urish/angular2-moment/issues/68))
- Removed impure flags from pure Pipes: `amDateFormat` and `amDifference` ([#75](https://github.com/urish/angular2-moment/pull/75), contributed by [tiagoroldao](https://github.com/tiagoroldao))

## 1.0.0-beta.1 - 2016-08-16
- Support angular-2.0.0-rc.5 NgModules, see [README](README.md) for details. 

## 0.8.2 - 2016-08-01
- Add `amDifference` pipe ([#54](https://github.com/urish/angular2-moment/pull/54), contributed by [josx](https://github.com/josx))

## 0.8.1 - 2016-07-03
- Add `omitSuffix` parameter to `amTimeAgo` pipe ([#47](https://github.com/urish/angular2-moment/pull/47), contributed by [bzums](https://github.com/bzums))

## 0.8.0 - 2016-05-22
- Publish typescript sources under `src` folder, should fix Ionic 2 issues such as [#28](https://github.com/urish/angular2-moment/issues/28) and [#33](https://github.com/urish/angular2-moment/issues/33).

## 0.7.0 - 2016-05-03
- Align with the angular 2.0.0-rc.0 and the new angular packaging system 

## 0.6.0 - 2016-04-28
- Align with angular 2.0.0-beta.16 ([#32](https://github.com/urish/angular2-moment/pull/32), contributed by [fknop](https://github.com/fknop))

## 0.5.0 - 2016-04-08
- Move `angular2` from npm `dependencies` to `peerDependencies` (see [#24](https://github.com/urish/angular2-moment/pull/24))
- Add `amDuration` pipe ([#29](https://github.com/urish/angular2-moment/pull/29), contributed by [xenolinguist](https://github.com/xenolinguist))

## 0.4.3 - 2016-03-06
- include `amFromUnix` pipe in the package's exports
- publish our `typings.json` to npm 

## 0.4.2 - 2016-02-24
- add `amFromUnix` pipe ([#16](https://github.com/urish/angular2-moment/pull/16), contributed by [lanocturne](https://github.com/lanocturne))

## 0.4.1 - 2016-02-21
- Don't run `typings install` on postinstall (fixes [#13](https://github.com/urish/angular2-moment/issues/13))

## 0.4.0 - 2016-02-16
- Switch from `tsd` to `typings`, stop publishing the `moment.js` typings to npm. 
- Additional unit-tests

Note: You may need to manually install moment.js typings, by running `typings install --save moment` in your project directory.

## 0.3.0 - 2016-01-27
- add `amDateFormat` pipe ([#9](https://github.com/urish/angular2-moment/pull/9), contributed by [andreialecu](https://github.com/andreialecu))
- refactor: remove the `supports()` from all the pipes (it is no longer used as of angular2-beta)

## 0.2.1 - 2016-01-16
- bugfix: wrong method name for cleanup, caused resource leak ([#8](https://github.com/urish/angular2-moment/pull/8), contributed by [andreialecu](https://github.com/andreialecu))

## 0.2.0 - 2016-01-12
- add `amCalendar` pipe ([#6](https://github.com/urish/angular2-moment/pull/6), contributed by [andreialecu](https://github.com/andreialecu))

## 0.1.1 - 2015-12-18
- Fix 'Cannot use in app due to triple-slash references' typescript error ([#2](https://github.com/urish/angular2-moment/issues/2))

## 0.1.0 - 2015-12-15
- Align with angular 2.0.0-beta.0

## 0.0.5 - 2015-11-12
- Align with angular-2.0.0-alpha.46

## 0.0.4 - 2015-10-25
- Add ES5 transpiled version and typescript definitions (.d.ts) file to the published npm package

## 0.0.3 - 2015-10-22
- Align with angular-2.0.0-alpha.44

## 0.0.2 - 2015-09-18
- Align with angular-2.0.0-alpha.37

## 0.0.1 - 2015-08-25

- Initial release
