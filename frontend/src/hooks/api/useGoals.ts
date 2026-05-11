import { useQuery } from '../useQuery';
import { listGoals, getGoalProgress } from '../../api/goalApi';
import type { Goal, GoalProgress } from '../../types';

interface GoalsData {
  goals: Goal[];
  progressMap: Record<number, GoalProgress>;
  failedProgressGoalIds: number[];
}

export const useGoals = () => {
  return useQuery<GoalsData>(async () => {
    const fetchedGoals = await listGoals();
    
    const progressPromises = fetchedGoals.map(goal =>
      getGoalProgress(goal.id).then(
        progress => ({ status: 'fulfilled' as const, value: progress, goalId: goal.id }),
        err => ({ status: 'rejected' as const, reason: err, goalId: goal.id })
      )
    );

    const progressResults = await Promise.all(progressPromises);

    const progressMap: Record<number, GoalProgress> = {};
    const failedProgressGoalIds: number[] = [];

    progressResults.forEach(result => {
      if (result.status === 'fulfilled') {
        progressMap[result.goalId] = result.value;
      } else {
        failedProgressGoalIds.push(result.goalId);
      }
    });

    return {
      goals: fetchedGoals,
      progressMap,
      failedProgressGoalIds
    };
  }, []);
};
