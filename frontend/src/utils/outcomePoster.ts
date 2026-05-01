export interface OutcomePosterData {
  title: string
  subtitle: string
  metrics: Array<{ label: string; value: string }>
  keywords: string[]
  highlights: string[]
}

export function exportOutcomePosterPng(data: OutcomePosterData): string {
  const width = 1080
  const scale = 2
  const keywordLines = chunk(data.keywords, 3)
  const highlightLines = data.highlights.map((item) => wrapText(item, 760, 28, '700'))
  const canvas = document.createElement('canvas')
  const height = Math.max(
    1320,
    620
      + Math.ceil(data.metrics.length / 2) * 160
      + keywordLines.length * 68
      + highlightLines.reduce((sum, lines) => sum + 58 + lines.length * 38, 0),
  )
  canvas.width = width * scale
  canvas.height = height * scale
  const ctx = canvas.getContext('2d')
  if (!ctx) {
    throw new Error('Canvas not supported')
  }
  ctx.scale(scale, scale)

  const background = ctx.createLinearGradient(0, 0, width, height)
  background.addColorStop(0, '#fffaf0')
  background.addColorStop(1, '#f2ead6')
  ctx.fillStyle = background
  ctx.fillRect(0, 0, width, height)

  ctx.fillStyle = 'rgba(31, 107, 84, 0.1)'
  ctx.beginPath()
  ctx.moveTo(0, 0)
  ctx.lineTo(430, 0)
  ctx.lineTo(0, 360)
  ctx.closePath()
  ctx.fill()

  roundRect(ctx, 64, 64, width - 128, height - 128, 28, '#fffdf8', '#d9d0c1', 2)
  drawText(ctx, 'Learning Outcomes', 120, 156, 24, '#8a5b13', '900')
  drawText(ctx, data.title, 120, 236, 60, '#202124', '900')
  drawText(ctx, data.subtitle, 120, 292, 28, '#5e584e', '700')

  let y = 356
  data.metrics.forEach((metric, index) => {
    const column = index % 2
    const row = Math.floor(index / 2)
    const x = 120 + column * 420
    const cardY = y + row * 144
    roundRect(ctx, x, cardY, 360, 112, 18, '#fff8eb', '#eadfca', 1)
    drawText(ctx, metric.label, x + 28, cardY + 38, 22, '#7c766a', '800')
    drawText(ctx, metric.value, x + 28, cardY + 84, 38, '#1f6b54', '900')
  })

  y += Math.ceil(data.metrics.length / 2) * 144 + 42
  drawText(ctx, '掌握关键词', 120, y, 30, '#1f6b54', '900')
  y += 36
  keywordLines.forEach((row) => {
    let x = 120
    row.forEach((item) => {
      const tagWidth = Math.max(120, Math.min(250, measureText(item, 22, '900') + 42))
      roundRect(ctx, x, y, tagWidth, 44, 22, '#eef8f2')
      drawText(ctx, item, x + tagWidth / 2, y + 29, 22, '#1f6b54', '900', 'center')
      x += tagWidth + 14
    })
    y += 62
  })

  y += 18
  drawText(ctx, '阶段亮点', 120, y, 30, '#1f6b54', '900')
  y += 34
  highlightLines.forEach((lines, index) => {
    const boxHeight = Math.max(74, 30 + lines.length * 38)
    roundRect(ctx, 120, y, 840, boxHeight, 16, '#fffdf8', '#e3dccf', 1)
    roundRect(ctx, 144, y + 18, 36, 36, 18, '#1f6b54')
    drawText(ctx, String(index + 1), 162, y + 42, 18, '#fffdf8', '900', 'center')
    drawLines(ctx, lines, 198, y + 42, 28, 38, '#2d2924', '700')
    y += boxHeight + 14
  })

  drawText(ctx, 'X-AgentStudy', 120, height - 86, 22, '#8a5b13', '900')
  drawText(ctx, `生成时间 ${new Date().toISOString().slice(0, 10)}`, 780, height - 86, 20, '#7c766a', '700')
  return canvas.toDataURL('image/png')
}

export function downloadOutcomePoster(dataUrl: string, filename: string) {
  const link = document.createElement('a')
  link.href = dataUrl
  link.download = filename
  link.rel = 'noopener'
  link.click()
}

export function buildOutcomePosterFilename(title: string) {
  const safe = String(title || 'learning-outcomes')
    .replace(/[\\/:*?"<>|]/g, '-')
    .replace(/\s+/g, '-')
    .slice(0, 80)
  return `${safe || 'learning-outcomes'}-${Date.now()}.png`
}

function wrapText(text: string, maxWidth: number, fontSize: number, weight = '500') {
  const source = String(text || '').trim()
  if (!source) {
    return []
  }
  const canvas = document.createElement('canvas')
  const ctx = canvas.getContext('2d')
  if (!ctx) {
    return [source]
  }
  ctx.font = `${weight} ${fontSize}px Aptos, "PingFang SC", "Microsoft YaHei", sans-serif`
  const tokens = source.match(/[\u3400-\u9fff]|[A-Za-z0-9_./#+-]+|[^\s]/g) || []
  const lines: string[] = []
  let current = ''
  let previous = ''
  for (const token of tokens) {
    const separator = needsSpace(previous, token) ? ' ' : ''
    const candidate = current ? `${current}${separator}${token}` : token
    if (current && ctx.measureText(candidate).width > maxWidth) {
      lines.push(current)
      current = token
    } else {
      current = candidate
    }
    previous = token
  }
  if (current) {
    lines.push(current)
  }
  return lines
}

function drawLines(
  ctx: CanvasRenderingContext2D,
  lines: string[],
  x: number,
  y: number,
  fontSize: number,
  lineHeight: number,
  color: string,
  weight = '500',
) {
  lines.forEach((line, index) => drawText(ctx, line, x, y + index * lineHeight, fontSize, color, weight))
}

function drawText(
  ctx: CanvasRenderingContext2D,
  text: string,
  x: number,
  y: number,
  size: number,
  color: string,
  weight = '500',
  align: CanvasTextAlign = 'left',
) {
  ctx.font = `${weight} ${size}px Aptos, "PingFang SC", "Microsoft YaHei", sans-serif`
  ctx.fillStyle = color
  ctx.textAlign = align
  ctx.fillText(text, x, y)
}

function roundRect(
  ctx: CanvasRenderingContext2D,
  x: number,
  y: number,
  width: number,
  height: number,
  radius: number,
  fill: string,
  stroke?: string,
  lineWidth = 1,
) {
  ctx.beginPath()
  ctx.moveTo(x + radius, y)
  ctx.arcTo(x + width, y, x + width, y + height, radius)
  ctx.arcTo(x + width, y + height, x, y + height, radius)
  ctx.arcTo(x, y + height, x, y, radius)
  ctx.arcTo(x, y, x + width, y, radius)
  ctx.closePath()
  ctx.fillStyle = fill
  ctx.fill()
  if (stroke) {
    ctx.strokeStyle = stroke
    ctx.lineWidth = lineWidth
    ctx.stroke()
  }
}

function needsSpace(previousToken: string, nextToken: string) {
  if (!previousToken || !nextToken) {
    return false
  }
  const latinLike = /^[A-Za-z0-9_./#+-]+$/
  return latinLike.test(previousToken) && latinLike.test(nextToken)
}

function measureText(text: string, size: number, weight = '500') {
  const canvas = document.createElement('canvas')
  const ctx = canvas.getContext('2d')
  if (!ctx) {
    return text.length * size
  }
  ctx.font = `${weight} ${size}px Aptos, "PingFang SC", "Microsoft YaHei", sans-serif`
  return ctx.measureText(text).width
}

function chunk<T>(source: T[], size: number) {
  const result: T[][] = []
  for (let index = 0; index < source.length; index += size) {
    result.push(source.slice(index, index + size))
  }
  return result
}
