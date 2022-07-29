import { TestBed } from '@angular/core/testing';
import { Injectable } from '@angular/core';

import { Location } from '@angular/common';
import { WikiLinksService, WikiPath, WikiRewriteContext, HREFAttribute, RouterLinkAttribute } from './wiki-links.service';

/**
 * Expose methods for testing.
 */
export class WikiRewriteContextExtended extends WikiRewriteContext {
    constructor( readonly location: Location, readonly path: WikiPath ) { super( location, path ); }

    hasScheme( uri: string | null ): boolean {
        return super.hasScheme( uri );
    }

    rewriteURI( uri: string | null ): string | null {
        return super.rewriteURI( uri );
    }

    makeAbsolute( uri: string | null, asset: boolean ): string | null {
        return super.makeAbsolute( uri, asset );
    }
}

class RewrittenLink {
    /** The normal link. */
    public href: string | null;
    /** The router link */
    public router: string | null;

    public constructor( html: string ) {
        const domParser = new DOMParser();
        const element: HTMLElement = domParser.parseFromString( html, 'text/html' ).documentElement;
        const link = element.querySelector( "a" );

        if ( link != null ) {
            this.href = link.getAttribute( HREFAttribute );
            this.router = link.getAttribute( RouterLinkAttribute );
        } else {
            this.href = null;
            this.router = null;
        }
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

    test( path: string, link: string ): Promise<RewrittenLink> {
        const context = this.createContext( TestBed.inject( Location ), WikiPath.create( path ) );
        return context.rewrite( "<a href=\"" + link + "\">text</a>", ( uri: string | null ) => {
            if ( uri != null ) {
                // Handle known file extensions (this is just for testing, remember)
                if ( uri.endsWith( ".json" ) ) return Promise.resolve( "application/json" );
            }
            return Promise.resolve( "text/html" );
        } ).then( result => new RewrittenLink( result ) );
    }
}

describe( 'WikiPath', () => {
    it( 'should resolve empty path', () => {
        expect( WikiPath.create( "" ).resolve() ).toEqual( WikiPath.create( "" ) );
    } );

    it( 'should resolve a path', () => {
        expect( WikiPath.create( "a" ).resolve() ).toEqual( WikiPath.create( "a" ) );
    } );

    it( 'should resolve ax path', () => {
        expect( WikiPath.create( "a/x" ).resolve() ).toEqual( WikiPath.create( "a/x" ) );
    } );

    it( 'should resolve self path', () => {
        expect( WikiPath.create( "." ).resolve() ).toEqual( WikiPath.create( "" ) );
    } );

    it( 'should resolve self/a path', () => {
        expect( WikiPath.create( "./a" ).resolve() ).toEqual( WikiPath.create( "a" ) );
    } );

    it( 'should resolve a/parent path', () => {
        expect( WikiPath.create( "a/.." ).resolve() ).toEqual( WikiPath.create( "" ) );
    } );

    it( 'should resolve parent path', () => {
        expect( WikiPath.create( ".." ).resolve() ).toEqual( WikiPath.create( ".." ) );
    } );

    it( 'should resolve a/parent/b/parent path', () => {
        expect( WikiPath.create( "a/../b/.." ).resolve() ).toEqual( WikiPath.create( "" ) );
    } );

    it( 'should resolve a/b/parent/parent path', () => {
        expect( WikiPath.create( "a/b/../.." ).resolve() ).toEqual( WikiPath.create( "" ) );
    } );
} );

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
        expect( service.createContext( TestBed.inject( Location ), WikiPath.create( "" ) ).makeAbsolute( context.rewriteURI( "relative" ), false ) ).toBe( "/wiki/relative" );
        expect( service.createContext( TestBed.inject( Location ), WikiPath.create( "subdirectory" ) ).makeAbsolute( context.rewriteURI( "relative" ), false ) ).toBe( "/wiki/subdirectory/relative" );
    } );

    it( 'should prefix content paths', () => {
        expect( service.getContentPath( "absolute" ) ).toBe( "assets/wiki/absolute" );
    } );

    it( 'should rewrite img src correctly', () => {
        expect( context.rewriteImgSrc( "image.png" ) ).toBe( TestBed.inject( Location ).prepareExternalUrl( "/assets/wiki/image.png" ) );
    } );



    it( 'should rewrite link - root to a relative simple', ( done ) => {
        service.test( "", "a" ).then( result => {
            expect( result.href ).toBe( "/wiki/a" );
            expect( result.router ).toBe( "/wiki/a" );
            done();
        } );
    } );

    it( 'should rewrite link - root to a relative explicit', ( done ) => {
        service.test( "", "./a" ).then( result => {
            expect( result.href ).toBe( "/wiki/a" );
            expect( result.router ).toBe( "/wiki/a" );
            done();
        } );
    } );

    it( 'should rewrite link - root to a absolute', ( done ) => {
        service.test( "", "/wiki/a" ).then( result => {
            expect( result.href ).toBe( "/wiki/a" );
            expect( result.router ).toBe( "/wiki/a" );
            done();
        } );
    } );

    it( 'should rewrite link - root to external', ( done ) => {
        service.test( "", "http://example.com" ).then( result => {
            expect( result.href ).toBe( "http://example.com" );
            expect( result.router ).toBeNull();
            done();
        } );
    } );

    it( 'should rewrite link - root to spaces', ( done ) => {
        service.test( "", "./a b.html" ).then( result => {
            expect( result.href ).toBe( "/wiki/a b.html" );
            expect( result.router ).toBe( "/wiki/a b.html" );
            done();
        } );
    } );



    it( 'should rewrite link - root to data relative simple', ( done ) => {
        service.test( "", "data.json" ).then( result => {
            expect( result.href ).toBe( "/assets/wiki/data.json" );
            expect( result.router ).toBeNull();
            done();
        } );
    } );

    it( 'should rewrite link - root to data relative explicit', ( done ) => {
        service.test( "", "data.json" ).then( result => {
            expect( result.href ).toBe( "/assets/wiki/data.json" );
            expect( result.router ).toBeNull();
            done();
        } );
    } );

    it( 'should rewrite link - root to data absolute', ( done ) => {
        service.test( "", "/data.json" ).then( result => {
            expect( result.href ).toBe( "/data.json" );
            expect( result.router ).toBeNull();
            done();
        } );
    } );



    it( 'should rewrite link - self fragment', ( done ) => {
        service.test( "", "#fragment" ).then( result => {
            expect( result.href ).toBe( "/wiki/#fragment" );
            expect( result.router ).toBe( "/wiki/#fragment" );
            done();
        } );
    } );

    it( 'should rewrite link - root to b/y# relative simple', ( done ) => {
        service.test( "", "b/y.html#fragment" ).then( result => {
            expect( result.href ).toBe( "/wiki/b/y.html#fragment" );
            expect( result.router ).toBe( "/wiki/b/y.html#fragment" );
            done();
        } );
    } );

    it( 'should rewrite link - root to b/y# relative explicit', ( done ) => {
        service.test( "", "./b/y.html#fragment" ).then( result => {
            expect( result.href ).toBe( "/wiki/b/y.html#fragment" );
            expect( result.router ).toBe( "/wiki/b/y.html#fragment" );
            done();
        } );
    } );

    it( 'should rewrite link - root to b/y# absolute', ( done ) => {
        service.test( "", "/wiki/b/y.html#fragment" ).then( result => {
            expect( result.href ).toBe( "/wiki/b/y.html#fragment" );
            expect( result.router ).toBe( "/wiki/b/y.html#fragment" );
            done();
        } );
    } );



    it( 'should rewrite link - a to a/x relative simple', ( done ) => {
        service.test( "a", "x.html" ).then( result => {
            expect( result.href ).toBe( "/wiki/a/x.html" );
            expect( result.router ).toBe( "/wiki/a/x.html" );
            done();
        } );
    } );

    it( 'should rewrite link - a to a/x relative explicit', ( done ) => {
        service.test( "a", "./x.html" ).then( result => {
            expect( result.href ).toBe( "/wiki/a/x.html" );
            expect( result.router ).toBe( "/wiki/a/x.html" );
            done();
        } );
    } );

    it( 'should rewrite link - a to a/x absolute', ( done ) => {
        service.test( "a", "/wiki/a/x.html" ).then( result => {
            expect( result.href ).toBe( "/wiki/a/x.html" );
            expect( result.router ).toBe( "/wiki/a/x.html" );
            done();
        } );
    } );



    it( 'should rewrite link - a/x to a relative simple file', ( done ) => {
        service.test( "a/x.html", "index.html" ).then( result => {
            expect( result.href ).toBe( "/wiki/a/index.html" );
            expect( result.router ).toBe( "/wiki/a/index.html" );
            done();
        } );
    } );

    it( 'should rewrite link - a/x to a relative explicit file', ( done ) => {
        service.test( "a/x.html", "./index.html" ).then( result => {
            expect( result.href ).toBe( "/wiki/a/index.html" );
            expect( result.router ).toBe( "/wiki/a/index.html" );
            done();
        } );
    } );

    it( 'should rewrite link - a/x to a relative explicit directory', ( done ) => {
        service.test( "a/x.html", "./" ).then( result => {
            expect( result.href ).toBe( "/wiki/a" );
            expect( result.router ).toBe( "/wiki/a" );
            done();
        } );
    } );

    it( 'should rewrite link - a/x to a absolute file', ( done ) => {
        service.test( "a/x.html", "/wiki/a/index.html" ).then( result => {
            expect( result.href ).toBe( "/wiki/a/index.html" );
            expect( result.router ).toBe( "/wiki/a/index.html" );
            done();
        } );
    } );

    it( 'should rewrite link - a/x to a absolute directory', ( done ) => {
        service.test( "a/x.html", "/wiki/a" ).then( result => {
            expect( result.href ).toBe( "/wiki/a" );
            expect( result.router ).toBe( "/wiki/a" );
            done();
        } );
    } );



    it( 'should rewrite link - a/x to b relative file', ( done ) => {
        service.test( "a/x.html", "../b/index.html" ).then( result => {
            expect( result.href ).toBe( "/wiki/b/index.html" );
            expect( result.router ).toBe( "/wiki/b/index.html" );
            done();
        } );
    } );

    it( 'should rewrite link - a/x to b relative directory', ( done ) => {
        service.test( "a/x.html", "../b" ).then( result => {
            expect( result.href ).toBe( "/wiki/b" );
            expect( result.router ).toBe( "/wiki/b" );
            done();
        } );
    } );

    it( 'should rewrite link - a/x to b absolute file', ( done ) => {
        service.test( "a/x.html", "/wiki/b/index.html" ).then( result => {
            expect( result.href ).toBe( "/wiki/b/index.html" );
            expect( result.router ).toBe( "/wiki/b/index.html" );
            done();
        } );
    } );

    it( 'should rewrite link - a/x to b absolute directory', ( done ) => {
        service.test( "a/x.html", "/wiki/b" ).then( result => {
            expect( result.href ).toBe( "/wiki/b" );
            expect( result.router ).toBe( "/wiki/b" );
            done();
        } );
    } );
} );
