#!/usr/bin/env python3
"""
Retry-only migration: finds products whose translations are incomplete (missing
en/fr/zh rows in product_translations) and translates ONLY those. This avoids
re-translating the 191 products that already succeeded and minimises Google
Translate API calls so we stay under the free-tier throttle.

Usage:
    MONGO_URI="mongodb+srv://..." python src/scripts/translate_retry_failed.py
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
DEST = {"en": "en", "fr": "fr", "zh": "zh-cn"}
EXPECTED_LANGS = set(DEST.keys())  # {"en", "fr", "zh"}


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
    try:
        db = client.get_default_database()
    except Exception:
        db = None
    if db is None:
        db = client[MONGO_DB or "ecommerce"]

    products = list(db["products"].find({}))
    translations_col = db["product_translations"]

    # ── Find products missing any of en/fr/zh translations ──
    # Build a set of (product_id, lang) pairs that already exist.
    existing = translations_col.find(
        {"language_code": {"$in": list(EXPECTED_LANGS)}},
        {"product_id": 1, "language_code": 1},
    )
    done_pairs = set()
    for doc in existing:
        done_pairs.add((str(doc["product_id"]), doc["language_code"]))

    to_retry = []
    for p in products:
        pid = str(p["_id"])
        missing = EXPECTED_LANGS - {lang for lang in EXPECTED_LANGS if (pid, lang) in done_pairs}
        if missing:
            to_retry.append((p, missing))

    if not to_retry:
        print("All products already have en/fr/zh translations — nothing to do.")
        client.close()
        return

    print(f"Found {len(to_retry)} products with incomplete translations. Retrying…")
    for p, missing in to_retry:
        print(f"  {p['_id']}  ({p.get('name','')[:40]})  missing: {sorted(missing)}")

    done = failed = 0
    async with Translator() as translator:
        for p, missing_langs in to_retry:
            now = datetime.now(timezone.utc)
            name = p.get("name", "") or ""
            short_desc = p.get("short_description", "") or ""
            desc = p.get("description", "") or ""
            try:
                # Ensure vi row exists too.
                translations_col.update_one(
                    {"product_id": p["_id"], "language_code": "vi"},
                    {"$set": {"name": name, "short_description": short_desc,
                              "description": desc, "updatedAt": now},
                     "$setOnInsert": {"createdAt": now}},
                    upsert=True,
                )
                for lang in sorted(missing_langs):
                    dest = DEST[lang]
                    tname, tshort, tdesc = await translate_fields(
                        translator, [name, short_desc, desc], dest)
                    translations_col.update_one(
                        {"product_id": p["_id"], "language_code": lang},
                        {"$set": {"name": tname, "short_description": tshort,
                                  "description": tdesc, "updatedAt": now},
                         "$setOnInsert": {"createdAt": now}},
                        upsert=True,
                    )
                    # Slightly longer sleep between calls to avoid throttle.
                    await asyncio.sleep(1.0)
                done += 1
            except Exception as e:
                failed += 1
                print(f"  ✗ Failed product {p.get('_id')} ({name}): {e}")

            print(f"  Processed {done + failed}/{len(to_retry)} (ok={done}, failed={failed})")
            await asyncio.sleep(0.5)

    print(f"\nRetry completed. ok={done}, failed={failed}")
    client.close()


if __name__ == "__main__":
    asyncio.run(main())
