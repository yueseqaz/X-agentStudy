export type MindMapNodeKind = 'root' | 'chapter' | 'unit' | 'point' | 'task'

export interface MindMapKnowledgePoint {
  id: string
  title: string
  level?: string
  outcome?: string
  estimatedMinutes?: number
}

export interface MindMapUnit {
  unitIndex: number
  name: string
  goal?: string
  knowledgePoints?: MindMapKnowledgePoint[]
}

export interface MindMapStage {
  name: string
  focus?: string
  duration?: string
  tasks?: string[]
  chapterIndex?: number
  outcome?: string
  units?: MindMapUnit[]
}

export interface MindMapNode {
  id: string
  parentId: string | null
  kind: MindMapNodeKind
  label: string
  subtitle: string
  x: number
  y: number
  width: number
  height: number
  chapterIndex?: number
  unitIndex?: number
  pointIndex?: number
}

export interface MindMapLink {
  id: string
  sourceId: string
  targetId: string
}

export interface MindMapGraph {
  nodes: MindMapNode[]
  links: MindMapLink[]
}

export function buildPlanMindMap(title: string, goal: string, stages: MindMapStage[]): MindMapGraph {
  const nodes: MindMapNode[] = []
  const links: MindMapLink[] = []
  nodes.push(node('root', null, 'root', title || '学习计划', goal || '当前计划目标', 0, 0, 260, 96))

  if (stages.length === 0) {
    return { nodes, links }
  }

  const chapterRadiusX = 420
  const chapterRadiusY = Math.max(260, stages.length * 72)
  stages.forEach((stage, chapterIndex) => {
    const angle = distributeAngle(chapterIndex, stages.length)
    const chapterId = `chapter-${chapterIndex}`
    const chapterX = Math.cos(angle) * chapterRadiusX
    const chapterY = Math.sin(angle) * chapterRadiusY
    nodes.push(node(
      chapterId,
      'root',
      'chapter',
      stage.name,
      stage.outcome || stage.focus || stage.duration || '阶段目标',
      chapterX,
      chapterY,
      250,
      90,
      { chapterIndex },
    ))
    links.push(link('root', chapterId))

    if (stage.units?.length) {
      const unitSpread = Math.max(180, stage.units.length * 96)
      stage.units.forEach((unit, unitIndex) => {
        const unitId = `chapter-${chapterIndex}-unit-${unitIndex}`
        const unitOffset = centeredOffset(unitIndex, stage.units?.length || 1, unitSpread)
        const unitX = chapterX + Math.cos(angle) * 320 - Math.sin(angle) * unitOffset
        const unitY = chapterY + Math.sin(angle) * 210 + Math.cos(angle) * unitOffset
        nodes.push(node(
          unitId,
          chapterId,
          'unit',
          unit.name,
          unit.goal || '学习单元',
          unitX,
          unitY,
          220,
          82,
          { chapterIndex, unitIndex },
        ))
        links.push(link(chapterId, unitId))

        const points = unit.knowledgePoints || []
        const pointSpread = Math.max(170, points.length * 64)
        points.forEach((point, pointIndex) => {
          const pointId = `chapter-${chapterIndex}-unit-${unitIndex}-point-${pointIndex}`
          const pointOffset = centeredOffset(pointIndex, points.length || 1, pointSpread)
          const pointX = unitX + Math.cos(angle) * 300 - Math.sin(angle) * pointOffset
          const pointY = unitY + Math.sin(angle) * 190 + Math.cos(angle) * pointOffset
          nodes.push(node(
            pointId,
            unitId,
            'point',
            point.title,
            point.level || `${point.estimatedMinutes || 45} 分钟`,
            pointX,
            pointY,
            190,
            74,
            { chapterIndex, unitIndex, pointIndex },
          ))
          links.push(link(unitId, pointId))
        })
      })
      return
    }

    const tasks = stage.tasks || []
    const taskSpread = Math.max(170, tasks.length * 62)
    tasks.forEach((taskText, taskIndex) => {
      const taskId = `chapter-${chapterIndex}-task-${taskIndex}`
      const taskOffset = centeredOffset(taskIndex, tasks.length || 1, taskSpread)
      const taskX = chapterX + Math.cos(angle) * 300 - Math.sin(angle) * taskOffset
      const taskY = chapterY + Math.sin(angle) * 190 + Math.cos(angle) * taskOffset
      nodes.push(node(taskId, chapterId, 'task', taskText, '阶段任务', taskX, taskY, 200, 72, { chapterIndex }))
      links.push(link(chapterId, taskId))
    })
  })

  return { nodes, links }
}

function distributeAngle(index: number, total: number) {
  if (total === 1) {
    return 0
  }
  return -Math.PI * 0.78 + (index * Math.PI * 1.56) / Math.max(1, total - 1)
}

function centeredOffset(index: number, total: number, spread: number) {
  if (total <= 1) {
    return 0
  }
  return (index / (total - 1) - 0.5) * spread
}

function node(
  id: string,
  parentId: string | null,
  kind: MindMapNodeKind,
  label: string,
  subtitle: string,
  x: number,
  y: number,
  width: number,
  height: number,
  meta: Partial<Pick<MindMapNode, 'chapterIndex' | 'unitIndex' | 'pointIndex'>> = {},
): MindMapNode {
  return {
    id,
    parentId,
    kind,
    label,
    subtitle,
    x,
    y,
    width,
    height,
    ...meta,
  }
}

function link(sourceId: string, targetId: string): MindMapLink {
  return {
    id: `${sourceId}->${targetId}`,
    sourceId,
    targetId,
  }
}
