import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export type BlogLanguage = 'vi' | 'en' | 'fr' | 'zh';

export interface BlogTranslation {
  title: string;
  caption?: string;
  content?: string;
  post_category?: string;
}

export interface AdminBlogPost {
  _id?: string;
  title: string;
  slug?: string;
  caption?: string;
  content?: string;
  thumbnail_url?: string;
  post_category?: string;
  status: 'draft' | 'published';
  published_at?: string | null;
  translations?: Partial<Record<BlogLanguage, BlogTranslation>>;
  createdAt?: string;
  updatedAt?: string;
}

export interface AdminBlogListResponse {
  page: number;
  limit: number;
  total: number;
  totalPages: number;
  items: AdminBlogPost[];
}

@Injectable({ providedIn: 'root' })
export class AdminBlogsService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:3000/api/posts';

  getPosts(params: Record<string, string | number | undefined>): Observable<AdminBlogListResponse> {
    let httpParams = new HttpParams();
    for (const [key, value] of Object.entries(params)) {
      if (value !== undefined && value !== '') {
        httpParams = httpParams.set(key, String(value));
      }
    }
    return this.http.get<AdminBlogListResponse>(this.apiUrl, { params: httpParams });
  }

  createPost(data: AdminBlogPost): Observable<AdminBlogPost> {
    return this.http.post<AdminBlogPost>(this.apiUrl, data);
  }

  updatePost(id: string, data: AdminBlogPost): Observable<AdminBlogPost> {
    return this.http.put<AdminBlogPost>(`${this.apiUrl}/${id}`, data);
  }

  deletePost(id: string): Observable<{ success: boolean }> {
    return this.http.delete<{ success: boolean }>(`${this.apiUrl}/${id}`);
  }
}
