import {
  AfterViewInit,
  Component,
  OnInit,
  ChangeDetectorRef,
  ElementRef,
  HostListener,
  ViewChild,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  AdminBlogsService,
  AdminBlogPost,
  BlogLanguage,
  BlogTranslation,
} from '../../services/admin-blogs';
import { normalizeRichMediaHtml } from '../../shared/rich-media.util';

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
export class AdminBlogs implements OnInit, AfterViewInit {
  private blogService = inject(AdminBlogsService);
  private cdr = inject(ChangeDetectorRef);
  @ViewChild('contentEditor') contentEditor?: ElementRef<HTMLDivElement>;
  private savedRange: Range | null = null;

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
  coverUploading = false;
  coverPreviewUrl = '';
  showModal = false;
  isEdit = false;
  validationError = '';
  resultMessage = '';
  showImageMenu = false;
  selectedEditorImage: HTMLImageElement | null = null;
  selectedImagePercent = 100;
  isEditingImagePercent = false;
  draftImagePercent = '';

  ngOnInit(): void {
    this.loadPosts();
  }

  ngAfterViewInit(): void {
    this.syncEditorFromActiveTranslation();
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
    this.coverPreviewUrl = '';
    this.coverUploading = false;
    this.clearSelectedEditorImage();
    this.showModal = true;
    this.queueEditorSync();
  }

  openEdit(post: AdminBlogPost): void {
    this.isEdit = true;
    this.currentPost = this.toFormPost(post);
    this.activeLanguage = 'vi';
    this.validationError = '';
    this.coverPreviewUrl = '';
    this.coverUploading = false;
    this.clearSelectedEditorImage();
    this.showModal = true;
    this.queueEditorSync();
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
    this.coverUploading = false;
    this.showImageMenu = false;
    this.clearSelectedEditorImage();
  }

  setActiveLanguage(lang: BlogLanguage): void {
    if (this.activeLanguage !== lang) {
      this.updateActiveContentFromEditor();
      this.clearSelectedEditorImage();
    }
    this.activeLanguage = lang;
    this.queueEditorSync();
  }

  setPreviewLanguage(lang: BlogLanguage): void {
    this.previewLanguage = lang;
  }

  get previewTranslation(): BlogTranslationForm | null {
    return this.previewPost?.translations[this.previewLanguage] || null;
  }

  savePost(): void {
    this.updateActiveContentFromEditor();
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

  coverImageSrc(): string {
    return this.coverPreviewUrl || this.imageUrl(this.currentPost.thumbnail_url);
  }

  onCoverImageSelected(event: Event): void {
    const input = event.target as HTMLInputElement | null;
    const file = input?.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = () => {
      const result = reader.result;
      if (typeof result !== 'string') return;

      this.coverPreviewUrl = result;
      this.coverUploading = true;
      this.validationError = '';
      this.cdr.detectChanges();

      this.blogService.uploadImage(result, this.activeUploadName('blog-cover')).subscribe({
        next: (res) => {
          const imageUrl = String(res?.image_url || '').trim();
          if (imageUrl) {
            this.currentPost.thumbnail_url = imageUrl;
            this.coverPreviewUrl = this.imageUrl(imageUrl);
          }
          this.coverUploading = false;
          this.cdr.detectChanges();
        },
        error: () => {
          this.coverUploading = false;
          this.validationError = 'Không tải được ảnh đại diện. Vui lòng thử lại.';
          this.cdr.detectChanges();
        },
      });
    };

    reader.readAsDataURL(file);
    if (input) input.value = '';
  }

  removeCoverImage(): void {
    this.currentPost.thumbnail_url = '';
    this.coverPreviewUrl = '';
  }

  preventToolbarFocus(event: MouseEvent): void {
    event.preventDefault();
  }

  captureSelection(): void {
    const editor = this.contentEditor?.nativeElement;
    const selection = window.getSelection();
    if (!editor || !selection || selection.rangeCount === 0) return;
    const range = selection.getRangeAt(0);
    if (editor.contains(range.startContainer)) this.savedRange = range.cloneRange();
  }

  formatContent(command: string): void {
    this.runEditorCommand(command);
  }

  alignContent(command: 'justifyLeft' | 'justifyCenter' | 'justifyRight'): void {
    this.runEditorCommand(command);
  }

  formatBlock(tag: 'p' | 'h2' | 'h3' | 'blockquote'): void {
    this.runEditorCommand('formatBlock', tag);
  }

  toggleList(command: 'insertUnorderedList' | 'insertOrderedList'): void {
    this.runEditorCommand(command);
  }

  insertLink(): void {
    const url = window.prompt('Nhập liên kết');
    if (!url) return;
    this.runEditorCommand('createLink', url.trim());
  }

  clearFormatting(): void {
    this.runEditorCommand('removeFormat');
  }

  insertImageFromUrl(): void {
    this.showImageMenu = false;
    const url = window.prompt('Nhập URL ảnh');
    if (!url) return;
    this.runEditorCommand('insertImage', url.trim());
  }

  toggleImageMenu(event: MouseEvent): void {
    event.stopPropagation();
    this.showImageMenu = !this.showImageMenu;
  }

  openImagePicker(input: HTMLInputElement): void {
    this.showImageMenu = false;
    this.captureSelection();
    input.click();
  }

  onEditorImageFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement | null;
    const file = input?.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = () => {
      const result = reader.result;
      if (typeof result !== 'string') return;

      this.blogService.uploadImage(result, this.activeUploadName('blog-content')).subscribe({
        next: (res) => {
          const imageUrl = String(res?.image_url || '').trim();
          if (imageUrl) this.runEditorCommand('insertImage', imageUrl);
        },
        error: () => {
          this.validationError = 'Không tải được ảnh trong nội dung. Vui lòng thử lại.';
          this.cdr.detectChanges();
        },
      });
    };

    reader.readAsDataURL(file);
    if (input) input.value = '';
  }

  onEditorClick(event: MouseEvent): void {
    const target = event.target as HTMLElement | null;
    if (target instanceof HTMLImageElement) {
      this.selectEditorImage(target);
      return;
    }
    this.clearSelectedEditorImage();
  }

  onContentInput(): void {
    this.updateActiveContentFromEditor();
  }

  increaseSelectedImage(): void {
    this.applySelectedImagePercent(this.selectedImagePercent + 5);
  }

  decreaseSelectedImage(): void {
    this.applySelectedImagePercent(this.selectedImagePercent - 5);
  }

  startEditImagePercent(): void {
    this.isEditingImagePercent = true;
    this.draftImagePercent = String(this.selectedImagePercent);
  }

  onPercentInputKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter') {
      event.preventDefault();
      this.applyDraftImagePercent();
    } else if (event.key === 'Escape') {
      event.preventDefault();
      this.cancelEditImagePercent();
    }
  }

  applyDraftImagePercent(): void {
    const percent = Number(this.draftImagePercent || 0);
    if (!Number.isFinite(percent) || percent <= 0) {
      this.cancelEditImagePercent();
      return;
    }
    this.isEditingImagePercent = false;
    this.applySelectedImagePercent(percent);
  }

  cancelEditImagePercent(): void {
    this.isEditingImagePercent = false;
    this.draftImagePercent = String(this.selectedImagePercent);
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: Event): void {
    const target = event.target as HTMLElement | null;
    if (!target?.closest('.blog-image-menu')) this.showImageMenu = false;
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
        content: this.normalizeContentHtml(item.content.trim()),
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
      if (!this.hasMeaningfulContent(item.content)) return `Nội dung ${lang.label} không được để trống.`;
    }
    if (!['draft', 'published'].includes(post.status)) return 'Trạng thái không hợp lệ.';
    return '';
  }

  private queueEditorSync(): void {
    setTimeout(() => {
      this.syncEditorFromActiveTranslation();
      this.cdr.detectChanges();
    });
  }

  private syncEditorFromActiveTranslation(): void {
    const editor = this.contentEditor?.nativeElement;
    if (!editor || !this.showModal) return;
    const html = this.normalizeContentHtml(this.currentPost.translations[this.activeLanguage]?.content || '');
    if (editor.innerHTML !== html) editor.innerHTML = html;
  }

  private updateActiveContentFromEditor(): void {
    const editor = this.contentEditor?.nativeElement;
    if (!editor || !this.showModal) return;
    this.currentPost.translations[this.activeLanguage].content = this.normalizeContentHtml(editor.innerHTML || '');
  }

  private focusEditor(): void {
    this.contentEditor?.nativeElement.focus();
  }

  private restoreSelection(): void {
    const selection = window.getSelection();
    if (!selection) return;
    selection.removeAllRanges();
    if (this.savedRange) selection.addRange(this.savedRange);
  }

  private runEditorCommand(command: string, value?: string): void {
    this.focusEditor();
    this.restoreSelection();
    document.execCommand('styleWithCSS', false, 'true');
    document.execCommand(command, false, value);
    this.onContentInput();
    this.captureSelection();
  }

  private selectEditorImage(image: HTMLImageElement): void {
    this.clearSelectedEditorImage();
    this.selectedEditorImage = image;
    this.selectedEditorImage.classList.add('editor-selected-image');
    const editorWidth = this.contentEditor?.nativeElement.clientWidth || 1;
    const imageWidth = image.getBoundingClientRect().width || editorWidth;
    const ratio = (imageWidth / editorWidth) * 100;
    this.selectedImagePercent = Math.max(10, Math.min(100, Math.round(ratio)));
  }

  private clearSelectedEditorImage(): void {
    if (this.selectedEditorImage) this.selectedEditorImage.classList.remove('editor-selected-image');
    this.selectedEditorImage = null;
    this.selectedImagePercent = 100;
    this.isEditingImagePercent = false;
    this.draftImagePercent = '';
  }

  private applySelectedImagePercent(nextPercent: number): void {
    if (!this.selectedEditorImage) return;
    const percent = Math.max(10, Math.min(100, nextPercent));
    this.selectedEditorImage.style.width = `${percent}%`;
    this.selectedEditorImage.style.maxWidth = '560px';
    this.selectedEditorImage.style.height = 'auto';
    this.selectedImagePercent = percent;
    this.onContentInput();
  }

  private normalizeContentHtml(html: string): string {
    if (!html) return '';
    return normalizeRichMediaHtml(html);
  }

  private hasMeaningfulContent(html: string | undefined): boolean {
    const value = String(html || '');
    if (/<(img|video|iframe)\b/i.test(value)) return true;
    const text = value
      .replace(/<[^>]*>/g, ' ')
      .replace(/&nbsp;/g, ' ')
      .replace(/\s+/g, ' ')
      .trim();
    return !!text;
  }

  private activeUploadName(fallback: string): string {
    return (
      this.currentPost.translations[this.activeLanguage]?.title?.trim() ||
      this.currentPost.translations.vi.title?.trim() ||
      this.currentPost.title?.trim() ||
      fallback
    );
  }
}
