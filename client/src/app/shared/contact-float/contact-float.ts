import { Component, HostListener, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { UiStateService } from '../ui-state.service';

@Component({
    selector: 'app-contact-float',
    imports: [CommonModule],
    templateUrl: './contact-float.html',
    styleUrl: './contact-float.css',
})
export class ContactFloat {
    ui = inject(UiStateService);

    position = {
        right: 28,
        bottom: 28,
    };

    private isDragging = false;
    private dragMoved = false;
    private dragOffsetX = 0;
    private dragOffsetY = 0;
    private readonly dragThreshold = 6;

    get wrapperStyles(): Record<string, string> {
        return {
            right: `${this.position.right}px`,
            bottom: `${this.position.bottom}px`,
        };
    }

    startDrag(event: MouseEvent | TouchEvent): void {
        const point = this.getPoint(event);
        if (!point) return;

        const viewportWidth = window.innerWidth;
        const viewportHeight = window.innerHeight;

        this.isDragging = true;
        this.dragMoved = false;
        this.dragOffsetX = viewportWidth - this.position.right - point.clientX;
        this.dragOffsetY = viewportHeight - this.position.bottom - point.clientY;
    }

    onToggleClick(event: Event): void {
        if (this.dragMoved) {
            event.preventDefault();
            event.stopPropagation();
            this.dragMoved = false;
            return;
        }

        this.ui.toggleContact();
    }

    @HostListener('window:mousemove', ['$event'])
    onMouseMove(event: MouseEvent): void {
        this.updateDrag(event.clientX, event.clientY);
    }

    @HostListener('window:touchmove', ['$event'])
    onTouchMove(event: TouchEvent): void {
        const point = event.touches?.[0];
        if (!point) return;
        this.updateDrag(point.clientX, point.clientY);
    }

    @HostListener('window:mouseup')
    @HostListener('window:touchend')
    @HostListener('window:touchcancel')
    stopDrag(): void {
        this.isDragging = false;
    }

    private updateDrag(clientX: number, clientY: number): void {
        if (!this.isDragging) return;

        const viewportWidth = window.innerWidth;
        const viewportHeight = window.innerHeight;
        const nextRight = viewportWidth - clientX + this.dragOffsetX;
        const nextBottom = viewportHeight - clientY + this.dragOffsetY;

        const clampedRight = this.clamp(nextRight, 12, Math.max(12, viewportWidth - 72));
        const clampedBottom = this.clamp(nextBottom, 12, Math.max(12, viewportHeight - 72));

        if (
            !this.dragMoved &&
            (Math.abs(clampedRight - this.position.right) > this.dragThreshold ||
                Math.abs(clampedBottom - this.position.bottom) > this.dragThreshold)
        ) {
            this.dragMoved = true;
        }

        this.position = {
            right: clampedRight,
            bottom: clampedBottom,
        };
    }

    private getPoint(event: MouseEvent | TouchEvent): MouseEvent | Touch {
        if (event instanceof MouseEvent) {
            return event;
        }
        return event.touches?.[0] || event.changedTouches?.[0];
    }

    private clamp(value: number, min: number, max: number): number {
        return Math.min(Math.max(value, min), max);
    }
}
