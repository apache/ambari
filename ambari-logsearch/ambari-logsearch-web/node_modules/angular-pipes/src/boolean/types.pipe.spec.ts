import {IsNullPipe,
        IsUndefinedPipe,
        IsFunctionPipe,
        IsNumberPipe,
        IsStringPipe,
        IsArrayPipe,
        IsObjectPipe,
        IsDefinedPipe, 
        IsNilPipe } from './types.pipe';
        


describe('IsNullPipe', () => {
    
    let pipe: IsNullPipe;
    
    beforeEach(() => {
       pipe = new IsNullPipe(); 
    });
    
    it('Should return true', () => {
        
        expect(pipe.transform(null)).toEqual(true);
    });
    
    it('Should return false', () => {
        
        expect(pipe.transform(1)).toEqual(false);
    });
    
    it('Should return false #2', () => {
        
        expect(pipe.transform(undefined)).toEqual(false);
    });
    
});

describe('IsNilPipe', () => {
    
    let pipe: IsNilPipe;
    
    beforeEach(() => {
       pipe = new IsNilPipe(); 
    });
    
    it('Should return true', () => {
        
        expect(pipe.transform(null)).toEqual(true);
    });
    
    it('Should return false', () => {
        
        expect(pipe.transform(1)).toEqual(false);
    });
    
    it('Should return true #2', () => {
        
        expect(pipe.transform(undefined)).toEqual(true);
    });
    
});


describe('IsUndefinedPipe', () => {
    
    let pipe: IsUndefinedPipe;
    
    beforeEach(() => {
       pipe = new IsUndefinedPipe(); 
    });
    
    it('Should return true', () => {
        
        expect(pipe.transform(undefined)).toEqual(true);
    });
    
    it('Should return false', () => {
        
        expect(pipe.transform(1)).toEqual(false);
    });
    
    it('Should return false #2', () => {
        
        expect(pipe.transform(null)).toEqual(false);
    });
    
});


describe('IsFunctionPipe', () => {
    
    let pipe: IsFunctionPipe;
    
    beforeEach(() => {
       pipe = new IsFunctionPipe(); 
    });
    
    it('Should return true', () => {
        
        expect(pipe.transform(function () {})).toEqual(true);
    });
    
    it('Should return false', () => {
        
        expect(pipe.transform(1)).toEqual(false);
    });
    
    it('Should return false #2', () => {
        
        expect(pipe.transform(null)).toEqual(false);
    });
    
});

describe('IsNumberPipe', () => {
    
    let pipe: IsNumberPipe;
    
    beforeEach(() => {
       pipe = new IsNumberPipe(); 
    });
    
    it('Should return true', () => {
        
        expect(pipe.transform(1)).toEqual(true);
    });
    
    it('Should return true', () => {
        
        expect(pipe.transform(1.2)).toEqual(true);
    });
    
    it('Should return false', () => {
        
        expect(pipe.transform('a')).toEqual(false);
    });
    
    it('Should return false', () => {
        
        expect(pipe.transform(null)).toEqual(false);
    });
    
});


describe('IsStringPipe', () => {
    
    let pipe: IsStringPipe;
    
    beforeEach(() => {
       pipe = new IsStringPipe(); 
    });
    
    it('Should return true', () => {
        
        expect(pipe.transform('a')).toEqual(true);
    });
    
    it('Should return false', () => {
        
        expect(pipe.transform(1)).toEqual(false);
    });
    
});

describe('IsStringPipe', () => {
    
    let pipe: IsStringPipe;
    
    beforeEach(() => {
       pipe = new IsStringPipe(); 
    });
    
    it('Should return true', () => {
        
        expect(pipe.transform('a')).toEqual(true);
    });
    
    it('Should return false', () => {
        
        expect(pipe.transform(1)).toEqual(false);
    });
    
});


describe('IsArrayPipe', () => {
    
    let pipe: IsArrayPipe;
    
    beforeEach(() => {
       pipe = new IsArrayPipe(); 
    });
    
    it('Should return true', () => {
        
        expect(pipe.transform([1,2])).toEqual(true);
    });
    
    it('Should return false', () => {
        
        expect(pipe.transform(1)).toEqual(false);
    });
    
});


describe('IsObjectPipe', () => {
    
    let pipe: IsObjectPipe;
    
    beforeEach(() => {
       pipe = new IsObjectPipe(); 
    });
    
    it('Should return true', () => {
        
        expect(pipe.transform({})).toEqual(true);
    });
    
    it('Should return false', () => {
        
        expect(pipe.transform(1)).toEqual(false);
    });
    
    it('Should return true #2', () => {
        
        expect(pipe.transform([])).toEqual(true);
    });
    
    it('Should return false #3', () => {
        
        expect(pipe.transform('a')).toEqual(false);
    });
});


describe('IsDefinedPipe', () => {
    
    let pipe: IsDefinedPipe;
    
    beforeEach(() => {
       pipe = new IsDefinedPipe(); 
    });
    
    it('Should return true', () => {
        
        expect(pipe.transform({})).toEqual(true);
    });
    
    it('Should return false', () => {
        
        expect(pipe.transform(undefined)).toEqual(false);
    });
   
});