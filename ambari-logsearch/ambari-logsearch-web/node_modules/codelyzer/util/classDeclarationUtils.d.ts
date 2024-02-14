import * as ts from 'typescript';
export declare const getDeclaredProperties: (declaration: ts.ClassDeclaration) => ts.ClassElement[];
export declare const getDeclaredPropertyNames: (declaration: ts.ClassDeclaration) => any[];
export declare const getDeclaredMethods: (declaration: ts.ClassDeclaration) => ts.ClassElement[];
export declare const getDeclaredMethodNames: (declaration: ts.ClassDeclaration) => string[];
