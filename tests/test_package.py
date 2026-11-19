# SPDX-FileCopyrightText: Copyright (c) 2012-2026 Yegor Bugayenko
# SPDX-License-Identifier: MIT
"""Check the artifacts after running Maven's clean install lifecycle."""

from pathlib import Path
import unittest


class PackageTest(unittest.TestCase):
    """A POM-only project must not attach JAR artifacts."""

    def test_no_jars(self):
        target = Path(__file__).resolve().parents[1] / "target"
        self.assertEqual(
            sorted(target.rglob("*.jar")),
            [],
            "The POM-only build must not produce JARs for deployment",
        )


if __name__ == "__main__":
    unittest.main()
