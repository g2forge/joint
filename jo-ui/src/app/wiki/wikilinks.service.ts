import { Injectable } from '@angular/core';
import { Location } from '@angular/common';

export const WikiPathPrefix: string = "wiki";
export const AssetsPathPrefix: string = "assets";
export const RouterLinkAttribute: string = "router-link";

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

    public toActual(): string {
        if ( this.components.length < 1 ) return "/" + WikiPathPrefix;
        return "/" + WikiPathPrefix + '/' + this.components.map( component => component.actual ).join( "/" );
    }

    public append( suffix: WikiPath ): WikiPath {
        return new WikiPath( this.components.concat( suffix.components ) );
    }

    public resolve(): WikiPath {
        return new WikiPath(this.components.filter(component => component.actual !== '.'));
    }
}

export class WikiRewriteContext {
    constructor( readonly location: Location, readonly path: WikiPath ) { }

    rewriteAnchorHREF( href: string | null ): string | null {
        return this.rewriteURI( href );
    }

    rewriteImgSrc( src: string | null ): string | null {
        var updated = this.makeAbsolute( this.rewriteURI( src ) );
        if ( updated === null ) return null;

        var rewritten = updated.startsWith( "/" + AssetsPathPrefix ) ? updated : ( "/" + AssetsPathPrefix + updated );
        return this.location.prepareExternalUrl( rewritten );
    }

    protected rewriteURI( uri: string | null ): string | null {
        // Don't modify URLs (which have a scheme like HTTP), only URIs
        if ( ( uri == null ) || this.hasScheme( uri ) ) return null;
        return uri;
    }

    protected makeAbsolute( uri: string | null ): string | null {
        if ( uri == null ) return null;

        if ( uri.startsWith( "/" ) ) return uri;

        // Get a link to the "current" directory (remove the HTML file name if there is one)
        var lastIsFile = this.path.getLast().isHTML();
        var directoryPathLink = this.path.getParent( -( lastIsFile ? 2 : 1 ) );
        // Construct an absolute path by concatenating the current directory before the target URI
        return directoryPathLink.append( WikiPath.create( uri ) ).resolve().toActual();
    }

    protected hasScheme( uri: string | null ): boolean {
        return ( uri != null ) && uri.match( /^[a-zA-Z]+:.*$/ ) != null;
    }

    /**
     * Rewrite all the wiki content as appropriate.  In particular this changes links to use the router.
     */
    rewrite( elements: HTMLElement[], onclick: ( a: HTMLElement ) => () => boolean ) {
        elements.forEach( element => {
            var anchors: NodeListOf<HTMLElement> = element.querySelectorAll( ".wiki-content a" );
            anchors.forEach( a => {
                const attribute = "href";
                var rewritten = this.rewriteAnchorHREF( a.getAttribute( attribute ) );
                if ( rewritten !== null ) {
                    var absolute = this.makeAbsolute( rewritten );
                    if ( absolute !== null ) a.setAttribute( attribute, absolute );
                    a.setAttribute( RouterLinkAttribute, rewritten );

                    a.onclick = onclick( a );
                }
            } );

            var images: NodeListOf<HTMLElement> = element.querySelectorAll( ".wiki-content img" );
            images.forEach( img => {
                const attribute = "src";
                var rewritten = this.rewriteImgSrc( img.getAttribute( attribute ) );
                if ( rewritten !== null ) img.setAttribute( attribute, rewritten );
            } );
        } );
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
