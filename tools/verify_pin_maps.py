#!/usr/bin/env python3
"""Compare bundled processor pin maps directly with the supplied NXP DOCX.

The source document is read in place and is never copied into the repository.
Only Python's standard library is required.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
import zipfile
from pathlib import Path
from xml.etree import ElementTree


WORD_NAMESPACE = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
PROCESSOR_HEADING = re.compile(r"^(Processor[ABC])\s+[\u2013-]\s+\d+\s+pins$")
PIN_DEFINITION = re.compile(r"^([A-Z]{3}\d+)\s*->\s*(.+)$")
FILES = {
    "ProcessorA": "processor-a.json",
    "ProcessorB": "processor-b.json",
    "ProcessorC": "processor-c.json",
}


def read_paragraphs(docx: Path) -> list[str]:
    with zipfile.ZipFile(docx) as archive:
        document_xml = archive.read("word/document.xml")
    root = ElementTree.fromstring(document_xml)
    paragraphs: list[str] = []
    for paragraph in root.findall(f".//{{{WORD_NAMESPACE}}}p"):
        text = "".join(
            node.text or ""
            for node in paragraph.findall(f".//{{{WORD_NAMESPACE}}}t")
        ).strip()
        if text:
            paragraphs.append(text)
    return paragraphs


def extract_pin_maps(docx: Path) -> dict[str, list[dict[str, object]]]:
    extracted: dict[str, list[dict[str, object]]] = {key: [] for key in FILES}
    current: str | None = None
    for text in read_paragraphs(docx):
        heading = PROCESSOR_HEADING.fullmatch(text)
        if heading:
            current = heading.group(1)
            continue
        pin = PIN_DEFINITION.fullmatch(text.replace("\u00a0", " "))
        if current and pin:
            functions = [value.strip() for value in pin.group(2).split(",")]
            extracted[current].append(
                {
                    "name": pin.group(1),
                    "physicalNumber": len(extracted[current]) + 1,
                    "availableFunctions": functions,
                }
            )
    return extracted


def canonical_digest(pins: list[dict[str, object]]) -> str:
    canonical = "\n".join(
        f"{pin['name']}|{pin['physicalNumber']}|{','.join(pin['availableFunctions'])}"
        for pin in pins
    )
    return hashlib.sha256(canonical.encode("utf-8")).hexdigest()


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Verify all bundled pin mappings against Mini Configuration Tool 2.docx"
    )
    parser.add_argument("--docx", required=True, type=Path, help="Path to the source DOCX")
    parser.add_argument(
        "--project-root",
        type=Path,
        default=Path(__file__).resolve().parents[1],
        help="Project root containing src/main/resources/processors",
    )
    arguments = parser.parse_args()

    expected = extract_pin_maps(arguments.docx)
    mismatches: list[str] = []
    total = 0
    for processor_id, filename in FILES.items():
        path = (
            arguments.project_root
            / "src"
            / "main"
            / "resources"
            / "processors"
            / filename
        )
        actual = json.loads(path.read_text(encoding="utf-8"))["pins"]
        source_pins = expected[processor_id]
        total += len(source_pins)
        if actual != source_pins:
            maximum = max(len(actual), len(source_pins))
            for index in range(maximum):
                source_pin = source_pins[index] if index < len(source_pins) else None
                actual_pin = actual[index] if index < len(actual) else None
                if source_pin != actual_pin:
                    mismatches.append(
                        f"{processor_id} item {index + 1}: DOCX={source_pin!r} JSON={actual_pin!r}"
                    )
        print(
            f"{processor_id}: DOCX={len(source_pins)} JSON={len(actual)} "
            f"SHA-256={canonical_digest(source_pins)}"
        )

    if mismatches:
        print(f"FAILED: {len(mismatches)} mismatch(es)", file=sys.stderr)
        for mismatch in mismatches:
            print(mismatch, file=sys.stderr)
        return 1
    print(f"PASS: {total} pin definitions match the source DOCX exactly")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
