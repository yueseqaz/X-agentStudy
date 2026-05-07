export function renderMarkdown(markdown: string) {
  if (!markdown) {
    return ''
  }
  const codeBlocks: string[] = []
  let source = markdown.replace(/\r\n/g, '\n').replace(/```(\w*)\n([\s\S]*?)```/g, (_match, lang, code) => {
    const index = codeBlocks.length
    codeBlocks.push(
      `<pre><code class="language-${escapeHtml(lang || 'text')}">${escapeHtml(code.trim())}</code></pre>`,
    )
    return `@@CODE_BLOCK_${index}@@`
  })

  const lines = source.split('\n')
  const html: string[] = []
  let listOpen = false
  let orderedListOpen = false

  for (const rawLine of lines) {
    const line = rawLine.trim()
    if (!line) {
      if (listOpen) {
        html.push('</ul>')
        listOpen = false
      }
      if (orderedListOpen) {
        html.push('</ol>')
        orderedListOpen = false
      }
      continue
    }
    if (line.startsWith('@@CODE_BLOCK_')) {
      if (listOpen) {
        html.push('</ul>')
        listOpen = false
      }
      if (orderedListOpen) {
        html.push('</ol>')
        orderedListOpen = false
      }
      const index = Number(line.match(/@@CODE_BLOCK_(\d+)@@/)?.[1] || 0)
      html.push(codeBlocks[index] || '')
      continue
    }
    const heading = line.match(/^(#{1,6})\s+(.+)$/)
    if (heading) {
      if (listOpen) {
        html.push('</ul>')
        listOpen = false
      }
      if (orderedListOpen) {
        html.push('</ol>')
        orderedListOpen = false
      }
      const level = Math.min(4, heading[1].length)
      html.push(`<h${level}>${renderInline(heading[2])}</h${level}>`)
      continue
    }
    const bullet = line.match(/^[-*]\s+(.+)$/)
    if (bullet) {
      if (orderedListOpen) {
        html.push('</ol>')
        orderedListOpen = false
      }
      if (!listOpen) {
        html.push('<ul>')
        listOpen = true
      }
      html.push(`<li>${renderInline(bullet[1])}</li>`)
      continue
    }
    const ordered = line.match(/^\d+\.\s+(.+)$/)
    if (ordered) {
      if (listOpen) {
        html.push('</ul>')
        listOpen = false
      }
      if (!orderedListOpen) {
        html.push('<ol>')
        orderedListOpen = true
      }
      html.push(`<li>${renderInline(ordered[1])}</li>`)
      continue
    }
    if (listOpen) {
      html.push('</ul>')
      listOpen = false
    }
    if (orderedListOpen) {
      html.push('</ol>')
      orderedListOpen = false
    }
    html.push(`<p>${renderInline(line)}</p>`)
  }
  if (listOpen) {
    html.push('</ul>')
  }
  if (orderedListOpen) {
    html.push('</ol>')
  }
  return html.join('')
}

function renderInline(value: string) {
  return escapeHtml(value)
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
    .replace(/\*([^*]+)\*/g, '<em>$1</em>')
}

function escapeHtml(value: string) {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;')
}
