import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../hooks/useAuth';
import { listGoals, getGoalProgress, createGoal, contribute, deleteGoal } from '../api/goalApi';
import type { Goal, GoalProgress } from '../types';
import { GoalCard } from '../components/goals/GoalCard';
import { GoalForm } from '../components/goals/GoalForm';
import { ContributeModal } from '../components/goals/ContributeModal';
import { PremiumUpsellCard } from '../components/goals/PremiumUpsellCard';
import { EmptyGoalSlot } from '../components/goals/EmptyGoalSlot';
import { TopBar } from '../components/layout/TopBar';
import { useGoals } from '../hooks/api/useGoals';
import { useMutation } from '../hooks/useMutation';

export const Goals: React.FC = () => {
  const { isPremium } = useAuth();
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [contributeModalState, setContributeModalState] = useState<{ isOpen: boolean; goalId: number; title: string }>({
    isOpen: false,
    goalId: 0,
    title: ''
  });

  const { data, isLoading, error: fetchError, refetch } = useGoals();
  const goals = data?.goals || [];
  const progressMap = data?.progressMap || {};

  const { mutate: performCreate } = useMutation(createGoal, {
    onSuccess: () => {
      setIsFormOpen(false);
      refetch();
    }
  });

  const { mutate: performContribute } = useMutation(
    (vars: { goalId: number, amount: number }) => contribute(vars.goalId, { amount: vars.amount }),
    {
      onSuccess: () => {
        setContributeModalState({ isOpen: false, goalId: 0, title: '' });
        refetch();
      }
    }
  );

  const { mutate: performDelete } = useMutation(deleteGoal, {
    onSuccess: () => refetch()
  });

  const error = fetchError || (data?.failedProgressGoalIds.length ? 'Certains objectifs n\'ont pas pu être chargés.' : null);

  const activeGoals = goals.filter(g => g.status === 'EN_COURS' || g.status === 'EN_RETARD');
  const achievedGoals = goals.filter(g => g.status === 'ATTEINT');

  const handleCreateGoal = async (data: { title: string; targetAmount: number; targetDate: string }) => {
    await performCreate(data);
  };

  const handleContribute = async (amount: string | number) => {
    await performContribute({ goalId: contributeModalState.goalId, amount: Number(amount) });
  };

  const handleDelete = async (id: number) => {
    await performDelete(id);
  };

  if (isLoading && goals.length === 0) {
    return <div className="p-6 text-text-secondary">Chargement...</div>;
  }

  return (
    <div className="flex-1 flex flex-col h-full bg-bg-base">
      <TopBar title="Vos objectifs" action={isPremium ? { label: 'Nouvel objectif', onClick: () => setIsFormOpen(true) } : undefined} />

      <div className="flex-1 p-8 overflow-y-auto space-y-6">
        {error && <div className="text-danger mb-4">{error}</div>}

        {/* FREEMIUM LAYOUT */}
        {!isPremium && (
          <div className="mb-12">
            <div className="max-w-4xl mx-auto grid grid-cols-1 lg:grid-cols-2 gap-6 items-stretch">
              {/* Slot 1: The Free Slot */}
              <div className="h-full">
                {activeGoals.length === 0 ? (
                  <EmptyGoalSlot onClick={() => setIsFormOpen(true)} />
                ) : (
                  progressMap[activeGoals[0].id] && (
                    <GoalCard
                      goal={activeGoals[0]}
                      progress={progressMap[activeGoals[0].id]}
                      onContribute={(goalId, title) => setContributeModalState({ isOpen: true, goalId, title })}
                      onDelete={handleDelete}
                    />
                  )
                )}
              </div>

              {/* Slot 2: The Premium Upsell Slot */}
              <div className="h-full">
                <PremiumUpsellCard />
              </div>
            </div>
          </div>
        )}

        {/* PREMIUM LAYOUT (Grid of active goals) */}
        {isPremium && (
          <div className="mb-12">
            {activeGoals.length === 0 ? (
              <div className="text-center py-12 text-text-secondary bg-bg-card rounded-xl border border-border-subtle">
                Aucun objectif en cours.
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
                {activeGoals.map(goal => (
                  progressMap[goal.id] && (
                    <GoalCard
                      key={goal.id}
                      goal={goal}
                      progress={progressMap[goal.id]}
                      onContribute={(goalId, title) => setContributeModalState({ isOpen: true, goalId, title })}
                      onDelete={handleDelete}
                    />
                  )
                ))}
              </div>
            )}
          </div>
        )}

        {/* HISTORIQUE (Achieved Goals) */}
        {achievedGoals.length > 0 && (
          <div>
            <h2 className="text-xl font-bold text-text-primary mb-4 border-t border-border-subtle pt-6">
              Historique (Objectifs Atteints)
            </h2>
            <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6 opacity-75">
              {achievedGoals.map(goal => (
                progressMap[goal.id] && (
                  <GoalCard
                    key={goal.id}
                    goal={goal}
                    progress={progressMap[goal.id]}
                    onContribute={(goalId, title) => setContributeModalState({ isOpen: true, goalId, title })}
                    onDelete={handleDelete}
                  />
                )
              ))}
            </div>
          </div>
        )}
      </div>

      {/* Create Goal Modal */}
      {isFormOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div className="w-full max-w-lg">
            <GoalForm
              onSuccess={handleCreateGoal}
              onCancel={() => setIsFormOpen(false)}
            />
          </div>
        </div>
      )}

      <ContributeModal
        isOpen={contributeModalState.isOpen}
        goalId={contributeModalState.goalId}
        goalTitle={contributeModalState.title}
        onSuccess={handleContribute}
        onClose={() => setContributeModalState({ isOpen: false, goalId: 0, title: '' })}
      />
    </div>
  );
};