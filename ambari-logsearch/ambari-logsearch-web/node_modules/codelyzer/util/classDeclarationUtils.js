"use strict";
var syntaxKind_1 = require("./syntaxKind");
var SyntaxKind = syntaxKind_1.current();
exports.getDeclaredProperties = function (declaration) {
    var m = declaration.members;
    var ctr = m.filter(function (m) { return m.kind === SyntaxKind.Constructor; }).pop();
    var params = [];
    if (ctr) {
        params = (ctr.parameters || [])
            .filter(function (p) { return p.kind === SyntaxKind.Parameter; });
    }
    return m.filter(function (m) { return m.kind === SyntaxKind.PropertyDeclaration ||
        m.kind === SyntaxKind.GetAccessor || m.kind === SyntaxKind.SetAccessor; }).concat(params);
};
exports.getDeclaredPropertyNames = function (declaration) {
    return exports.getDeclaredProperties(declaration).filter(function (p) { return p && p.name; }).map(function (p) { return p.name.text; });
};
exports.getDeclaredMethods = function (declaration) {
    return declaration.members.filter(function (m) { return m.kind === SyntaxKind.MethodDeclaration; });
};
exports.getDeclaredMethodNames = function (declaration) {
    return exports.getDeclaredMethods(declaration)
        .map(function (d) { return d.name.text; });
};
