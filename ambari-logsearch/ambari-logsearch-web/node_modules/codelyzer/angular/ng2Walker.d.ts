import * as Lint from 'tslint';
import * as ts from 'typescript';
import * as compiler from '@angular/compiler';
import { CssAst } from './styles/cssAst';
import { CssAstVisitorCtrl } from './styles/basicCssAstVisitor';
import { RecursiveAngularExpressionVisitorCtr, TemplateAstVisitorCtr } from './templates/basicTemplateAstVisitor';
import { MetadataReader } from './metadataReader';
import { ComponentMetadata, DirectiveMetadata, StyleMetadata } from './metadata';
export interface Ng2WalkerConfig {
    expressionVisitorCtrl?: RecursiveAngularExpressionVisitorCtr;
    templateVisitorCtrl?: TemplateAstVisitorCtr;
    cssVisitorCtrl?: CssAstVisitorCtrl;
}
export declare class Ng2Walker extends Lint.RuleWalker {
    protected _originalOptions: Lint.IOptions;
    private _config;
    protected _metadataReader: MetadataReader;
    constructor(sourceFile: ts.SourceFile, _originalOptions: Lint.IOptions, _config?: Ng2WalkerConfig, _metadataReader?: MetadataReader);
    visitClassDeclaration(declaration: ts.ClassDeclaration): void;
    visitMethodDeclaration(method: ts.MethodDeclaration): void;
    visitPropertyDeclaration(prop: ts.PropertyDeclaration): void;
    protected visitMethodDecorator(decorator: ts.Decorator): void;
    protected visitPropertyDecorator(decorator: ts.Decorator): void;
    protected visitClassDecorator(decorator: ts.Decorator): void;
    protected visitNg2Component(metadata: ComponentMetadata): void;
    protected visitNg2Directive(metadata: DirectiveMetadata): void;
    protected visitNg2Pipe(controller: ts.ClassDeclaration, decorator: ts.Decorator): void;
    protected visitNg2Input(property: ts.PropertyDeclaration, input: ts.Decorator, args: string[]): void;
    protected visitNg2Output(property: ts.PropertyDeclaration, output: ts.Decorator, args: string[]): void;
    protected visitNg2HostBinding(property: ts.PropertyDeclaration, decorator: ts.Decorator, args: string[]): void;
    protected visitNg2HostListener(method: ts.MethodDeclaration, decorator: ts.Decorator, args: string[]): void;
    protected visitNg2TemplateHelper(roots: compiler.TemplateAst[], context: ComponentMetadata, baseStart: number): void;
    protected visitNg2StyleHelper(style: CssAst, context: ComponentMetadata, styleMetadata: StyleMetadata, baseStart: number): void;
    protected getContextSourceFile(path: string, content: string): ts.SourceFile;
}
