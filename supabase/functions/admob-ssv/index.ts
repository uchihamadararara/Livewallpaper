import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { supabaseAdmin, corsHeaders } from "../shared/supabase.ts";

serve(async (req) => {
    if (req.method === 'OPTIONS') return new Response('ok', { headers: corsHeaders });
    
    try {
        const url = new URL(req.url);
        const customData = url.searchParams.get('custom_data'); // format: "uid:wallpaperId"
        
        if (customData) {
            const [uid, wallpaperId] = customData.split(':');
            if (uid && wallpaperId) {
                // Fetch user directly using Admin API to access app_metadata
                const { data: { user } } = await supabaseAdmin.auth.admin.getUserById(uid);
                
                if (user) {
                    const appMetadata = user.app_metadata || {};
                    const unlocked = appMetadata.ad_unlocked_wallpapers || [];
                    
                    // Add the wallpaper ID to unlocked list if not already present
                    if (!unlocked.includes(wallpaperId)) {
                        unlocked.push(wallpaperId);
                        await supabaseAdmin.auth.admin.updateUserById(uid, {
                            app_metadata: { ...appMetadata, ad_unlocked_wallpapers: unlocked }
                        });
                    }
                }
            }
        }
        
        // AdMob SSV requires a 200 OK response
        return new Response("OK", { status: 200, headers: corsHeaders });
    } catch (e: any) {
        console.error("SSV Error:", e.message);
        // Returning 200 even on error prevents AdMob from infinitely retrying bad requests
        return new Response("Error", { status: 200, headers: corsHeaders });
    }
});
