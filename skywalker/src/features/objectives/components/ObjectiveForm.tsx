import { Button, Input } from '@death-star/millennium';
import type React from 'react';
import { useState } from 'react';

interface ObjectiveFormProps {
  onSubmit: (title: string) => Promise<void>;
}

export function ObjectiveForm({ onSubmit }: ObjectiveFormProps) {
  const [newTodo, setNewTodo] = useState('');

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newTodo.trim()) return;
    try {
      await onSubmit(newTodo);
      setNewTodo('');
    } catch (err) {
      console.error('Failed to create todo', err);
    }
  };

  return (
    <form onSubmit={handleCreate} className="mb-8 flex gap-4">
      <Input
        type="text"
        value={newTodo}
        onChange={(e) => setNewTodo(e.target.value)}
        placeholder="Assign new objective..."
        className="bg-white border-0 text-gray-900 placeholder:text-gray-400 shadow-sm rounded-2xl py-6 px-5"
      />
      <Button
        type="submit"
        className="bg-[#1c1c21] text-white hover:bg-black font-medium rounded-2xl px-8 h-auto"
        variant="default"
      >
        Deploy
      </Button>
    </form>
  );
}
