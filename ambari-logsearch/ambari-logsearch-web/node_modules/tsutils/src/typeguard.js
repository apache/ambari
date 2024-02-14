"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var ts = require("typescript");
function isAccessorDeclaration(node) {
    return node.kind === ts.SyntaxKind.GetAccessor ||
        node.kind === ts.SyntaxKind.SetAccessor;
}
exports.isAccessorDeclaration = isAccessorDeclaration;
function isArrayBindingPattern(node) {
    return node.kind === ts.SyntaxKind.ArrayBindingPattern;
}
exports.isArrayBindingPattern = isArrayBindingPattern;
function isArrayLiteralExpression(node) {
    return node.kind === ts.SyntaxKind.ArrayLiteralExpression;
}
exports.isArrayLiteralExpression = isArrayLiteralExpression;
function isArrayTypeNode(node) {
    return node.kind === ts.SyntaxKind.ArrayType;
}
exports.isArrayTypeNode = isArrayTypeNode;
function isArrowFunction(node) {
    return node.kind === ts.SyntaxKind.ArrowFunction;
}
exports.isArrowFunction = isArrowFunction;
function isAsExpression(node) {
    return node.kind === ts.SyntaxKind.AsExpression;
}
exports.isAsExpression = isAsExpression;
function isAssertionExpression(node) {
    return node.kind === ts.SyntaxKind.AsExpression ||
        node.kind === ts.SyntaxKind.TypeAssertionExpression;
}
exports.isAssertionExpression = isAssertionExpression;
function isAwaitExpression(node) {
    return node.kind === ts.SyntaxKind.AwaitExpression;
}
exports.isAwaitExpression = isAwaitExpression;
function isBinaryExpression(node) {
    return node.kind === ts.SyntaxKind.BinaryExpression;
}
exports.isBinaryExpression = isBinaryExpression;
function isBindingElement(node) {
    return node.kind === ts.SyntaxKind.BindingElement;
}
exports.isBindingElement = isBindingElement;
function isBindingPattern(node) {
    return node.kind === ts.SyntaxKind.ArrayBindingPattern ||
        node.kind === ts.SyntaxKind.ObjectBindingPattern;
}
exports.isBindingPattern = isBindingPattern;
function isBlock(node) {
    return node.kind === ts.SyntaxKind.Block;
}
exports.isBlock = isBlock;
function isBlockLike(node) {
    return node.statements !== undefined;
}
exports.isBlockLike = isBlockLike;
function isBreakOrContinueStatement(node) {
    return node.kind === ts.SyntaxKind.BreakStatement ||
        node.kind === ts.SyntaxKind.ContinueStatement;
}
exports.isBreakOrContinueStatement = isBreakOrContinueStatement;
function isBreakStatement(node) {
    return node.kind === ts.SyntaxKind.BreakStatement;
}
exports.isBreakStatement = isBreakStatement;
function isCallExpression(node) {
    return node.kind === ts.SyntaxKind.CallExpression;
}
exports.isCallExpression = isCallExpression;
function isCallSignatureDeclaration(node) {
    return node.kind === ts.SyntaxKind.CallSignature;
}
exports.isCallSignatureDeclaration = isCallSignatureDeclaration;
function isCaseBlock(node) {
    return node.kind === ts.SyntaxKind.CaseBlock;
}
exports.isCaseBlock = isCaseBlock;
function isCaseClause(node) {
    return node.kind === ts.SyntaxKind.CaseClause;
}
exports.isCaseClause = isCaseClause;
function isCaseOrDefaultClause(node) {
    return node.kind === ts.SyntaxKind.CaseClause ||
        node.kind === ts.SyntaxKind.DefaultClause;
}
exports.isCaseOrDefaultClause = isCaseOrDefaultClause;
function isCatchClause(node) {
    return node.kind === ts.SyntaxKind.CatchClause;
}
exports.isCatchClause = isCatchClause;
function isClassDeclaration(node) {
    return node.kind === ts.SyntaxKind.ClassDeclaration;
}
exports.isClassDeclaration = isClassDeclaration;
function isClassExpression(node) {
    return node.kind === ts.SyntaxKind.ClassExpression;
}
exports.isClassExpression = isClassExpression;
function isClassLikeDeclaration(node) {
    return node.kind === ts.SyntaxKind.ClassDeclaration ||
        node.kind === ts.SyntaxKind.ClassExpression;
}
exports.isClassLikeDeclaration = isClassLikeDeclaration;
function isConditionalExpression(node) {
    return node.kind === ts.SyntaxKind.ConditionalExpression;
}
exports.isConditionalExpression = isConditionalExpression;
function isConstructorDeclaration(node) {
    return node.kind === ts.SyntaxKind.Constructor;
}
exports.isConstructorDeclaration = isConstructorDeclaration;
function isConstructorTypeNode(node) {
    return node.kind === ts.SyntaxKind.ConstructorType;
}
exports.isConstructorTypeNode = isConstructorTypeNode;
function isConstructSignatureDeclaration(node) {
    return node.kind === ts.SyntaxKind.ConstructSignature;
}
exports.isConstructSignatureDeclaration = isConstructSignatureDeclaration;
function isContinueStatement(node) {
    return node.kind === ts.SyntaxKind.ContinueStatement;
}
exports.isContinueStatement = isContinueStatement;
function isComputedPropertyName(node) {
    return node.kind === ts.SyntaxKind.ComputedPropertyName;
}
exports.isComputedPropertyName = isComputedPropertyName;
function isDebuggerStatement(node) {
    return node.kind === ts.SyntaxKind.DebuggerStatement;
}
exports.isDebuggerStatement = isDebuggerStatement;
function isDefaultClause(node) {
    return node.kind === ts.SyntaxKind.DefaultClause;
}
exports.isDefaultClause = isDefaultClause;
function isDoStatement(node) {
    return node.kind === ts.SyntaxKind.DoStatement;
}
exports.isDoStatement = isDoStatement;
function isElementAccessExpression(node) {
    return node.kind === ts.SyntaxKind.ElementAccessExpression;
}
exports.isElementAccessExpression = isElementAccessExpression;
function isEmptyStatement(node) {
    return node.kind === ts.SyntaxKind.EmptyStatement;
}
exports.isEmptyStatement = isEmptyStatement;
function isEntityName(node) {
    return node.kind === ts.SyntaxKind.Identifier || isQualifiedName(node);
}
exports.isEntityName = isEntityName;
function isEntityNameExpression(node) {
    return node.kind === ts.SyntaxKind.Identifier ||
        isPropertyAccessExpression(node) && isEntityNameExpression(node.expression);
}
exports.isEntityNameExpression = isEntityNameExpression;
function isEnumDeclaration(node) {
    return node.kind === ts.SyntaxKind.EnumDeclaration;
}
exports.isEnumDeclaration = isEnumDeclaration;
function isEnumMember(node) {
    return node.kind === ts.SyntaxKind.EnumMember;
}
exports.isEnumMember = isEnumMember;
function isExportAssignment(node) {
    return node.kind === ts.SyntaxKind.ExportAssignment;
}
exports.isExportAssignment = isExportAssignment;
function isExportDeclaration(node) {
    return node.kind === ts.SyntaxKind.ExportDeclaration;
}
exports.isExportDeclaration = isExportDeclaration;
function isExportSpecifier(node) {
    return node.kind === ts.SyntaxKind.ExportSpecifier;
}
exports.isExportSpecifier = isExportSpecifier;
function isExpressionStatement(node) {
    return node.kind === ts.SyntaxKind.ExpressionStatement;
}
exports.isExpressionStatement = isExpressionStatement;
function isExpressionWithTypeArguments(node) {
    return node.kind === ts.SyntaxKind.ExpressionWithTypeArguments;
}
exports.isExpressionWithTypeArguments = isExpressionWithTypeArguments;
function isExternalModuleReference(node) {
    return node.kind === ts.SyntaxKind.ExternalModuleReference;
}
exports.isExternalModuleReference = isExternalModuleReference;
function isForInStatement(node) {
    return node.kind === ts.SyntaxKind.ForInStatement;
}
exports.isForInStatement = isForInStatement;
function isForOfStatement(node) {
    return node.kind === ts.SyntaxKind.ForOfStatement;
}
exports.isForOfStatement = isForOfStatement;
function isForStatement(node) {
    return node.kind === ts.SyntaxKind.ForStatement;
}
exports.isForStatement = isForStatement;
function isFunctionDeclaration(node) {
    return node.kind === ts.SyntaxKind.FunctionDeclaration;
}
exports.isFunctionDeclaration = isFunctionDeclaration;
function isFunctionExpression(node) {
    return node.kind === ts.SyntaxKind.FunctionExpression;
}
exports.isFunctionExpression = isFunctionExpression;
function isFunctionTypeNode(node) {
    return node.kind === ts.SyntaxKind.FunctionType;
}
exports.isFunctionTypeNode = isFunctionTypeNode;
function isGetAccessorDeclaration(node) {
    return node.kind === ts.SyntaxKind.GetAccessor;
}
exports.isGetAccessorDeclaration = isGetAccessorDeclaration;
function isIdentifier(node) {
    return node.kind === ts.SyntaxKind.Identifier;
}
exports.isIdentifier = isIdentifier;
function isIfStatement(node) {
    return node.kind === ts.SyntaxKind.IfStatement;
}
exports.isIfStatement = isIfStatement;
function isImportClause(node) {
    return node.kind === ts.SyntaxKind.ImportClause;
}
exports.isImportClause = isImportClause;
function isImportDeclaration(node) {
    return node.kind === ts.SyntaxKind.ImportDeclaration;
}
exports.isImportDeclaration = isImportDeclaration;
function isImportEqualsDeclaration(node) {
    return node.kind === ts.SyntaxKind.ImportEqualsDeclaration;
}
exports.isImportEqualsDeclaration = isImportEqualsDeclaration;
function isImportSpecifier(node) {
    return node.kind === ts.SyntaxKind.ImportSpecifier;
}
exports.isImportSpecifier = isImportSpecifier;
function isIndexedAccessTypeNode(node) {
    return node.kind === ts.SyntaxKind.IndexedAccessType;
}
exports.isIndexedAccessTypeNode = isIndexedAccessTypeNode;
function isIndexSignatureDeclaration(node) {
    return node.kind === ts.SyntaxKind.IndexSignature;
}
exports.isIndexSignatureDeclaration = isIndexSignatureDeclaration;
function isInterfaceDeclaration(node) {
    return node.kind === ts.SyntaxKind.InterfaceDeclaration;
}
exports.isInterfaceDeclaration = isInterfaceDeclaration;
function isIntersectionTypeNode(node) {
    return node.kind === ts.SyntaxKind.IntersectionType;
}
exports.isIntersectionTypeNode = isIntersectionTypeNode;
function isIterationStatement(node) {
    switch (node.kind) {
        case ts.SyntaxKind.ForStatement:
        case ts.SyntaxKind.ForOfStatement:
        case ts.SyntaxKind.ForInStatement:
        case ts.SyntaxKind.WhileStatement:
        case ts.SyntaxKind.DoStatement:
            return true;
        default:
            return false;
    }
}
exports.isIterationStatement = isIterationStatement;
function isJsxAttribute(node) {
    return node.kind === ts.SyntaxKind.JsxAttribute;
}
exports.isJsxAttribute = isJsxAttribute;
function isJsxAttributeLike(node) {
    return node.kind === ts.SyntaxKind.JsxAttribute ||
        node.kind === ts.SyntaxKind.JsxSpreadAttribute;
}
exports.isJsxAttributeLike = isJsxAttributeLike;
function isJsxAttributes(node) {
    return node.kind === ts.SyntaxKind.JsxAttributes;
}
exports.isJsxAttributes = isJsxAttributes;
function isJsxClosingElement(node) {
    return node.kind === ts.SyntaxKind.JsxClosingElement;
}
exports.isJsxClosingElement = isJsxClosingElement;
function isJsxElement(node) {
    return node.kind === ts.SyntaxKind.JsxElement;
}
exports.isJsxElement = isJsxElement;
function isJsxExpression(node) {
    return node.kind === ts.SyntaxKind.JsxExpression;
}
exports.isJsxExpression = isJsxExpression;
function isJsxOpeningElement(node) {
    return node.kind === ts.SyntaxKind.JsxOpeningElement;
}
exports.isJsxOpeningElement = isJsxOpeningElement;
function isJsxOpeningLikeElement(node) {
    return node.kind === ts.SyntaxKind.JsxOpeningElement ||
        node.kind === ts.SyntaxKind.JsxSelfClosingElement;
}
exports.isJsxOpeningLikeElement = isJsxOpeningLikeElement;
function isJsxSelfClosingElement(node) {
    return node.kind === ts.SyntaxKind.JsxSelfClosingElement;
}
exports.isJsxSelfClosingElement = isJsxSelfClosingElement;
function isJsxSpreadAttribute(node) {
    return node.kind === ts.SyntaxKind.JsxSpreadAttribute;
}
exports.isJsxSpreadAttribute = isJsxSpreadAttribute;
function isJsxText(node) {
    return node.kind === ts.SyntaxKind.JsxText;
}
exports.isJsxText = isJsxText;
function isLabeledStatement(node) {
    return node.kind === ts.SyntaxKind.LabeledStatement;
}
exports.isLabeledStatement = isLabeledStatement;
function isLiteralExpression(node) {
    return node.kind >= ts.SyntaxKind.FirstLiteralToken &&
        node.kind <= ts.SyntaxKind.LastLiteralToken;
}
exports.isLiteralExpression = isLiteralExpression;
function isMetaProperty(node) {
    return node.kind === ts.SyntaxKind.MetaProperty;
}
exports.isMetaProperty = isMetaProperty;
function isMethodDeclaration(node) {
    return node.kind === ts.SyntaxKind.MethodDeclaration;
}
exports.isMethodDeclaration = isMethodDeclaration;
function isMethodSignature(node) {
    return node.kind === ts.SyntaxKind.MethodSignature;
}
exports.isMethodSignature = isMethodSignature;
function isModuleBlock(node) {
    return node.kind === ts.SyntaxKind.ModuleBlock;
}
exports.isModuleBlock = isModuleBlock;
function isModuleDeclaration(node) {
    return node.kind === ts.SyntaxKind.ModuleDeclaration;
}
exports.isModuleDeclaration = isModuleDeclaration;
function isNamedExports(node) {
    return node.kind === ts.SyntaxKind.NamedExports;
}
exports.isNamedExports = isNamedExports;
function isNamedImports(node) {
    return node.kind === ts.SyntaxKind.NamedImports;
}
exports.isNamedImports = isNamedImports;
function isNamespaceDeclaration(node) {
    return isModuleDeclaration(node) &&
        node.name.kind === ts.SyntaxKind.Identifier &&
        node.body !== undefined &&
        (node.body.kind === ts.SyntaxKind.ModuleBlock ||
            isNamespaceDeclaration(node.body));
}
exports.isNamespaceDeclaration = isNamespaceDeclaration;
function isNamespaceImport(node) {
    return node.kind === ts.SyntaxKind.NamespaceImport;
}
exports.isNamespaceImport = isNamespaceImport;
function isNamespaceExportDeclaration(node) {
    return node.kind === ts.SyntaxKind.NamespaceExportDeclaration;
}
exports.isNamespaceExportDeclaration = isNamespaceExportDeclaration;
function isNewExpression(node) {
    return node.kind === ts.SyntaxKind.NewExpression;
}
exports.isNewExpression = isNewExpression;
function isNonNullExpression(node) {
    return node.kind === ts.SyntaxKind.NonNullExpression;
}
exports.isNonNullExpression = isNonNullExpression;
function isNoSubstitutionTemplateLiteral(node) {
    return node.kind === ts.SyntaxKind.NoSubstitutionTemplateLiteral;
}
exports.isNoSubstitutionTemplateLiteral = isNoSubstitutionTemplateLiteral;
function isNumericLiteral(node) {
    return node.kind === ts.SyntaxKind.NumericLiteral;
}
exports.isNumericLiteral = isNumericLiteral;
function isNumericliteral(node) {
    return isNumericLiteral(node);
}
exports.isNumericliteral = isNumericliteral;
function isObjectBindingPattern(node) {
    return node.kind === ts.SyntaxKind.ObjectBindingPattern;
}
exports.isObjectBindingPattern = isObjectBindingPattern;
function isObjectLiteralExpression(node) {
    return node.kind === ts.SyntaxKind.ObjectLiteralExpression;
}
exports.isObjectLiteralExpression = isObjectLiteralExpression;
function isOmittedExpression(node) {
    return node.kind === ts.SyntaxKind.OmittedExpression;
}
exports.isOmittedExpression = isOmittedExpression;
function isParameterDeclaration(node) {
    return node.kind === ts.SyntaxKind.Parameter;
}
exports.isParameterDeclaration = isParameterDeclaration;
function isParenthesizedExpression(node) {
    return node.kind === ts.SyntaxKind.ParenthesizedExpression;
}
exports.isParenthesizedExpression = isParenthesizedExpression;
function isPostfixUnaryExpression(node) {
    return node.kind === ts.SyntaxKind.PostfixUnaryExpression;
}
exports.isPostfixUnaryExpression = isPostfixUnaryExpression;
function isPrefixUnaryExpression(node) {
    return node.kind === ts.SyntaxKind.PrefixUnaryExpression;
}
exports.isPrefixUnaryExpression = isPrefixUnaryExpression;
function isPropertyAccessExpression(node) {
    return node.kind === ts.SyntaxKind.PropertyAccessExpression;
}
exports.isPropertyAccessExpression = isPropertyAccessExpression;
function isPropertyAssignment(node) {
    return node.kind === ts.SyntaxKind.PropertyAssignment;
}
exports.isPropertyAssignment = isPropertyAssignment;
function isPropertyDeclaration(node) {
    return node.kind === ts.SyntaxKind.PropertyDeclaration;
}
exports.isPropertyDeclaration = isPropertyDeclaration;
function isPropertySignature(node) {
    return node.kind === ts.SyntaxKind.PropertySignature;
}
exports.isPropertySignature = isPropertySignature;
function isQualifiedName(node) {
    return node.kind === ts.SyntaxKind.QualifiedName;
}
exports.isQualifiedName = isQualifiedName;
function isRegularExpressionLiteral(node) {
    return node.kind === ts.SyntaxKind.RegularExpressionLiteral;
}
exports.isRegularExpressionLiteral = isRegularExpressionLiteral;
function isReturnStatement(node) {
    return node.kind === ts.SyntaxKind.ReturnStatement;
}
exports.isReturnStatement = isReturnStatement;
function isSetAccessorDeclaration(node) {
    return node.kind === ts.SyntaxKind.SetAccessor;
}
exports.isSetAccessorDeclaration = isSetAccessorDeclaration;
function isShorthandPropertyAssignment(node) {
    return node.kind === ts.SyntaxKind.ShorthandPropertyAssignment;
}
exports.isShorthandPropertyAssignment = isShorthandPropertyAssignment;
function isSignatureDeclaration(node) {
    return node.parameters !== undefined;
}
exports.isSignatureDeclaration = isSignatureDeclaration;
function isSourceFile(node) {
    return node.kind === ts.SyntaxKind.SourceFile;
}
exports.isSourceFile = isSourceFile;
function isSpreadAssignment(node) {
    return node.kind === ts.SyntaxKind.SpreadAssignment;
}
exports.isSpreadAssignment = isSpreadAssignment;
function isSpreadElement(node) {
    return node.kind === ts.SyntaxKind.SpreadElement;
}
exports.isSpreadElement = isSpreadElement;
function isStringLiteral(node) {
    return node.kind === ts.SyntaxKind.StringLiteral;
}
exports.isStringLiteral = isStringLiteral;
function isSwitchStatement(node) {
    return node.kind === ts.SyntaxKind.SwitchStatement;
}
exports.isSwitchStatement = isSwitchStatement;
function isSyntaxList(node) {
    return node.kind === ts.SyntaxKind.SyntaxList;
}
exports.isSyntaxList = isSyntaxList;
function isTaggedTemplateExpression(node) {
    return node.kind === ts.SyntaxKind.TaggedTemplateExpression;
}
exports.isTaggedTemplateExpression = isTaggedTemplateExpression;
function isTemplateExpression(node) {
    return node.kind === ts.SyntaxKind.TemplateExpression;
}
exports.isTemplateExpression = isTemplateExpression;
function isTemplateLiteral(node) {
    return node.kind === ts.SyntaxKind.TemplateExpression ||
        node.kind === ts.SyntaxKind.NoSubstitutionTemplateLiteral;
}
exports.isTemplateLiteral = isTemplateLiteral;
function isTextualLiteral(node) {
    return node.kind === ts.SyntaxKind.StringLiteral ||
        node.kind === ts.SyntaxKind.NoSubstitutionTemplateLiteral;
}
exports.isTextualLiteral = isTextualLiteral;
function isThrowStatement(node) {
    return node.kind === ts.SyntaxKind.ThrowStatement;
}
exports.isThrowStatement = isThrowStatement;
function isTryStatement(node) {
    return node.kind === ts.SyntaxKind.TryStatement;
}
exports.isTryStatement = isTryStatement;
function isTupleTypeNode(node) {
    return node.kind === ts.SyntaxKind.TupleType;
}
exports.isTupleTypeNode = isTupleTypeNode;
function isTypeAliasDeclaration(node) {
    return node.kind === ts.SyntaxKind.TypeAliasDeclaration;
}
exports.isTypeAliasDeclaration = isTypeAliasDeclaration;
function isTypeAssertion(node) {
    return node.kind === ts.SyntaxKind.TypeAssertionExpression;
}
exports.isTypeAssertion = isTypeAssertion;
function isTypeLiteralNode(node) {
    return node.kind === ts.SyntaxKind.TypeLiteral;
}
exports.isTypeLiteralNode = isTypeLiteralNode;
function isTypeOfExpression(node) {
    return node.kind === ts.SyntaxKind.TypeOfExpression;
}
exports.isTypeOfExpression = isTypeOfExpression;
function isTypeOperatorNode(node) {
    return node.kind === ts.SyntaxKind.TypeOperator;
}
exports.isTypeOperatorNode = isTypeOperatorNode;
function isTypeParameterDeclaration(node) {
    return node.kind === ts.SyntaxKind.TypeParameter;
}
exports.isTypeParameterDeclaration = isTypeParameterDeclaration;
function isTypePredicateNode(node) {
    return node.kind === ts.SyntaxKind.TypePredicate;
}
exports.isTypePredicateNode = isTypePredicateNode;
function isTypeReferenceNode(node) {
    return node.kind === ts.SyntaxKind.TypeReference;
}
exports.isTypeReferenceNode = isTypeReferenceNode;
function isTypeQueryNode(node) {
    return node.kind === ts.SyntaxKind.TypeQuery;
}
exports.isTypeQueryNode = isTypeQueryNode;
function isUnionTypeNode(node) {
    return node.kind === ts.SyntaxKind.UnionType;
}
exports.isUnionTypeNode = isUnionTypeNode;
function isVariableDeclaration(node) {
    return node.kind === ts.SyntaxKind.VariableDeclaration;
}
exports.isVariableDeclaration = isVariableDeclaration;
function isVariableStatement(node) {
    return node.kind === ts.SyntaxKind.VariableStatement;
}
exports.isVariableStatement = isVariableStatement;
function isVariableDeclarationList(node) {
    return node.kind === ts.SyntaxKind.VariableDeclarationList;
}
exports.isVariableDeclarationList = isVariableDeclarationList;
function isVoidExpression(node) {
    return node.kind === ts.SyntaxKind.VoidExpression;
}
exports.isVoidExpression = isVoidExpression;
function isWhileStatement(node) {
    return node.kind === ts.SyntaxKind.WhileStatement;
}
exports.isWhileStatement = isWhileStatement;
function isWithStatement(node) {
    return node.kind === ts.SyntaxKind.WithStatement;
}
exports.isWithStatement = isWithStatement;
function isEnumType(type) {
    return (type.flags & ts.TypeFlags.Enum) !== 0;
}
exports.isEnumType = isEnumType;
function isEnumLiteralType(type) {
    return (type.flags & ts.TypeFlags.EnumLiteral) !== 0;
}
exports.isEnumLiteralType = isEnumLiteralType;
function isGenericType(type) {
    return (type.flags & ts.TypeFlags.Object) !== 0 &&
        (type.objectFlags & ts.ObjectFlags.ClassOrInterface) !== 0 &&
        (type.objectFlags & ts.ObjectFlags.Reference) !== 0;
}
exports.isGenericType = isGenericType;
function isIndexedAccessType(type) {
    return (type.flags & ts.TypeFlags.IndexedAccess) !== 0;
}
exports.isIndexedAccessType = isIndexedAccessType;
function isIndexedAccessype(type) {
    return (type.flags & ts.TypeFlags.Index) !== 0;
}
exports.isIndexedAccessype = isIndexedAccessype;
function isInterfaceType(type) {
    return (type.flags & ts.TypeFlags.Object) !== 0 &&
        (type.objectFlags & ts.ObjectFlags.ClassOrInterface) !== 0;
}
exports.isInterfaceType = isInterfaceType;
function isIntersectionType(type) {
    return (type.flags & ts.TypeFlags.Intersection) !== 0;
}
exports.isIntersectionType = isIntersectionType;
function isLiteralType(type) {
    return (type.flags & ts.TypeFlags.Literal) !== 0;
}
exports.isLiteralType = isLiteralType;
function isObjectType(type) {
    return (type.flags & ts.TypeFlags.Object) !== 0;
}
exports.isObjectType = isObjectType;
function isTypeParameter(type) {
    return (type.flags & ts.TypeFlags.TypeParameter) !== 0;
}
exports.isTypeParameter = isTypeParameter;
function isTypeReference(type) {
    return (type.flags & ts.TypeFlags.Object) !== 0 &&
        (type.objectFlags & ts.ObjectFlags.Reference) !== 0;
}
exports.isTypeReference = isTypeReference;
function isTypeVariable(type) {
    return (type.flags & ts.TypeFlags.TypeVariable) !== 0;
}
exports.isTypeVariable = isTypeVariable;
function isUnionOrIntersectionType(type) {
    return (type.flags & ts.TypeFlags.UnionOrIntersection) !== 0;
}
exports.isUnionOrIntersectionType = isUnionOrIntersectionType;
function isUnionType(type) {
    return (type.flags & ts.TypeFlags.Union) !== 0;
}
exports.isUnionType = isUnionType;
//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJmaWxlIjoidHlwZWd1YXJkLmpzIiwic291cmNlUm9vdCI6IiIsInNvdXJjZXMiOlsidHlwZWd1YXJkLnRzIl0sIm5hbWVzIjpbXSwibWFwcGluZ3MiOiI7O0FBQUEsK0JBQWlDO0FBRWpDLCtCQUFzQyxJQUFhO0lBQy9DLE1BQU0sQ0FBQyxJQUFJLENBQUMsSUFBSSxLQUFLLEVBQUUsQ0FBQyxVQUFVLENBQUMsV0FBVztRQUMxQyxJQUFJLENBQUMsSUFBSSxLQUFLLEVBQUUsQ0FBQyxVQUFVLENBQUMsV0FBVyxDQUFDO0FBQ2hELENBQUM7QUFIRCxzREFHQztBQUVELCtCQUFzQyxJQUFhO0lBQy9DLE1BQU0sQ0FBQyxJQUFJLENBQUMsSUFBSSxLQUFLLEVBQUUsQ0FBQyxVQUFVLENBQUMsbUJBQW1CLENBQUM7QUFDM0QsQ0FBQztBQUZELHNEQUVDO0FBRUQsa0NBQXlDLElBQWE7SUFDbEQsTUFBTSxDQUFDLElBQUksQ0FBQyxJQUFJLEtBQUssRUFBRSxDQUFDLFVBQVUsQ0FBQyxzQkFBc0IsQ0FBQztBQUM5RCxDQUFDO0FBRkQsNERBRUM7QUFFRCx5QkFBZ0MsSUFBYTtJQUN6QyxNQUFNLENBQUMsSUFBSSxDQUFDLElBQUksS0FBSyxFQUFFLENBQUMsVUFBVSxDQUFDLFNBQVMsQ0FBQztBQUNqRCxDQUFDO0FBRkQsMENBRUM7QUFFRCx5QkFBZ0MsSUFBYTtJQUN6QyxNQUFNLENBQUMsSUFBSSxDQUFDLElBQUksS0FBSyxFQUFFLENBQUMsVUFBVSxDQUFDLGFBQWEsQ0FBQztBQUNyRCxDQUFDO0FBRkQsMENBRUM7QUFFRCx3QkFBK0IsSUFBYTtJQUN4QyxNQUFNLENBQUMsSUFBSSxDQUFDLElBQUksS0FBSyxFQUFFLENBQUMsVUFBVSxDQUFDLFlBQVksQ0FBQztBQUNwRCxDQUFDO0FBRkQsd0NBRUM7QUFFRCwrQkFBc0MsSUFBYTtJQUMvQyxNQUFNLENBQUMsSUFBSSxDQUFDLElBQUksS0FBSyxFQUFFLENBQUMsVUFBVSxDQUFDLFlBQVk7UUFDM0MsSUFBSSxDQUFDLElBQUks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