import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {

  email = '';
  password = '';
  error = signal<string | null>(null);
  cargando = signal(false);

  constructor(private authService: AuthService, private router: Router) {}

  onSubmit(): void {
    this.error.set(null);
    this.cargando.set(true);

    this.authService.login({ email: this.email, password: this.password }).subscribe({
      next: () => {
        this.cargando.set(false);
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.cargando.set(false);
        this.error.set(err.error?.error ?? 'Credenciales inválidas');
      }
    });
  }
}