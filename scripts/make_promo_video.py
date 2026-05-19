from __future__ import annotations

import math
import subprocess
from pathlib import Path

import imageio
import imageio_ffmpeg
import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont


ROOT = Path("/Users/alfa/Documents/vibeCode/SoundDrop")
ARTIFACTS = ROOT / "artifacts"
ICON_PATH = ARTIFACTS / "FallOuch-icon-source.png"
MOCKUP_PATH = Path(
    "/Users/alfa/.codex/generated_images/019e40a5-6202-7dc2-ac1c-f61ff9f08e53/"
    "ig_0907449f7bdf49b5016a0c85b5db3c819180a2c64864524945.png"
)
OUTPUT_MP4 = ARTIFACTS / "FallOuch-promo-15s.mp4"
OUTPUT_MOV = ARTIFACTS / "FallOuch-promo-15s-silent.mp4"
OUTPUT_AUDIO = ARTIFACTS / "FallOuch-promo-voice.aiff"

WIDTH = 1080
HEIGHT = 1920
FPS = 30
DURATION = 15
FRAME_COUNT = FPS * DURATION

FONT_REG = "/System/Library/Fonts/SFNS.ttf"
FONT_BOLD = "/System/Library/Fonts/Supplemental/Arial Bold.ttf"

BG_TOP = (6, 11, 30)
BG_BOTTOM = (17, 6, 40)
CYAN = (71, 230, 255)
PINK = (255, 80, 194)
WHITE = (245, 248, 255)
SOFT = (201, 216, 245)
PANEL = (17, 30, 60, 190)
LINE = (120, 165, 255, 92)

VOICEOVER = (
    "Fall Ouch! Это приложение, которое орёт, когда ты роняешь телефон. "
    "Встряска, падение и фоновый режим. Встроенные реакции или свои звуки. "
    "Fall Ouch! Самый драматичный телефон на Android."
)


def load_font(path: str, size: int) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(path, size=size)


TITLE_FONT = load_font(FONT_BOLD, 112)
H2_FONT = load_font(FONT_BOLD, 82)
H3_FONT = load_font(FONT_BOLD, 52)
BODY_FONT = load_font(FONT_REG, 42)
SMALL_FONT = load_font(FONT_REG, 32)
CHIP_FONT = load_font(FONT_BOLD, 30)


def lerp(a: float, b: float, t: float) -> float:
    return a + (b - a) * t


def clamp(value: float, min_value: float, max_value: float) -> float:
    return max(min_value, min(value, max_value))


def ease_out(t: float) -> float:
    t = clamp(t, 0.0, 1.0)
    return 1 - pow(1 - t, 3)


def ease_in_out(t: float) -> float:
    t = clamp(t, 0.0, 1.0)
    return 0.5 - 0.5 * math.cos(math.pi * t)


def alpha(value: float) -> int:
    return int(clamp(value, 0.0, 1.0) * 255)


def make_gradient() -> Image.Image:
    canvas = Image.new("RGBA", (WIDTH, HEIGHT))
    draw = ImageDraw.Draw(canvas)
    for y in range(HEIGHT):
        t = y / max(HEIGHT - 1, 1)
        color = tuple(int(lerp(BG_TOP[i], BG_BOTTOM[i], t)) for i in range(3)) + (255,)
        draw.line((0, y, WIDTH, y), fill=color)
    return canvas


def create_glow(size: tuple[int, int], color: tuple[int, int, int], blur: int) -> Image.Image:
    layer = Image.new("RGBA", size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(layer)
    draw.ellipse((0, 0, size[0], size[1]), fill=color + (220,))
    return layer.filter(ImageFilter.GaussianBlur(blur))


def fit_cover(image: Image.Image, size: tuple[int, int]) -> Image.Image:
    scale = max(size[0] / image.width, size[1] / image.height)
    resized = image.resize((int(image.width * scale), int(image.height * scale)), Image.Resampling.LANCZOS)
    left = (resized.width - size[0]) // 2
    top = (resized.height - size[1]) // 2
    return resized.crop((left, top, left + size[0], top + size[1]))


def fit_contain(image: Image.Image, size: tuple[int, int]) -> Image.Image:
    scale = min(size[0] / image.width, size[1] / image.height)
    return image.resize((int(image.width * scale), int(image.height * scale)), Image.Resampling.LANCZOS)


def draw_multiline(draw: ImageDraw.ImageDraw, text: str, position: tuple[int, int], font, fill, spacing: int = 8):
    draw.multiline_text(position, text, font=font, fill=fill, spacing=spacing)


def rounded_panel(canvas: Image.Image, box: tuple[int, int, int, int], fill_color=PANEL):
    layer = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(layer)
    draw.rounded_rectangle(box, radius=44, fill=fill_color, outline=LINE, width=2)
    soft = layer.filter(ImageFilter.GaussianBlur(2))
    canvas.alpha_composite(soft)


def draw_chip(draw: ImageDraw.ImageDraw, x: int, y: int, text: str, fill_color: tuple[int, int, int]):
    pad_x = 24
    pad_y = 14
    bbox = draw.textbbox((0, 0), text, font=CHIP_FONT)
    width = bbox[2] - bbox[0] + pad_x * 2
    height = bbox[3] - bbox[1] + pad_y * 2
    draw.rounded_rectangle((x, y, x + width, y + height), radius=26, fill=fill_color + (60,), outline=fill_color + (180,), width=2)
    draw.text((x + pad_x, y + pad_y - 2), text, font=CHIP_FONT, fill=WHITE)


def compose_background(base_icon: Image.Image) -> Image.Image:
    bg = make_gradient()
    icon_cover = fit_cover(base_icon, (WIDTH + 220, HEIGHT + 220)).filter(ImageFilter.GaussianBlur(30))
    icon_cover.putalpha(85)
    bg.alpha_composite(icon_cover, (-90, -80))
    bg.alpha_composite(create_glow((420, 420), CYAN, 90), (-80, 120))
    bg.alpha_composite(create_glow((480, 480), PINK, 110), (WIDTH - 360, 210))
    bg.alpha_composite(create_glow((560, 560), (255, 160, 50), 130), (260, HEIGHT - 520))
    return bg


def add_shadow(image: Image.Image, blur: int = 24, offset: tuple[int, int] = (0, 22), opacity: int = 150) -> Image.Image:
    shadow = Image.new("RGBA", (image.width + blur * 2, image.height + blur * 2), (0, 0, 0, 0))
    alpha_channel = image.split()[-1]
    base = Image.new("RGBA", image.size, (0, 0, 0, opacity))
    shadow.paste(base, (blur, blur), mask=alpha_channel)
    shadow = shadow.filter(ImageFilter.GaussianBlur(blur))
    result = Image.new("RGBA", shadow.size, (0, 0, 0, 0))
    result.alpha_composite(shadow, offset)
    result.alpha_composite(image, (blur, blur))
    return result


def render_frame(t: float, icon: Image.Image, mockup: Image.Image) -> Image.Image:
    frame = compose_background(icon)
    draw = ImageDraw.Draw(frame)

    if t < 4:
        p = ease_out(t / 4)
        scale = lerp(1.1, 0.92, p)
        hero = fit_contain(icon, (int(760 * scale), int(760 * scale)))
        hero = add_shadow(hero, blur=28, offset=(0, 24), opacity=140)
        frame.alpha_composite(hero, ((WIDTH - hero.width) // 2, 280))

        title_y = 1100
        draw.text((110, title_y), "Fall Ouch!", font=TITLE_FONT, fill=WHITE)
        draw.text((110, title_y + 130), "Телефон орет,\nкогда ты его роняешь", font=H3_FONT, fill=SOFT, spacing=10)
        draw_chip(draw, 110, 1410, "Android", CYAN)
        draw_chip(draw, 300, 1410, "15 сек рекламы", PINK)
        draw_chip(draw, 610, 1410, "Фон + крик", (255, 170, 60))

    elif t < 8:
        p = ease_in_out((t - 4) / 4)
        title_alpha = alpha(1.0)
        draw.text((84, 110), "Тряска. Падение. Фон.", font=H2_FONT, fill=WHITE[:3] + (title_alpha,))
        draw.text((88, 220), "Fall Ouch! реагирует сразу и продолжает\nработать даже после сворачивания.", font=BODY_FONT, fill=SOFT, spacing=10)

        phone = fit_contain(mockup, (820, 1500))
        extra_scale = lerp(1.02, 0.96, p)
        phone = phone.resize((int(phone.width * extra_scale), int(phone.height * extra_scale)), Image.Resampling.LANCZOS)
        phone = add_shadow(phone, blur=22, offset=(0, 20), opacity=135)
        x = (WIDTH - phone.width) // 2
        y = int(420 - 30 * math.sin(p * math.pi))
        frame.alpha_composite(phone, (x, y))

    elif t < 12:
        p = ease_in_out((t - 8) / 4)
        icon_small = fit_contain(icon, (420, 420))
        icon_small = add_shadow(icon_small, blur=18, offset=(0, 16), opacity=130)
        frame.alpha_composite(icon_small, (80, 190))

        rounded_panel(frame, (64, 1100, WIDTH - 64, 1660))
        draw = ImageDraw.Draw(frame)
        draw.text((108, 1140), "Что умеет приложение", font=H2_FONT, fill=WHITE)
        bullets = [
            "• Орёт при встряске и падении",
            "• Работает в фоне через сервис",
            "• Встроенные реакции или свои звуки",
            "• Один тап — и телефон уже драматизирует",
        ]
        for idx, bullet in enumerate(bullets):
            draw.text((112, 1265 + idx * 110), bullet, font=BODY_FONT, fill=SOFT)
        draw_chip(draw, 108, 1500, "Свои аудио", CYAN)
        draw_chip(draw, 320, 1500, "Русский TTS", PINK)
        draw_chip(draw, 574, 1500, "Full chaos", (255, 170, 60))

    else:
        p = ease_in_out((t - 12) / 3)
        hero = fit_contain(icon, (540, 540))
        hero = hero.resize((int(hero.width * lerp(0.9, 1.0, p)), int(hero.height * lerp(0.9, 1.0, p))), Image.Resampling.LANCZOS)
        hero = add_shadow(hero, blur=24, offset=(0, 22), opacity=150)
        frame.alpha_composite(hero, ((WIDTH - hero.width) // 2, 220))
        draw.text((150, 930), "Fall Ouch!", font=TITLE_FONT, fill=WHITE)
        draw.text((152, 1070), "Самый драматичный телефон\nна Android", font=H3_FONT, fill=SOFT, spacing=10)
        rounded_panel(frame, (124, 1350, WIDTH - 124, 1560), fill_color=(22, 38, 78, 210))
        draw = ImageDraw.Draw(frame)
        draw.text((178, 1410), "Тряска • Падение • Фон • Свои звуки", font=BODY_FONT, fill=WHITE)
        draw.text((230, 1660), "Попробуй 15-секундный хаос.", font=BODY_FONT, fill=CYAN)

    return frame.convert("RGB")


def make_voiceover():
    subprocess.run(
        [
            "say",
            "-v",
            "Milena",
            "-r",
            "182",
            "-o",
            str(OUTPUT_AUDIO),
            VOICEOVER,
        ],
        check=True,
    )


def main():
    ARTIFACTS.mkdir(parents=True, exist_ok=True)
    icon = Image.open(ICON_PATH).convert("RGBA")
    mockup = Image.open(MOCKUP_PATH).convert("RGBA")

    with imageio.get_writer(OUTPUT_MOV, fps=FPS, codec="libx264", quality=8, macro_block_size=None) as writer:
        for frame_idx in range(FRAME_COUNT):
            t = frame_idx / FPS
            frame = render_frame(t, icon, mockup)
            writer.append_data(np.array(frame))

    try:
        make_voiceover()
        ffmpeg = imageio_ffmpeg.get_ffmpeg_exe()
        subprocess.run(
            [
                ffmpeg,
                "-y",
                "-i",
                str(OUTPUT_MOV),
                "-i",
                str(OUTPUT_AUDIO),
                "-c:v",
                "copy",
                "-c:a",
                "aac",
                "-b:a",
                "192k",
                "-shortest",
                str(OUTPUT_MP4),
            ],
            check=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
    except Exception:
        OUTPUT_MOV.replace(OUTPUT_MP4)


if __name__ == "__main__":
    main()
