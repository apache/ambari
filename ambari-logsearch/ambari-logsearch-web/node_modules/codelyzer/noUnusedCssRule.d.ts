import * as Lint from 'tslint';
import * as ts from 'typescript';
import { Ng2Walker } from './angular/ng2Walker';
import { CssAst } from './angular/styles/cssAst';
import { ComponentMetadata, StyleMetadata } from './angular/metadata';
export declare class Rule extends Lint.Rules.AbstractRule {
    static FAILURE: string;
    apply(sourceFile: ts.SourceFile): Lint.RuleFailure[];
}
export declare class UnusedCssNg2Visitor extends Ng2Walker {
    private templateAst;
    visitClassDeclaration(declaration: ts.ClassDeclaration): void;
    protected visitNg2StyleHelper(style: CssAst, context: ComponentMetadata, styleMetadata: StyleMetadata, baseStart: number): void;
}
