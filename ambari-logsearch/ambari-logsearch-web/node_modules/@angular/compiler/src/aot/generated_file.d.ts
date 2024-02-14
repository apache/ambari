import { Statement } from '../output/output_ast';
export declare class GeneratedFile {
    srcFileUrl: string;
    genFileUrl: string;
    source: string | null;
    stmts: Statement[] | null;
    constructor(srcFileUrl: string, genFileUrl: string, sourceOrStmts: string | Statement[]);
}
export declare function toTypeScript(file: GeneratedFile, preamble?: string): string;
