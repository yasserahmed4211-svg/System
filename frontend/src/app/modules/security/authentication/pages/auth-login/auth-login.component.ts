import { Component, signal, inject, ChangeDetectorRef, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router, ActivatedRoute } from '@angular/router';
import { form, Field } from '@angular/forms/signals';
import { TranslateModule } from '@ngx-translate/core';

import { SharedModule } from 'src/app/theme/shared/shared.module';
import { AuthenticationService } from 'src/app/core/services/authentication.service';
import { LanguageService } from 'src/app/core/services/language.service';
import { FeatureFlagService } from 'src/app/core/services/feature-flag.service';
import { DASHBOARD_PATH } from 'src/app/app-config';

import { IconService } from '@ant-design/icons-angular';
import { EyeInvisibleOutline, EyeOutline } from '@ant-design/icons-angular/icons';

interface RoleOption {
  name: string;
  username: string;
  password: string;
  role: string;
}

interface LoginData {
  username: string;
  password: string;
}

@Component({
  selector: 'app-auth-login',
  standalone: true,
  imports: [CommonModule, RouterModule, SharedModule, Field, TranslateModule],
  templateUrl: './auth-login.component.html',
  styleUrls: ['./auth-login.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AuthLoginComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  authenticationService = inject(AuthenticationService);
  languageService = inject(LanguageService);
  readonly featureFlags = inject(FeatureFlagService);
  private iconService = inject(IconService);
  private cd = inject(ChangeDetectorRef);

  showPassword = true;
  submitted = false;
  error = '';
  loading = false;
  returnUrl!: string;

  private readonly loginData = signal<LoginData>({
    username: '',
    password: ''
  });

  loginForm = form(this.loginData);

  roles: RoleOption[] = [
    { name: 'Admin', username: 'admin', password: 'admin123', role: 'Admin' },
    { name: 'User', username: 'user', password: 'user123', role: 'User' }
  ];

  selectedRole = this.roles[0];

  onSelectRole(role: RoleOption): void {
    this.selectedRole = role;
    this.loginData.set({ username: role.username, password: role.password });
  }

  constructor() {
    this.iconService.addIcon(...[EyeOutline, EyeInvisibleOutline]);

    if (window.location.pathname !== '/auth/login') {
      if (this.authenticationService.currentUserValue) {
        this.router.navigate([DASHBOARD_PATH]);
      }
    }
  }

  ngOnInit(): void {
    this.returnUrl = this.route.snapshot.queryParams['returnUrl'] || DASHBOARD_PATH;
    this.loginData.set({ username: this.selectedRole.username, password: this.selectedRole.password });
  }

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  get isFormValid(): boolean {
    const val = this.loginForm().value();
    return val.username.trim() !== '' && val.password.trim() !== '';
  }

  onSubmit(): void {
    this.submitted = true;

    if (!this.isFormValid) {
      return;
    }

    this.loading = true;
    this.error = '';

    const { username, password } = this.loginForm().value();

    this.authenticationService.login(username, password).subscribe({
      next: (success) => {
        if (success) {
          this.router.navigate([this.returnUrl]);
        } else {
          this.error = 'AUTH.LOGIN_FAILED';
          this.loading = false;
          this.cd.detectChanges();
        }
      },
      error: (error: { error?: { message?: string }; message?: string }) => {
        this.error = error?.error?.message || error?.message || 'AUTH.INVALID_CREDENTIALS';
        this.loading = false;
        this.cd.detectChanges();
      }
    });
  }

  socialMedia = [
    { name: 'Google', logo: 'google.svg' },
    { name: 'Twitter', logo: 'twitter.svg' },
    { name: 'Facebook', logo: 'facebook.svg' }
  ];
}
