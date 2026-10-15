export type RequestStatus = 'SUBMITTED' | 'APPROVED' | 'NEEDS_CHANGES' | 'REJECTED';
export type DecisionOutcome = Exclude<RequestStatus, 'SUBMITTED'>;

export const REQUEST_STATUSES: RequestStatus[] = ['SUBMITTED', 'APPROVED', 'NEEDS_CHANGES', 'REJECTED'];
export const DECISION_OUTCOMES: DecisionOutcome[] = ['APPROVED', 'NEEDS_CHANGES', 'REJECTED'];

export interface ReviewDecision {
  id: number;
  outcome: DecisionOutcome;
  notes: string;
  actionItems: string[];
  decidedBy: string;
  decidedAt: string;
}

export interface ArchitectureRequest {
  id: number;
  title: string;
  applicationName: string;
  applicationOwner: string | null;
  description: string;
  status: RequestStatus;
  submittedBy: string;
  createdAt: string;
  updatedAt: string;
  decisions: ReviewDecision[];
}

export interface NewArchitectureRequest {
  title: string;
  applicationName: string;
  description: string;
}

export interface NewDecision {
  outcome: DecisionOutcome;
  notes: string;
  actionItems: string[];
}
