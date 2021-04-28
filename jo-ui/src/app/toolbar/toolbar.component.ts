import { Component, OnInit } from '@angular/core';

import { Title } from '@angular/platform-browser';
import { ConfigService, JointConfig } from '../config.service';

@Component({
	selector: 'app-toolbar',
	templateUrl: './toolbar.component.html',
	styleUrls: ['./toolbar.component.scss']
})
export class ToolbarComponent implements OnInit {
	config: JointConfig | null = null;

	constructor(
		private configService: ConfigService,
		private titleService: Title
	) { }

	ngOnInit(): void {
		this.configService.getConfig().subscribe((data: JointConfig) => {
			this.config = data;
			if (this.config != null) this.titleService.setTitle(this.config!.title);
		});
	}
}
