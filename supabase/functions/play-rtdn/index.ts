import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { supabaseAdmin } from "../shared/supabase.ts";
import { google } from "npm:googleapis";

serve(async (req) => {
    try {
        const body = await req.json();
        if (!body?.message?.data) return new Response("Missing data", { status: 400 });

        const decoded = JSON.parse(atob(body.message.data));
        const subNotif = decoded.subscriptionNotification;
        if (!subNotif) return new Response("Not a subscription notification", { status: 200 });

        const { purchaseToken, subscriptionId } = subNotif;
        const packageName = Deno.env.get("GOOGLE_PLAY_PACKAGE_NAME");

        const auth = new google.auth.GoogleAuth({
            credentials: JSON.parse(Deno.env.get('GOOGLE_PLAY_SERVICE_ACCOUNT') || '{}'),
            scopes: ['https://www.googleapis.com/auth/androidpublisher']
        });
        const play = google.androidpublisher({ version: 'v3', auth });

        const res = await play.purchases.subscriptions.get({
            packageName,
            subscriptionId,
            token: purchaseToken
        });

        const expiry = parseInt(res.data.expiryTimeMillis || "0", 10);
        const isActive = expiry > Date.now();

        // Update any user in PostgreSQL holding this active purchase token
        const { data: users } = await supabaseAdmin.from('users').select('id').eq('subscription_id', subscriptionId);
        
        if (users && users.length > 0) {
            for (const u of users) {
                await supabaseAdmin.from('users').update({
                    subscription_status: isActive ? "ACTIVE" : "EXPIRED",
                    subscription_expiry: expiry
                }).eq('id', u.id);
            }
        }

        return new Response("OK", { status: 200 });
    } catch (e) {
        console.error(e);
        return new Response("Error", { status: 500 });
    }
});
