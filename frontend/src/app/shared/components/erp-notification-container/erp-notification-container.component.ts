import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';

import { ErpNotificationService, ErpNotificationType } from 'src/app/shared/services/erp-notification.service';

@Component({
  selector: 'erp-notification-container',
  standalone: true,
  imports: [CommonModule, TranslateModule],
  templateUrl: './erp-notification-container.component.html',
  styleUrl: './erp-notification-container.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ErpNotificationContainerComponent {
  private readonly notificationsService = inject(ErpNotificationService);

  readonly notifications = this.notificationsService.notifications;

  dismiss(id: number): void {
    this.notificationsService.dismiss(id);
  }

  cssClass(type: ErpNotificationType): string {
    switch (type) {
      case 'success':
        return 'alert-success';
      case 'error':
        return 'alert-danger';
      case 'warning':
        return 'alert-warning';
      default:
        return 'alert-info';
    }
  }
}
