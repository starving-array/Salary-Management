import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

@Component({
  selector: 'app-placeholder',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './placeholder.component.html',
  styleUrl: './placeholder.component.css'
})
export class PlaceholderComponent implements OnInit {
  moduleName = 'Module';
  phase = 'Upcoming Phase';
  description = 'This feature module will be implemented in the next assessment phase.';

  constructor(private readonly route: ActivatedRoute) {}

  ngOnInit(): void {
    const data = this.route.snapshot.data;
    if (data['moduleName']) {
      this.moduleName = data['moduleName'];
    }
    if (data['phase']) {
      this.phase = data['phase'];
    }
    if (data['description']) {
      this.description = data['description'];
    }
  }
}
