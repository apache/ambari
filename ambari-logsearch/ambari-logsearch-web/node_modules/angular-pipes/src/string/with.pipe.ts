import {Pipe, PipeTransform} from '@angular/core';
import {isString, isNull} from '../utils/utils';

@Pipe({name: 'with'})
export class WithPipe implements PipeTransform {
  
  transform(input: string, start: string|null = null, ends: string|null = null, csensitive: boolean = false): any {
    
    if (!isString(input) || (isNull(start) && isNull(ends)) || (start == '') || (ends == '')) {
      return input;
    }
    
    input = (csensitive) ? input : input.toLowerCase();
    
    if (!isNull(start) && !isNull(ends)) {
      let a: boolean = !input.indexOf((csensitive) ? start : start.toLowerCase());
      let b: boolean = input.indexOf((csensitive) ? ends : ends.toLowerCase(), (input.length - ends.length)) !== -1;
      
      if (a == true && b == true) {
        return true
      } else {
        return false;
      }
    }
    
    if (!isNull(start)) {
      return !input.indexOf((csensitive) ? start : start.toLowerCase());
    }
    
    if (!isNull(ends)) {
      let position: any = input.length - ends.length;
      
      return input.indexOf((csensitive) ? ends : ends.toLowerCase(), position) !== -1;
    }
  }
}