// angular import
import { Component, HostListener, output, inject } from '@angular/core';

// project import
import { SharedModule } from 'src/app/theme/shared/shared.module';
import { NavLeftComponent } from './nav-left/nav-left.component';
import { NavRightComponent } from './nav-right/nav-right.component';
import { LayoutStateService } from 'src/app/theme/shared/service/layout-state.service';

@Component({
  selector: 'app-nav-bar',
  imports: [SharedModule, NavLeftComponent, NavRightComponent],
  templateUrl: './nav-bar.component.html',
  styleUrls: ['./nav-bar.component.scss']
})
export class NavBarComponent {
  private readonly layout = inject(LayoutStateService);

  // public props
  readonly NavCollapse = output();
  readonly NavCollapsedMob = output<void>();

  get navCollapsed(): boolean {
    return this.layout.sidebarCollapsed();
  }

  // public method
  navCollapse() {
    if (!this.layout.isMobile()) {
      this.NavCollapse.emit();
    }
  }

  @HostListener('window:resize', ['$event'])
  // eslint-disable-next-line
  onResize(event: any): void {
    this.layout.updateWidth(event.target.innerWidth);
  }

  navCollapseMob(): void {
    if (this.layout.isMobile()) {
      this.NavCollapsedMob.emit();
    }
  }
}
