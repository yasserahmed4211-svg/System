// angular import
import { Component, OnInit, Renderer2, DOCUMENT, inject } from '@angular/core';

// project import
import { MantisConfig } from 'src/app/app-config';
import { ThemeService } from 'src/app/theme/shared/service/customs-theme.service';
import { SharedModule } from 'src/app/theme/shared/shared.module';
import { LanguageService } from 'src/app/core/services/language.service';

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
  private languageService = inject(LanguageService);

  ngOnInit(): void {
    // RTL/LTR is managed entirely by LanguageService.

    // Apply font family from static config (not theme-switchable)
    const fontFamily = MantisConfig.font_family;
    if (fontFamily) {
      this.renderer.addClass(this.document.body, fontFamily);
    }

    // ThemeService effects handle dark mode, theme color, and container mode
    // automatically from persisted localStorage values (or MantisConfig defaults).
    // No manual DOM manipulation needed here.
  }
}
