import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { Location, CommonModule } from '@angular/common';

@Component({
  selector: 'app-access-denied',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './access-denied.component.html',
  styleUrls: ['./access-denied.component.scss']
})
export class AccessDeniedComponent {
  private router = inject(Router);
  private location = inject(Location);

  goBack(): void {
    this.location.back();
  }

  goHome(): void {
    this.router.navigate(['/']);
  }
}
