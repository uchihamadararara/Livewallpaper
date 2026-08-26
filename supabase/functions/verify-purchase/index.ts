import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { supabaseAdmin, corsHeaders } from "../shared/supabase.ts";
import { google } from "npm:googleapis";

serve(async (req) => {
    if (req.method === 'OPTIONS') return new Response('ok', { headers: corsHeaders });
    try {
        const authHeader = req.headers.get('Authorization');
        if (!authHeader) throw new Error("Missing auth");
        const token = authHeader.replace('Bearer ', '');
        
        const { data: { user } } = await supabaseAdmin.auth.getUser(token);
        if (!user) throw new Error("Unauthorized");

        const { purchaseToken, productId } = await req.json();
        const packageName = Deno.env.get("GOOGLE_PLAY_PACKAGE_NAME");

        const auth = new google.auth.GoogleAuth({
            credentials: JSON.parse(Deno.env.get('GOOGLE_PLAY_SERVICE_ACCOUNT') || '{}'),
            scopes: ['https://www.googleapis.com/auth/androidpublisher']
        });
        const play = google.androidpublisher({ version: 'v3', auth });

        const res = await play.purchases.subscriptions.get({
            packageName,
            subscriptionId: productId,
            token: purchaseToken
        });

        const expiry = parseInt(res.data.expiryTimeMillis || "0", 10);
        const isActive = expiry > Date.now();

        await supabaseAdmin.from('users').update({
            subscription_status: isActive ? "ACTIVE" : "EXPIRED",
            subscription_id: productId,
            subscription_expiry: expiry
        }).eq('id', user.id);

        return new Response(JSON.stringify({ success: true, isActive }), { headers: { ...corsHeaders, 'Content-Type': 'application/json' } });
    } catch (e) {
        return new Response(JSON.stringify({ error: e.message }), { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } });
    }
});
