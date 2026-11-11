#!/usr/bin/env python3
"""Reproduce legacy PNG fallbacks from the current three-chevron launcher mark.
Requires Pillow for local asset generation only, not an Android runtime dependency.
"""
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
LAUNCHER = [(35, 33), (54, 45), (73, 33), (35, 50), (54, 62), (73, 50), (35, 67), (54, 79), (73, 67)]
STATUS = [(4.83, 3.32), (12, 7.85), (19.17, 3.32), (4.83, 9.74), (12, 14.26), (19.17, 9.74), (4.83, 16.15), (12, 20.68), (19.17, 16.15)]

def draw_mark(points, viewport, stroke, size):
    scale = size * 8 / viewport
    image = Image.new("RGBA", (size * 8, size * 8), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    radius = stroke * scale / 2
    for i in range(0, len(points), 3):
        triangle = [(x * scale, y * scale) for x, y in points[i:i+3]]
        draw.line(triangle, fill="white", width=round(stroke * scale), joint="curve")
        for x, y in triangle:
            draw.ellipse((x-radius, y-radius, x+radius, y+radius), fill="white")
    return image.resize((size, size), Image.Resampling.LANCZOS)

def generate():
    for density, size in [("mdpi", 24), ("hdpi", 36), ("xhdpi", 48), ("xxhdpi", 72)]:
        draw_mark(STATUS, 24, 2.64, size).save(ROOT / f"app/src/main/res/drawable-{density}/ic_stat_seal.png")
    for density, size in [("mdpi", 108), ("hdpi", 162), ("xhdpi", 216), ("xxhdpi", 324), ("xxxhdpi", 432)]:
        draw_mark(LAUNCHER, 108, 7, size).save(ROOT / f"app/src/main/res/mipmap-{density}/ic_launcher_foreground.png")
    Image.open(ROOT / "fastlane/metadata/android/en-US/images/icon.png").convert("RGBA").resize((144, 144), Image.Resampling.LANCZOS).save(ROOT / ".idea/icon.png")

if __name__ == "__main__":
    generate()
