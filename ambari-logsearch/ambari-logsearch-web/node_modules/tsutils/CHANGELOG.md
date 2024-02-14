# Change Log

## v1.9.1
**Bugfixes:**
* `isObjectFlagSet` now uses the correct `objectFlags` property

## v1.9.0
**Bugfixes:**
* `getNextToken` no longer omits `EndOfFileToken` when there is no trivia before EOF. That means the only inputs where `getNextToken` returns `undefined` are `SourceFile` and `EndOfFileToken`

**Features**:
* Added typeguards for types
* Added utilities for flag checking: `isNodeFlagSet`, `isTypeFlagSet`, `isSymbolFlagSet`,`isObjectFlagSet`, `isModifierFlagSet` 

## v1.8.0
**Features:**
* Support peer dependency of typescript nightlies of 2.4.0
* Added typeguards: `isJsxAttributes`, `isIntersectionTypeNode`, `isTypeOperatorNode`, `isTypePredicateNode`, `isTypeQueryNode`, `isUnionTypeNode`

## v1.7.0
**Bugfixes:**
* `isFunctionScopeBoundary` now handles Interfaces, TypeAliases, FunctionSignatures, etc

**Features:**
* Added utilities: `isThisParameter`, `isSameLine` and `isFunctionWithBody`

## v1.6.0
**Features:**
* Add `isValidPropertyAccess`, `isValidNumericLiteral` and `isValidPropertyName`

## v1.5.0
**Features:**
* Add `isValidIdentifier`

## v1.4.0
**Features:**
* Add `contentLength` property to the result of `getLineRanges`

## v1.3.0
**Bugfixes:**
* canHaveLeadingTrivia:
  * Fix property access on undefined parent reference
  * Fixes: https://github.com/palantir/tslint/issues/2330
* hasOwnThisReference: now includes accessors on object literals

**Features:**
* Typeguards:
  * isTypeParameterDeclaration
  * isEnitityName

## v1.2.2
**Bugfixes:**
* hasOwnThisReference:
  * exclude overload signatures of function declarations
  * add method declarations on object literals

## v1.2.1
**Bugfixes:**
* Fix name of isNumericLiteral

## v1.2.0
**Features:**
* Typeguards:
  * isEnumMember
  * isExpressionWithTypeArguments
  * isImportSpecifier
* Utilities:
  * isJsDocKind, isTypeNodeKind
* Allow typescript@next in peerDependencies

## v1.1.0
**Bugfixes:**
* Fix isBlockScopeBoundary: Remove WithStatement, IfStatment, DoStatement and WhileStatement because they are no scope boundary whitout a block.

**Features:**
* Added more typeguards:
  * isAssertionExpression
  * isEmptyStatement
  * isJsxAttributeLike
  * isJsxOpeningLikeElement
  * isNonNullExpression
  * isSyntaxList
* Utilities:
  * getNextToken, getPreviousToken
  * hasOwnThisReference
  * getLineRanges


## v1.0.0
**Features:**

* Initial implementation of typeguards
* Utilities:
  * getChildOfKind
  * isNodeKind, isAssignmentKind
  * hasModifier, isParameterProperty, hasAccessModifier
  * getPreviousStatement, getNextStatement
  * getPropertyName
  * forEachDestructuringIdentifier, forEachDeclaredVariable
  * getVariableDeclarationKind, isBlockScopedVariableDeclarationList, isBlockScopedVariableDeclaration
  * isScopeBoundary, isFunctionScopeBoundary, isBlockScopeBoundary
  * forEachToken, forEachTokenWithTrivia, forEachComment
  * endsControlFlow