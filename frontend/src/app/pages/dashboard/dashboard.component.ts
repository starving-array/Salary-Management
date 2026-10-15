import { DatePipe } from '@angular/common';
import { Component, Inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { API_BASE_URL } from '../../core/config/api.config';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink, DatePipe],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent {
  today = new Date();

  constructor(@Inject(API_BASE_URL) readonly apiBaseUrl: string) {}
}
