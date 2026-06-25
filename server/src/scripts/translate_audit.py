#!/usr/bin/env python3
"""
Audit: check product_translations for rows that exist but have empty or
suspiciously un-translated content (e.g. the translated name is identical
to the original Vietnamese name for en/fr/zh rows).
"""

import os
from dotenv import load_dotenv
from pymongo import MongoClient

load_dotenv()

MONGO_URI = os.environ.get("MONGO_URI")
MONGO_DB = os.environ.get("MONGO_DB")

client = MongoClient(MONGO_URI)
try:
    db = client.get_default_database()
except Exception:
    db = None
if db is None:
    db = client[MONGO_DB or "ecommerce"]

products = {str(p["_id"]): p for p in db["products"].find({})}
translations = list(db["product_translations"].find({"language_code": {"$in": ["en", "fr", "zh"]}}))

print(f"Products: {len(products)}, Translation rows (en/fr/zh): {len(translations)}")
print()

# Check for issues.
issues = []
for t in translations:
    pid = str(t["product_id"])
    lang = t["language_code"]
    orig = products.get(pid)
    if not orig:
        issues.append(f"  Orphan translation: pid={pid} lang={lang} (product not found)")
        continue

    orig_name = (orig.get("name") or "").strip()
    trans_name = (t.get("name") or "").strip()

    # Issue 1: translated name is empty.
    if not trans_name and orig_name:
        issues.append(f"  EMPTY name: pid={pid} lang={lang} orig_name={orig_name[:50]}")

    # Issue 2: translated name is identical to Vietnamese original (not translated).
    if trans_name and orig_name and trans_name == orig_name and lang != "vi":
        issues.append(f"  UNTRANSLATED name: pid={pid} lang={lang} name={trans_name[:50]}")

if issues:
    print(f"Found {len(issues)} potential issues:")
    for i in issues:
        print(i)
else:
    print("✓ All en/fr/zh translation rows look good (non-empty and different from Vietnamese).")

# Summary: count per language.
from collections import Counter
lang_counts = Counter(t["language_code"] for t in translations)
print(f"\nTranslation row counts: {dict(lang_counts)}")
print(f"Expected per language: {len(products)}")

client.close()
