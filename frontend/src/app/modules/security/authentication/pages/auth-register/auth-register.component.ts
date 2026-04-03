import { Component, signal, inject, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { ReactiveFormsModule } from '@angular/forms';
import { email, Field, form, minLength, required } from '@angular/forms/signals';

import { SharedModule } from 'src/app/theme/shared/shared.module';
import { AuthenticationService } from 'src/app/core/services/authentication.service';
import { DASHBOARD_PATH } from 'src/app/app-config';

import { first } from 'rxjs/operators';

interface RegisterData {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
}

@Component({
  selector: 'app-auth-register',
  standalone: true,
  imports: [CommonModule, RouterModule, SharedModule, ReactiveFormsModule, Field],
  templateUrl: './auth-register.component.html',
  styleUrls: ['./auth-register.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AuthRegisterComponent {
  private router = inject(Router);
  authenticationService = inject(AuthenticationService);

  registerModel = signal<RegisterData>({
    firstName: '',
    lastName: '',
    email: '',
    password: ''
  });

  registerForm = form(this.registerModel, (schemaPath) => {
    required(schemaPath.firstName, { message: 'First name is required' });
    required(schemaPath.lastName, { message: 'Last name is required' });
    required(schemaPath.email, { message: 'Email is required' });
    email(schemaPath.email, { message: 'Please enter a valid email address' });
    required(schemaPath.password, { message: 'Password is required' });
    minLength(schemaPath.password, 8, { message: 'Password must be at least 8 characters' });
  });

  loading = signal(false);
  submitted = signal(false);
  error = signal('');

  constructor() {
    if (this.authenticationService.isLoggedIn()) {
      this.router.navigate([DASHBOARD_PATH]);
    }
  }

  onSubmit(): void {
    this.submitted.set(true);

    if (!this.registerForm().valid()) {
      return;
    }

    this.error.set('');
    this.loading.set(true);

    const formData = this.registerModel();
    const username = formData.email.split('@')[0];

    this.authenticationService
      .register(username, formData.password)
      .pipe(first())
      .subscribe({
        next: (success) => {
          if (success) {
            this.router.navigate([DASHBOARD_PATH]);
          } else {
            this.error.set('Registration failed. Please try again.');
            this.loading.set(false);
          }
        },
        error: (err: { error?: { message?: string }; message?: string }) => {
          this.error.set(err?.error?.message || err?.message || 'Registration failed. Please try again.');
          this.loading.set(false);
        }
      });
  }
}
