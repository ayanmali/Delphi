export type CandidateAttempt = {
    id: number;
    candidateId: number;
    assessmentId: number;
    status: 'started' | 'submitted' | 'evaluated';
    startedAt?: Date | null;
    submittedAt?: Date | null;
    evaluatedAt?: Date | null;
}