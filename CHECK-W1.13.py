#!/usr/bin/env python3
from pathlib import Path
import argparse

parser = argparse.ArgumentParser()
parser.add_argument("--project-root", default=".")
args = parser.parse_args()

project = Path(args.project_root).resolve()

files = [
    project / "src/main/java/com/k1ngtle/vsia/signality/engineering/wifi/ip/raw/RawIcmpQuote.java",
    project / "src/main/java/com/k1ngtle/vsia/signality/engineering/wifi/ip/raw/RawIcmpErrorPolicy.java",
    project / "src/main/java/com/k1ngtle/vsia/signality/engineering/wifi/ip/raw/RawIcmpSemanticsTestResult.java",
    project / "src/main/java/com/k1ngtle/vsia/signality/engineering/wifi/ip/raw/RawIcmpSemanticsTestSuite.java",
    project / "src/main/java/com/k1ngtle/vsia/signality/debug/WifiRawIcmpSemanticsTestCommand.java",
]

checks = []

for path in files:
    checks.append(
        (path.name, path.exists())
    )

codec = (
    project
    / "src/main/java/com/k1ngtle/vsia/signality/engineering/wifi/ip/router/live/IcmpRawLiveCarrierCodec.java"
)

ndbe = (
    project
    / "src/main/java/com/k1ngtle/vsia/signality/internet/NetworkDeviceBlockEntity.java"
)

checks.append(
    (
        "ICMP codec exact quote",
        codec.exists()
        and "W1.13 exact raw quote support" in codec.read_text(encoding="utf-8")
    )
)

if ndbe.exists():
    text = ndbe.read_text(encoding="utf-8")
    checks.extend(
        [
            ("NDBE RawIcmpQuote import", "RawIcmpQuote" in text),
            ("NDBE RawIcmpErrorPolicy import", "RawIcmpErrorPolicy" in text),
            ("raw original retained", "icmp_original_raw" in text),
            ("raw quote retained", "\"icmp_quote\"" in text),
            ("live ICMP suppression", "ICMP ERROR SUPPRESSED" in text),
        ]
    )
else:
    checks.append(("NetworkDeviceBlockEntity.java", False))

passed = 0

for label, ok in checks:
    if ok:
        passed += 1
        print(f"PASS {label}")
    else:
        print(f"FAIL {label}")

failed = len(checks) - passed

print(
    f"Result: {passed} passed, {failed} failed"
)

raise SystemExit(
    0 if failed == 0 else 1
)
