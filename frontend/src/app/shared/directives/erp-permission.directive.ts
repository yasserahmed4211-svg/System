import {
  Directive,
  ElementRef,
  Input,
  Renderer2,
  TemplateRef,
  ViewContainerRef,
  inject
} from '@angular/core';

import { PermissionService } from 'src/app/core/services/permission.service';

type PermissionInput = string | string[];

@Directive({
  selector: '[erpPermission]',
  standalone: true
})
export class ErpPermissionDirective {
  private readonly el = inject<ElementRef<unknown>>(ElementRef);
  private readonly renderer = inject(Renderer2);
  private readonly permService = inject(PermissionService);
  private readonly templateRef = inject<TemplateRef<unknown> | null>(TemplateRef, { optional: true });
  private readonly viewContainerRef = inject<ViewContainerRef | null>(ViewContainerRef, { optional: true });

  private hasView = false;

  private required: string[] = [];
  private negate = false;

  @Input('erpPermission')
  set erpPermission(value: PermissionInput) {
    const permissions = Array.isArray(value) ? value : [value];

    // Negation is supported for single-string inputs: erpPermission="!ROLE.VIEW"
    this.negate = !Array.isArray(value) && typeof value === 'string' && value.trim().startsWith('!');

    this.required = permissions
      .map((p) => (p ?? '').trim())
      .filter((p) => p.length > 0)
      .map((p) => (p.startsWith('!') ? p.slice(1) : p));

    this.applyVisibility();
  }

  private applyVisibility(): void {
    if (this.required.length === 0) {
      this.applyResult(true);
      return;
    }

    const allowed = this.required.some((p) => this.permService.hasPermission(p));
    const shouldShow = this.negate ? !allowed : allowed;
    this.applyResult(shouldShow);
  }

  private applyResult(shouldShow: boolean): void {
    // Structural directive mode: *erpPermission
    if (this.templateRef && this.viewContainerRef) {
      if (shouldShow && !this.hasView) {
        this.viewContainerRef.clear();
        this.viewContainerRef.createEmbeddedView(this.templateRef);
        this.hasView = true;
      } else if (!shouldShow && this.hasView) {
        this.viewContainerRef.clear();
        this.hasView = false;
      }
      return;
    }

    // Attribute directive mode: [erpPermission] on a real DOM element
    if (shouldShow) {
      this.show();
    } else {
      this.hide();
    }
  }

  private hide(): void {
    const native = this.el.nativeElement as any;
    if (!native || !native.style) return;
    this.renderer.setStyle(native, 'display', 'none');
  }

  private show(): void {
    const native = this.el.nativeElement as any;
    if (!native || !native.style) return;
    this.renderer.removeStyle(native, 'display');
  }
}
