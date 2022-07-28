import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';

import { WikiContentTypeService } from './wiki-content-type.service';

describe( 'WikiContentTypeService', () => {
    let service: WikiContentTypeService;

    beforeEach( () => {
        TestBed.configureTestingModule( { imports: [HttpClientTestingModule] } );
        service = TestBed.inject( WikiContentTypeService );
    } );

    it( 'should be created', () => {
        expect( service ).toBeTruthy();
    } );
} );
