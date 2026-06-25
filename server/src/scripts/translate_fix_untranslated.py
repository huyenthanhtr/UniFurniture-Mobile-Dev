#!/usr/bin/env python3
"""
Fix the 8 product translation rows where the translated name is identical
to the Vietnamese original (translation failed silently during the first run).
Re-translates all 3 fields (name, short_description, description) for each
affected (product_id, language_code) pair.
"""

import asyncio
import os
from datetime import datetime, timezone

from dotenv import load_dotenv
from pymongo import MongoClient
from googletrans import Translator

load_dotenv()

MONGO_URI = os.environ.get("MONGO_URI")
MONGO_DB = os.environ.get("MONGO_DB")
SOURCE = "vi"
DEST_MAP = {"en": "en", "fr": "fr", "zh": "zh-cn"}


async def translate_fields(translator, texts, dest):
    idx = [i for i, t in enumerate(texts) if t and t.strip()]
    out = ["" for _ in texts]
    if not idx:
        return out
    payload = [texts[i] for i in idx]
    res = await translator.translate(payload, src=SOURCE, dest=dest)
    if not isinstance(res, list):
        res = [res]
    for j, i in enumerate(idx):
        out[i] = res[j].text
    return out


async def main():
    if not MONGO_URI:
        raise SystemExit("MONGO_URI is required.")

    client = MongoClient(MONGO_URI)
    try:
        db = client.get_default_database()
    except Exception:
        db = None
    if db is None:
        db = client[MONGO_DB or "ecommerce"]

    products = {str(p["_id"]): p for p in db["products"].find({})}
    translations_col = db["product_translations"]
    all_trans = list(translations_col.find({"language_code": {"$in": ["en", "fr", "zh"]}}))

    # Find rows where translated name == original Vietnamese name.
    to_fix = []
    for t in all_trans:
        pid = str(t["product_id"])
        lang = t["language_code"]
        orig = products.get(pid)
        if not orig:
            continue
        orig_name = (orig.get("name") or "").strip()
        trans_name = (t.get("name") or "").strip()
        if trans_name and orig_name and trans_name == orig_name:
            to_fix.append((t["product_id"], lang, orig))

    if not to_fix:
        print("No untranslated rows found — everything looks good!")
        client.close()
        return

    print(f"Found {len(to_fix)} rows to re-translate:\n")
    for pid, lang, orig in to_fix:
        print(f"  {pid} [{lang}] — {(orig.get('name') or '')[:50]}")
    print()

    done = failed = 0
    async with Translator() as translator:
        for pid, lang, orig in to_fix:
            now = datetime.now(timezone.utc)
            name = orig.get("name", "") or ""
            short_desc = orig.get("short_description", "") or ""
            desc = orig.get("description", "") or ""
            dest = DEST_MAP[lang]
            try:
                tname, tshort, tdesc = await translate_fields(
                    translator, [name, short_desc, desc], dest)
                # Verify translation actually changed the name.
                if tname.strip() == name.strip():
                    print(f"  ⚠ {pid} [{lang}] — translation returned same text, skipping write")
                    failed += 1
                    continue
                translations_col.update_one(
                    {"product_id": pid, "language_code": lang},
                    {"$set": {"name": tname, "short_description": tshort,
                              "description": tdesc, "updatedAt": now}},
                )
                done += 1
                print(f"  ✓ {pid} [{lang}] — \"{name[:30]}\" → \"{tname[:30]}\"")
            except Exception as e:
                failed += 1
                print(f"  ✗ {pid} [{lang}] — failed: {e}")
            await asyncio.sleep(2.0)  # generous sleep to avoid throttle

    print(f"\nDone. Fixed={done}, Failed={failed}")
    client.close()


if __name__ == "__main__":
    asyncio.run(main())
