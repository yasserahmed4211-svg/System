// angular import
import { Component, effect, inject, Input, untracked } from '@angular/core';

// third party
import { NgScrollbarModule } from 'ngx-scrollbar';

// project import
import { ThemeService } from '../../service/customs-theme.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-scrollbar',
  imports: [NgScrollbarModule, CommonModule],
  templateUrl: './scrollbar.component.html',
  styleUrl: './scrollbar.component.scss'
})
export class ScrollbarComponent {
  private themeService = inject(ThemeService);

  @Input() customStyle: { [key: string]: string } = {};

  direction: string = 'ltr';

  // constructor
  constructor() {
    effect(() => {
      const isRtl = this.themeService.isRTLMode();
      untracked(() => this.isRtlTheme(isRtl));
    });
  }

  // private method
  private isRtlTheme(isRtl: boolean) {
    this.direction = isRtl === true ? 'rtl' : 'ltr';
  }
}
