export function cleanupElementPlusOverlays() {
  window.setTimeout(() => {
    document.querySelectorAll('.el-overlay').forEach((overlay) => overlay.remove())
    document.querySelectorAll('.el-popper, .el-message-box__wrapper').forEach((node) => {
      if (!node.closest('.el-overlay')) {
        node.remove()
      }
    })
    document.body.classList.remove('el-popup-parent--hidden')
    document.body.style.removeProperty('overflow')
    document.body.style.removeProperty('padding-right')
  }, 0)
}
