#!/bin/bash
set -e

echo "Cleaning Firebase dependencies from build.gradle.kts..."
sed -i '/firebase/d' app/build.gradle.kts
sed -i '/google.services/d' app/build.gradle.kts
sed -i '/google-services/d' app/build.gradle.kts
sed -i '/crashlytics/d' app/build.gradle.kts
sed -i '/googleid/d' app/build.gradle.kts

echo "Updating SupabaseApiService..."
cat << 'INNER_EOF' > app/src/main/java/com/example/data/network/SupabaseApiService.kt
package com.example.data.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Query

interface SupabaseApiService {
    @POST("auth/v1/signup")
    suspend fun signUpAnonymously(
        @Header("apikey") apiKey: String,
        @Body request: Map<String, String>
    ): Response<Map<String, Any>>

    @POST("auth/v1/token?grant_type=password")
    suspend fun signIn(
        @Header("apikey") apiKey: String,
        @Body request: Map<String, String>
    ): Response<Map<String, Any>>

    @GET("rest/v1/wallpapers")
    suspend fun getWallpapers(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Query("select") select: String = "*",
        @Query("is_active") isActive: String = "eq.true",
        @Query("order") order: String = "created_at.desc",
        @Query("limit") limit: Int = 100
    ): Response<List<Map<String, Any>>>

    @GET("rest/v1/users")
    suspend fun getUser(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Query("id") eqId: String
    ): Response<List<Map<String, Any>>>

    @POST("rest/v1/users")
    suspend fun createUser(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body user: Map<String, Any>
    ): Response<List<Map<String, Any>>>

    @POST("functions/v1/apply-wallpaper")
    suspend fun applyWallpaper(
        @Header("Authorization") auth: String,
        @Body request: Map<String, String>
    ): Response<Map<String, Any>>

    @POST("functions/v1/verify-purchase")
    suspend fun verifyPurchase(
        @Header("Authorization") auth: String,
        @Body request: Map<String, String?>
    ): Response<Map<String, Any>>

    @POST("functions/v1/get-premium-url")
    suspend fun getPremiumMediaUrl(
        @Header("Authorization") auth: String,
        @Body request: Map<String, String>
    ): Response<Map<String, Any>>
}
INNER_EOF

echo "Creating Supabase Deno Functions..."
mkdir -p supabase/functions/shared

cat << 'INNER_EOF' > supabase/functions/shared/cors.ts
export const corsHeaders = {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
};
INNER_EOF

cat << 'INNER_EOF' > supabase/functions/shared/supabase.ts
import { createClient } from 'npm:@supabase/supabase-js@2';
import { corsHeaders } from './cors.ts';

const supabaseUrl = Deno.env.get('SUPABASE_URL') || '';
const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') || '';

export const supabaseAdmin = createClient(supabaseUrl, supabaseServiceKey, {
  auth: { autoRefreshToken: false, persistSession: false }
});

export { corsHeaders };
INNER_EOF

cat << 'INNER_EOF' > supabase/functions/apply-wallpaper/index.ts
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

        if (isPremium && subStatus !== 'ACTIVE' && retainedId !== wallpaperId) {
            throw new Error("Premium subscription required");
        }

        const updates: any = { current_applied_wallpaper_id: wallpaperId };
        if (subStatus === 'EXPIRED' && (!isPremium || retainedId !== wallpaperId)) {
            updates.retained_wallpaper_id = null;
        }

        await supabaseAdmin.from('users').update(updates).eq('id', uid);

        return new Response(JSON.stringify({ success: true }), { headers: { ...corsHeaders, 'Content-Type': 'application/json' } });
    } catch (e) {
        return new Response(JSON.stringify({ error: e.message }), { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } });
    }
});
INNER_EOF

cat << 'INNER_EOF' > supabase/functions/verify-purchase/index.ts
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
INNER_EOF

cat << 'INNER_EOF' > supabase/functions/get-premium-url/index.ts
import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { supabaseAdmin, corsHeaders } from "../shared/supabase.ts";
import { S3Client, GetObjectCommand } from "npm:@aws-sdk/client-s3";
import { getSignedUrl } from "npm:@aws-sdk/s3-request-presigner";

serve(async (req) => {
    if (req.method === 'OPTIONS') return new Response('ok', { headers: corsHeaders });
    try {
        const authHeader = req.headers.get('Authorization');
        if (!authHeader) throw new Error("Missing auth");
        
        const { data: { user } } = await supabaseAdmin.auth.getUser(authHeader.replace('Bearer ', ''));
        if (!user) throw new Error("Unauthorized");

        const { wallpaperId } = await req.json();

        const { data: userProfile } = await supabaseAdmin.from('users').select('*').eq('id', user.id).single();
        const subStatus = userProfile?.subscription_status || 'NONE';
        const retainedId = userProfile?.retained_wallpaper_id;

        if (subStatus !== 'ACTIVE' && retainedId !== wallpaperId) {
            throw new Error("Entitlement verification failed");
        }

        const { data: wallpaper } = await supabaseAdmin.from('wallpapers').select('video_url').eq('id', wallpaperId).single();
        if (!wallpaper || !wallpaper.video_url) throw new Error("Media not found");

        const s3 = new S3Client({
            region: "auto",
            endpoint: `https://${Deno.env.get("R2_ACCOUNT_ID")}.r2.cloudflarestorage.com`,
            credentials: {
                accessKeyId: Deno.env.get("R2_ACCESS_KEY_ID") || "",
                secretAccessKey: Deno.env.get("R2_SECRET_ACCESS_KEY") || "",
            },
        });

        const command = new GetObjectCommand({
            Bucket: Deno.env.get("R2_BUCKET_NAME"),
            Key: wallpaper.video_url
        });
        
        const url = await getSignedUrl(s3, command, { expiresIn: 900 });

        return new Response(JSON.stringify({ url }), { headers: { ...corsHeaders, 'Content-Type': 'application/json' } });
    } catch (e) {
        return new Response(JSON.stringify({ error: e.message }), { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } });
    }
});
INNER_EOF

cat << 'INNER_EOF' > supabase/functions/play-rtdn/index.ts
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
INNER_EOF

echo "Done writing files."
