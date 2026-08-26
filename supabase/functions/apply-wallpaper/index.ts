import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { supabaseAdmin, corsHeaders } from "../shared/supabase.ts";

serve(async (req) => {
    if (req.method === 'OPTIONS') return new Response('ok', { headers: corsHeaders });
    
    try {
        const authHeader = req.headers.get('Authorization');
        if (!authHeader) throw new Error("Missing auth");
        const token = authHeader.replace('Bearer ', '');
        
        const { data: { user }, error: authError } = await supabaseAdmin.auth.getUser(token);
        if (authError || !user) throw new Error("Unauthorized");
        const uid = user.id;

        const { wallpaperId } = await req.json();

        const { data: wallpaper } = await supabaseAdmin.from('wallpapers').select('*').eq('id', wallpaperId).single();
        if (!wallpaper || !wallpaper.is_active) throw new Error("Wallpaper not found");

        const { data: userProfile } = await supabaseAdmin.from('users').select('*').eq('id', uid).single();
        
        const isPremium = wallpaper.is_premium;
        const subStatus = userProfile?.subscription_status || 'NONE';
        const retainedId = userProfile?.retained_wallpaper_id;

        if (subStatus !== 'ACTIVE') {
            if (isPremium) {
                // If not active, premium wallpaper applying is blocked (unless retained)
                if (retainedId !== wallpaperId) {
                    throw new Error("Premium subscription required");
                }
            } else {
                // Free wallpaper, check app_metadata for ad reward SSV unlock
                const { data: { user: adminUser } } = await supabaseAdmin.auth.admin.getUserById(uid);
                const appMetadata = adminUser?.app_metadata || {};
                const unlocked = appMetadata.ad_unlocked_wallpapers || [];
                
                if (!unlocked.includes(wallpaperId)) {
                    throw new Error("Reward ad verification missing");
                }
                
                // Consume the reward token so they have to watch again if they re-apply later
                const newUnlocked = unlocked.filter((id: string) => id !== wallpaperId);
                await supabaseAdmin.auth.admin.updateUserById(uid, {
                    app_metadata: { ...appMetadata, ad_unlocked_wallpapers: newUnlocked }
                });
            }
        }

        const updates: any = { current_applied_wallpaper_id: wallpaperId };
        
        if (subStatus === 'EXPIRED' && (!isPremium || retainedId !== wallpaperId)) {
            updates.retained_wallpaper_id = null;
        }

        await supabaseAdmin.from('users').update(updates).eq('id', uid);

        return new Response(JSON.stringify({ success: true }), { headers: { ...corsHeaders, 'Content-Type': 'application/json' } });
    } catch (e: any) {
        return new Response(JSON.stringify({ error: e.message }), { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } });
    }
});
