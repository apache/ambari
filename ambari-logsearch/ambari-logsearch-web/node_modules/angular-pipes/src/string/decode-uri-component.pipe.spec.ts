import { DecodeURIComponentPipe } from './decode-uri-component.pipe';
        


describe('DecodeURIComponentPipe', () => {
    
    let pipe: DecodeURIComponentPipe;
    
    beforeEach(() => {
       pipe = new DecodeURIComponentPipe(); 
    });
    
    it('Should return the value decoded', () => {
        const url = 'https%3A%2F%2Ftest.com';
        expect(pipe.transform(url)).toEqual(decodeURIComponent(url));
    });

    it('Should return the value unchanged', () => {
       
       expect(pipe.transform(1)).toEqual(1); 
    });
});