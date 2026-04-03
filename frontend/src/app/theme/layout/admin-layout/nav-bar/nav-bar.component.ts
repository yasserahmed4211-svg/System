// angular import
import { Component, HostListener, output } from '@angular/core';

// project import
import { MantisConfig } from 'src/app/app-config';
import { SharedModule } from 'src/app/theme/shared/shared.module';
import { NavLeftComponent } from './nav-left/nav-left.component';
import { NavRightComponent } from './nav-right/nav-right.component';

@Component({
  selector: 'app-nav-bar',
  imports: [SharedModule, NavLeftComponent, NavRightComponent],
  templateUrl: './nav-bar.component.html',
  styleUrls: ['./nav-bar.component.scss']
})
export class NavBarComponent {
  // public props
  readonly NavCollapse = output();
  readonly NavCollapsedMob = output<void>();

  navCollapsed;
  windowWidth: number;
  navCollapsedMob;

  // Constructor
  constructor() {
    this.windowWidth = window.innerWidth;
    this.navCollapsed = this.windowWidth >= 1025 ? MantisConfig.isCollapseMenu : false;
    this.navCollapsedMob = false;
  }

  // public method
  navCollapse() {
    if (this.windowWidth >= 1025) {
      this.navCollapsed = !this.navCollapsed;
      this.NavCollapse.emit();
    }
  }

  @HostListener('window:resize', ['$event'])
  // eslint-disable-next-line
  onResize(event: any): void {
    this.windowWidth = event.target.innerWidth;
    this.navCollapseMob();
  }

  navCollapseMob(): void {
    if (this.windowWidth < 1025) {
      this.NavCollapsedMob.emit();
    }
  }
}
