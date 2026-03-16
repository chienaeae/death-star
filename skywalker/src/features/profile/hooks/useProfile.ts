import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../../../api/client';
import type { components } from '@death-star/holocron';

type UserProfile = components['schemas']['UserProfile'];
type UserProfileRequest = components['schemas']['UserProfileRequest'];
type PresignedUrlRequest = components['schemas']['PresignedUrlRequest'];
type PresignedUrlResponse = components['schemas']['PresignedUrlResponse'];

const GET_PROFILE_KEY = ['userProfile'];

export function useProfileQuery() {
  return useQuery({
    queryKey: GET_PROFILE_KEY,
    queryFn: async (): Promise<UserProfile> => {
      const res = await apiClient.customFetch('/api/v1/users/profile');
      if (!res.ok) throw new Error('Failed to fetch profile');
      return res.json();
    },
  });
}

export function useUpdateProfileMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (data: UserProfileRequest): Promise<UserProfile> => {
      const res = await apiClient.customFetch('/api/v1/users/profile', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data),
      });
      if (!res.ok) throw new Error('Failed to update profile');
      return res.json();
    },
    onSuccess: (data) => {
      queryClient.setQueryData(GET_PROFILE_KEY, data);
    },
  });
}

export function useUploadAvatarMutation() {
  return useMutation({
    mutationFn: async (file: File): Promise<string> => {
      // 1. Get Presigned URL
      const reqPayload: PresignedUrlRequest = {
        filename: file.name,
        contentType: file.type,
      };

      const resUrl = await apiClient.customFetch('/api/v1/assets/upload-url', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(reqPayload),
      });
      if (!resUrl.ok) throw new Error('Failed to get upload URL');
      const presignedData: PresignedUrlResponse = await resUrl.json();

      // 2. Direct PUT to MinIO/S3 using RAW fetch (no Auth Headers)
      // The customFetch hook injects Authorization headers, which S3 rejects due to signature mismatch.
      // We must use native fetch here.
      const s3Res = await fetch(presignedData.uploadUrl, {
        method: presignedData.method,
        headers: {
          'Content-Type': file.type,
        },
        body: file,
      });

      if (!s3Res.ok) throw new Error('Failed to upload file to storage');

      return presignedData.assetId;
    },
  });
}
