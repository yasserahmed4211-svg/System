// angular import
import { Component, OnInit, Renderer2, DOCUMENT, inject } from '@angular/core';

// project import
import { MantisConfig } from 'src/app/app-config';
import { ThemeService } from 'src/app/theme/shared/service/customs-theme.service';
import { SharedModule } from 'src/app/theme/shared/shared.module';

@Component({
  selector: 'app-configuration',
  imports: [SharedModule],
  templateUrl: './configuration.component.html',
  styleUrls: ['./configuration.component.scss']
})
export class ConfigurationComponent implements OnInit {
  private renderer = inject(Renderer2);
  private document = inject(DOCUMENT);
  readonly themeService = inject(ThemeService);
  rtlLayout!: boolean;
  bodyColor!: string;
  darkLayout!: boolean;
  boxLayout!: boolean;
  fontFamily!: string;

  ngOnInit(): void {
    this.renderer.addClass(document.body, 'mantis-ltr');
    if ((this.rtlLayout = MantisConfig.isRtlLayout)) {
      this.renderer.addClass(document.body, 'mantis-rtl');
      this.renderer.removeClass(document.body, 'mantis-ltr');
      this.themeService.isRTLMode.set(this.rtlLayout);
    }
    if ((this.fontFamily = MantisConfig.font_family)) {
      this.renderer.addClass(document.body, this.fontFamily);
    }
    if ((this.darkLayout = MantisConfig.isDarkMode)) {
      this.document.body.classList.add('mantis-dark');
      this.themeService.isDarkMode.set(this.darkLayout);
    }
    if ((this.boxLayout = MantisConfig.isBox_container)) {
      this.document.querySelector('.coded-content')?.classList.add('container');
    }
    if ((this.bodyColor = MantisConfig.theme_color)) {
      this.document.body.part.add(this.bodyColor);
    }
  }
}
