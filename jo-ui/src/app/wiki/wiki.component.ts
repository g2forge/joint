import { Component, OnInit, ElementRef, ViewEncapsulation } from '@angular/core';

import { Location } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
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
    path: PathComponent[] = [];
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
        this.route.fragment.subscribe( ( fragment: string ) => {
            // Save the fragment, and try to scroll to it
            // We save the fragment because this callback may happen before the content loads, so scroll may not work
            this.fragment = fragment;
            this.scroll();
        } );
    }

    /**
     * Load wiki content from the server and display it.
     * @param path The path within the wiki to load & display.
     */
    load( path: string ): void {
        if ( path !== null && path !== undefined ) {
            // Store the display path component
            this.path = path.split( '/' ).filter( component => component != "" ).map( component => new PathComponent( component ) );
            // Remove any ".html" suffix from the page for a prettier display
            if ( this.path.length > 0 ) {
                let last = this.path[this.path.length - 1];
                last.display = last.isIndex() ? "" : last.display.replace( /\.html$/, "" );
            }

            // HTTP GET the content
            this.http.get( this.getContentPath( path ), { responseType: 'text' } ).pipe(
                catchError( ( error: HttpErrorResponse ) => {
                    return of( "<h1>Page not found</h1>\n<p><span>" + this.getPathLink( this.path.length - 1 ) + "</span> could not be found!</p>" );
                } )
            ).subscribe( ( data: string ) => {
                // Save the content
                this.content = data;

                // Wait 1ms and then do anything that depends on the content, since things like the fragment may be specified before the content is loaded.
                setTimeout( () => {
                    this.scroll();
                    this.rewrite();
                }, 1 );
            } );
        }
    }

    /**
     * Rewrite all the wiki content as appropriate.  In particular this changes links to use the router.
     */
    rewrite() {
        var anchors: HTMLElement[] = this.element.nativeElement.querySelectorAll( ".wiki-content a" );
        anchors.forEach( a => {
            var updated = this.rewriteElement( a, "href" );
            if ( updated != null ) {
                a.setAttribute( "href", updated );
                a.onclick = () => {
                    var href = a.getAttribute( "href" );
                    if ( ( href == null ) || this.hasScheme( href ) ) return true;
					var extras: NavigationExtras = { relativeTo: this.route.parent };
					var split: string[] = href.split('#');
					if (split.length > 1) extras.fragment = split[1];
                    this.router.navigate( [split[0]], extras );
                    return false;
                };
            }
        } );

        var images: HTMLElement[] = this.element.nativeElement.querySelectorAll( ".wiki-content img" );
        images.forEach( img => {
            var updated = this.rewriteElement( img, "src" );
            var asset = ( updated != null ) ? updated : img.getAttribute( 'src' );
            img.setAttribute( "src", this.location.prepareExternalUrl( '/assets' + asset ) );
        } );
    }

    rewriteElement( element: HTMLElement, attribute: string ): string | null {
        var uri = element.getAttribute( attribute );
        // Don't modify URLs (which have a scheme like HTTP), only URIs
        if ( ( uri == null ) || this.hasScheme( uri ) ) return null;

        var absolute = uri.startsWith( "/" );
        if ( !absolute ) {
            // Get a link to the "current" directory (remove the HTML file name if there is one)
            var lastIsFile = this.path.length > 0 ? this.path[this.path.length - 1].isHTML() : false;
            var directoryPathLink = this.getPathLink( this.path.length - ( lastIsFile ? 2 : 1 ) );
			// Construct an absolute path by concatenating the current directory before the target URI
            return directoryPathLink + ( directoryPathLink.endsWith( "/" ) ? "" : "/" ) + uri;
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
        if ( this.fragment != null ) {
            const element = document.getElementById( this.fragment );
            if ( element != null ) element.scrollIntoView( { behavior: "smooth", block: "start", inline: "nearest" } );
        }
    }

    /**
     * Create a router link to the relevant parent of this content.
     * @param endInclusive The index (inclusive) of the last part of the path to include in this link.
     */
    getPathLink( endInclusive: number ) {
        return '/wiki/' + this.path.slice( 0, endInclusive + 1 ).map( component => component.actual ).join( "/" );
    }

    /**
     * Get the server side path to the wiki content to be display.
     * @param path The "wiki" path to the content.
     */
    getContentPath( path: string ) {
        return "assets/wiki/".concat( path );
    }
}
