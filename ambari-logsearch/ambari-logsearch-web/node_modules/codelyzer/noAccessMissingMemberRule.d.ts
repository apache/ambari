import * as Lint from 'tslint';
import * as ts from 'typescript';
import * as e from '@angular/compiler/src/expression_parser/ast';
export interface ASTField {
    obj?: ASTField;
    receiver?: ASTField;
    name?: string;
    span: e.ParseSpan;
}
export declare class Rule extends Lint.Rules.AbstractRule {
    static FAILURE: string;
    apply(sourceFile: ts.SourceFile): Lint.RuleFailure[];
}
