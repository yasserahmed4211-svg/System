import { Injectable, computed, signal } from '@angular/core';

/**
 * Notification type/severity.
 */
export type ErpNotificationType = 'success' | 'error' | 'info' | 'warning';

/**
 * Options for displaying a notification.
 */
export interface ErpNotificationOptions {
  /** Translation key for the notification message */
  messageKey: string;
  /** Parameters for message translation interpolation */
  messageParams?: Record<string, unknown>;
  /** Type/severity of the notification */
  type?: ErpNotificationType;
  /** Duration in milliseconds before auto-dismiss (0 = no auto-dismiss) */
  duration?: number;
  /** Whether the notification can be manually dismissed */
  dismissible?: boolean;
}

/**
 * Internal notification structure.
 */
interface Notification {
  id: number;
  messageKey: string;
  messageParams?: Record<string, unknown>;
  type: ErpNotificationType;
  dismissible: boolean;
  timeoutId?: ReturnType<typeof setTimeout>;
}

/**
 * ErpNotificationService
 * 
 * UI-level service for displaying toast/snackbar notifications.
 * Translation-key based, contains no feature-specific messages.
 * 
 * @requirement FE-REQ-SHARED-001
 * @task TASK-FE-SHARED-001
 */
@Injectable({
  providedIn: 'root'
})
export class ErpNotificationService {
  private readonly notificationsSignal = signal<Notification[]>([]);
  readonly notifications = computed(() => this.notificationsSignal());

  private nextId = 1;
  
  /** Default duration for auto-dismiss in milliseconds */
  private readonly DEFAULT_DURATION = 5000;

  /**
   * Get the current list of active notifications.
   * Used by notification container component for rendering.
   */
  getNotifications(): readonly Notification[] {
    return this.notificationsSignal();
  }

  /**
   * Show a notification with full options.
   * 
   * @param options - Notification configuration
   * @returns The notification ID for manual dismissal
   */
  show(options: ErpNotificationOptions): number {
    const id = this.nextId++;
    const type = options.type || 'info';
    const duration = options.duration ?? this.DEFAULT_DURATION;
    const dismissible = options.dismissible ?? true;

    const notification: Notification = {
      id,
      messageKey: options.messageKey,
      messageParams: options.messageParams,
      type,
      dismissible
    };
    
    // Set auto-dismiss timeout if duration > 0
    if (duration > 0) {
      notification.timeoutId = setTimeout(() => {
        this.dismiss(id);
      }, duration);
    }
    
    this.notificationsSignal.update((list) => [...list, notification]);
    
    return id;
  }

  /**
   * Show a success notification.
   * 
   * @param messageKey - Translation key for the message
   * @param messageParams - Optional parameters for translation
   */
  success(messageKey: string, messageParams?: Record<string, unknown>): number {
    return this.show({
      messageKey,
      messageParams,
      type: 'success'
    });
  }

  /**
   * Show an error notification.
   * Error notifications have longer duration by default.
   * 
   * @param messageKey - Translation key for the message
   * @param messageParams - Optional parameters for translation
   */
  error(messageKey: string, messageParams?: Record<string, unknown>): number {
    return this.show({
      messageKey,
      messageParams,
      type: 'error',
      duration: 8000 // Longer duration for errors
    });
  }

  /**
   * Show a warning notification.
   * 
   * @param messageKey - Translation key for the message
   * @param messageParams - Optional parameters for translation
   */
  warning(messageKey: string, messageParams?: Record<string, unknown>): number {
    return this.show({
      messageKey,
      messageParams,
      type: 'warning'
    });
  }

  /**
   * Show an info notification.
   * 
   * @param messageKey - Translation key for the message
   * @param messageParams - Optional parameters for translation
   */
  info(messageKey: string, messageParams?: Record<string, unknown>): number {
    return this.show({
      messageKey,
      messageParams,
      type: 'info'
    });
  }

  /**
   * Dismiss a specific notification by ID.
   * 
   * @param id - The notification ID to dismiss
   */
  dismiss(id: number): void {
    const current = this.notificationsSignal();
    const target = current.find((n) => n.id === id);
    if (target?.timeoutId) clearTimeout(target.timeoutId);
    this.notificationsSignal.update((list) => list.filter((n) => n.id !== id));
  }

  /**
   * Dismiss all active notifications.
   */
  dismissAll(): void {
    this.notificationsSignal().forEach((notification) => {
      if (notification.timeoutId) clearTimeout(notification.timeoutId);
    });
    this.notificationsSignal.set([]);
  }
}
