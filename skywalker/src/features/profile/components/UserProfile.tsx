import { Input, Label } from '@death-star/millennium';
import { Camera, Loader2 } from 'lucide-react';
import { useEffect, useState } from 'react';
import { SettingsLayout } from '../../settings/layouts/SettingsLayout';
import {
  useProfileQuery,
  useUpdateProfileMutation,
  useUploadAvatarMutation,
} from '../hooks/useProfile';

export function UserProfile() {
  const { data: profile, isLoading } = useProfileQuery();
  const updateProfile = useUpdateProfileMutation();
  const uploadAvatar = useUploadAvatarMutation();

  const [displayName, setDisplayName] = useState('');
  const [bio, setBio] = useState('');
  const [avatarPreview, setAvatarPreview] = useState<string | null>(null);

  // Sync profile data to local state when fetched
  useEffect(() => {
    if (profile) {
      setDisplayName(profile.displayName || '');
      setBio(profile.bio || '');
      setAvatarPreview(profile.avatarUrl || null);
    }
  }, [profile]);

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const file = e.target.files[0];
      // Create local preview immediately for UX
      const objectUrl = URL.createObjectURL(file);
      setAvatarPreview(objectUrl);
      
      try {
        const avatarAssetId = await uploadAvatar.mutateAsync(file);
        // Save immediately
        await updateProfile.mutateAsync({
          displayName,
          bio,
          avatarAssetId,
        });
      } catch (err) {
        console.error('Failed to update avatar immediately:', err);
        // Optionally revert local preview here if needed
      }
    }
  };

  const handleSave = async () => {
    try {
      await updateProfile.mutateAsync({
        displayName,
        bio,
      });
    } catch (e) {
      console.error('Failed to save profile:', e);
    }
  };

  if (isLoading) {
    return (
      <SettingsLayout>
        <div className="flex justify-center items-center h-64">
          <Loader2 className="w-8 h-8 animate-spin text-blue-500" />
        </div>
      </SettingsLayout>
    );
  }

  const isSaving = updateProfile.isPending || uploadAvatar.isPending;

  return (
    <SettingsLayout>
      <div className="bg-card text-card-foreground border shadow-sm rounded-lg p-10 animate-in fade-in duration-500">
        
        <div className="space-y-10">
          {/* Avatar Header Section */}
          <div className="flex justify-center mb-8">
            <div className="relative">
              <div className="w-32 h-32 rounded-full overflow-hidden bg-muted flex items-center justify-center border shadow-sm relative group">
                {avatarPreview ? (
                  <img src={avatarPreview} alt="Avatar" className="w-full h-full object-cover" />
                ) : (
                  <span className="text-muted-foreground text-4xl">👤</span>
                )}
              </div>

              {/* Overlapping Camera Icon Badge */}
              <label
                htmlFor="avatar-upload"
                className="absolute bottom-0 right-0 w-10 h-10 bg-primary rounded-full flex items-center justify-center shadow-[0_0_0_4px_white] cursor-pointer hover:bg-primary/90 transition-colors hover:scale-105"
                title="Upload New Avatar"
              >
                <Camera className="w-5 h-5 text-primary-foreground" />
                <input
                  id="avatar-upload"
                  type="file"
                  accept="image/*"
                  className="hidden"
                  onChange={handleFileChange}
                />
              </label>
            </div>
          </div>

          {/* Form Grid Section */}
          <div className="grid grid-cols-1 gap-y-6">
            
            {/* Display Name (representing First/Last in our model) */}
            <div className="space-y-2">
               <Label htmlFor="displayName" className="text-sm font-semibold">
                Display Name <span className="text-destructive">*</span>
              </Label>
              <Input
                id="displayName"
                placeholder="First/Last name"
                value={displayName}
                onChange={(e) => setDisplayName(e.target.value)}
              />
            </div>

            {/* Email */}
            <div className="space-y-2">
              <Label htmlFor="email" className="text-sm font-semibold">
                Email
              </Label>
              <Input
                id="email"
                value={profile?.email || ''}
                readOnly
                disabled
              />
            </div>

            {/* Bio */}
            <div className="space-y-2">
              <Label htmlFor="bio" className="text-sm font-semibold">
                Bio
              </Label>
              <textarea
                id="bio"
                className="flex min-h-[120px] w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm shadow-sm placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50 resize-none"
                placeholder="Tell us about yourself..."
                value={bio}
                onChange={(e) => setBio(e.target.value)}
              />
            </div>
            
          </div>

          <div className="pt-4">
            <button 
              onClick={handleSave} 
              disabled={isSaving} 
              className="bg-primary hover:bg-primary/90 text-primary-foreground font-medium rounded-md text-sm px-8 py-3 w-48 transition-colors flex justify-center items-center"
            >
              {isSaving ? (
                <Loader2 className="w-5 h-5 animate-spin" />
              ) : (
                "Save Changes"
              )}
            </button>
          </div>
        </div>
      </div>
    </SettingsLayout>
  );
}
