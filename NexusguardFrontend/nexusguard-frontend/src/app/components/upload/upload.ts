// import { Component } from '@angular/core';

// @Component({
//   selector: 'app-upload',
//   imports: [],
//   templateUrl: './upload.html',
//   styleUrl: './upload.scss',
// })
// export class Upload {}
import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Scan } from '../../services/scan';

@Component({
  selector: 'app-upload',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './upload.html',
  styleUrl: './upload.scss'
})
export class Upload {

  selectedFile: File | null = null;
  projectName: string = '';
  scanning: boolean = false;
  errorMessage: string = '';

  constructor(
    private scanService: Scan,
    private router: Router) {}

  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (file && file.name.endsWith('.xml')) {
      this.selectedFile = file;
      this.errorMessage = '';
    } else {
      this.errorMessage = 
        'Please select a valid pom.xml file';
    }
  }

  onDragOver(event: DragEvent) {
    event.preventDefault();
  }

  onDrop(event: DragEvent) {
    event.preventDefault();
    const file = event.dataTransfer?.files[0];
    if (file && file.name.endsWith('.xml')) {
      this.selectedFile = file;
      this.errorMessage = '';
    } else {
      this.errorMessage = 
        'Please drop a valid pom.xml file';
    }
  }

  startScan() {
    if (!this.selectedFile) {
      this.errorMessage = 
        'Please select a pom.xml file';
      return;
    }
    if (!this.projectName.trim()) {
      this.errorMessage = 
        'Please enter a project name';
      return;
    }

    this.scanning = true;
    this.errorMessage = '';

    this.scanService.uploadPom(
      this.selectedFile,
      this.projectName
    ).subscribe({
      next: (result) => {
        this.scanning = false;
        // Navigate to results page
        this.router.navigate(
          ['/result', result.scanId]);
      },
      error: (err) => {
        this.scanning = false;
        this.errorMessage = 
          'Scan failed. Please try again.';
        console.error(err);
      }
    });
  }
}