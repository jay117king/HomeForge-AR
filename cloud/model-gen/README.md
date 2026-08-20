# Image-to-3D Model Generation (Free Path)

This folder contains the free/near-zero-cost pipeline that turns product photos into to-scale GLB models.

## Recommended Stack

- **Model**: TripoSR or InstantMesh (both Apache / MIT friendly)
- **Host**: Hugging Face Spaces (ZeroGPU or free CPU tier)
- **Wrapper**: Simple FastAPI endpoint

## Quick Start (Hugging Face Space)

1. Create a new Space (Gradio or Docker)
2. Install:
   ```
   pip install torch torchvision triposr fastapi uvicorn pillow
   ```
3. Expose an endpoint that accepts 1–4 images + real-world dimensions (L×W×H in cm)
4. Return a downloadable `.glb` scaled to those dimensions

## App Integration

The Android app will:
- Check if a GLB already exists for the product ID (cached in Room / Supabase Storage)
- If missing, POST the product images + dimensions to this Space
- On success → download GLB → cache locally → load in Filament
- On timeout / failure → show photo billboard fallback (always available)

## Pre-generation (Recommended for Seed Catalog)

For the initial 50–200 popular products, run generation offline on Google Colab free GPU once, then upload the resulting GLBs to Supabase Storage / Cloudflare R2. This gives instant placement for the most common items.

## Scaling Note

Free HF Spaces have cold starts and rate limits. Always treat the 3D generation as progressive enhancement on top of the photo billboard.
