import { Component, OnInit, ElementRef, ViewEncapsulation } from '@angular/core';

import { Location } from '@angular/common';
import { HttpClient, HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { Router, ActivatedRoute, NavigationExtras } from '@angular/router';
import { of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { SafeHtmlPipe } from '../safe-html.pipe';
import { ToolbarComponent } from '../toolbar/toolbar.component';

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
class Path {
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

@Component( {
    selector: 'app-wiki',
    templateUrl: './wiki.component.html',
    styleUrls: ['./wiki.component.scss'],
    encapsulation: ViewEncapsulation.None
} )
export class WikiComponent implements OnInit {
    // Actual HTML content to display
    content: string = "";
    // The path within the wiki
    path: Path | null = null;
    // The fragment to scroll to
    fragment: string | null = null;

    constructor(
        private http: HttpClient,
        private router: Router,
        private route: ActivatedRoute,
        private element: ElementRef,
        private location: Location
    ) { }

    ngOnInit(): void {
        this.route.url.subscribe( segments => {
            this.load( segments.map( s => s.path ).join( "/" ) );
        } );
        this.route.fragment.subscribe( ( fragmentValue: string ) => {
            // Save the fragment, and try to scroll to it
            // We save the fragment because this callback may happen before the content loads, so scroll may not work
            this.fragment = fragmentValue;
            this.scroll();
        } );
    }

    /**
     * Load wiki content from the server and display it.
     * @param path The path within the wiki to load & display.
     */
    load( path: string ): void {
        if ( path === null && path === undefined ) return;
        this.path = new Path( path );

        // HTTP GET the content
        this.http.get( this.getContentPath( path ), { responseType: 'text', observe: 'response' } ).pipe(
            catchError( ( error: HttpErrorResponse ) => {
                this.setContent( "<h1>Page not found</h1>\n<p><span>" + this.path?.getPathLink( -1 ) + "</span> could not be found!</p>" );
                console.log( error );
                return of( null );
            } )
        ).subscribe( response => {
            if ( response != null ) {
                var contentType = response.headers.get( "Content-Type" );
                console.log( "Content type: " + contentType );
                if ( ( response.body != null ) && ( ( contentType == null ) || ( contentType.match( /^text\/html(;.*)?$/i ) != null ) ) ) {
                    console.log( "Displaying HTML content inline" );
                    this.setContent( response.body.toString() );
                } else {
                    var redirect: string = this.location.prepareExternalUrl( this.getContentPath( path ) );
                    console.log( "Redirecting to static content: " + redirect );
                    window.location.replace( redirect );
                }
            }
        } );
    }

    setContent( content: string ) {
        // Save the content
        this.content = content;

        // Wait 10ms and then do anything that depends on the content, since things like the fragment may be specified before the content is loaded.
        setTimeout( () => {
            this.scroll();
            this.rewrite();
        }, 10 );
    }
    /**
     * Rewrite all the wiki content as appropriate.  In particular this changes links to use the router.
     */
    rewrite() {
        var anchors: HTMLElement[] = this.element.nativeElement.querySelectorAll( ".wiki-content a" );
        anchors.forEach( a => {
            var updated = this.rewriteURI( a.getAttribute( "href" ), false );
            if ( updated === null ) return;

            a.setAttribute( "href", updated );
            a.onclick = () => {
                var href = a.getAttribute( "href" );
                // Don't route when the URI has a scheme (should never be triggered as these URIs aren't rewritten)
                if ( ( href == null ) || this.hasScheme( href ) ) return true;

                // Routes are relative to the page directory, but if we're currently displaying a directory (e.g. an index.html) then this *IS* the page directory
                var base = this.getPath().getLast().isHTML() ? this.route.parent : this.route;

                var extras: NavigationExtras = { relativeTo: base };
                var split: string[] = href.split( '#' );
                if ( split.length > 1 ) extras.fragment = split[1];
                this.router.navigate( [split[0]], extras );
                return false;
            };
        } );

        var images: HTMLElement[] = this.element.nativeElement.querySelectorAll( ".wiki-content img" );
        images.forEach( img => {
            var updated = this.rewriteURI( img.getAttribute( "src" ), true );
            if ( updated === null ) return;

            var rewritten = updated.startsWith( "/assets" ) ? updated : ( '/assets' + updated );
            img.setAttribute( "src", this.location.prepareExternalUrl( rewritten ) );
        } );
    }

    getPath(): Path {
        if ( this.path == null ) throw new Error( "No path" );
        return this.path;
    }

    rewriteURI( uri: string | null, makeAbsolute: boolean ): string | null {
        // Don't modify URLs (which have a scheme like HTTP), only URIs
        if ( ( uri == null ) || this.hasScheme( uri ) ) return null;

        if ( makeAbsolute ) {
            var absolute = uri.startsWith( "/" );
            if ( !absolute ) {
                var path: Path = this.getPath();

                // Get a link to the "current" directory (remove the HTML file name if there is one)
                var lastIsFile = path.getLast().isHTML();
                var directoryPathLink = path.getPathLink( -( lastIsFile ? 2 : 1 ) );
                // Construct an absolute path by concatenating the current directory before the target URI
                return directoryPathLink + "/" + uri;
            }
        }
        return uri;
    }

    hasScheme( url: string | null ): boolean {
        return ( url != null ) && url.match( /^[a-zA-Z]+:.*$/ ) != null;
    }

    /**
     * Try to scroll to the relevant fragment.
     */
    scroll() {
        if ( this.fragment === null ) return;
        
        const element = document.getElementById( this.fragment );
        if ( element != null ) element.scrollIntoView( { behavior: "smooth", block: "start", inline: "nearest" } );
    }

    /**
     * Get the server side path to the wiki content to be display.
     * @param path The "wiki" path to the content.
     */
    getContentPath( path: string ): string {
        return "assets/wiki/".concat( path );
    }
}
