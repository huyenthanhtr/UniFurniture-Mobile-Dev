#!/usr/bin/env python3
"""
One-off migration: populate `product_translations` (en/fr/zh) from the original Vietnamese
product content using py-googletrans (free, unofficial Google Translate web API — no API key,
no cost). Replaces the old regex/word-by-word dictionary, which produced broken word order and
wrong terminology. Runtime is unchanged: the backend just overlays these stored translations.

Setup:
    pip install -r src/scripts/requirements-translate.txt
Run (needs the same DB as the server):
    MONGO_URI="mongodb+srv://.../ecommerce" python src/scripts/translate_products.py

Notes:
- py-googletrans is unofficial; if Google rate-limits your IP you may get HTTP 5xx — rerun later.
- Single-text limit is ~15k chars. It translates plain text, so heavy inline HTML in a
  description may be altered; product descriptions here are mostly spec text + line breaks.
"""

import asyncio
import os
from datetime import datetime, timezone

from dotenv import load_dotenv
from pymongo import MongoClient
from googletrans import Translator

load_dotenv()

MONGO_URI = os.environ.get("MONGO_URI")
MONGO_DB = os.environ.get("MONGO_DB")  # optional; falls back to the URI's db, then "ecommerce"
SOURCE = "vi"
# App language code -> googletrans dest code.
DEST = {"en": "en", "fr": "fr", "zh": "zh-cn"}


async def translate_fields(translator, texts, dest):
    """Translate a list of strings in one call, preserving empties and positions."""
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
        raise SystemExit("MONGO_URI is required (set it in .env or the environment).")

    client = MongoClient(MONGO_URI)
    db = None
    try:
        db = client.get_default_database()
    except Exception:
        db = None
    if db is None:
        db = client[MONGO_DB or "ecommerce"]

    products = list(db["products"].find({}))
    translations = db["product_translations"]
    print(f"Found {len(products)} products to translate (db={db.name}).")

    done = failed = 0
    async with Translator() as translator:
        for p in products:
            now = datetime.now(timezone.utc)
            name = p.get("name", "") or ""
            short_desc = p.get("short_description", "") or ""
            desc = p.get("description", "") or ""
            try:
                # vi = original content (no translation).
                translations.update_one(
                    {"product_id": p["_id"], "language_code": "vi"},
                    {"$set": {"name": name, "short_description": short_desc,
                              "description": desc, "updatedAt": now},
                     "$setOnInsert": {"createdAt": now}},
                    upsert=True,
                )
                for lang, dest in DEST.items():
                    tname, tshort, tdesc = await translate_fields(
                        translator, [name, short_desc, desc], dest)
                    translations.update_one(
                        {"product_id": p["_id"], "language_code": lang},
                        {"$set": {"name": tname, "short_description": tshort,
                                  "description": tdesc, "updatedAt": now},
                         "$setOnInsert": {"createdAt": now}},
                        upsert=True,
                    )
                done += 1
            except Exception as e:  # noqa: BLE001 - keep going on a single product failure
                failed += 1
                print(f"Failed product {p.get('_id')} ({name}): {e}")

            processed = done + failed
            if processed % 10 == 0 or processed == len(products):
                print(f"Processed {processed}/{len(products)} (ok={done}, failed={failed})")
            await asyncio.sleep(0.2)  # be gentle on the free endpoint

    print(f"Migration completed. ok={done}, failed={failed}")
    client.close()


if __name__ == "__main__":
    asyncio.run(main())
