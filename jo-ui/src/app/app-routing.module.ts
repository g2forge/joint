import { NgModule } from '@angular/core';
import { RouterModule, Routes, ExtraOptions } from '@angular/router';

import { WikiComponent } from './wiki/wiki.component';
import { NotfoundComponent } from './notfound/notfound.component';

const routes: Routes = [
    { path: 'wiki', children: [ { path: "**", component: WikiComponent } ] },
    { path: '', redirectTo: '/wiki', pathMatch: 'full' },
    { path: '**', component: NotfoundComponent }
];

const options: ExtraOptions = {
	anchorScrolling: 'enabled'
};

@NgModule( {
    imports: [RouterModule.forRoot( routes, options )],
    exports: [RouterModule]
} )
export class AppRoutingModule { }
