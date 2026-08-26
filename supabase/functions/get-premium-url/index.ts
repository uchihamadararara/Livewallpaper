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

        // No longer checking for subscription status here. Previews are free.
        
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
        
        // 15-minute signed URL for preview and apply
        const url = await getSignedUrl(s3, command, { expiresIn: 900 });

        return new Response(JSON.stringify({ url }), { headers: { ...corsHeaders, 'Content-Type': 'application/json' } });
    } catch (e: any) {
        return new Response(JSON.stringify({ error: e.message }), { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } });
    }
});
