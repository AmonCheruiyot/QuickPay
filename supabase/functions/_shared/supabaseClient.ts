import { createClient } from 'npm:@supabase/supabase-js@2.49.1';

export function getSupabaseAdminClient() {
  const supabaseUrl = Deno.env.get('SUPABASE_URL') || 'https://injsofnemhbedeswjyev.supabase.co';
  const serviceRoleKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') || Deno.env.get('SUPABASE_ANON_KEY') || '';

  return createClient(supabaseUrl, serviceRoleKey, {
    auth: {
      persistSession: false,
    },
  });
}
