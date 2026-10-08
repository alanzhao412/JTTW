"""Lay the form gallery's pictures out as labelled contact sheets.

Run after `./gradlew runFormGallery`:

    python tools/form_sheets.py [gallery_dir] [out_dir]

Defaults: the newest run-gallery/screenshots/gallery-<time> -> run-gallery/sheets/gallery-<time>. Each row is one form with its
front, side and looking-up pictures; each sheet holds ROWS forms. A form whose pictures were
taken before its form or camera arrived is marked in red (see index.txt).

What to look for: in "front" the model's face is toward you; in "side" its head points left;
in "up" it tilts its head or nose up.
"""
import os
import sys

from PIL import Image, ImageDraw

VIEWS = ["front", "side", "up"]
ROWS = 8
CELL_W = 320
LABEL_W = 230


def renders_nothing(path):
    """True if nothing but sky and grass is drawn where the form stands (the frame's centre)."""
    im = Image.open(path).convert("RGB")
    w, h = im.size
    box = im.crop((int(w * 0.35), int(h * 0.3), int(w * 0.65), int(h * 0.75)))
    other = 0
    for r, g, b in box.getdata():
        sky = b > 150 and b >= g >= r - 10
        grass = g >= r and g > b and g < 160
        if not (sky or grass):
            other += 1
    return other < 25


def main():
    shots = os.path.join("run-gallery", "screenshots")
    if len(sys.argv) > 1:
        src = sys.argv[1]
    else:  # the newest run
        runs = sorted(d for d in os.listdir(shots) if d.startswith("gallery-"))
        src = os.path.join(shots, runs[-1])
    out = sys.argv[2] if len(sys.argv) > 2 else os.path.join("run-gallery", "sheets", os.path.basename(src))
    os.makedirs(out, exist_ok=True)

    index = {}
    with open(os.path.join(src, "index.txt"), encoding="utf-8") as f:
        for line in f:
            parts = line.rstrip("\n").split("\t")
            if len(parts) >= 5:
                index[parts[0]] = parts

    forms = []
    for name in sorted(index):
        key = name.rsplit("__", 1)[0]
        if key not in forms:
            forms.append(key)

    sample = Image.open(os.path.join(src, next(iter(index))))
    cell_h = int(CELL_W * sample.height * 0.8 / (sample.width * 0.6))
    nothing = []

    for sheet_no, start in enumerate(range(0, len(forms), ROWS), 1):
        rows = forms[start:start + ROWS]
        sheet = Image.new("RGB", (LABEL_W + CELL_W * len(VIEWS), cell_h * len(rows)), "white")
        draw = ImageDraw.Draw(sheet)
        for r, key in enumerate(rows):
            y = r * cell_h
            problems = []
            blank = []
            for c, view in enumerate(VIEWS):
                name = f"{key}__{view}.png"
                path = os.path.join(src, name)
                if os.path.exists(path):
                    im = Image.open(path).convert("RGB")
                    w, h = im.size
                    im = im.crop((int(w * 0.2), int(h * 0.1), int(w * 0.8), int(h * 0.9)))
                    sheet.paste(im.resize((CELL_W, cell_h)), (LABEL_W + c * CELL_W, y))
                    if renders_nothing(path):
                        blank.append(view)
                entry = index.get(name)
                if entry and ("clientForm=none" in entry[3] or "camera=false" in entry[4]):
                    problems.append(view)
            form = key.replace("__", " / ")
            draw.text((6, y + 6), form, fill="black")
            draw.text((6, y + 22), "front | side | up", fill="gray")
            if problems:
                draw.text((6, y + 38), "harness miss: " + ",".join(problems), fill="red")
            if blank:
                draw.text((6, y + 54), "draws nothing: " + ",".join(blank), fill="red")
                nothing.append(key.replace("__", ":") + " (" + ",".join(blank) + ")")
            draw.line([(0, y), (sheet.width, y)], fill="lightgray")
        path = os.path.join(out, f"sheet_{sheet_no:02d}.png")
        sheet.save(path)
        print(path)
    with open(os.path.join(out, "draws_nothing.txt"), "w", encoding="utf-8") as f:
        f.write(chr(10).join(nothing) + chr(10))
    print(f"{len(nothing)} forms draw nothing in some view: see draws_nothing.txt")


if __name__ == "__main__":
    main()
