import { Button } from '@death-star/millennium';
import { AlignLeft, LogOut, Settings } from 'lucide-react';
import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router';

interface TopbarProps {
  onLogout: () => void;
  title?: string;
}

export function Topbar({ onLogout, title = 'Death Star Objectives' }: TopbarProps) {
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  // Close dropdown when clicking outside
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setDropdownOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  return (
    <div className="w-full flex justify-between items-center px-8 py-4 bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60 shadow-md sticky top-0 z-50">
      <h1 className="text-xl font-bold text-foreground tracking-tight">
        <Link to="/">{title}</Link>
      </h1>

      <div className="relative" ref={dropdownRef}>
        <Button
          variant="ghost"
          size="icon"
          onClick={() => setDropdownOpen(!dropdownOpen)}
          className="text-foreground hover:bg-neutral-100 dark:hover:bg-neutral-800 transition-colors"
        >
          <AlignLeft className="w-6 h-6" />
        </Button>

        {/* Dropdown Panel */}
        {dropdownOpen && (
          <div className="absolute top-12 right-0 w-56 bg-white border border-gray-100 rounded-xl shadow-lg shadow-gray-200/50 py-2 z-50 animate-in fade-in slide-in-from-top-2">
            <Link
              to="/settings/profile"
              className="w-full flex items-center gap-3 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50 hover:text-gray-900 transition-colors"
              onClick={() => setDropdownOpen(false)}
            >
              <Settings className="w-4 h-4 text-gray-400" />
              Account Settings
            </Link>
            <div className="h-px bg-gray-100 my-1 mx-2"></div>
            <button
              type="button"
              onClick={() => {
                setDropdownOpen(false);
                onLogout();
              }}
              className="w-full flex items-center gap-3 px-4 py-2 text-sm text-red-600 hover:bg-red-50 transition-colors"
            >
              <LogOut className="w-4 h-4 text-red-400" />
              Logout
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
