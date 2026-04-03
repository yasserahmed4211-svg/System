// angular import
import { Component, OnInit, inject, DestroyRef, ChangeDetectionStrategy } from '@angular/core';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { filter } from 'rxjs/operators';

// project import
import { SpinnerComponent } from 'src/app/theme/shared/components/spinner/spinner.component';
import { LanguageService } from './core/services/language.service';
import { ErpNotificationContainerComponent } from 'src/app/shared/components/erp-notification-container/erp-notification-container.component';

@Component({
  selector: 'app-root',
  standalone: true,
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
  imports: [RouterOutlet, SpinnerComponent, ErpNotificationContainerComponent],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AppComponent implements OnInit {
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);
  // Initialize language service at app startup
  private languageService = inject(LanguageService);

  title = 'erp-system';

  ngOnInit(): void {
    this.router.events
      .pipe(
        filter((evt): evt is NavigationEnd => evt instanceof NavigationEnd),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(() => window.scrollTo(0, 0));
  }
}
