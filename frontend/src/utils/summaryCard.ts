export interface SummaryCardContent {
  title: string
  summary: string
  highlights: string[]
  keywords: string[]
  tips: string[]
  sourceDocumentId: string
  sourceDocumentName: string
  planId: string
}

export function parseSummaryCardContent(value: string | SummaryCardContent | null | undefined): SummaryCardContent {
  if (typeof value === 'object' && value) {
    return normalizeSummaryCardContent(value)
  }
  try {
    return normalizeSummaryCardContent(JSON.parse(value || '{}'))
  } catch {
    return normalizeSummaryCardContent({})
  }
}

export function normalizeSummaryCardContent(value: Partial<SummaryCardContent>): SummaryCardContent {
  return {
    title: value.title || '知识总结图',
    summary: value.summary || '这张总结图提炼了当前文档中的核心知识点。',
    highlights: normalizeList(value.highlights, ['理解核心概念', '梳理关键方法', '记录易错提醒']),
    keywords: normalizeList(value.keywords, ['知识总结', '复习', '重点']),
    tips: normalizeList(value.tips, []),
    sourceDocumentId: String(value.sourceDocumentId || ''),
    sourceDocumentName: value.sourceDocumentName || '来源文档',
    planId: String(value.planId || ''),
  }
}

export async function exportSummaryCardPng(element: HTMLElement, content?: SummaryCardContent | null): Promise<string> {
  const data = content || contentFromElement(element)
  return drawSummaryCardToCanvas(data)
}

export function downloadSummaryCardImage(dataUrl: string, filename: string) {
  const link = document.createElement('a')
  link.href = dataUrl
  link.download = filename
  link.rel = 'noopener'
  link.click()
}

export function buildSummaryCardFilename(title: string) {
  const safe = String(title || 'summary')
    .replace(/[\\/:*?"<>|]/g, '-')
    .replace(/\s+/g, '-')
    .slice(0, 80)
  return `${safe || 'summary'}-${Date.now()}.png`
}

function drawSummaryCardToCanvas(content: SummaryCardContent): string {
  const width = 1080
  const scale = 2
  const titleLines = wrapText(content.title, 800, 54, '900')
  const summaryLines = wrapText(content.summary, 800, 30, '700')
  const highlightLines = content.highlights.map((item) => wrapText(item, 700, 25, '800'))
  const tipLines = content.tips.map((item) => wrapText(item, 760, 24, '700'))
  const height = Math.max(
    1480,
    620
    + titleLines.length * 76
    + summaryLines.length * 48
    + highlightLines.reduce((sum, lines) => sum + 86 + lines.length * 40, 0)
    + Math.ceil(content.keywords.length / 4) * 62
    + tipLines.reduce((sum, lines) => sum + 56 + lines.length * 36, 0),
  )
  const canvas = document.createElement('canvas')
  canvas.width = width * scale
  canvas.height = height * scale
  const ctx = canvas.getContext('2d')
  if (!ctx) {
    throw new Error('Canvas not supported')
  }
  ctx.scale(scale, scale)

  const gradient = ctx.createLinearGradient(0, 0, width, height)
  gradient.addColorStop(0, '#fffdf8')
  gradient.addColorStop(1, '#f5efe2')
  ctx.fillStyle = gradient
  ctx.fillRect(0, 0, width, height)
  ctx.fillStyle = 'rgba(31, 107, 84, 0.12)'
  ctx.beginPath()
  ctx.moveTo(0, 0)
  ctx.lineTo(420, 0)
  ctx.lineTo(0, 360)
  ctx.closePath()
  ctx.fill()

  roundRect(ctx, 72, 72, 936, height - 144, 30, '#fffdf8', '#d9d0c1', 2)
  let y = 118
  roundRect(ctx, 120, y, 64, 64, 18, '#1f6b54')
  drawText(ctx, '学', 152, y + 44, 30, '#fffdf8', '900', 'center')
  y += 118

  drawText(ctx, 'Knowledge Summary', 120, y, 22, '#8a5b13', '900')
  y += 74
  y = drawLines(ctx, titleLines, 120, y, 54, 74, '#202124', '900')
  y += 44
  y = drawLines(ctx, summaryLines, 120, y, 30, 48, '#4d473e', '700')
  y += 58

  drawText(ctx, '核心要点', 120, y, 30, '#1f6b54', '900')
  y += 42
  highlightLines.forEach((lines, index) => {
    const boxHeight = Math.max(100, 46 + lines.length * 40)
    roundRect(ctx, 120, y, 840, boxHeight, 18, '#fffefa', '#ded8cc', 1)
    roundRect(ctx, 144, y + 24, 48, 48, 24, '#eef8f2')
    drawText(ctx, String(index + 1), 168, y + 56, 22, '#1f6b54', '900', 'center')
    drawLines(ctx, lines, 214, y + 40, 25, 40, '#2d2924', '800')
    y += boxHeight + 20
  })
  y += 22

  drawText(ctx, '关键词', 120, y, 30, '#1f6b54', '900')
  y += 44
  let tagX = 120
  content.keywords.forEach((keyword) => {
    const tagWidth = Math.min(260, Math.max(98, ctx.measureText(keyword).width + 42))
    if (tagX + tagWidth > 960) {
      tagX = 120
      y += 62
    }
    roundRect(ctx, tagX, y, tagWidth, 44, 22, '#eef8f2')
    drawText(ctx, keyword, tagX + tagWidth / 2, y + 29, 21, '#1f6b54', '900', 'center')
    tagX += tagWidth + 14
  })
  y += 98

  if (tipLines.length) {
    drawText(ctx, '学习提醒', 120, y, 30, '#1f6b54', '900')
    y += 44
    tipLines.forEach((lines) => {
      const boxHeight = Math.max(68, 32 + lines.length * 36)
      roundRect(ctx, 120, y, 840, boxHeight, 14, '#fff8e8')
      ctx.fillStyle = '#c58a2c'
      ctx.fillRect(120, y, 8, boxHeight)
      drawLines(ctx, lines, 146, y + 38, 24, 36, '#4f3b18', '700')
      y += boxHeight + 16
    })
  }

  ctx.strokeStyle = '#ded8cc'
  ctx.lineWidth = 1
  ctx.beginPath()
  ctx.moveTo(120, height - 158)
  ctx.lineTo(960, height - 158)
  ctx.stroke()
  drawText(ctx, `来源文档：${content.sourceDocumentName}`, 120, height - 112, 22, '#766f63', '700')
  drawText(ctx, `计划编号：${content.planId}`, 120, height - 76, 22, '#766f63', '700')

  return canvas.toDataURL('image/png')
}

function normalizeList(value: unknown, fallback: string[]) {
  return Array.isArray(value)
    ? value.map((item) => String(item || '').trim()).filter(Boolean)
    : fallback
}

function contentFromElement(element: HTMLElement): SummaryCardContent {
  return normalizeSummaryCardContent({
    title: element.querySelector('h1')?.textContent || '',
    summary: element.querySelector('.summary-poster-summary')?.textContent || '',
    highlights: Array.from(element.querySelectorAll('.summary-poster-highlight strong')).map((item) => item.textContent || ''),
    keywords: Array.from(element.querySelectorAll('.summary-poster-tags span')).map((item) => item.textContent || ''),
    tips: Array.from(element.querySelectorAll('.summary-poster-tip')).map((item) => item.textContent || ''),
    sourceDocumentName: element.querySelector('.summary-poster-source')?.textContent?.replace(/来源文档：/, '').split('计划编号：')[0]?.trim(),
    planId: element.querySelector('.summary-poster-source')?.textContent?.split('计划编号：')[1]?.trim(),
  })
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
  const lines: string[] = []
  let current = ''
  let previousToken = ''
  const tokens = source.match(/[\u3400-\u9fff]|[A-Za-z0-9_./#+-]+|[^\s]/g) || []
  for (const token of tokens) {
    const separator = needsSpace(previousToken, token) ? ' ' : ''
    const candidate = current ? `${current}${separator}${token}` : token
    if (current && ctx.measureText(candidate).width > maxWidth) {
      lines.push(current)
      current = token
    } else {
      current = candidate
    }
    previousToken = token
  }
  if (current) {
    lines.push(current)
  }
  return lines
}

function needsSpace(previousToken: string, nextToken: string) {
  if (!previousToken || !nextToken) {
    return false
  }
  const latinLike = /^[A-Za-z0-9_./#+-]+$/
  return latinLike.test(previousToken) && latinLike.test(nextToken)
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
  return y + lines.length * lineHeight
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
  ctx.fillStyle = color
  ctx.font = `${weight} ${size}px Aptos, "PingFang SC", "Microsoft YaHei", sans-serif`
  ctx.textAlign = align
  ctx.textBaseline = 'alphabetic'
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
  ctx.lineTo(x + width - radius, y)
  ctx.quadraticCurveTo(x + width, y, x + width, y + radius)
  ctx.lineTo(x + width, y + height - radius)
  ctx.quadraticCurveTo(x + width, y + height, x + width - radius, y + height)
  ctx.lineTo(x + radius, y + height)
  ctx.quadraticCurveTo(x, y + height, x, y + height - radius)
  ctx.lineTo(x, y + radius)
  ctx.quadraticCurveTo(x, y, x + radius, y)
  ctx.closePath()
  ctx.fillStyle = fill
  ctx.fill()
  if (stroke) {
    ctx.strokeStyle = stroke
    ctx.lineWidth = lineWidth
    ctx.stroke()
  }
}
