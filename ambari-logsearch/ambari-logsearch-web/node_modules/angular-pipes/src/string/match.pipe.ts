import { Pipe, PipeTransform  } from '@angular/core';
import { isString } from '../utils/utils';

@Pipe({
  name: 'match'
})
export class MatchPipe implements PipeTransform {
  
  transform (input: any, pattern: any, flag: any): any {
    
    if (!isString(input)) {
      return input;
    }
    
    const regexp = pattern instanceof RegExp ? pattern : new RegExp(pattern, flag);
    return input.match(regexp);
  }
}