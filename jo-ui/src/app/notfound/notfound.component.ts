import { Component, OnInit } from '@angular/core';

import { Router, NavigationEnd } from '@angular/router';

import { ToolbarComponent } from '../toolbar/toolbar.component';
import { ConfigService, JointConfig } from '../config.service';

@Component({
	selector: 'app-notfound',
	templateUrl: './notfound.component.html',
	styleUrls: ['./notfound.component.scss']
})
export class NotfoundComponent implements OnInit {
	url: string | null = null;
	rewrites?: any;

	constructor(
		private router: Router,
		private configService: ConfigService
	) { }

	ngOnInit(): void {
		this.load(this.router.url);
		this.router.events.subscribe(event => {
			if (event instanceof NavigationEnd) this.load(event.url);
		});

		this.configService.getConfig().subscribe((data: JointConfig) => {
			this.rewrites = data.rewrites;
			this.rewrite();
		});
	}

	load(url: string): void {
		this.url = url;
		this.rewrite();
	}

	rewrite() {
		if (this.url == null) return;
		if (this.rewrites == undefined) return;

		var current = this.url;
		console.log("Attempting to rewrite " + current);
		for (let [key, value] of Object.entries(this.rewrites)) {
			var replacement = String(value);
			console.log(key + " -> " + replacement);
			current = current.replace(new RegExp(key, ""), String(value));
		}

		if (this.url != current) {
			console.log("Rewrote " + this.url + " to " + current);
			this.router.navigate([current]);
		} else console.log("Rewrite kept " + this.url + " as " + current);
	}
}
