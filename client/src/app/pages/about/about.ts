import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PostDataService, NewsPost } from '../../services/post-data.service';

@Component({
  selector: 'app-about-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './about.html',
  styleUrl: './about.css',
})
export class AboutPageComponent implements OnInit {
  private readonly postService = inject(PostDataService);

  blogPosts: NewsPost[] = [];
  selectedBlogCategory = 'all';
  blogsLoading = true;

  ngOnInit(): void {
    this.postService.getLatestPosts(8).subscribe({
      next: (posts) => {
        this.blogPosts = posts;
        this.blogsLoading = false;
      },
      error: () => {
        this.blogPosts = [];
        this.blogsLoading = false;
      },
    });
  }

  get blogCategories(): string[] {
    return Array.from(
      new Set(
        this.blogPosts
          .map((post) => post.category?.trim())
          .filter((category): category is string => !!category),
      ),
    );
  }

  get filteredBlogPosts(): NewsPost[] {
    if (this.selectedBlogCategory === 'all') {
      return this.blogPosts;
    }
    return this.blogPosts.filter((post) => post.category === this.selectedBlogCategory);
  }

  selectBlogCategory(category: string): void {
    this.selectedBlogCategory = category;
  }
}
