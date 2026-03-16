import { Button, Card, CardContent, CardHeader, CardTitle } from '@death-star/millennium';
import { Eye, EyeOff, Lock, LogIn, Mail } from 'lucide-react';
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
  const [showPassword, setShowPassword] = useState(false);

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
      <Card className="w-[400px] rounded-3xl border bg-card text-card-foreground shadow-xl">
        <CardHeader className="flex flex-col items-center pt-8 pb-4">
          <div className="mb-6 flex h-16 w-16 items-center justify-center rounded-2xl bg-muted shadow-sm border border-border">
            <LogIn className="h-7 w-7 text-muted-foreground" strokeWidth={2} />
          </div>
          <CardTitle className="text-xl font-bold tracking-tight">
            {isLoginMode ? 'Sign in with email' : 'Sign up with email'}
          </CardTitle>
          <div className="mt-2 text-center text-sm text-muted-foreground max-w-[260px] leading-relaxed">
            Make a new doc to bring your words, data, and teams together. For free
          </div>
        </CardHeader>

        <CardContent className="px-8 pb-8">
          {errorMsg && (
            <div className="mb-4 rounded-xl bg-destructive/10 p-3 text-center text-sm text-destructive border border-destructive/20">
              {errorMsg}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="relative">
              <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-4">
                <Mail className="h-5 w-5 text-muted-foreground" />
              </div>
              <input
                id="email"
                type="email"
                required
                placeholder="Email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="block w-full rounded-2xl border-0 bg-muted py-3.5 pl-11 pr-4 text-foreground placeholder:text-muted-foreground focus:bg-background focus:ring-2 focus:ring-ring transition-colors sm:text-sm sm:leading-6"
              />
            </div>

            <div className="relative">
              <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-4">
                <Lock className="h-5 w-5 text-muted-foreground" />
              </div>
              <input
                id="password"
                type={showPassword ? "text" : "password"}
                required
                placeholder="Password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="block w-full rounded-2xl border-0 bg-muted py-3.5 pl-11 pr-12 text-foreground placeholder:text-muted-foreground focus:bg-background focus:ring-2 focus:ring-ring transition-colors sm:text-sm sm:leading-6"
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="absolute inset-y-0 right-0 flex items-center pr-4 text-muted-foreground hover:text-foreground transition-colors"
                tabIndex={-1}
              >
                {showPassword ? (
                  <EyeOff className="h-5 w-5" />
                ) : (
                  <Eye className="h-5 w-5" />
                )}
              </button>
            </div>

            <div className="flex justify-end items-center gap-1.5 pt-1 pb-2">
              <span className="text-xs text-muted-foreground">
                {isLoginMode ? "Don't have an account?" : "Already a member?"}
              </span>
              <button
                type="button"
                onClick={() => {
                  setErrorMsg('');
                  setIsLoginMode(!isLoginMode);
                }}
                className="text-xs font-semibold text-foreground hover:text-primary transition-colors"
              >
                {isLoginMode ? 'Sign up' : 'Log in'}
              </button>
            </div>

            <div className="pt-2">
              <Button
                type="submit"
                className="w-full rounded-2xl bg-primary py-6 text-[15px] font-medium text-primary-foreground hover:bg-primary/90"
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
