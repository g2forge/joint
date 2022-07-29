import { Injectable } from '@angular/core';
import { Location } from '@angular/common';

export const WikiPathPrefix: string = "wiki";
export const AssetsPathPrefix: string = "assets";
export const HREFAttribute: string = "href";
export const RouterLinkAttribute: string = "router-link";

/**
 * A component of a WikiPath.
  */
class PathComponent {
    /** The user friendly rendering of this path component. */
    public display: string;
    /** The server friendly (actual) path component. */
    public actual: string;

    public constructor( component: string ) {
        this.display = component;
        this.actual = component;
    }

    public isHTML(): boolean {
        return this.actual.endsWith( ".html" );
    }

    public isIndex(): boolean {
        return this.actual == 'index.html';
    }
}

/**
 * A path.
 */
export class WikiPath {
    public constructor( readonly components: PathComponent[] ) { }

    static create( path: string ): WikiPath {
        // Store the display path component
        var components: PathComponent[] = path.split( '/' ).filter( component => component != "" ).map( component => new PathComponent( component ) );
        // Remove any ".html" suffix from the page for a prettier display
        if ( components.length > 0 ) {
            let last = components[components.length - 1];
            last.display = last.isIndex() ? "" : last.display.replace( /\.html$/, "" );
        }
        return new WikiPath( components );
    }

    public getLast(): PathComponent {
        if ( this.components.length <= 0 ) return new PathComponent( "" );
        return this.components[this.components.length - 1];
    }

    /**
     * Create a WikiPath to a parent of this one.
     * @param end The index (exclusive) of the last part of the path to include in this returned path. Can be negative to specify removing a certain number of items (-1 removes 0 items, -2 removes 1 item, etc)
     */
    public getParent( end: number ): WikiPath {
        if ( end < 0 ) end = this.components.length + end + 1;
        if ( end <= 0 ) return new WikiPath( [] );
        return new WikiPath( this.components.slice( 0, end ) );
    }

    public toActual( asset: boolean ): string {
        const prefix = asset ? AssetsPathPrefix + "/" + WikiPathPrefix : WikiPathPrefix;

        if ( this.components.length < 1 ) return "/" + prefix;
        return "/" + prefix + '/' + this.components.map( component => component.actual ).join( "/" );
    }

    public append( suffix: WikiPath ): WikiPath {
        return new WikiPath( this.components.concat( suffix.components ) );
    }

    public resolve(): WikiPath {
        var components = this.components.filter( component => component.actual !== '.' );

        if ( components.length > 0 ) {
            var i = 1;
            while ( i < components.length ) {
                if ( ( components[i].actual === '..' ) && ( components[i - 1].actual !== '..' ) ) {
                    components.splice( i - 1, 2 );
                    i = Math.min( 1, i - 1 );
                } else i++;
            }
        }

        return new WikiPath( components );
    }
}

export class WikiRewriteContext {
    constructor( readonly location: Location, readonly path: WikiPath ) { }

    rewriteAnchorHREF( href: string | null ): string | null {
        return this.rewriteURI( href );
    }

    rewriteImgSrc( src: string | null ): string | null {
        var rewritten = this.makeAbsolute( this.rewriteURI( src ), true );
        if ( rewritten === null ) return null;
        return this.location.prepareExternalUrl( rewritten );
    }

    protected rewriteURI( uri: string | null ): string | null {
        // Don't modify URLs (which have a scheme like HTTP), only URIs
        if ( ( uri == null ) || this.hasScheme( uri ) ) return null;
        return uri;
    }

    protected makeAbsolute( uri: string | null, asset: boolean ): string | null {
        if ( uri == null ) return null;

        if ( uri.startsWith( "/" ) ) return uri;

        // Get a link to the "current" directory (remove the HTML file name if there is one)
        var lastIsFile = this.path.getLast().isHTML();
        var directoryPathLink = this.path.getParent( -( lastIsFile ? 2 : 1 ) );
        // Construct an absolute path by concatenating the current directory before the target URI
        return directoryPathLink.append( WikiPath.create( uri ) ).resolve().toActual( asset );
    }

    protected hasScheme( uri: string | null ): boolean {
        return ( uri != null ) && uri.match( /^[a-zA-Z]+:.*$/ ) != null;
    }

    /**
     * Rewrite all the wiki content as appropriate. 
     */
    rewrite( html: string, contentType: ( ( uri: string | null ) => Promise<string | null> ) | null ): Promise<string> {
        const domParser = new DOMParser();
        const element: HTMLElement = domParser.parseFromString( html, 'text/html' ).documentElement;

        var promises: Promise<any>[] = [];
        var anchors: NodeListOf<HTMLElement> = element.querySelectorAll( "a" );
        anchors.forEach( a => {
            var rewritten = this.rewriteAnchorHREF( a.getAttribute( HREFAttribute ) );
            if ( rewritten !== null ) {
                var isHTML: Promise<boolean>;
                if ( contentType == null ) isHTML = Promise.resolve( true );
                else isHTML = contentType( this.makeAbsolute( rewritten, true ) ).then( resolved => ( resolved == null ) || ( resolved.match( /^text\/html(;.*)?$/i ) != null ) );
                promises.push( isHTML );

                isHTML.then( resolved => {
                    var absolute = this.makeAbsolute( rewritten, !resolved );
                    if ( absolute !== null ) {
                        // Set the HREF to an absolute link, because the browser side paths are complex due to HTML5/Angular cleverness
                        a.setAttribute( HREFAttribute, absolute );
                        if ( resolved ) a.setAttribute( RouterLinkAttribute, absolute );
                    }
                } );
            }
        } );

        var images: NodeListOf<HTMLElement> = element.querySelectorAll( "img" );
        images.forEach( img => {
            const attribute = "src";
            var rewritten = this.rewriteImgSrc( img.getAttribute( attribute ) );
            if ( rewritten !== null ) img.setAttribute( attribute, rewritten );
        } );

        return Promise.all( promises ).then( resolved => element.outerHTML );
    }
}

@Injectable( {
    providedIn: 'root'
} )
export class WikiLinksService {
    constructor() { }

    /**
     * Get the server side path to the wiki content to be display.
     * @param path The "wiki" path to the content.
     */
    getContentPath( path: string ): string {
        return AssetsPathPrefix + "/" + WikiPathPrefix + "/" + path;
    }

    createContext( location: Location, path: WikiPath ): WikiRewriteContext {
        return new WikiRewriteContext( location, path );
    }
}
