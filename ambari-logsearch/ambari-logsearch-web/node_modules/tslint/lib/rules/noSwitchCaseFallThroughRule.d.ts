import * as ts from "typescript";
import * as Lint from "../index";
export declare class Rule extends Lint.Rules.AbstractRule {
    static metadata: Lint.IRuleMetadata;
    static FAILURE_STRING_PART: string;
    apply(sourceFile: ts.SourceFile): Lint.RuleFailure[];
}
export declare class NoSwitchCaseFallThroughWalker extends Lint.AbstractWalker<void> {
    walk(sourceFile: ts.SourceFile): void;
    private visitSwitchStatement(node);
    private isFallThroughAllowed(clause);
    private reportError(clause);
}
