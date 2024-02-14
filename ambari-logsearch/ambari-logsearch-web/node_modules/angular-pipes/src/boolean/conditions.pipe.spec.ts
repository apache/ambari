import {IsGreaterPipe,
        IsGreaterOrEqualPipe,
        IsLessPipe,
        IsLessOrEqualPipe,
        IsEqualPipe,
        IsNotEqualPipe,
        IsIdenticalPipe,
        IsNotIdenticalPipe } from './conditions.pipe';
        


describe('IsGreaterPipe', () => {
    
    let pipe: IsGreaterPipe;
    
    beforeEach(() => {
       pipe = new IsGreaterPipe(); 
    });
    
    it('Should return true', () => {
        
        expect(pipe.transform(3, 2)).toEqual(true);
    });
    
    it('Should return false', () => {
        
        expect(pipe.transform(1, 2)).toEqual(false);
    });
    
    it('Should return false #2', () => {
        
        expect(pipe.transform(1, 1)).toEqual(false);
    });
    
});

describe('IsGreaterOrEqualPipe', () => {
    
    let pipe: IsGreaterOrEqualPipe;
    
    beforeEach(() => {
       pipe = new IsGreaterOrEqualPipe(); 
    });
    
    it('Should return true', () => {
        
        expect(pipe.transform(3, 2)).toEqual(true);
    });
    
    it('Should return false', () => {
        
        expect(pipe.transform(1, 2)).toEqual(false);
    });
    
    it('Should return true #2', () => {
        
        expect(pipe.transform(1, 1)).toEqual(true);
    });
    
});

describe('IsLessPipe', () => {
    
    let pipe: IsLessPipe;
    
    beforeEach(() => {
       pipe = new IsLessPipe(); 
    });
    
    it('Should return false', () => {
        
        expect(pipe.transform(3, 2)).toEqual(false);
    });
    
    it('Should return true', () => {
        
        expect(pipe.transform(1, 2)).toEqual(true);
    });
    
    it('Should return false #2', () => {
        
        expect(pipe.transform(1, 1)).toEqual(false);
    });
    
});

describe('IsLessOrEqualPipe', () => {
    
    let pipe: IsLessOrEqualPipe;
    
    beforeEach(() => {
       pipe = new IsLessOrEqualPipe(); 
    });
    
    it('Should return false', () => {
        
        expect(pipe.transform(3, 2)).toEqual(false);
    });
    
    it('Should return true', () => {
        
        expect(pipe.transform(1, 2)).toEqual(true);
    });
    
    it('Should return true #2', () => {
        
        expect(pipe.transform(1, 1)).toEqual(true);
    });
    
});

describe('IsEqualPipe', () => {
    
    let pipe: IsEqualPipe;
    
    beforeEach(() => {
       pipe = new IsEqualPipe(); 
    });
    
    it('Should return false', () => {
        
        expect(pipe.transform(3, 2)).toEqual(false);
    });
    
    it('Should return true', () => {
        
        expect(pipe.transform(1, 1)).toEqual(true);
    });
    
    it('Should return true #2', () => {
        
        expect(pipe.transform(1, '1')).toEqual(true);
    });
    
});


describe('IsNotEqualPipe', () => {
    
    let pipe: IsNotEqualPipe;
    
    beforeEach(() => {
       pipe = new IsNotEqualPipe(); 
    });
    
    it('Should return true', () => {
        
        expect(pipe.transform(3, 2)).toEqual(true);
    });
    
    it('Should return false', () => {
        
        expect(pipe.transform(1, 1)).toEqual(false);
    });
    
    it('Should return false #2', () => {
        
        expect(pipe.transform(1, '1')).toEqual(false);
    });
    
});


describe('IsIdenticalPipe', () => {
    
    let pipe: IsIdenticalPipe;
    
    beforeEach(() => {
       pipe = new IsIdenticalPipe(); 
    });
    
    it('Should return false', () => {
        
        expect(pipe.transform(3, 2)).toEqual(false);
    });
    
    it('Should return true', () => {
        
        expect(pipe.transform(1, 1)).toEqual(true);
    });
    
    it('Should return false #2', () => {
        
        expect(pipe.transform(1, '1')).toEqual(false);
    });
    
});

describe('IsNotIdenticalPipe', () => {
    
    let pipe: IsNotIdenticalPipe;
    
    beforeEach(() => {
       pipe = new IsNotIdenticalPipe(); 
    });
    
    it('Should return true', () => {
        
        expect(pipe.transform(3, 2)).toEqual(true);
    });
    
    it('Should return false', () => {
        
        expect(pipe.transform(1, 1)).toEqual(false);
    });
    
    it('Should return true #2', () => {
        
        expect(pipe.transform(1, '1')).toEqual(true);
    });
    
});

