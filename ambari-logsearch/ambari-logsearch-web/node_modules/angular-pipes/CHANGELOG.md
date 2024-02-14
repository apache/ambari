# 6.5.0

## New feature 

* Byte pipes 2.0 [`bytes`](./docs/math.md#bytes)
  + The maximum unit is now the terabyte
  + You can now specify a base unit for the conversion (B, KB, MB, GB, TB)

# 6.4.0 

## New pipes 

* [`decodeURI`](./docs/string.md#decodeuri)
* [`decodeURIComponent`](./docs/string.md#decodeuricomponent)

## Other 

* The library should work with `strictNullChecks`

# 6.3.0 

## New pipes 

* [`takeWhile`](./docs/array.md#takeWhile)
* [`takeUntil`](./docs/array.md#takeUntil)

# 6.2.0

## New pipes 

* [`union`](./docs/array.md#union)

# 6.1.0

## New pipes 

* [`intersection`](./docs/array.md#intersection)

# 6.0.0

## New pipes 

* [`firstOrDefault`](./docs/array.md#firstordefault) [#41](https://github.com/fknop/angular-pipes/issues/41)

## Breaking changes 

* All the `types` pipes have been renamed with a `is` prefix. [#40](https://github.com/fknop/angular-pipes/issues/40)
* The complete list is:
  + `null` has been renamed to `isNull`
  + `undefined` has been renamed to `isUndefined`
  + `nil` has been renamed to `isNil`
  + `number` has been renamed to `isNumber`
  + `string` has been renamed to `isString`
  + `function` has been renamed to `isFunction`
  + `array` has been renamed to `isArray`
  + `object` has been renamed to `isObject`
  + `defined` has been renamed to `isDefined`

* Reason: the `number` pipe was in conflict with the `Angular` built-in `number` pipe so it had to be renamed. To keep consistency between pipes, all type pipes have been renamed with the same prefix.

# 5.8.0 

## Bundle distribution

* `angular-pipes` now distributes UMD bundles. Click [here](./docs/bundles.md) for more information.
* `angular-pipes` now distributes ESM files. Click [here](./docs/esm.md) for more information.

If you encounter any issues related to these bundles / ESM files, please comment on the issue [#37](https://github.com/fknop/angular-pipes/issues/37) created for this.

# 5.7.0

## New pipes 

* [`groupBy`](./docs/aggregate.md#groupby) [#38](https://github.com/fknop/angular-pipes/pull/38)
* [`reverseStr`](./docs/string.md#reversestr) [#38](https://github.com/fknop/angular-pipes/pull/38)

# 5.6.0 

* Removed **ngfactory** files. Only ngsummaries are needed for AoT.
* Switch to `karma-typscript` for testing.
* Moved all spec files next to their source.

# 5.5.0 

* Add noUnusedLocals to tsconfig to improve type checks ([#33](https://github.com/fknop/angular-pipes/pull/33))
* Update Angular to `2.3.1`.

# 5.4.0

* Update Angular to `2.3.0`. `ngc` uses a new feature called `ngsummary`.

# 5.3.0

## New pipes 

* [`with`](./docs/string.md#with) [#31](https://github.com/fknop/angular-pipes/pull/31)
* [`wrap`](./docs/string.md#wrap) [#31](https://github.com/fknop/angular-pipes/pull/31)
* [`latinize`](./docs/string.md#latinize) [#31](https://github.com/fknop/angular-pipes/pull/31)

# 5.2.0

## New pipes

* [`slugify`](./docs/string.md#slugify) [#29](https://github.com/fknop/angular-pipes/pull/29)

# 5.1.0

## New pipes

* [`flatten`](./docs/array.md#flatten)
* [`defaults`](./docs/object.md#defaults)

# 5.0.0

## AoT

* The project is now compiled with `ngc` and should work with `AoT`. Create an issue if you're still having trouble.

## Fixes 

* Fix CountPipe
* Fix EveryPipe test
* Add IsNilPipe to boolean module

## BREAKING CHANGES

* Rename modules (Remove the `2` from `Ng2...`)
  + Angular "2" will now be angular 3 soon with semver, keeping the `2` does not make sense anymore
* Change directory structure
  + The dist folder has been removed. The compiled files are located next to their source.
  + Aggregate pipes are now in their own folder instead of being in the math folder (import change, see docs).


## Tests

* Removed JSPM to keep things simple. It should be easier to contribute.
    + It now use Karma with a simple webpack preprocessor.

# 4.0.0

Support for Angular Final

# 3.0.0

* Update Angular to RC.6
* Remove deprecated tokens.
    + `NG2_PIPES`, `NG2_BOOLEAN_PIPES`, etc.

# 2.2.0

* Update Angular to RC.5
* Add support for `NgModule`
    + Ng2ArrayPipesModule
    + Ng2MathPipesModule
    + Ng2BooleanPipesModule
    + Ng2StringPipesModule
    + Ng2ObjectPipesModule
    + Ng2AggregatePipesModule
    + Ng2PipesModule (imports all the module above)
+ The old token `NG2_PIPES` and the tokens for the categories will be removed for `rc.6` to allow people to migrate easily.
+ The library will keep exporting invidual pipes as we may not need the all category in our application.

# 2.0.0

2.0.0 is mainly an update to angular release candidate with some **breaking changes**.

## Travis CI

* The project now has a Travis CI. It's now easier to contribute with build made for every PR.

## Breaking changes

* Updated to angular 2 RC.
* Moved categories files to src folder, this means:
    + To import a category you now have to do: `import { NG2_STRING_PIPES } from 'angular-pipes/pipes/src/string'` instead of
     `import { NG2_STRING_PIPES } from 'angular-pipes/pipes/string'`
* All the pipes are now **PURE**. This means you have to use `immutability` to update the pipe value. This is a design choice that may be discussed in the future.

# 1.6.0

## New pipes

* `take`
* `drop`
* `deep`

The deep pipe has to be used with other pipes that can work with deep comparaisons.
The pipes working with deep comparaisons (for now) are:
* `uniq`
* `without`

If you need to use deep equal, you can use it like this:

```
{{ collection | deep | uniq }}
```


# 1.5.0

## Breaking change

* Updated to angular2-beta.16. New versions will not work under a lower version than beta.16.

## New pipes

* `pow`
* `sqrt`

## Other

* Closes [#6](https://github.com/fknop/angular-pipes/issues/6)
* Closes [#7](https://github.com/fknop/angular-pipes/issues/7)

# 1.4.0

## New pipes

* `capitalize`
* `upperfirst`
* `template`
* `encodeURI`
* `encodeURIComponent`
* `repeat`
* `truncate`
* `shuffle`
* `random`

# 1.3.0

## New pipes

* `CountPipe`
* `KeysPipe`
* `EveryPipe`
* `SomePipe`
* `ToArrayPipe`
* `NewlinesPipe`
* `DegreesPipe`
* `RadiansPipe`

## Fixes

Fix immutability for pipes that were updating the original input.

## Other

* Added all the documentation in a separate `docs` folder to keep the `README.md` as clean as possible.

# 1.2.0

## New pipes

* `OrderByPipe`
* `ReversePipe`

## Breaking changes

* When the type of the input is not valid, the input is now returned unchanged.


# 1.1.0

## New pipes

* `WherePipe`
* `NilPipe`
* `PluckPipe`
* `MapPipe`
* `RangePipe`

## New features on existing pipes

* `TypeError` array messages are now more consistent.

## Breaking changes

None.

## Other

* Added documentation
* Added tests for
    + `replace`
    + `match`
    + `test`
