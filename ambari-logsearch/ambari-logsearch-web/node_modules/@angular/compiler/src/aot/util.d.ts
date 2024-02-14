export declare function ngfactoryFilePath(filePath: string, forceSourceFile?: boolean): string;
export declare function stripGeneratedFileSuffix(filePath: string): string;
export declare function isGeneratedFile(filePath: string): boolean;
export declare function splitTypescriptSuffix(path: string, forceSourceFile?: boolean): string[];
export declare function summaryFileName(fileName: string): string;
export declare function summaryForJitFileName(fileName: string, forceSourceFile?: boolean): string;
export declare function stripSummaryForJitFileSuffix(filePath: string): string;
export declare function summaryForJitName(symbolName: string): string;
export declare function stripSummaryForJitNameSuffix(symbolName: string): string;
