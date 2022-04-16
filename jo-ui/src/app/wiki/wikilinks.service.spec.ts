import { TestBed } from '@angular/core/testing';
import { Injectable } from '@angular/core';

import { Location } from '@angular/common';
import { WikiLinksService, WikiPath, WikiRewriteContext } from './wikilinks.service';

export class WikiRewriteContextExtended extends WikiRewriteContext {
    constructor( readonly location: Location, readonly path: WikiPath ) { super( location, path ); }

    hasScheme( uri: string | null ): boolean {
        return super.hasScheme( uri );
    }

    rewriteURI( uri: string | null ): string | null {
        return super.rewriteURI( uri );
    }

    makeAbsolute( uri: string | null ): string | null {
        return super.makeAbsolute( uri );
    }
}

@Injectable( {
    providedIn: 'root'
} )
class WikiLinksServiceExtended extends WikiLinksService {
    constructor() { super(); }

    createContext( location: Location, path: WikiPath ): WikiRewriteContextExtended {
        return new WikiRewriteContextExtended( location, path );
    }
}

describe( 'WikiLinksService', () => {
    let service: WikiLinksServiceExtended;
    let context: WikiRewriteContextExtended;

    beforeEach( () => {
        TestBed.configureTestingModule( {} );
        service = TestBed.inject( WikiLinksServiceExtended );
        context = service.createContext( TestBed.inject( Location ), WikiPath.create( "" ) );
    } );

    it( 'should be created', () => {
        expect( service ).toBeTruthy();
    } );

    it( 'should recognize http', () => {
        expect( context.hasScheme( "http://example.com/" ) ).toBeTrue();
    } );

    it( 'should recognize https', () => {
        expect( context.hasScheme( "https://example.com/" ) ).toBeTrue();
    } );

    it( 'should recognize mailto', () => {
        expect( context.hasScheme( "mailto:user@example.com" ) ).toBeTrue();
    } );

    it( 'should recognize non-scheme urls', () => {
        expect( context.hasScheme( "dir/file.html" ) ).toBeFalse();
    } );

    it( 'shouldn\'t rewrite uris with schemes', () => {
        expect( context.rewriteURI( "https://example.com/" ) ).toBeNull();
    } );

    it( 'shouldn\'t rewrite some URIs', () => {
        expect( context.rewriteURI( "/absolute" ) ).toBe( "/absolute" );
        expect( context.rewriteURI( "/" ) ).toBe( "/" );
        expect( context.rewriteURI( "relative" ) ).toBe( "relative" );
    } );

    it( 'should rewrite URI to absolute', () => {
        expect( service.createContext( TestBed.inject( Location ), WikiPath.create( "" ) ).makeAbsolute( context.rewriteURI( "relative" ) ) ).toBe( "/wiki/relative" );
        expect( service.createContext( TestBed.inject( Location ), WikiPath.create( "subdirectory" ) ).makeAbsolute( context.rewriteURI( "relative" ) ) ).toBe( "/wiki/subdirectory/relative" );
    } );

    it( 'should prefix content paths', () => {
        expect( service.getContentPath( "absolute" ) ).toBe( "assets/wiki/absolute" );
    } );

    it( 'should rewrite img src correctly', () => {
        expect( context.rewriteImgSrc( "image.png" ) ).toBe( TestBed.inject( Location ).prepareExternalUrl( "/assets/wiki/image.png" ) );
    } );
} );
