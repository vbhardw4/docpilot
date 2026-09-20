#!/usr/bin/env python3
"""DocPilot eval harness — asks sample questions, reports latency and escalation behavior.

Usage:  python3 eval.py [base_url]        (default http://localhost:8080)

Reads eval-questions.jsonl (one {"question": ..., "expect_escalation": bool} per line),
POSTs each to /api/v1/chat, and prints a summary table.

All numbers are ILLUSTRATIVE of this demo setup — they depend on the model, network,
docs, and thresholds, and are not client benchmarks.
"""

import json
import sys
import time
import urllib.request

BASE = sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8080"


def ask(question):
    payload = json.dumps({"question": question}).encode()
    req = urllib.request.Request(
        BASE + "/api/v1/chat", data=payload, headers={"Content-Type": "application/json"})
    started = time.monotonic()
    with urllib.request.urlopen(req, timeout=60) as resp:
        body = json.loads(resp.read().decode())
    latency_ms = int((time.monotonic() - started) * 1000)
    return body, latency_ms


def main():
    with open("eval-questions.jsonl") as f:
        cases = [json.loads(line) for line in f if line.strip()]

    rows = []
    for case in cases:
        q, expected = case["question"], case["expect_escalation"]
        try:
            body, latency = ask(q)
            escalated = body.get("escalated", False)
            cited = len(body.get("citations", []))
            ok = "PASS" if escalated == expected else "FAIL"
        except Exception as e:  # noqa: BLE001 - eval harness, report and continue
            latency, escalated, cited, ok = -1, None, 0, f"ERROR: {e}"
        rows.append((q, expected, escalated, cited, latency, ok))
        print(f"[{ok}] ({latency}ms, citations={cited}) {q[:70]}")

    latencies = [r[4] for r in rows if r[4] >= 0]
    passed = sum(1 for r in rows if r[5] == "PASS")
    print("\n--- summary (illustrative, demo setup only) ---")
    print(f"questions: {len(rows)}, behavior as expected: {passed}/{len(rows)}")
    if latencies:
        latencies.sort()
        p95 = latencies[min(len(latencies) - 1, int(len(latencies) * 0.95))]
        print(f"latency: avg={sum(latencies)//len(latencies)}ms p95={p95}ms (target: p95 < 2500ms)")
    esc = sum(1 for r in rows if r[2] is True)
    print(f"escalated to ticket: {esc}/{len(rows)}")


if __name__ == "__main__":
    main()
