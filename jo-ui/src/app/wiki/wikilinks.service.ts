import { Injectable } from '@angular/core';

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
    public components: PathComponent[];

    public constructor( path: string ) {
        // Store the display path component
        this.components = path.split( '/' ).filter( component => component != "" ).map( component => new PathComponent( component ) );
        // Remove any ".html" suffix from the page for a prettier display
        if ( this.components.length > 0 ) {
            let last = this.components[this.components.length - 1];
            last.display = last.isIndex() ? "" : last.display.replace( /\.html$/, "" );
        }
    }

    public getLast(): PathComponent {
        if ( this.components.length <= 0 ) return new PathComponent( "" );
        return this.components[this.components.length - 1];
    }

    /**
     * Create a router link to this path, or a subset thereof.
     * @param end The index (exclusive) of the last part of the path to include in this link. Can be negative to specify removing a certain number of items (-1 removes 0 items, -2 removes 1 item, etc)
     */
    public getPathLink( end: number ): string {
        if ( end < 0 ) end = this.components.length + end + 1;
        if ( this.components.length < 1 ) return '/wiki';
        return '/wiki/' + this.components.slice( 0, end ).map( component => component.actual ).join( "/" );
    }
}

@Injectable( {
    providedIn: 'root'
} )
export class WikiLinksService {
    constructor() { }

    rewriteURI( uri: string | null ): string | null {
        // Don't modify URLs (which have a scheme like HTTP), only URIs
        if ( ( uri == null ) || this.hasScheme( uri ) ) return null;
        return uri;
    }

    makeAbsolute( path: WikiPath, uri: string | null ): string | null {
        if ( uri == null ) return null;

        if ( uri.startsWith( "/" ) ) return uri;

        // Get a link to the "current" directory (remove the HTML file name if there is one)
        var lastIsFile = path.getLast().isHTML();
        var directoryPathLink = path.getPathLink( -( lastIsFile ? 2 : 1 ) );
        // Construct an absolute path by concatenating the current directory before the target URI
        return directoryPathLink + "/" + uri;
    }

    hasScheme( uri: string | null ): boolean {
        return ( uri != null ) && uri.match( /^[a-zA-Z]+:.*$/ ) != null;
    }
}
