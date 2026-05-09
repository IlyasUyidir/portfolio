import React from 'react';
import { Outlet } from 'react-router-dom';
import { Sidebar } from './Sidebar';

export const AppShell: React.FC = () => {
  return (
    <div className="flex h-screen w-full bg-bg-base overflow-hidden">
      <Sidebar />
      <main className="flex-1 ml-[220px] overflow-y-auto relative bg-bg-base">
        <Outlet />
      </main>
    </div>
  );
};
