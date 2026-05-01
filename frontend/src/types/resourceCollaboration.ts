export interface CollaboratorApplication {
  id: number
  userId: number
  status: 'PENDING' | 'APPROVED' | 'REJECTED'
  reason: string
  expertise: string | null
  reviewNote: string | null
  reviewedBy: number | null
  reviewedAt: string | null
  createdAt: string
  updatedAt: string
}

export interface ResourceSource {
  id: number
  ownerUserId: number
  name: string
  sourceType: string
  baseUrl: string
  enabled: boolean
  sourceCategory: string | null
  createdAt: string
  updatedAt: string
}

export interface IngestionTask {
  id: number
  sourceId: number | null
  ownerUserId: number
  sourceType: string
  targetUrl: string
  titleHint: string | null
  summaryHint: string | null
  tags: string | null
  status: string
  createdAt: string
  updatedAt: string
}

export interface CandidateResource {
  id: number
  ownerUserId: number
  sourceId: number | null
  taskId: number | null
  sourceType: string
  title: string
  summary: string | null
  resourceUrl: string
  authorName: string | null
  coverImageUrl: string | null
  durationSeconds: number | null
  tags: string
  reviewStatus: string
  contentCaptureMode: string
  rawContent: string | null
  reviewNote: string | null
  reviewedBy: number | null
  reviewedAt: string | null
  publishedResourceId: number | null
  createdAt: string
  updatedAt: string
}

export interface CollaboratorWorkspace {
  sources: ResourceSource[]
  tasks: IngestionTask[]
  candidates: CandidateResource[]
  publishedResources: {
    id: number
    title: string
    resourceType: string
    subjectName: string
    subjectScope: string
    tags: string
    createdAt: string
  }[]
}

export type CollaboratorApplicationReview = CollaboratorApplication
export type CandidateResourceReview = CandidateResource
export type SourceOverview = ResourceSource
