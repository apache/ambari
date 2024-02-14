"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var ts = require("typescript");
var typeguard_1 = require("./typeguard");
function getChildOfKind(node, kind, sourceFile) {
    for (var _i = 0, _a = node.getChildren(sourceFile); _i < _a.length; _i++) {
        var child = _a[_i];
        if (child.kind === kind)
            return child;
    }
}
exports.getChildOfKind = getChildOfKind;
function isTokenKind(kind) {
    return kind >= ts.SyntaxKind.FirstToken && kind <= ts.SyntaxKind.LastToken ||
        kind === ts.SyntaxKind.JsxText;
}
exports.isTokenKind = isTokenKind;
function isNodeKind(kind) {
    return kind >= ts.SyntaxKind.FirstNode &&
        kind !== ts.SyntaxKind.JsxText;
}
exports.isNodeKind = isNodeKind;
function isAssignmentKind(kind) {
    return kind >= ts.SyntaxKind.FirstAssignment && kind <= ts.SyntaxKind.LastAssignment;
}
exports.isAssignmentKind = isAssignmentKind;
function isTypeNodeKind(kind) {
    return kind >= ts.SyntaxKind.FirstTypeNode && kind <= ts.SyntaxKind.LastTypeNode;
}
exports.isTypeNodeKind = isTypeNodeKind;
function isJsDocKind(kind) {
    return kind >= ts.SyntaxKind.FirstJSDocNode && kind <= ts.SyntaxKind.LastJSDocNode;
}
exports.isJsDocKind = isJsDocKind;
function isThisParameter(parameter) {
    return parameter.name.kind === ts.SyntaxKind.Identifier && parameter.name.originalKeywordKind === ts.SyntaxKind.ThisKeyword;
}
exports.isThisParameter = isThisParameter;
function hasModifier(modifiers) {
    var kinds = [];
    for (var _i = 1; _i < arguments.length; _i++) {
        kinds[_i - 1] = arguments[_i];
    }
    if (modifiers === undefined)
        return false;
    for (var _a = 0, modifiers_1 = modifiers; _a < modifiers_1.length; _a++) {
        var modifier = modifiers_1[_a];
        if (kinds.indexOf(modifier.kind) !== -1)
            return true;
    }
    return false;
}
exports.hasModifier = hasModifier;
function isParameterProperty(node) {
    return hasModifier(node.modifiers, ts.SyntaxKind.PublicKeyword, ts.SyntaxKind.ProtectedKeyword, ts.SyntaxKind.PrivateKeyword, ts.SyntaxKind.ReadonlyKeyword);
}
exports.isParameterProperty = isParameterProperty;
function hasAccessModifier(node) {
    return hasModifier(node.modifiers, ts.SyntaxKind.PublicKeyword, ts.SyntaxKind.ProtectedKeyword, ts.SyntaxKind.PrivateKeyword);
}
exports.hasAccessModifier = hasAccessModifier;
function isFlagSet(obj, flag) {
    return (obj.flags & flag) !== 0;
}
exports.isNodeFlagSet = isFlagSet;
exports.isTypeFlagSet = isFlagSet;
exports.isSymbolFlagSet = isFlagSet;
function isObjectFlagSet(objectType, flag) {
    return (objectType.objectFlags & flag) !== 0;
}
exports.isObjectFlagSet = isObjectFlagSet;
function isModfierFlagSet(node, flag) {
    return (ts.getCombinedModifierFlags(node) & flag) !== 0;
}
exports.isModfierFlagSet = isModfierFlagSet;
function getPreviousStatement(statement) {
    var parent = statement.parent;
    if (typeguard_1.isBlockLike(parent)) {
        var index = parent.statements.indexOf(statement);
        if (index > 0)
            return parent.statements[index - 1];
    }
}
exports.getPreviousStatement = getPreviousStatement;
function getNextStatement(statement) {
    var parent = statement.parent;
    if (typeguard_1.isBlockLike(parent)) {
        var index = parent.statements.indexOf(statement);
        if (index < parent.statements.length)
            return parent.statements[index + 1];
    }
}
exports.getNextStatement = getNextStatement;
function getPreviousToken(node, sourceFile) {
    var parent = node.parent;
    while (parent !== undefined && parent.pos === node.pos)
        parent = parent.parent;
    if (parent === undefined)
        return;
    return findPreviousInternal(parent, node.pos, sourceFile);
}
exports.getPreviousToken = getPreviousToken;
function findPreviousInternal(node, pos, sourceFile) {
    var children = node.getChildren(sourceFile);
    for (var i = children.length - 1; i >= 0; --i) {
        var child = children[i];
        if (child.pos < pos && child.kind !== ts.SyntaxKind.JSDocComment) {
            if (isTokenKind(child.kind))
                return child;
            return findPreviousInternal(child, pos, sourceFile);
        }
    }
}
function getNextToken(node, sourceFile) {
    if (node.kind === ts.SyntaxKind.SourceFile || node.kind === ts.SyntaxKind.EndOfFileToken)
        return;
    var parent = node.parent;
    while (parent.end === node.end) {
        if (parent.parent === undefined)
            return parent.endOfFileToken;
        parent = parent.parent;
    }
    return findNextInternal(parent, node.end, sourceFile);
}
exports.getNextToken = getNextToken;
function findNextInternal(node, end, sourceFile) {
    for (var _i = 0, _a = node.getChildren(sourceFile); _i < _a.length; _i++) {
        var child = _a[_i];
        if (child.end > end && child.kind !== ts.SyntaxKind.JSDocComment) {
            if (isTokenKind(child.kind))
                return child;
            return findNextInternal(child, end, sourceFile);
        }
    }
}
function getPropertyName(propertyName) {
    if (propertyName.kind === ts.SyntaxKind.ComputedPropertyName) {
        if (!typeguard_1.isLiteralExpression(propertyName.expression))
            return;
        return propertyName.expression.text;
    }
    return propertyName.text;
}
exports.getPropertyName = getPropertyName;
function forEachDestructuringIdentifier(pattern, fn) {
    for (var _i = 0, _a = pattern.elements; _i < _a.length; _i++) {
        var element = _a[_i];
        if (element.kind !== ts.SyntaxKind.BindingElement)
            continue;
        var result = void 0;
        if (element.name.kind === ts.SyntaxKind.Identifier) {
            result = fn(element);
        }
        else {
            result = forEachDestructuringIdentifier(element.name, fn);
        }
        if (result)
            return result;
    }
}
exports.forEachDestructuringIdentifier = forEachDestructuringIdentifier;
function forEachDeclaredVariable(declarationList, cb) {
    for (var _i = 0, _a = declarationList.declarations; _i < _a.length; _i++) {
        var declaration = _a[_i];
        var result = void 0;
        if (declaration.name.kind === ts.SyntaxKind.Identifier) {
            result = cb(declaration);
        }
        else {
            result = forEachDestructuringIdentifier(declaration.name, cb);
        }
        if (result)
            return result;
    }
}
exports.forEachDeclaredVariable = forEachDeclaredVariable;
var VariableDeclarationKind;
(function (VariableDeclarationKind) {
    VariableDeclarationKind[VariableDeclarationKind["Var"] = 0] = "Var";
    VariableDeclarationKind[VariableDeclarationKind["Let"] = 1] = "Let";
    VariableDeclarationKind[VariableDeclarationKind["Const"] = 2] = "Const";
})(VariableDeclarationKind = exports.VariableDeclarationKind || (exports.VariableDeclarationKind = {}));
function getVariableDeclarationKind(declarationList) {
    if ((declarationList.flags & ts.NodeFlags.Let) !== 0)
        return 1;
    if ((declarationList.flags & ts.NodeFlags.Const) !== 0)
        return 2;
    return 0;
}
exports.getVariableDeclarationKind = getVariableDeclarationKind;
function isBlockScopedVariableDeclarationList(declarationList) {
    return getVariableDeclarationKind(declarationList) !== 0;
}
exports.isBlockScopedVariableDeclarationList = isBlockScopedVariableDeclarationList;
function isBlockScopedVariableDeclaration(declaration) {
    var parent = declaration.parent;
    return parent.kind === ts.SyntaxKind.CatchClause ||
        isBlockScopedVariableDeclarationList(parent);
}
exports.isBlockScopedVariableDeclaration = isBlockScopedVariableDeclaration;
var ScopeBoundary;
(function (ScopeBoundary) {
    ScopeBoundary[ScopeBoundary["None"] = 0] = "None";
    ScopeBoundary[ScopeBoundary["Function"] = 1] = "Function";
    ScopeBoundary[ScopeBoundary["Block"] = 2] = "Block";
})(ScopeBoundary = exports.ScopeBoundary || (exports.ScopeBoundary = {}));
function isScopeBoundary(node) {
    if (isFunctionScopeBoundary(node))
        return 1;
    if (isBlockScopeBoundary(node))
        return 2;
    return 0;
}
exports.isScopeBoundary = isScopeBoundary;
function isFunctionScopeBoundary(node) {
    switch (node.kind) {
        case ts.SyntaxKind.FunctionExpression:
        case ts.SyntaxKind.ArrowFunction:
        case ts.SyntaxKind.Constructor:
        case ts.SyntaxKind.ModuleDeclaration:
        case ts.SyntaxKind.ClassDeclaration:
        case ts.SyntaxKind.ClassExpression:
        case ts.SyntaxKind.EnumDeclaration:
        case ts.SyntaxKind.MethodDeclaration:
        case ts.SyntaxKind.FunctionDeclaration:
        case ts.SyntaxKind.GetAccessor:
        case ts.SyntaxKind.SetAccessor:
        case ts.SyntaxKind.InterfaceDeclaration:
        case ts.SyntaxKind.TypeAliasDeclaration:
        case ts.SyntaxKind.MethodSignature:
        case ts.SyntaxKind.CallSignature:
        case ts.SyntaxKind.ConstructSignature:
        case ts.SyntaxKind.ConstructorType:
        case ts.SyntaxKind.FunctionType:
        case ts.SyntaxKind.MappedType:
            return true;
        case ts.SyntaxKind.SourceFile:
            return ts.isExternalModule(node);
        default:
            return false;
    }
}
exports.isFunctionScopeBoundary = isFunctionScopeBoundary;
function isBlockScopeBoundary(node) {
    switch (node.kind) {
        case ts.SyntaxKind.Block:
            var parent = node.parent;
            return parent.kind !== ts.SyntaxKind.CatchClause &&
                (parent.kind === ts.SyntaxKind.SourceFile ||
                    !isFunctionScopeBoundary(parent));
        case ts.SyntaxKind.ForStatement:
        case ts.SyntaxKind.ForInStatement:
        case ts.SyntaxKind.ForOfStatement:
        case ts.SyntaxKind.CaseBlock:
        case ts.SyntaxKind.CatchClause:
            return true;
        default:
            return false;
    }
}
exports.isBlockScopeBoundary = isBlockScopeBoundary;
function hasOwnThisReference(node) {
    switch (node.kind) {
        case ts.SyntaxKind.ClassDeclaration:
        case ts.SyntaxKind.ClassExpression:
        case ts.SyntaxKind.FunctionExpression:
            return true;
        case ts.SyntaxKind.FunctionDeclaration:
            return node.body !== undefined;
        case ts.SyntaxKind.MethodDeclaration:
        case ts.SyntaxKind.GetAccessor:
        case ts.SyntaxKind.SetAccessor:
            return node.parent.kind === ts.SyntaxKind.ObjectLiteralExpression;
        default:
            return false;
    }
}
exports.hasOwnThisReference = hasOwnThisReference;
function isFunctionWithBody(node) {
    switch (node.kind) {
        case ts.SyntaxKind.GetAccessor:
        case ts.SyntaxKind.SetAccessor:
        case ts.SyntaxKind.FunctionDeclaration:
        case ts.SyntaxKind.MethodDeclaration:
            return node.body !== undefined;
        case ts.SyntaxKind.FunctionExpression:
        case ts.SyntaxKind.Constructor:
        case ts.SyntaxKind.ArrowFunction:
            return true;
        default:
            return false;
    }
}
exports.isFunctionWithBody = isFunctionWithBody;
function forEachToken(node, cb, sourceFile) {
    if (sourceFile === void 0) { sourceFile = node.getSourceFile(); }
    return (function iterate(child) {
        if (isTokenKind(child.kind))
            return cb(child);
        if (child.kind !== ts.SyntaxKind.JSDocComment)
            return child.getChildren(sourceFile).forEach(iterate);
    })(node);
}
exports.forEachToken = forEachToken;
function forEachTokenWithTrivia(node, cb, sourceFile) {
    if (sourceFile === void 0) { sourceFile = node.getSourceFile(); }
    var fullText = sourceFile.text;
    var notJsx = sourceFile.languageVariant !== ts.LanguageVariant.JSX;
    var scanner = ts.createScanner(sourceFile.languageVersion, false, sourceFile.languageVariant, fullText);
    return forEachToken(node, function (token) {
        var tokenStart = token.getStart(sourceFile);
        var end = token.end;
        if (tokenStart !== token.pos && (notJsx || canHaveLeadingTrivia(token))) {
            scanner.setTextPos(token.pos);
            var position = void 0;
            do {
                var kind = scanner.scan();
                position = scanner.getTextPos();
                cb(fullText, kind, { pos: scanner.getTokenPos(), end: position }, token.parent);
            } while (position < tokenStart);
        }
        return cb(fullText, token.kind, { end: end, pos: tokenStart }, token.parent);
    }, sourceFile);
}
exports.forEachTokenWithTrivia = forEachTokenWithTrivia;
function forEachComment(node, cb, sourceFile) {
    if (sourceFile === void 0) { sourceFile = node.getSourceFile(); }
    var fullText = sourceFile.text;
    var notJsx = sourceFile.languageVariant !== ts.LanguageVariant.JSX;
    return forEachToken(node, function (token) {
        if (notJsx || canHaveLeadingTrivia(token)) {
            var comments = ts.getLeadingCommentRanges(fullText, token.pos);
            if (comments !== undefined)
                for (var _i = 0, comments_1 = comments; _i < comments_1.length; _i++) {
                    var comment = comments_1[_i];
                    cb(fullText, comment);
                }
        }
        if (notJsx || canHaveTrailingTrivia(token)) {
            var comments = ts.getTrailingCommentRanges(fullText, token.end);
            if (comments !== undefined)
                for (var _a = 0, comments_2 = comments; _a < comments_2.length; _a++) {
                    var comment = comments_2[_a];
                    cb(fullText, comment);
                }
        }
    }, sourceFile);
}
exports.forEachComment = forEachComment;
function canHaveLeadingTrivia(_a) {
    var kind = _a.kind, parent = _a.parent;
    if (kind === ts.SyntaxKind.OpenBraceToken)
        return parent.kind !== ts.SyntaxKind.JsxExpression || parent.parent.kind !== ts.SyntaxKind.JsxElement;
    if (kind === ts.SyntaxKind.LessThanToken) {
        if (parent.kind === ts.SyntaxKind.JsxClosingElement)
            return false;
        if (parent.kind === ts.SyntaxKind.JsxOpeningElement || parent.kind === ts.SyntaxKind.JsxSelfClosingElement)
            return parent.parent.parent.kind !== ts.SyntaxKind.JsxElement;
    }
    return kind !== ts.SyntaxKind.JsxText;
}
function canHaveTrailingTrivia(_a) {
    var kind = _a.kind, parent = _a.parent;
    if (kind === ts.SyntaxKind.CloseBraceToken)
        return parent.kind !== ts.SyntaxKind.JsxExpression || parent.parent.kind !== ts.SyntaxKind.JsxElement;
    if (kind === ts.SyntaxKind.GreaterThanToken) {
        if (parent.kind === ts.SyntaxKind.JsxOpeningElement)
            return false;
        if (parent.kind === ts.SyntaxKind.JsxClosingElement || parent.kind === ts.SyntaxKind.JsxSelfClosingElement)
            return parent.parent.parent.kind !== ts.SyntaxKind.JsxElement;
    }
    return kind !== ts.SyntaxKind.JsxText;
}
function endsControlFlow(statement) {
    return getControlFlowEnd(statement) !== 0;
}
exports.endsControlFlow = endsControlFlow;
var StatementType;
(function (StatementType) {
    StatementType[StatementType["None"] = 0] = "None";
    StatementType[StatementType["Break"] = 1] = "Break";
    StatementType[StatementType["Other"] = 2] = "Other";
})(StatementType || (StatementType = {}));
function getControlFlowEnd(statement) {
    while (typeguard_1.isBlockLike(statement)) {
        if (statement.statements.length === 0)
            return 0;
        statement = statement.statements[statement.statements.length - 1];
    }
    return hasReturnBreakContinueThrow(statement);
}
function hasReturnBreakContinueThrow(statement) {
    switch (statement.kind) {
        case ts.SyntaxKind.ReturnStatement:
        case ts.SyntaxKind.ContinueStatement:
        case ts.SyntaxKind.ThrowStatement:
            return 2;
        case ts.SyntaxKind.BreakStatement:
            return 1;
    }
    if (typeguard_1.isIfStatement(statement)) {
        if (statement.elseStatement === undefined)
            return 0;
        var then = getControlFlowEnd(statement.thenStatement);
        if (!then)
            return then;
        return Math.min(then, getControlFlowEnd(statement.elseStatement));
    }
    if (typeguard_1.isSwitchStatement(statement)) {
        var hasDefault = false;
        var type = 0;
        for (var _i = 0, _a = statement.caseBlock.clauses; _i < _a.length; _i++) {
            var clause = _a[_i];
            type = getControlFlowEnd(clause);
            if (type === 1)
                return 0;
            if (clause.kind === ts.SyntaxKind.DefaultClause)
                hasDefault = true;
        }
        return hasDefault && type !== 0 ? 2 : 0;
    }
    return 0;
}
function getLineRanges(sourceFile) {
    var lineStarts = sourceFile.getLineStarts();
    var result = [];
    var length = lineStarts.length;
    var sourceText = sourceFile.text;
    var pos = 0;
    for (var i = 1; i < length; ++i) {
        var end = lineStarts[i];
        result.push({
            pos: pos,
            end: end,
            contentLength: end - pos - (sourceText[end - 2] === '\r' ? 2 : 1),
        });
        pos = end;
    }
    result.push({
        pos: pos,
        end: sourceFile.end,
        contentLength: sourceFile.end - pos,
    });
    return result;
}
exports.getLineRanges = getLineRanges;
var scanner;
function scanToken(text) {
    if (scanner === undefined)
        scanner = ts.createScanner(ts.ScriptTarget.Latest, false);
    scanner.setText(text);
    scanner.scan();
    return scanner;
}
function isValidIdentifier(text) {
    var scan = scanToken(text);
    return scan.isIdentifier() && scan.getTextPos() === text.length;
}
exports.isValidIdentifier = isValidIdentifier;
function isValidPropertyAccess(text) {
    var scan = scanToken(text);
    return scan.getTextPos() === text.length && (scan.isIdentifier() || scan.isReservedWord());
}
exports.isValidPropertyAccess = isValidPropertyAccess;
function isValidPropertyName(text) {
    var scan = scanToken(text);
    return scan.getTextPos() === text.length &&
        (scan.isIdentifier() ||
            scan.isReservedWord() ||
            scan.getToken() === ts.SyntaxKind.NumericLiteral && scan.getTokenValue() === text);
}
exports.isValidPropertyName = isValidPropertyName;
function isValidNumericLiteral(text) {
    var scan = scanToken(text);
    return scan.getToken() === ts.SyntaxKind.NumericLiteral && scan.getTextPos() === text.length;
}
exports.isValidNumericLiteral = isValidNumericLiteral;
function isSameLine(sourceFile, pos1, pos2) {
    return ts.getLineAndCharacterOfPosition(sourceFile, pos1).line
        === ts.getLineAndCharacterOfPosition(sourceFile, pos2).line;
}
exports.isSameLine = isSameLine;
//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJmaWxlIjoidXRpbHMuanMiLCJzb3VyY2VSb290IjoiIiwic291cmNlcyI6WyJ1dGlscy50cyJdLCJuYW1lcyI6W10sIm1hcHBpbmdzIjoiOztBQUFBLCtCQUFpQztBQUNqQyx5Q0FBaUc7QUFFakcsd0JBQStCLElBQWEsRUFBRSxJQUFtQixFQUFFLFVBQTBCO0lBQ3pGLEdBQUcsQ0FBQyxDQUFnQixVQUE0QixFQUE1QixLQUFBLElBQUksQ0FBQyxXQUFXLENBQUMsVUFBVSxDQUFDLEVBQTVCLGNBQTRCLEVBQTVCLElBQTRCO1FBQTNDLElBQU0sS0FBSyxTQUFBO1FBQ1osRUFBRSxDQUFDLENBQUMsS0FBSyxDQUFDLElBQUksS0FBSyxJQUFJLENBQUM7WUFDcEIsTUFBTSxDQUFDLEtBQUssQ0FBQztLQUFBO0FBQ3pCLENBQUM7QUFKRCx3Q0FJQztBQUVELHFCQUE0QixJQUFtQjtJQUMzQyxNQUFNLENBQUMsSUFBSSxJQUFJLEVBQUUsQ0FBQyxVQUFVLENBQUMsVUFBVSxJQUFJLElBQUksSUFBSSxFQUFFLENBQUMsVUFBVSxDQUFDLFNBQVM7UUFDdEUsSUFBSSxLQUFLLEVBQUUsQ0FBQyxVQUFVLENBQUMsT0FBTyxDQUFDO0FBQ3ZDLENBQUM7QUFIRCxrQ0FHQztBQUVELG9CQUEyQixJQUFtQjtJQUMxQyxNQUFNLENBQUMsSUFBSSxJQUFJLEVBQUUsQ0FBQyxVQUFVLENBQUMsU0FBUztRQUNsQyxJQUFJLEtBQUssRUFBRSxDQUFDLFVBQVUsQ0FBQyxPQUFPLENBQUM7QUFDdkMsQ0FBQztBQUhELGdDQUdDO0FBRUQsMEJBQWlDLElBQW1CO0lBQ2hELE1BQU0sQ0FBQyxJQUFJLElBQUksRUFBRSxDQUFDLFVBQVUsQ0FBQyxlQUFlLElBQUksSUFBSSxJQUFJLEVBQUUsQ0FBQyxVQUFVLENBQUMsY0FBYyxDQUFDO0FBQ3pGLENBQUM7QUFGRCw0Q0FFQztBQUVELHdCQUErQixJQUFtQjtJQUM5QyxNQUFNLENBQUMsSUFBSSxJQUFJLEVBQUUsQ0FBQyxVQUFVLENBQUMsYUFBYSxJQUFJ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