import { Directive, ElementRef, OnInit, OnDestroy, Renderer2 } from '@angular/core';

/**
 * 套用在 <th> 上，在右邊緣新增拖拉 handle，讓使用者可以調整欄位寬度。
 * CSS resize: horizontal 對 table-cell 元素無效，須用 JS 實作。
 */
@Directive({
  selector: 'th[appColResizable]',
  standalone: true,
})
export class ColResizableDirective implements OnInit, OnDestroy {
  private startX = 0;
  private startWidth = 0;
  private removeHandleMouseDown: (() => void) | null = null;
  private removeMouseMove: (() => void) | null = null;
  private removeMouseUp: (() => void) | null = null;

  constructor(
    private el: ElementRef<HTMLElement>,
    private renderer: Renderer2,
  ) {}

  ngOnInit(): void {
    const th = this.el.nativeElement;
    // 不主動設 position；handle 是 absolute，要靠 th 自身的 CSS（如 sticky / relative）當定位 reference
    // 若呼叫端的 th 沒有 positioned，請在 CSS 給 position: sticky 或 relative

    const handle = this.renderer.createElement('span') as HTMLElement;
    this.renderer.setStyle(handle, 'position', 'absolute');
    this.renderer.setStyle(handle, 'right', '0');
    this.renderer.setStyle(handle, 'top', '0');
    this.renderer.setStyle(handle, 'bottom', '0');
    this.renderer.setStyle(handle, 'width', '6px');
    this.renderer.setStyle(handle, 'cursor', 'col-resize');
    this.renderer.setStyle(handle, 'user-select', 'none');

    this.removeHandleMouseDown = this.renderer.listen(handle, 'mousedown', (e: MouseEvent) => {
      e.preventDefault();
      e.stopPropagation(); // 避免觸發 th 的 sortBy click
      this.startX = e.clientX;
      this.startWidth = th.offsetWidth;

      this.removeMouseMove = this.renderer.listen('document', 'mousemove', (ev: MouseEvent) => {
        const newWidth = Math.max(50, this.startWidth + (ev.clientX - this.startX));
        this.renderer.setStyle(th, 'width', `${newWidth}px`);
        this.renderer.setStyle(th, 'min-width', `${newWidth}px`);
      });

      this.removeMouseUp = this.renderer.listen('document', 'mouseup', () => {
        this.removeMouseMove?.();
        this.removeMouseUp?.();
        this.removeMouseMove = null;
        this.removeMouseUp = null;
      });
    });

    this.renderer.appendChild(th, handle);
  }

  ngOnDestroy(): void {
    this.removeHandleMouseDown?.();
    this.removeMouseMove?.();
    this.removeMouseUp?.();
  }
}
