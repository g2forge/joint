import { Component, OnInit, ElementRef, ViewEncapsulation } from '@angular/core';

import { Location } from '@angular/common';
import { HttpClient, HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { Router, ActivatedRoute, NavigationExtras } from '@angular/router';
import { of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { WikiLinksService, WikiPath, WikiPathPrefix, AssetsPathPrefix, RouterLinkAttribute } from './wikilinks.service';
import { SafeHtmlPipe } from '../safe-html.pipe';
import { ToolbarComponent } from '../toolbar/toolbar.component';

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
    path: WikiPath | null = null;
    // The fragment to scroll to
    fragment: string | null = null;

    constructor(
        private http: HttpClient,
        private router: Router,
        private route: ActivatedRoute,
        private element: ElementRef,
        private location: Location,
        private wikiLinksService: WikiLinksService
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
        this.path = WikiPath.create( path );

        // HTTP GET the content
        this.http.get( this.wikiLinksService.getContentPath( path ), { responseType: 'text', observe: 'response' } ).pipe(
            catchError( ( error: HttpErrorResponse ) => {
                this.setContent( "<h1>Page not found</h1>\n<p><span>" + this.path?.getParent( -1 ).resolve().toActual() + "</span> could not be found!</p>" );
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
                    var redirect: string = this.location.prepareExternalUrl( this.wikiLinksService.getContentPath( path ) );
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
            var elements: HTMLElement[] = this.element.nativeElement.querySelectorAll( ".wiki-content" );
            this.wikiLinksService.createContext( this.location, this.getPath() ).rewrite( elements, a => () => {
                var href = a.getAttribute( RouterLinkAttribute );
                if ( href == null ) return true;

                // Routes are relative to the page directory, but if we're currently displaying a directory (e.g. an index.html) then this *IS* the page directory
                var base = this.getPath().getParent( this.getPath().getLast().isHTML() ? -2 : -1 );

                var extras: NavigationExtras = {};
                var split: string[] = href.split( '#' );
                if ( split.length > 1 ) extras.fragment = split[1];
                var joined = base.append(WikiPath.create(split[0])).resolve().toActual();
                this.router.navigate( [joined], extras );
                return false;
            } );

            this.scroll();
        }, 10 );
    }

    getPath(): WikiPath {
        if ( this.path == null ) throw new Error( "No path" );
        return this.path;
    }

    /**
     * Try to scroll to the relevant fragment.
     */
    scroll() {
        if ( this.fragment === null ) return;

        const element = document.getElementById( this.fragment );
        if ( element != null ) element.scrollIntoView( { behavior: "smooth", block: "start", inline: "nearest" } );
    }
}
