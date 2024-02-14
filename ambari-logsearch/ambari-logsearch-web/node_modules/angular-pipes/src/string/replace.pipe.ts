import { Pipe, PipeTransform  } from '@angular/core';
import { isString } from '../utils/utils';

@Pipe({
  name: 'replace'
})
export class ReplacePipe implements PipeTransform {
  
  transform (input: any, pattern: any, replacement: any): any {
    
    if (!isString(input) || !pattern || !replacement) {
      return input;
    }
    
    return input.replace(pattern, replacement);
  }
}