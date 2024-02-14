import { Pipe, PipeTransform  } from '@angular/core';
import { isArray } from '../utils/utils';

@Pipe({
  name: 'join'
})
export class JoinPipe implements PipeTransform {
  
  transform (input: any, character: string = ''): any {
    
    if (!isArray(input)) {
      return input;
    }
    
    return input.join(character);
  }
}