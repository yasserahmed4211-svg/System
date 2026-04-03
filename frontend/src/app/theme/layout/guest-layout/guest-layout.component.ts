// Angular import
import { Component } from '@angular/core';

// project import
import { ConfigurationComponent } from '../admin-layout/configuration/configuration.component';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-guest-layouts',
  imports: [ConfigurationComponent, RouterModule],
  templateUrl: './guest-layout.component.html',
  styleUrl: './guest-layout.component.scss'
})
export class GuestLayouts {}
