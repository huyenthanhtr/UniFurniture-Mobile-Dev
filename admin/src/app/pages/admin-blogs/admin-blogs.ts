import { Component, OnInit, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  AdminBlogsService,
  AdminBlogPost,
  BlogLanguage,
  BlogTranslation,
} from '../../services/admin-blogs';

type BlogTranslationForm = Required<BlogTranslation>;
type BlogFormPost = AdminBlogPost & {
  translations: Record<BlogLanguage, BlogTranslationForm>;
};

@Component({
  selector: 'app-admin-blogs',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-blogs.html',
  styleUrls: ['./admin-blogs.css'],
})
export class AdminBlogs implements OnInit {
  private blogService = inject(AdminBlogsService);
  private cdr = inject(ChangeDetectorRef);

  readonly languages: { code: BlogLanguage; label: string }[] = [
    { code: 'vi', label: 'Tiếng Việt' },
    { code: 'en', label: 'English' },
    { code: 'fr', label: 'Français' },
    { code: 'zh', label: '中文' },
  ];

  posts: AdminBlogPost[] = [];
  currentPost: BlogFormPost = this.createEmptyPost();
  previewPost: BlogFormPost | null = null;
  activeLanguage: BlogLanguage = 'vi';
  previewLanguage: BlogLanguage = 'vi';
  filter = {
    search: '',
    status: '',
  };

  currentPage = 1;
  totalPages = 1;
  totalItems = 0;
  itemsPerPage = 8;
  loading = false;
  saving = false;
  showModal = false;
  isEdit = false;
  validationError = '';
  resultMessage = '';

  ngOnInit(): void {
    this.loadPosts();
  }

  loadPosts(page = this.currentPage): void {
    this.loading = true;
    this.currentPage = page;
    this.blogService.getPosts({
      page,
      limit: this.itemsPerPage,
      q: this.filter.search.trim(),
      status: this.filter.status,
      sort: '-published_at',
      lang: 'vi',
    }).subscribe({
      next: (response) => {
        this.posts = response.items || [];
        this.totalPages = response.totalPages || 1;
        this.totalItems = response.total || 0;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error(err);
        this.resultMessage = err.error?.message || 'Không thể tải danh sách blog.';
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  onFilterChange(): void {
    this.loadPosts(1);
  }

  resetFilters(): void {
    this.filter = { search: '', status: '' };
    this.loadPosts(1);
  }

  openCreate(): void {
    this.isEdit = false;
    this.currentPost = this.createEmptyPost();
    this.activeLanguage = 'vi';
    this.validationError = '';
    this.showModal = true;
  }

  openEdit(post: AdminBlogPost): void {
    this.isEdit = true;
    this.currentPost = this.toFormPost(post);
    this.activeLanguage = 'vi';
    this.validationError = '';
    this.showModal = true;
  }

  openPreview(post: AdminBlogPost): void {
    this.previewPost = this.toFormPost(post);
    this.previewLanguage = 'vi';
  }

  closePreview(): void {
    this.previewPost = null;
  }

  editFromPreview(): void {
    if (!this.previewPost) return;
    const post = this.previewPost;
    this.closePreview();
    this.openEdit(post);
  }

  closeModal(): void {
    this.showModal = false;
    this.validationError = '';
    this.saving = false;
  }

  setActiveLanguage(lang: BlogLanguage): void {
    this.activeLanguage = lang;
  }

  setPreviewLanguage(lang: BlogLanguage): void {
    this.previewLanguage = lang;
  }

  get previewTranslation(): BlogTranslationForm | null {
    return this.previewPost?.translations[this.previewLanguage] || null;
  }

  savePost(): void {
    this.validationError = this.validatePost(this.currentPost);
    if (this.validationError) return;

    const translations = this.trimTranslations(this.currentPost.translations);
    const vi = translations.vi;
    const payload: AdminBlogPost = {
      ...this.currentPost,
      title: vi.title,
      slug: this.currentPost.slug?.trim() || '',
      caption: vi.caption,
      content: vi.content,
      thumbnail_url: this.currentPost.thumbnail_url?.trim() || '',
      post_category: vi.post_category || 'Blog',
      status: this.currentPost.status || 'draft',
      translations,
    };

    this.saving = true;
    const request = this.isEdit && payload._id
      ? this.blogService.updatePost(payload._id, payload)
      : this.blogService.createPost(payload);

    request.subscribe({
      next: () => {
        this.resultMessage = this.isEdit ? 'Đã cập nhật bài blog.' : 'Đã tạo bài blog mới.';
        this.closeModal();
        this.loadPosts(this.currentPage);
      },
      error: (err) => {
        this.validationError = err.error?.message || 'Không thể lưu bài blog.';
        this.saving = false;
        this.cdr.detectChanges();
      },
    });
  }

  deletePost(post: AdminBlogPost): void {
    if (!post._id) return;
    const ok = window.confirm(`Xóa bài "${post.title}"?`);
    if (!ok) return;

    this.blogService.deletePost(post._id).subscribe({
      next: () => {
        this.resultMessage = 'Đã xóa bài blog.';
        this.loadPosts(this.currentPage);
      },
      error: (err) => {
        this.resultMessage = err.error?.message || 'Không thể xóa bài blog.';
        this.cdr.detectChanges();
      },
    });
  }

  changePage(page: number): void {
    if (page < 1 || page > this.totalPages || page === this.currentPage) return;
    this.loadPosts(page);
  }

  get visiblePages(): number[] {
    const pages: number[] = [];
    const start = Math.max(1, this.currentPage - 2);
    const end = Math.min(this.totalPages, start + 4);
    for (let i = start; i <= end; i++) pages.push(i);
    return pages;
  }

  statusLabel(status: string | undefined): string {
    return status === 'published' ? 'Đã xuất bản' : 'Bản nháp';
  }

  formatDate(value: string | null | undefined): string {
    if (!value) return '-';
    return new Date(value).toLocaleDateString('vi-VN');
  }

  imageUrl(value: string | undefined): string {
    const raw = String(value || '').trim();
    if (!raw) return '';
    if (raw.startsWith('http://') || raw.startsWith('https://')) return raw;
    if (raw.startsWith('/')) return `http://localhost:3000${raw}`;
    return raw;
  }

  private createEmptyPost(): BlogFormPost {
    return {
      title: '',
      slug: '',
      caption: '',
      content: '',
      thumbnail_url: '',
      post_category: 'Blog',
      status: 'draft',
      translations: this.createEmptyTranslations(),
    };
  }

  private createEmptyTranslations(): Record<BlogLanguage, BlogTranslationForm> {
    return {
      vi: { title: '', caption: '', content: '', post_category: 'Blog' },
      en: { title: '', caption: '', content: '', post_category: 'Blog' },
      fr: { title: '', caption: '', content: '', post_category: 'Blog' },
      zh: { title: '', caption: '', content: '', post_category: 'Blog' },
    };
  }

  private toFormPost(post: AdminBlogPost): BlogFormPost {
    const translations = this.createEmptyTranslations();

    for (const { code } of this.languages) {
      const translation = post.translations?.[code];
      translations[code] = {
        title: translation?.title?.trim() || post.title || '',
        caption: translation?.caption?.trim() || post.caption || '',
        content: translation?.content?.trim() || post.content || '',
        post_category: translation?.post_category?.trim() || post.post_category || 'Blog',
      };
    }

    return {
      ...post,
      title: post.title || translations.vi.title,
      caption: post.caption || translations.vi.caption,
      content: post.content || translations.vi.content,
      post_category: post.post_category || translations.vi.post_category,
      translations,
    };
  }

  private trimTranslations(
    translations: Record<BlogLanguage, BlogTranslationForm>,
  ): Record<BlogLanguage, BlogTranslationForm> {
    const result = this.createEmptyTranslations();
    for (const { code } of this.languages) {
      const item = translations[code];
      result[code] = {
        title: item.title.trim(),
        caption: item.caption.trim(),
        content: item.content.trim(),
        post_category: item.post_category.trim() || 'Blog',
      };
    }
    return result;
  }

  private validatePost(post: BlogFormPost): string {
    for (const lang of this.languages) {
      const item = post.translations[lang.code];
      if (!item.title.trim()) return `Tiêu đề ${lang.label} không được để trống.`;
      if (!item.caption.trim()) return `Caption ${lang.label} không được để trống.`;
      if (!item.content.trim()) return `Nội dung ${lang.label} không được để trống.`;
    }
    if (!['draft', 'published'].includes(post.status)) return 'Trạng thái không hợp lệ.';
    return '';
  }
}
