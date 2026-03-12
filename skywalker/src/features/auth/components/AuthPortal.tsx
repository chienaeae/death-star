import { Button, Card, CardContent, CardHeader, CardTitle } from '@death-star/millennium';
import { Lock, LogIn, Mail } from 'lucide-react';
import type React from 'react';
import { useState } from 'react';

interface AuthPortalProps {
  errorMsg: string;
  setErrorMsg: (msg: string) => void;
  onLogin: (email: string, password: string) => Promise<void>;
  onRegister: (email: string, password: string) => Promise<void>;
}

export function AuthPortal({ errorMsg, setErrorMsg, onLogin, onRegister }: AuthPortalProps) {
  const [isLoginMode, setIsLoginMode] = useState(true);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg('');
    if (isLoginMode) {
      await onLogin(email, password);
    } else {
      await onRegister(email, password);
    }
  };

  return (
    <div className="flex min-h-[calc(100vh-4rem)] w-full items-center justify-center p-4">
      <Card className="w-[400px] rounded-3xl border-0 bg-gradient-to-b from-[#e3f4ff] via-white to-white via-[40%] shadow-xl">
        <CardHeader className="flex flex-col items-center pt-8 pb-4">
          <div className="mb-6 flex h-16 w-16 items-center justify-center rounded-2xl bg-gradient-to-b from-white to-[#f0f6fa] shadow-[0_4px_12px_rgba(0,0,0,0.05)] border border-gray-100/50">
            <LogIn className="h-7 w-7 text-gray-700" strokeWidth={2} />
          </div>
          <CardTitle className="text-xl font-bold text-gray-900 tracking-tight">
            {isLoginMode ? 'Sign in with email' : 'Sign up with email'}
          </CardTitle>
          <div className="mt-2 text-center text-sm text-gray-500 max-w-[260px] leading-relaxed">
            Make a new doc to bring your words, data, and teams together. For free
          </div>
        </CardHeader>

        <CardContent className="px-8 pb-8">
          {errorMsg && (
            <div className="mb-4 rounded-xl bg-red-50 p-3 text-center text-sm text-red-600 border border-red-100">
              {errorMsg}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="relative">
              <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-4">
                <Mail className="h-5 w-5 text-gray-400" />
              </div>
              <input
                id="email"
                type="email"
                required
                placeholder="Email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="block w-full rounded-2xl border-0 bg-[#f4f5f7] py-3.5 pl-11 pr-4 text-gray-900 placeholder:text-gray-400 focus:bg-white focus:ring-2 focus:ring-gray-200 transition-colors sm:text-sm sm:leading-6"
              />
            </div>

            <div className="relative">
              <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-4">
                <Lock className="h-5 w-5 text-gray-400" />
              </div>
              <input
                id="password"
                type="password"
                required
                placeholder="Password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="block w-full rounded-2xl border-0 bg-[#f4f5f7] py-3.5 pl-11 pr-4 text-gray-900 placeholder:text-gray-400 focus:bg-white focus:ring-2 focus:ring-gray-200 transition-colors sm:text-sm sm:leading-6"
              />
            </div>

            <div className="flex justify-end pt-1 pb-2">
              <button
                type="button"
                onClick={() => {
                  setErrorMsg('');
                  setIsLoginMode(!isLoginMode);
                }}
                className="text-xs font-semibold text-gray-500 hover:text-gray-900 transition-colors"
              >
                {isLoginMode ? 'Sign up' : 'Log in'}
              </button>
            </div>

            <div className="pt-2">
              <Button
                type="submit"
                className="w-full rounded-2xl bg-[#1c1c21] py-6 text-[15px] font-medium text-white hover:bg-black"
                variant="default"
              >
                {isLoginMode ? 'Log in' : 'Sign up'}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
