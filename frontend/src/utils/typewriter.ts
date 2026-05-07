export function playTypewriter(
  text: string,
  onUpdate: (value: string) => void,
  options: { step?: number; interval?: number } = {},
) {
  const step = options.step ?? 2
  const interval = options.interval ?? 18
  let index = 0
  onUpdate('')
  const timer = window.setInterval(() => {
    index = Math.min(text.length, index + step)
    onUpdate(text.slice(0, index))
    if (index >= text.length) {
      window.clearInterval(timer)
    }
  }, interval)
  return () => window.clearInterval(timer)
}
