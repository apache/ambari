import { Pipe, PipeTransform  } from '@angular/core';
import {
  isNull,
  isNil,
  isUndefined, 
  isFunction,
  isArray,
  isString,
  isObject,
  isNumber 
} from '../utils/utils';

@Pipe({
  name: 'isNull'
})
export class IsNullPipe implements PipeTransform {
  
  transform (value: any): boolean {
    
    return isNull(value);
  }
}

@Pipe({
  name: 'isUndefined'
})
export class IsUndefinedPipe implements PipeTransform {
  
  transform (value: any): boolean {
    
    return isUndefined(value);
  }
}

@Pipe({
  name: 'isNil'
})
export class IsNilPipe implements PipeTransform {
  
  transform (value: any): boolean {
    
    return isNil(value);
  }
}

@Pipe({
  name: 'isFunction'
})
export class IsFunctionPipe implements PipeTransform {
  
  transform (value: any): boolean {
    
    return isFunction(value);
  }
}

@Pipe({
  name: 'isNumber'
})
export class IsNumberPipe implements PipeTransform {
  
  transform (value: any): boolean {
    
    return isNumber(value);
  }
}

@Pipe({
  name: 'isString'
})
export class IsStringPipe implements PipeTransform {
  
  transform (value: any): boolean {
    
    return isString(value);
  }
}


@Pipe({
  name: 'isArray'
})
export class IsArrayPipe implements PipeTransform {
  
  transform (value: any): boolean {
    
    return isArray(value);
  }
}

@Pipe({
  name: 'isObject'
})
export class IsObjectPipe implements PipeTransform {
  
  transform (value: any): boolean {
    
    return isObject(value);
  }
}


@Pipe({
  name: 'isDefined'
})
export class IsDefinedPipe implements PipeTransform {
  
  transform (value: any): boolean {
    
    return !isUndefined(value);
  }
}

