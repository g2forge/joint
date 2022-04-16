import { TestBed } from '@angular/core/testing';

import { WikiLinksService, Path } from './wikilinks.service';

describe( 'WikiLinksService', () => {
    let service: WikiLinksService;

    beforeEach( () => {
        TestBed.configureTestingModule( {} );
        service = TestBed.inject( WikiLinksService );
    } );

    it( 'should be created', () => {
        expect( service ).toBeTruthy();
    } );

    it( 'should recognize http', () => {
        expect( service.hasScheme( "http://example.com/" ) ).toBeTrue();
    } );

    it( 'should recognize https', () => {
        expect( service.hasScheme( "https://example.com/" ) ).toBeTrue();
    } );

    it( 'should recognize mailto', () => {
        expect( service.hasScheme( "mailto:user@example.com" ) ).toBeTrue();
    } );

    it( 'should recognize non-scheme urls', () => {
        expect( service.hasScheme( "dir/file.html" ) ).toBeFalse();
    } );

    it( 'shouldn\'t rewrite uris with schemes', () => {
        expect( service.rewriteURI( "https://example.com/" ) ).toBeNull();
    } );

    it( 'shouldn\'t rewrite some URIs', () => {
        expect( service.rewriteURI( "/absolute" ) ).toBe( "/absolute" );
        expect( service.rewriteURI( "/" ) ).toBe( "/" );
        expect( service.rewriteURI( "relative" ) ).toBe( "relative" );
    } );

    it( 'should rewrite URI to absolute', () => {
        expect( service.makeAbsolute( new Path( "/" ), service.rewriteURI( "relative" ) ) ).toBe( "/wiki/relative" );
    } );
} );
